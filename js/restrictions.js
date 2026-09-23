window.Randomizer = window.Randomizer || {};

Randomizer.RestrictionsUI = (function () {
    'use strict';

    let els = {};

    function init() {
        els.person1 = document.getElementById('person1Select');
        els.person2 = document.getElementById('person2Select');
        els.addBtn = document.getElementById('addRestrictionBtn');
        els.list = document.getElementById('restrictionsList');

        els.addBtn.addEventListener('click', handleAdd);

        render();
    }

    function handleAdd() {
        const person1 = els.person1.value;
        const person2 = els.person2.value;
        const result = Randomizer.State.addRestriction(person1, person2);

        if (!result.ok) {
            Randomizer.Toast.show(result.error, 'error');
            return;
        }

        els.person1.selectedIndex = 0;
        els.person2.selectedIndex = 0;
        renderList();
    }

    function handleRemove(index) {
        Randomizer.State.removeRestriction(index);
        renderList();
    }

    function renderSelects() {
        const names = Randomizer.State.getNames();

        [
            { select: els.person1, placeholder: 'Wybierz Osobę 1' },
            { select: els.person2, placeholder: 'Wybierz Osobę 2' },
        ].forEach(({ select, placeholder }) => {
            select.innerHTML = '';
            select.add(new Option(placeholder, ''));
            names.forEach((name) => select.add(new Option(name, name)));
        });
    }

    function renderList() {
        const restrictions = Randomizer.State.getRestrictions();
        els.list.innerHTML = '';

        if (restrictions.length === 0) {
            const li = document.createElement('li');
            li.className = 'empty-hint';
            li.textContent = 'Brak ograniczeń.';
            els.list.appendChild(li);
            return;
        }

        restrictions.forEach((r, index) => {
            const li = document.createElement('li');
            li.textContent = `${r.person1} ↔ ${r.person2}`;
            li.title = 'Kliknij, aby usunąć';
            li.addEventListener('click', () => handleRemove(index));
            els.list.appendChild(li);
        });
    }

    function render() {
        renderSelects();
        renderList();
    }

    return { init, render };
})();
