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
    // idle -> ready -> revealed -> ready -> ... -> done
    let phase = 'idle';

    function init(options) {
        onComplete = (options && options.onComplete) || onComplete;

        els.cabinet = document.getElementById('slotCabinet');
        els.turnHeading = document.getElementById('turnHeading');
        els.turnInfo = document.getElementById('turnInfo');
        els.turnMessage = document.getElementById('turnMessage');
        els.progressTracker = document.getElementById('progressTracker');
        els.lever = document.getElementById('slotLever');
        els.letterStrip = document.getElementById('letterReelStrip');
        els.personStrip = document.getElementById('personReelStrip');
        els.actionBtn = document.getElementById('rollActionBtn');
        els.nextBtn = document.getElementById('nextTurnBtn');
        els.ticket = document.getElementById('resultTicket');

        els.actionBtn.addEventListener('click', handleAction);
        els.nextBtn.addEventListener('click', handleNext);

        render();
    }

    // Wywoływane też z zewnątrz (main.js) po każdej zmianie listy uczestników -
    // zawsze wraca do stanu "idle", tak jak State.startGame() resetuje rundę.
    function render() {
        const names = Randomizer.State.getNames();
        phase = 'idle';

        els.nextBtn.hidden = true;
        els.actionBtn.hidden = false;
        els.ticket.classList.remove('visible');
        els.ticket.textContent = '';
        els.cabinet.classList.remove('win-flash');

        if (names.length < 2) {
            els.turnHeading.textContent = 'Załaduj nazwy aby rozpocząć grę';
            els.turnInfo.textContent = '';
            els.turnMessage.textContent = '';
            els.actionBtn.disabled = true;
            els.actionBtn.textContent = '🎰 Rozpocznij Losowanie';
            els.progressTracker.innerHTML = '';
            resetIdleReels();
            return;
        }

        els.turnHeading.textContent = 'Gotowy do rozpoczęcia losowania';
        els.turnInfo.textContent = `${names.length} graczy łącznie`;
        els.turnMessage.textContent = "Kliknij przycisk poniżej, aby rozpocząć losowanie kolejnych tur";
        els.actionBtn.disabled = false;
        els.actionBtn.textContent = '🎰 Rozpocznij Losowanie';
        resetIdleReels();
        renderProgressTracker();
    }

    function resetIdleReels() {
        const names = Randomizer.State.getNames();
        const personSymbols = names.length > 0 ? names : ['?'];
        const repeats = Math.max(8, Math.ceil(MIN_PERSON_ITEMS / personSymbols.length));

        buildReel(els.letterStrip, ALL_LETTERS, LETTER_REPEATS);
        buildReel(els.personStrip, personSymbols, repeats);
        resetReel(els.letterStrip);
        resetReel(els.personStrip);
    }

    function handleAction() {
        if (phase === 'idle') {
            const result = Randomizer.State.beginRound();

            if (!result.ok) {
                Randomizer.Toast.show(result.error, 'error');
                return;
            }

            phase = 'ready';
            prepareCurrentTurn();
            return;
        }

        if (phase === 'ready') {
            spinCurrentTurn();
        }
    }

    function prepareCurrentTurn() {
        const names = Randomizer.State.getNames();
        const turnIndex = Randomizer.State.getCurrentTurnIndex();

        if (turnIndex >= names.length) {
            finishRound();
            return;
        }

        const player = names[turnIndex];

        els.turnHeading.textContent = `${player} - Twoja Kolej!`;
        els.turnInfo.textContent = `Tura ${turnIndex + 1} z ${names.length}`;
        els.turnMessage.textContent = `${player}, kliknij przycisk poniżej, aby zakręcić bębnami i wylosować literę oraz osobę!`;

        els.ticket.classList.remove('visible');
        els.ticket.textContent = '';
        els.cabinet.classList.remove('win-flash');

        els.actionBtn.hidden = false;
        els.actionBtn.disabled = false;
        els.actionBtn.textContent = '🎰 Losuj!';
        els.nextBtn.hidden = true;

        personRepeats = Math.max(8, Math.ceil(MIN_PERSON_ITEMS / names.length));
        buildReel(els.letterStrip, ALL_LETTERS, LETTER_REPEATS);
        buildReel(els.personStrip, names, personRepeats);
        resetReel(els.letterStrip);
        resetReel(els.personStrip);

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

    function spinCurrentTurn() {
        els.actionBtn.disabled = true;
        pullLever();

        const result = Randomizer.State.rollForCurrentPlayer();
        const names = Randomizer.State.getNames();

        const letterSpin = spinReelTo(els.letterStrip, ALL_LETTERS, LETTER_REPEATS, result.letter, LETTER_SPIN_MS);
        const personSpin = spinReelTo(els.personStrip, names, personRepeats, result.target, PERSON_SPIN_MS);

        Promise.all([letterSpin, personSpin]).then(() => {
            showResult(result);
        });
    }

    function showResult(result) {
        els.ticket.textContent = `🎉 ${result.drawer}: litera ${result.letter} → wylosowana osoba: ${result.target || '(brak)'}`;
        els.ticket.classList.add('visible');
        els.cabinet.classList.add('win-flash');

        phase = 'revealed';
        els.actionBtn.hidden = true;
        els.nextBtn.hidden = false;
        els.nextBtn.disabled = false;
    }

    function handleNext() {
        const turnIndex = Randomizer.State.advanceTurn();
        const names = Randomizer.State.getNames();

        if (turnIndex < names.length) {
            phase = 'ready';
            prepareCurrentTurn();
        } else {
            finishRound();
        }
    }

    function finishRound() {
        phase = 'done';
        els.turnHeading.textContent = 'Wszyscy gracze wylosowali!';
        els.turnInfo.textContent = '';
        els.turnMessage.textContent = '';
        els.actionBtn.hidden = true;
        els.nextBtn.hidden = true;
        els.cabinet.classList.remove('win-flash');
        renderProgressTracker();
        onComplete();
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

    return { init, render };
})();
