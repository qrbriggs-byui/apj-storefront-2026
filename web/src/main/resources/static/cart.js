// cart.js - Loads cart via GET /api/cart and renders table. Handles update qty and remove via forms.

document.addEventListener('DOMContentLoaded', () => {
    const cartStatus = document.getElementById('cartStatus');
    const cartContent = document.getElementById('cartContent');
    const cartEmpty = document.getElementById('cartEmpty');
    const cartTableBody = document.getElementById('cartTableBody');
    const cartTotalEl = document.getElementById('cartTotal');
    const checkoutBtn = document.getElementById('checkoutBtn');

    function formatPrice(val) {
        const num = typeof val === 'string' ? Number(String(val).replace(/[^0-9.-]+/g, '')) : Number(val);
        if (!Number.isFinite(num)) return '$0.00';
        return new Intl.NumberFormat(undefined, { style: 'currency', currency: 'USD', maximumFractionDigits: 2 }).format(num);
    }

    function renderCart(cart) {
        if (!cart || !cart.items || cart.items.length === 0) {
            cartContent.hidden = true;
            cartEmpty.hidden = false;
            if (cartStatus) cartStatus.hidden = true;
            return;
        }

        cartEmpty.hidden = true;
        cartContent.hidden = false;
        if (cartStatus) cartStatus.hidden = true;

        let total = 0;
        cartTableBody.innerHTML = '';
        for (const item of cart.items) {
            const subtotal = item.price * item.quantity;
            total += subtotal;

            const tr = document.createElement('tr');
            tr.setAttribute('data-item-id', item.id);

            const updateForm = document.createElement('form');
            updateForm.action = '/cart/update';
            updateForm.method = 'post';
            updateForm.className = 'cart-item-qty';
            const qtyInput = document.createElement('input');
            qtyInput.type = 'number';
            qtyInput.name = 'quantity';
            qtyInput.min = '1';
            qtyInput.value = item.quantity;
            qtyInput.setAttribute('aria-label', 'Quantity for ' + (item.productName || 'Item'));
            const itemIdInput = document.createElement('input');
            itemIdInput.type = 'hidden';
            itemIdInput.name = 'itemId';
            itemIdInput.value = item.id;
            const updateBtn = document.createElement('button');
            updateBtn.type = 'submit';
            updateBtn.textContent = 'Update';
            updateBtn.className = 'cart-item-update-btn';
            updateForm.appendChild(qtyInput);
            updateForm.appendChild(itemIdInput);
            updateForm.appendChild(updateBtn);

            const removeForm = document.createElement('form');
            removeForm.action = '/cart/remove';
            removeForm.method = 'post';
            removeForm.style.display = 'inline';
            const removeItemId = document.createElement('input');
            removeItemId.type = 'hidden';
            removeItemId.name = 'itemId';
            removeItemId.value = item.id;
            const removeBtn = document.createElement('button');
            removeBtn.type = 'submit';
            removeBtn.textContent = 'Remove';
            removeBtn.className = 'cart-item-remove';
            removeForm.appendChild(removeItemId);
            removeForm.appendChild(removeBtn);

            tr.innerHTML = '<td>' + escapeHtml(item.productName || 'Item') + '</td>' +
                '<td>' + formatPrice(item.price) + '</td>' +
                '<td></td>' +
                '<td>' + formatPrice(subtotal) + '</td>' +
                '<td></td>';
            const qtyCell = tr.querySelectorAll('td')[2];
            const actionCell = tr.querySelectorAll('td')[4];
            qtyCell.appendChild(updateForm);
            actionCell.appendChild(removeForm);

            cartTableBody.appendChild(tr);
        }

        cartTotalEl.textContent = 'Total: ' + formatPrice(total);
        checkoutBtn.hidden = false;
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
                renderCart({ items: [] });
                return;
            }
            const cart = await resp.json();
            renderCart(cart);
        } catch (err) {
            console.error('cart.js error:', err);
            if (cartStatus) {
                cartStatus.textContent = 'Failed to load cart. Please try again.';
                cartStatus.hidden = false;
            }
            cartContent.hidden = true;
            cartEmpty.hidden = true;
        }
    })();
});
