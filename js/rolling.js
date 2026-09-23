window.Randomizer = window.Randomizer || {};

Randomizer.RollingUI = (function () {
    'use strict';

    const ALL_LETTERS = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ';
    const SPIN_DURATION_MS = 500;
    const SPIN_INTERVAL_MS = 60;

    let els = {};
    let onComplete = () => {};

    function init(options) {
        onComplete = (options && options.onComplete) || onComplete;

        els.status = document.getElementById('gameStatus');
        els.currentTurn = document.getElementById('currentTurn');
        els.beginBtn = document.getElementById('beginRollingBtn');
        els.progressTracker = document.getElementById('progressTracker');

        els.modal = document.getElementById('rollingModal');
        els.modalPlayerName = document.getElementById('modalPlayerName');
        els.modalTurnInfo = document.getElementById('modalTurnInfo');
        els.modalInstructions = document.getElementById('modalInstructions');
        els.modalRollBtn = document.getElementById('modalRollBtn');
        els.modalResult = document.getElementById('modalResult');
        els.modalNextBtn = document.getElementById('modalNextBtn');

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
        els.modalInstructions.textContent = `${player}, kliknij przycisk poniżej aby wylosować swoją literę i osobę!`;
        els.modalResult.classList.remove('visible');
        els.modalResult.innerHTML = '';
        els.modalRollBtn.disabled = false;
        els.modalRollBtn.textContent = 'Losuj!';
        els.modalNextBtn.disabled = true;

        els.modal.classList.add('open');
        renderProgressTracker();
    }

    function handleRoll() {
        els.modalRollBtn.disabled = true;
        spinLetter(() => {
            const result = Randomizer.State.rollForCurrentPlayer();
            showResult(result.letter, result.target);
            els.modalRollBtn.textContent = 'Zakończ';
            els.modalNextBtn.disabled = false;
        });
    }

    function spinLetter(onDone) {
        els.modalResult.innerHTML = '<div class="result-letter spinning" id="spinLetter">A</div>';
        els.modalResult.classList.add('visible');
        const spinEl = document.getElementById('spinLetter');

        const start = Date.now();
        const timer = setInterval(() => {
            spinEl.textContent = ALL_LETTERS[Math.floor(Math.random() * ALL_LETTERS.length)];
            if (Date.now() - start >= SPIN_DURATION_MS) {
                clearInterval(timer);
                onDone();
            }
        }, SPIN_INTERVAL_MS);
    }

    function showResult(letter, target) {
        els.modalResult.innerHTML = `
            <div class="result-letter pop">${letter}</div>
            <p class="result-letter-desc">Wylosowana litera</p>
            <div class="result-target-box">
                <p class="result-target-title">Wylosowana osoba:</p>
                <p class="result-target-value">${target || '(brak)'}</p>
            </div>
        `;
        els.modalResult.classList.add('visible');
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
