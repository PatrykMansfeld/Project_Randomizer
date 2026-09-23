(function () {
    'use strict';

    document.addEventListener('DOMContentLoaded', () => {
        Randomizer.Toast.init();
        Randomizer.ResultsUI.init();
        Randomizer.RollingUI.init({
            onComplete: () => Randomizer.ResultsUI.render(),
        });
        Randomizer.RestrictionsUI.init();
        Randomizer.NamesUI.init({
            onChange: () => {
                Randomizer.RestrictionsUI.render();
                Randomizer.RollingUI.render();
                Randomizer.ResultsUI.clear();
            },
        });

        initNavigation();
    });

    function initNavigation() {
        const navButtons = document.querySelectorAll('.nav-btn');
        const sections = document.querySelectorAll('.section');

        navButtons.forEach((btn) => {
            btn.addEventListener('click', () => {
                navButtons.forEach((b) => b.classList.remove('active'));
                sections.forEach((s) => s.classList.remove('active'));
                btn.classList.add('active');
                document.getElementById(btn.dataset.section).classList.add('active');
            });
        });
    }
})();
