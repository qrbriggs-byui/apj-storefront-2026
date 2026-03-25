/**
 * Shows the header Cart link only when GET /api/me/session reports authenticated.
 * Include after the closing </header> on pages that use the shared site header.
 */
(function () {
    function setCartVisible(navCart, show) {
        if (!navCart) return;
        if (show) {
            navCart.removeAttribute('hidden');
            navCart.removeAttribute('aria-hidden');
        } else {
            navCart.setAttribute('hidden', '');
            navCart.setAttribute('aria-hidden', 'true');
        }
    }

    async function init() {
        var navCart = document.querySelector('.nav-cart');
        if (!navCart) return;
        try {
            var resp = await fetch('/api/me/session', {
                credentials: 'same-origin',
                headers: { Accept: 'application/json' }
            });
            if (!resp.ok) return;
            var data = await resp.json();
            setCartVisible(navCart, data && data.authenticated === true);
        } catch (e) {
            console.warn('header-auth.js:', e);
        }
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
