window.Randomizer = window.Randomizer || {};

Randomizer.NamesUI = (function () {
    'use strict';

    const CHIP_COLORS = ['#2ecc71', '#3498db', '#e74c3c', '#ffc107', '#9c27b0', '#ff9800', '#4caf50'];

    let els = {};
    let onChange = () => {};

    function init(options) {
        onChange = (options && options.onChange) || onChange;

        els.nameList = document.getElementById('nameList');
        els.excludedLetters = document.getElementById('excludedLetters');
        els.loadBtn = document.getElementById('loadNamesBtn');
        els.clearBtn = document.getElementById('clearAllBtn');
        els.display = document.getElementById('nameDisplay');

        els.loadBtn.addEventListener('click', handleLoad);
        els.clearBtn.addEventListener('click', handleClear);

        render();
    }

    function handleLoad() {
        const result = Randomizer.State.loadNames(els.nameList.value, els.excludedLetters.value);

        if (!result.ok) {
            Randomizer.Toast.show(result.error, 'error');
            return;
        }

        if (result.excludedLetters.length > 0) {
            Randomizer.Toast.show(
                `Wykluczone litery: ${result.excludedLetters.join(', ')} — nie będą losowane.`,
                'info'
            );
        }

        render();
        onChange();
    }

    function handleClear() {
        Randomizer.State.resetAll();
        els.nameList.value = '';
        els.excludedLetters.value = '';
        render();
        onChange();
        Randomizer.Toast.show('Wyczyszczono wszystkie dane.', 'info');
    }

    function handleRemove(name) {
        Randomizer.State.removeName(name);
        render();
        onChange();
        Randomizer.Toast.show(`Usunięto „${name}” z listy uczestników.`, 'info');
    }

    function render() {
        const names = Randomizer.State.getNames();
        els.display.innerHTML = '';

        if (names.length === 0) {
            const hint = document.createElement('p');
            hint.className = 'hint';
            hint.textContent = 'Brak załadowanych uczestników.';
            els.display.appendChild(hint);
            return;
        }

        names.forEach((name, i) => {
            const chip = document.createElement('span');
            chip.className = 'chip';
            chip.style.background = CHIP_COLORS[i % CHIP_COLORS.length];

            const label = document.createElement('span');
            label.className = 'chip-label';
            label.textContent = name;
            chip.appendChild(label);

            const removeBtn = document.createElement('button');
            removeBtn.type = 'button';
            removeBtn.className = 'chip-remove';
            removeBtn.setAttribute('aria-label', `Usuń ${name}`);
            removeBtn.textContent = '×';
            removeBtn.addEventListener('click', () => handleRemove(name));
            chip.appendChild(removeBtn);

            els.display.appendChild(chip);
        });
    }

    return { init, render };
})();
