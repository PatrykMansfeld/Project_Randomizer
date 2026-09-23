window.Randomizer = window.Randomizer || {};

Randomizer.ResultsUI = (function () {
    'use strict';

    let els = {};

    function init() {
        els.container = document.getElementById('pairResults');
        els.downloadBtn = document.getElementById('downloadResultsBtn');

        els.downloadBtn.addEventListener('click', handleDownload);

        clear();
    }

    function clear() {
        els.container.innerHTML = '<p class="hint">Ukończ losowanie, aby zobaczyć finalne przydziały.</p>';
        els.downloadBtn.disabled = true;
    }

    function render() {
        const finalPairs = Randomizer.State.getFinalPairs();
        els.container.innerHTML = '';

        if (finalPairs.length === 0) {
            els.container.innerHTML = '<p class="error-text">Nie udało się wygenerować przydziałów z obecnymi ograniczeniami.</p>';
            els.downloadBtn.disabled = true;
            return;
        }

        finalPairs.forEach((a) => {
            const card = document.createElement('div');
            card.className = 'assignment-card';
            card.innerHTML = `
                <span class="assignment-names">${a.drawer} → ${a.target}</span>
                <span class="assignment-letter">${a.letter}</span>
            `;
            els.container.appendChild(card);
        });

        els.downloadBtn.disabled = false;
    }

    function handleDownload() {
        const finalPairs = Randomizer.State.getFinalPairs();
        if (finalPairs.length === 0) return;

        const names = Randomizer.State.getNames();
        const restrictions = Randomizer.State.getRestrictions();
        const excludedLetters = Randomizer.State.getExcludedLetters();
        const playerResults = Randomizer.State.getPlayerResults();

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

    return { init, render, clear };
})();
