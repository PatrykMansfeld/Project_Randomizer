(function () {
    'use strict';

    // === STAN APLIKACJI ===
    let names = [];
    let excludedLetters = new Set();
    let restrictions = []; // {person1, person2}
    let currentTurnIndex = 0;
    let playerResults = []; // {name, letter}
    let finalPairs = []; // {drawer, target, letter}
    let availableTargetsPool = [];

    const CHIP_COLORS = ['#2ecc71', '#3498db', '#e74c3c', '#ffc107', '#9c27b0', '#ff9800', '#4caf50'];

    // === ELEMENTY DOM ===
    const nameListArea = document.getElementById('nameList');
    const excludedLettersArea = document.getElementById('excludedLetters');
    const loadNamesBtn = document.getElementById('loadNamesBtn');
    const nameDisplay = document.getElementById('nameDisplay');

    const person1Select = document.getElementById('person1Select');
    const person2Select = document.getElementById('person2Select');
    const addRestrictionBtn = document.getElementById('addRestrictionBtn');
    const restrictionsList = document.getElementById('restrictionsList');

    const gameStatus = document.getElementById('gameStatus');
    const currentTurn = document.getElementById('currentTurn');
    const beginRollingBtn = document.getElementById('beginRollingBtn');

    const pairResults = document.getElementById('pairResults');
    const downloadResultsBtn = document.getElementById('downloadResultsBtn');

    const navButtons = document.querySelectorAll('.nav-btn');
    const sections = document.querySelectorAll('.section');

    const rollingModal = document.getElementById('rollingModal');
    const modalPlayerName = document.getElementById('modalPlayerName');
    const modalTurnInfo = document.getElementById('modalTurnInfo');
    const modalInstructions = document.getElementById('modalInstructions');
    const modalRollBtn = document.getElementById('modalRollBtn');
    const modalResult = document.getElementById('modalResult');
    const modalNextBtn = document.getElementById('modalNextBtn');

    // === NAWIGACJA ===
    navButtons.forEach((btn) => {
        btn.addEventListener('click', () => {
            navButtons.forEach((b) => b.classList.remove('active'));
            sections.forEach((s) => s.classList.remove('active'));
            btn.classList.add('active');
            document.getElementById(btn.dataset.section).classList.add('active');
        });
    });

    // === ŁADOWANIE NAZW ===
    loadNamesBtn.addEventListener('click', loadNames);

    function loadNames() {
        const text = nameListArea.value.trim();
        if (!text) {
            alert('Proszę wprowadzić przynajmniej 2 nazwy.');
            return;
        }

        names = text
            .split(/[,\n]/)
            .map((n) => n.trim())
            .filter((n) => n.length > 0);

        if (names.length < 2) {
            alert('Proszę wprowadzić przynajmniej 2 nazwy.');
            return;
        }

        excludedLetters = new Set();
        const excludedText = excludedLettersArea.value.trim().toUpperCase().replace(/[^A-ZĄĆĘŁŃÓŚŹŻ]/g, '');
        for (const ch of excludedText) {
            excludedLetters.add(ch);
        }

        if (excludedLetters.size > 0) {
            alert('Wykluczone litery: ' + Array.from(excludedLetters).join(', ') + '\nTe litery nie będą losowane.');
        }

        restrictions = [];
        updateNameDisplay();
        updateSelectOptions();
        updateRestrictionsDisplay();
        startGame();
    }

    function updateNameDisplay() {
        nameDisplay.innerHTML = '';
        names.forEach((name, i) => {
            const chip = document.createElement('span');
            chip.className = 'chip';
            chip.textContent = name;
            chip.style.background = CHIP_COLORS[i % CHIP_COLORS.length];
            nameDisplay.appendChild(chip);
        });
    }

    // === OGRANICZENIA ===
    function updateSelectOptions() {
        person1Select.innerHTML = '';
        person2Select.innerHTML = '';

        const opt1 = new Option('Wybierz Osobę 1', '');
        const opt2 = new Option('Wybierz Osobę 2', '');
        person1Select.add(opt1);
        person2Select.add(opt2);

        names.forEach((name) => {
            person1Select.add(new Option(name, name));
            person2Select.add(new Option(name, name));
        });
    }

    addRestrictionBtn.addEventListener('click', addRestriction);

    function addRestriction() {
        const person1 = person1Select.value;
        const person2 = person2Select.value;

        if (!person1 || !person2) {
            alert('Proszę wybrać obie osoby dla ograniczenia.');
            return;
        }

        if (person1 === person2) {
            alert('Osoba nie może być ograniczona sama ze sobą.');
            return;
        }

        const exists = restrictions.some(
            (r) => (r.person1 === person1 && r.person2 === person2) || (r.person1 === person2 && r.person2 === person1)
        );

        if (exists) {
            alert('To ograniczenie już istnieje.');
            return;
        }

        restrictions.push({ person1, person2 });
        updateRestrictionsDisplay();

        person1Select.selectedIndex = 0;
        person2Select.selectedIndex = 0;
    }

    function updateRestrictionsDisplay() {
        restrictionsList.innerHTML = '';

        if (restrictions.length === 0) {
            const li = document.createElement('li');
            li.className = 'empty-hint';
            li.textContent = 'Brak ograniczeń.';
            restrictionsList.appendChild(li);
            return;
        }

        restrictions.forEach((r, index) => {
            const li = document.createElement('li');
            li.textContent = `${r.person1} ↔ ${r.person2}`;
            li.title = 'Kliknij, aby usunąć';
            li.addEventListener('click', () => {
                restrictions.splice(index, 1);
                updateRestrictionsDisplay();
            });
            restrictionsList.appendChild(li);
        });
    }

    function isRestrictedPair(name1, name2) {
        return restrictions.some(
            (r) => (r.person1 === name1 && r.person2 === name2) || (r.person1 === name2 && r.person2 === name1)
        );
    }

    // === GRA / LOSOWANIE ===
    function startGame() {
        if (names.length < 2) return;

        currentTurnIndex = 0;
        playerResults = [];
        finalPairs = [];
        availableTargetsPool = [...names];

        beginRollingBtn.disabled = false;
        downloadResultsBtn.disabled = true;

        pairResults.innerHTML = '<p class="hint">Ukończ losowanie, aby zobaczyć finalne przydziały.</p>';

        updateGameStatus();
    }

    function updateGameStatus() {
        if (currentTurnIndex < names.length) {
            gameStatus.textContent = `Gotowy do rozpoczęcia losowania - ${names.length} graczy łącznie`;
            currentTurn.textContent = "Kliknij 'Rozpocznij Losowanie' aby rozpocząć losowanie kolejnych tur";
        } else {
            gameStatus.textContent = 'Wszyscy gracze wylosowali!';
            currentTurn.textContent = '';
            displayAssignments();
            downloadResultsBtn.disabled = false;
        }
    }

    beginRollingBtn.addEventListener('click', startRolling);

    function startRolling() {
        beginRollingBtn.disabled = true;
        showRollingModal();
    }

    function showRollingModal() {
        if (currentTurnIndex >= names.length) {
            updateGameStatus();
            return;
        }

        const currentPlayer = names[currentTurnIndex];

        modalPlayerName.textContent = `${currentPlayer} - Twoja Kolej!`;
        modalTurnInfo.textContent = `Tura ${currentTurnIndex + 1} z ${names.length}`;
        modalInstructions.textContent = `${currentPlayer}, kliknij przycisk poniżej aby wylosować swoją literę i osobę!`;
        modalResult.classList.remove('visible');
        modalResult.innerHTML = '';
        modalRollBtn.disabled = false;
        modalRollBtn.textContent = 'Losuj!';
        modalNextBtn.disabled = true;

        rollingModal.classList.add('open');
    }

    function getRandomLetter() {
        const allLetters = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ';
        const usedLetters = new Set(playerResults.map((r) => r.letter));
        excludedLetters.forEach((l) => usedLetters.add(l));

        const availableLetters = [...allLetters].filter((c) => !usedLetters.has(c));

        if (availableLetters.length === 0) {
            const nonExcluded = [...allLetters].filter((c) => !excludedLetters.has(c));
            if (nonExcluded.length > 0) {
                return nonExcluded[Math.floor(Math.random() * nonExcluded.length)];
            }
            return allLetters[Math.floor(Math.random() * allLetters.length)];
        }

        return availableLetters[Math.floor(Math.random() * availableLetters.length)];
    }

    function shuffle(arr) {
        for (let i = arr.length - 1; i > 0; i--) {
            const j = Math.floor(Math.random() * (i + 1));
            [arr[i], arr[j]] = [arr[j], arr[i]];
        }
        return arr;
    }

    function assignTargetForOnRoll(drawer, letter) {
        if (availableTargetsPool.length === 0) {
            availableTargetsPool = [...names];
            finalPairs.forEach((a) => {
                const idx = availableTargetsPool.indexOf(a.target);
                if (idx !== -1) availableTargetsPool.splice(idx, 1);
            });
        }

        let possibleTargets = availableTargetsPool.filter((t) => t !== drawer && !isRestrictedPair(drawer, t));
        shuffle(possibleTargets);
        let assignedTarget = possibleTargets.length > 0 ? possibleTargets[0] : null;

        if (assignedTarget === null && availableTargetsPool.length === 1 && availableTargetsPool[0] === drawer) {
            for (const prev of finalPairs) {
                const candidate = prev.target;
                const prevDrawer = prev.drawer;
                if (candidate === drawer) continue;
                if (!isRestrictedPair(drawer, candidate) && !isRestrictedPair(prevDrawer, drawer) && prevDrawer !== drawer) {
                    prev.target = drawer;
                    assignedTarget = candidate;
                    const idx = availableTargetsPool.indexOf(drawer);
                    if (idx !== -1) availableTargetsPool.splice(idx, 1);
                    finalPairs.push({ drawer, target: assignedTarget, letter });
                    return assignedTarget;
                }
            }
        }

        if (assignedTarget === null) {
            assignedTarget = availableTargetsPool.find((t) => t !== drawer) || drawer;
        }

        const idx = availableTargetsPool.indexOf(assignedTarget);
        if (idx !== -1) availableTargetsPool.splice(idx, 1);
        finalPairs.push({ drawer, target: assignedTarget, letter });
        return assignedTarget;
    }

    modalRollBtn.addEventListener('click', () => {
        const currentPlayer = names[currentTurnIndex];
        const letter = getRandomLetter();
        const target = assignTargetForOnRoll(currentPlayer, letter);

        playerResults.push({ name: currentPlayer, letter });

        showModalResult(letter, target);

        modalRollBtn.disabled = true;
        modalRollBtn.textContent = 'Zakończ';
        modalNextBtn.disabled = false;
    });

    function showModalResult(letter, target) {
        modalResult.innerHTML = `
            <div class="result-letter">${letter}</div>
            <p class="result-letter-desc">Wylosowana litera</p>
            <div class="result-target-box">
                <p class="result-target-title">Wylosowana osoba:</p>
                <p class="result-target-value">${target || '(brak)'}</p>
            </div>
        `;
        modalResult.classList.add('visible');
    }

    modalNextBtn.addEventListener('click', () => {
        rollingModal.classList.remove('open');
        currentTurnIndex++;

        if (currentTurnIndex < names.length) {
            showRollingModal();
        } else {
            updateGameStatus();
        }
    });

    // === WYNIKI ===
    function displayAssignments() {
        pairResults.innerHTML = '';

        if (finalPairs.length === 0) {
            pairResults.innerHTML = '<p class="error-text">Nie udało się wygenerować przydziałów z obecnymi ograniczeniami.</p>';
            return;
        }

        finalPairs.forEach((a) => {
            const card = document.createElement('div');
            card.className = 'assignment-card';
            card.innerHTML = `
                <span class="assignment-names">${a.drawer} → ${a.target}</span>
                <span class="assignment-letter">${a.letter}</span>
            `;
            pairResults.appendChild(card);
        });
    }

    downloadResultsBtn.addEventListener('click', downloadResults);

    function downloadResults() {
        if (finalPairs.length === 0) return;

        const now = new Date();
        const pad = (n) => String(n).padStart(2, '0');
        const timestamp = `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}_${pad(now.getHours())}-${pad(now.getMinutes())}`;
        const dateStr = `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())} ${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`;

        let out = '';
        out += '============================================================\n';
        out += '                    RANDOMIZER - WYNIKI\n';
        out += '============================================================\n\n';

        out += `Data wygenerowania: ${dateStr}\n`;
        out += `Liczba uczestników: ${names.length}\n`;
        out += `Liczba przydziałów: ${finalPairs.length}\n`;

        if (excludedLetters.size > 0) {
            out += `Wykluczone litery: ${Array.from(excludedLetters).join(', ')}\n`;
        }

        if (restrictions.length > 0) {
            out += `Ograniczenia par: ${restrictions.length}\n`;
            restrictions.forEach((r) => {
                out += `   - ${r.person1} <-> ${r.person2}\n`;
            });
        }

        out += '\n' + '='.repeat(60) + '\n';
        out += '                       FINALNE PRZYDZIAŁY\n';
        out += '='.repeat(60) + '\n\n';

        out += 'WYNIKI LOSOWANIA LITER:\n';
        out += '-'.repeat(30) + '\n';
        playerResults.forEach((r) => {
            out += `${r.name.padEnd(20)} -> ${r.letter}\n`;
        });

        out += '\nFINALNE PRZYDZIAŁY (kto kogo wylosował):\n';
        out += '-'.repeat(50) + '\n';
        finalPairs.forEach((a, i) => {
            out += `${i + 1}. ${a.drawer.padEnd(15)} -> ${a.target.padEnd(15)} [Litera: ${a.letter}]\n`;
        });

        out += '\n' + '='.repeat(60) + '\n';
        out += '                        PODSUMOWANIE\n';
        out += '='.repeat(60) + '\n';
        out += 'Wszyscy uczestnicy mają swoje przydziały\n';
        out += 'Nikt nie wylosował samego siebie\n';
        if (restrictions.length > 0) {
            out += 'Wszystkie ograniczenia zostały uwzględnione\n';
        }
        if (excludedLetters.size > 0) {
            out += 'Wykluczone litery nie zostały wylosowane\n';
        }

        out += '\nPlik wygenerowany przez Randomizer (web)\n';
        out += `${dateStr}\n`;

        const blob = new Blob([out], { type: 'text/plain;charset=utf-8' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `randomizer-wyniki-${timestamp}.txt`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);
    }

    updateRestrictionsDisplay();
})();
