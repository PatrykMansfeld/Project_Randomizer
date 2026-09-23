window.Randomizer = window.Randomizer || {};

Randomizer.State = (function () {
    'use strict';

    const ALL_LETTERS = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ';

    let names = [];
    let excludedLetters = new Set();
    let restrictions = []; // {person1, person2}
    let currentTurnIndex = 0;
    let playerResults = []; // {name, letter}
    let finalPairs = []; // {drawer, target, letter}
    let availableTargetsPool = [];

    function parseNames(text) {
        return text
            .split(/[,\n]/)
            .map((n) => n.trim())
            .filter((n) => n.length > 0);
    }

    function parseExcludedLetters(text) {
        const cleaned = text.trim().toUpperCase().replace(/[^A-ZĄĆĘŁŃÓŚŹŻ]/g, '');
        return new Set([...cleaned]);
    }

    function loadNames(rawNames, rawExcluded) {
        const parsedNames = parseNames(rawNames);
        if (parsedNames.length < 2) {
            return { ok: false, error: 'Proszę wprowadzić przynajmniej 2 nazwy.' };
        }

        names = parsedNames;
        excludedLetters = parseExcludedLetters(rawExcluded);
        restrictions = [];
        startGame();

        return { ok: true, excludedLetters: Array.from(excludedLetters) };
    }

    function removeName(name) {
        const idx = names.indexOf(name);
        if (idx === -1) return;

        names.splice(idx, 1);
        restrictions = restrictions.filter((r) => r.person1 !== name && r.person2 !== name);
        startGame();
    }

    function resetAll() {
        names = [];
        excludedLetters = new Set();
        restrictions = [];
        currentTurnIndex = 0;
        playerResults = [];
        finalPairs = [];
        availableTargetsPool = [];
    }

    function addRestriction(person1, person2) {
        if (!person1 || !person2) {
            return { ok: false, error: 'Proszę wybrać obie osoby dla ograniczenia.' };
        }

        if (person1 === person2) {
            return { ok: false, error: 'Osoba nie może być ograniczona sama ze sobą.' };
        }

        if (isRestrictedPair(person1, person2)) {
            return { ok: false, error: 'To ograniczenie już istnieje.' };
        }

        restrictions.push({ person1, person2 });
        return { ok: true };
    }

    function removeRestriction(index) {
        restrictions.splice(index, 1);
    }

    function isRestrictedPair(name1, name2) {
        return restrictions.some(
            (r) => (r.person1 === name1 && r.person2 === name2) || (r.person1 === name2 && r.person2 === name1)
        );
    }

    function startGame() {
        currentTurnIndex = 0;
        playerResults = [];
        finalPairs = [];
        availableTargetsPool = [...names];
    }

    function getRandomLetter() {
        const usedLetters = new Set(playerResults.map((r) => r.letter));
        excludedLetters.forEach((l) => usedLetters.add(l));

        const availableLetters = [...ALL_LETTERS].filter((c) => !usedLetters.has(c));

        if (availableLetters.length === 0) {
            const nonExcluded = [...ALL_LETTERS].filter((c) => !excludedLetters.has(c));
            if (nonExcluded.length > 0) {
                return nonExcluded[Math.floor(Math.random() * nonExcluded.length)];
            }
            return ALL_LETTERS[Math.floor(Math.random() * ALL_LETTERS.length)];
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

    function rollForCurrentPlayer() {
        const drawer = names[currentTurnIndex];
        const letter = getRandomLetter();
        const target = assignTargetForOnRoll(drawer, letter);
        playerResults.push({ name: drawer, letter });
        return { drawer, letter, target };
    }

    function advanceTurn() {
        currentTurnIndex++;
        return currentTurnIndex;
    }

    function isRollingComplete() {
        return currentTurnIndex >= names.length;
    }

    function getNames() {
        return [...names];
    }

    function getRestrictions() {
        return restrictions.map((r) => ({ ...r }));
    }

    function getExcludedLetters() {
        return new Set(excludedLetters);
    }

    function getCurrentTurnIndex() {
        return currentTurnIndex;
    }

    function getPlayerResults() {
        return playerResults.map((r) => ({ ...r }));
    }

    function getFinalPairs() {
        return finalPairs.map((a) => ({ ...a }));
    }

    return {
        loadNames,
        removeName,
        resetAll,
        addRestriction,
        removeRestriction,
        isRestrictedPair,
        startGame,
        getRandomLetter,
        assignTargetForOnRoll,
        rollForCurrentPlayer,
        advanceTurn,
        isRollingComplete,
        getNames,
        getRestrictions,
        getExcludedLetters,
        getCurrentTurnIndex,
        getPlayerResults,
        getFinalPairs,
    };
})();
