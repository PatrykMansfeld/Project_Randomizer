window.Randomizer = window.Randomizer || {};

Randomizer.RollingUI = (function () {
    'use strict';

    const ALL_LETTERS = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'.split('');
    const REEL_ITEM_HEIGHT = 64;
    const LETTER_REPEATS = 6;
    const MIN_PERSON_ITEMS = 40;
    const LETTER_SPIN_MS = 1000;
    const PERSON_SPIN_MS = 1500;

    let els = {};
    let onComplete = () => {};
    let personRepeats = 8;

    function init(options) {
        onComplete = (options && options.onComplete) || onComplete;

        els.status = document.getElementById('gameStatus');
        els.currentTurn = document.getElementById('currentTurn');
        els.beginBtn = document.getElementById('beginRollingBtn');
        els.progressTracker = document.getElementById('progressTracker');

        els.modal = document.getElementById('rollingModal');
        els.cabinet = document.getElementById('slotCabinet');
        els.lever = document.getElementById('slotLever');
        els.modalPlayerName = document.getElementById('modalPlayerName');
        els.modalTurnInfo = document.getElementById('modalTurnInfo');
        els.modalInstructions = document.getElementById('modalInstructions');
        els.modalRollBtn = document.getElementById('modalRollBtn');
        els.modalResult = document.getElementById('modalResult');
        els.modalNextBtn = document.getElementById('modalNextBtn');
        els.letterStrip = document.getElementById('letterReelStrip');
        els.personStrip = document.getElementById('personReelStrip');

        els.beginBtn.addEventListener('click', handleBegin);
        els.modalRollBtn.addEventListener('click', handleRoll);
        els.modalNextBtn.addEventListener('click', handleNext);

        render();
    }

    function handleBegin() {
        const result = Randomizer.State.beginRound();

        if (!result.ok) {
            Randomizer.Toast.show(result.error, 'error');
            return;
        }

        els.beginBtn.disabled = true;
        showModalForCurrentTurn();
    }

    function showModalForCurrentTurn() {
        const names = Randomizer.State.getNames();
        const turnIndex = Randomizer.State.getCurrentTurnIndex();

        if (turnIndex >= names.length) {
            render();
            return;
        }

        const player = names[turnIndex];

        els.modalPlayerName.textContent = `${player} - Twoja Kolej!`;
        els.modalTurnInfo.textContent = `Tura ${turnIndex + 1} z ${names.length}`;
        els.modalInstructions.textContent = `${player}, kliknij przycisk poniżej, aby zakręcić bębnami i wylosować literę oraz osobę!`;
        els.modalResult.classList.remove('visible');
        els.modalResult.textContent = '';
        els.cabinet.classList.remove('win-flash');
        els.modalRollBtn.disabled = false;
        els.modalRollBtn.textContent = '🎰 Losuj!';
        els.modalNextBtn.disabled = true;

        personRepeats = Math.max(8, Math.ceil(MIN_PERSON_ITEMS / names.length));
        buildReel(els.letterStrip, ALL_LETTERS, LETTER_REPEATS);
        buildReel(els.personStrip, names, personRepeats);
        resetReel(els.letterStrip);
        resetReel(els.personStrip);

        els.modal.classList.add('open');
        renderProgressTracker();
    }

    function buildReel(stripEl, symbols, repeatCount) {
        stripEl.innerHTML = '';
        for (let r = 0; r < repeatCount; r++) {
            symbols.forEach((symbol) => {
                const item = document.createElement('div');
                item.className = 'reel-item';
                item.textContent = symbol;
                stripEl.appendChild(item);
            });
        }
    }

    function resetReel(stripEl) {
        stripEl.style.transition = 'none';
        stripEl.style.transform = 'translateY(0px)';
    }

    // Liczy przesunięcie tak, by `target` wylądował dokładnie w podświetlonym
    // środkowym wierszu okna, w jednej z ostatnich powtórek paska (żeby bęben
    // miał kawałek drogi do przewinięcia zanim się zatrzyma).
    function spinReelTo(stripEl, symbols, repeatCount, target, durationMs) {
        return new Promise((resolve) => {
            const cycleLen = symbols.length;
            const indexInCycle = symbols.indexOf(target);
            const landingCycle = Math.max(repeatCount - 2, 0);
            const landingIndex = landingCycle * cycleLen + indexInCycle;
            const offset = (landingIndex - 1) * REEL_ITEM_HEIGHT;

            stripEl.style.transition = `transform ${durationMs}ms cubic-bezier(0.15, 0.85, 0.32, 1)`;
            // wymuszenie reflow, żeby transition na pewno wystartował z aktualnej pozycji
            void stripEl.offsetHeight;
            stripEl.style.transform = `translateY(${-offset}px)`;

            const onEnd = (event) => {
                if (event.propertyName !== 'transform') return;
                stripEl.removeEventListener('transitionend', onEnd);
                resolve();
            };
            stripEl.addEventListener('transitionend', onEnd);
        });
    }

    function pullLever() {
        els.lever.classList.remove('pulled');
        void els.lever.offsetWidth;
        els.lever.classList.add('pulled');
    }

    function handleRoll() {
        els.modalRollBtn.disabled = true;
        pullLever();

        const result = Randomizer.State.rollForCurrentPlayer();
        const names = Randomizer.State.getNames();

        const letterSpin = spinReelTo(els.letterStrip, ALL_LETTERS, LETTER_REPEATS, result.letter, LETTER_SPIN_MS);
        const personSpin = spinReelTo(els.personStrip, names, personRepeats, result.target, PERSON_SPIN_MS);

        Promise.all([letterSpin, personSpin]).then(() => {
            showResult(result);
            els.modalRollBtn.textContent = 'Zakończ';
            els.modalNextBtn.disabled = false;
        });
    }

    function showResult(result) {
        els.modalResult.textContent = `🎉 ${result.drawer}: litera ${result.letter} → wylosowana osoba: ${result.target || '(brak)'}`;
        els.modalResult.classList.add('visible');
        els.cabinet.classList.add('win-flash');
    }

    function handleNext() {
        els.modal.classList.remove('open');
        const turnIndex = Randomizer.State.advanceTurn();
        const names = Randomizer.State.getNames();

        if (turnIndex < names.length) {
            showModalForCurrentTurn();
        } else {
            render();
            onComplete();
        }
    }

    function renderProgressTracker() {
        const names = Randomizer.State.getNames();
        const turnIndex = Randomizer.State.getCurrentTurnIndex();
        els.progressTracker.innerHTML = '';

        if (names.length === 0) return;

        names.forEach((name, i) => {
            const item = document.createElement('div');
            let status = 'waiting';
            if (i < turnIndex) status = 'done';
            else if (i === turnIndex) status = 'current';
            item.className = `progress-item progress-${status}`;
            item.textContent = name;
            els.progressTracker.appendChild(item);
        });
    }

    function render() {
        const names = Randomizer.State.getNames();
        const turnIndex = Randomizer.State.getCurrentTurnIndex();

        if (names.length < 2) {
            els.status.textContent = 'Załaduj nazwy aby rozpocząć grę';
            els.currentTurn.textContent = '';
            els.beginBtn.disabled = true;
            els.progressTracker.innerHTML = '';
            return;
        }

        if (turnIndex < names.length) {
            els.status.textContent = `Gotowy do rozpoczęcia losowania - ${names.length} graczy łącznie`;
            els.currentTurn.textContent = "Kliknij 'Rozpocznij Losowanie' aby rozpocząć losowanie kolejnych tur";
            els.beginBtn.disabled = false;
        } else {
            els.status.textContent = 'Wszyscy gracze wylosowali!';
            els.currentTurn.textContent = '';
            els.beginBtn.disabled = true;
        }

        renderProgressTracker();
    }

    return { init, render };
})();
