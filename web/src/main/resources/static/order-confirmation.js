// order-confirmation.js - Polls GET /order-status/{orderId} every 2s until status is COMPLETED.

document.addEventListener('DOMContentLoaded', () => {
    const params = new URLSearchParams(window.location.search);
    const orderId = params.get('orderId');

    const statusEl = document.getElementById('confirmStatus');
    const contentEl = document.getElementById('confirmContent');
    const errorEl = document.getElementById('confirmError');
    const orderIdLine = document.getElementById('orderIdLine');
    const orderStatusLine = document.getElementById('orderStatusLine');
    const loadingIndicator = document.getElementById('loadingIndicator');
    const orderComplete = document.getElementById('orderComplete');

    if (!orderId) {
        statusEl.hidden = true;
        contentEl.hidden = true;
        errorEl.hidden = false;
        return;
    }

    orderIdLine.textContent = 'Order ID: ' + orderId;
    statusEl.hidden = true;
    contentEl.hidden = false;
    errorEl.hidden = true;

    let pollInterval;

    function updateStatus(status) {
        orderStatusLine.textContent = 'Status: ' + (status || '—');
        if (status === 'COMPLETED') {
            orderStatusLine.textContent = 'Status: Completed';
            loadingIndicator.hidden = true;
            orderComplete.hidden = false;
            if (pollInterval) clearInterval(pollInterval);
        } else if (status === 'FAILED') {
            loadingIndicator.hidden = true;
            orderStatusLine.textContent = 'Status: Failed';
            if (pollInterval) clearInterval(pollInterval);
        }
    }

    async function fetchStatus() {
        try {
            const resp = await fetch('/order-status/' + encodeURIComponent(orderId), {
                headers: { 'Accept': 'application/json' }
            });
            if (!resp.ok) return;
            const data = await resp.json();
            updateStatus(data.status);
        } catch (e) {
            console.warn('order-confirmation.js poll error:', e);
        }
    }

    fetchStatus();
    pollInterval = setInterval(fetchStatus, 2000);
});
