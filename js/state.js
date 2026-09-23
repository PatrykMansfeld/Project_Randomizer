window.Randomizer = window.Randomizer || {};

Randomizer.State = (function () {
    'use strict';

    const ALL_LETTERS = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ';
    const RANDOM_ATTEMPTS = 200;
    const MAX_BACKTRACK_STEPS = 200000;

    let names = [];
    let excludedLetters = new Set();
    let restrictions = []; // {person1, person2}
    let currentTurnIndex = 0;
    let playerResults = []; // {name, letter}
    let finalPairs = []; // {drawer, target, letter}
    let roundAssignments = null; // Map drawer -> target, precomputed for the whole round
    let roundLetters = null; // Map drawer -> letter, precomputed for the whole round

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
        startGame();
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
        roundAssignments = null;
        roundLetters = null;
    }

    function shuffle(arr) {
        const copy = [...arr];
        for (let i = copy.length - 1; i > 0; i--) {
            const j = Math.floor(Math.random() * (i + 1));
            [copy[i], copy[j]] = [copy[j], copy[i]];
        }
        return copy;
    }

    // Szybka ścieżka: kilkaset losowych tasowań i sprawdzenie warunków —
    // w praktyce wystarcza dla typowej liczby ograniczeń.
    function tryRandomAssignments() {
        for (let attempt = 0; attempt < RANDOM_ATTEMPTS; attempt++) {
            const targets = shuffle(names);
            const valid = names.every((drawer, i) => drawer !== targets[i] && !isRestrictedPair(drawer, targets[i]));
            if (valid) {
                return names.map((drawer, i) => ({ drawer, target: targets[i] }));
            }
        }
        return null;
    }

    // Awaryjna ścieżka: losowo uporządkowany backtracking, który gwarantuje
    // znalezienie poprawnego przydziału, jeśli taki w ogóle istnieje.
    function backtrackAssignments() {
        const usedTargets = new Set();
        const result = new Array(names.length);
        let steps = 0;

        function solve(i) {
            steps++;
            if (steps > MAX_BACKTRACK_STEPS) return false;
            if (i === names.length) return true;

            const drawer = names[i];
            const candidates = shuffle(names.filter((t) => !usedTargets.has(t)));

            for (const target of candidates) {
                if (target !== drawer && !isRestrictedPair(drawer, target)) {
                    usedTargets.add(target);
                    result[i] = { drawer, target };
                    if (solve(i + 1)) return true;
                    usedTargets.delete(target);
                    result[i] = null;
                }
            }
            return false;
        }

        return solve(0) ? result : null;
    }

    function generateAssignments() {
        if (names.length < 2) return null;
        return tryRandomAssignments() || backtrackAssignments();
    }

    function generateLetters() {
        const availableLetters = shuffle([...ALL_LETTERS].filter((c) => !excludedLetters.has(c)));
        const fallbackPool = availableLetters.length > 0 ? availableLetters : [...ALL_LETTERS];

        return names.map((_, i) => {
            if (i < availableLetters.length) {
                return availableLetters[i];
            }
            return fallbackPool[Math.floor(Math.random() * fallbackPool.length)];
        });
    }

    // Liczy cały przydział (pary + litery) dla rundy na raz — losowanie
    // turowe w UI tylko odsłania gotowy wynik po kolei.
    function beginRound() {
        const assignments = generateAssignments();
        if (!assignments) {
            return {
                ok: false,
                error: 'Nie udało się znaleźć przydziału spełniającego wszystkie ograniczenia. Usuń część ograniczeń lub dodaj więcej uczestników.',
            };
        }

        const letters = generateLetters();

        currentTurnIndex = 0;
        playerResults = [];
        finalPairs = [];
        roundAssignments = new Map(assignments.map((a) => [a.drawer, a.target]));
        roundLetters = new Map(names.map((name, i) => [name, letters[i]]));

        return { ok: true };
    }

    function rollForCurrentPlayer() {
        const drawer = names[currentTurnIndex];
        const letter = roundLetters.get(drawer);
        const target = roundAssignments.get(drawer);

        playerResults.push({ name: drawer, letter });
        finalPairs.push({ drawer, target, letter });

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
        beginRound,
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
