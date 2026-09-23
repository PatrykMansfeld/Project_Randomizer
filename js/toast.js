window.Randomizer = window.Randomizer || {};

Randomizer.Toast = (function () {
    'use strict';

    const DISPLAY_MS = 3500;
    const FADE_MS = 300;

    let container = null;

    function init() {
        container = document.getElementById('toastContainer');
    }

    function show(message, type) {
        if (!container) return;

        const toast = document.createElement('div');
        toast.className = `toast toast-${type || 'info'}`;
        toast.textContent = message;
        container.appendChild(toast);

        requestAnimationFrame(() => toast.classList.add('visible'));

        setTimeout(() => {
            toast.classList.remove('visible');
            setTimeout(() => toast.remove(), FADE_MS);
        }, DISPLAY_MS);
    }

    return { init, show };
})();
