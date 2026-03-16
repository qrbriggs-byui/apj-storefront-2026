// checkout.js - Loads cart via GET /api/cart, shows summary and total. "Place Order" POSTs to /checkout.

document.addEventListener('DOMContentLoaded', () => {
    const statusEl = document.getElementById('checkoutStatus');
    const contentEl = document.getElementById('checkoutContent');
    const emptyEl = document.getElementById('checkoutEmpty');
    const tableBody = document.getElementById('checkoutTableBody');
    const totalEl = document.getElementById('checkoutTotal');
    const placeOrderForm = document.getElementById('placeOrderForm');
    const placeOrderBtn = document.getElementById('placeOrderBtn');

    function formatPrice(val) {
        const num = typeof val === 'string' ? Number(String(val).replace(/[^0-9.-]+/g, '')) : Number(val);
        if (!Number.isFinite(num)) return '$0.00';
        return new Intl.NumberFormat(undefined, { style: 'currency', currency: 'USD', maximumFractionDigits: 2 }).format(num);
    }

    function escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    (async function loadCart() {
        try {
            const resp = await fetch('/api/cart', { headers: { 'Accept': 'application/json' } });
            if (resp.status === 404 || !resp.ok) {
                contentEl.hidden = true;
                emptyEl.hidden = false;
                statusEl.hidden = true;
                return;
            }
            const cart = await resp.json();
            if (!cart.items || cart.items.length === 0) {
                contentEl.hidden = true;
                emptyEl.hidden = false;
                statusEl.hidden = true;
                return;
            }
            let total = 0;
            tableBody.innerHTML = '';
            for (const item of cart.items) {
                const subtotal = item.price * item.quantity;
                total += subtotal;
                const tr = document.createElement('tr');
                tr.innerHTML = '<td>' + escapeHtml(item.productName || 'Item') + '</td>' +
                    '<td>' + formatPrice(item.price) + '</td>' +
                    '<td>' + item.quantity + '</td>' +
                    '<td>' + formatPrice(subtotal) + '</td>';
                tableBody.appendChild(tr);
            }
            totalEl.textContent = 'Total: ' + formatPrice(total);
            statusEl.hidden = true;
            contentEl.hidden = false;
            emptyEl.hidden = true;
        } catch (err) {
            console.error('checkout.js error:', err);
            statusEl.textContent = 'Failed to load cart. Please try again.';
            contentEl.hidden = true;
            emptyEl.hidden = true;
        }
    })();
});
