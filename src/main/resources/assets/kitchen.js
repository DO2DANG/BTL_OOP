const totalSelector = '.metric-tile:nth-child(3) .metric-value';
const metricsSelector = '.metrics-grid';
const ordersSelector = '.orders-grid';

let currentTotal = Number(document.querySelector(totalSelector)?.textContent || 0);
let currentTickets = document.querySelector(ordersSelector)?.innerHTML || '';
let currentMetrics = document.querySelector(metricsSelector)?.innerHTML || '';

async function refreshOrders() {
  try {
    const response = await fetch('/');
    if (!response.ok) return;

    const html = await response.text();
    const page = new DOMParser().parseFromString(html, 'text/html');
    const totalEl = page.querySelector(totalSelector);
    const nextMetricsEl = page.querySelector(metricsSelector);
    const nextTicketsEl = page.querySelector(ordersSelector);
    if (!totalEl || !nextMetricsEl || !nextTicketsEl) return;

    const nextTotal = Number(totalEl.textContent);
    if (nextTotal > currentTotal) {
      new Audio('https://assets.mixkit.co/active_storage/sfx/2869/2869-preview.mp3').play().catch(() => {});
    }
    currentTotal = nextTotal;

    if (nextMetricsEl.innerHTML !== currentMetrics) {
      document.querySelector(metricsSelector).innerHTML = nextMetricsEl.innerHTML;
      currentMetrics = nextMetricsEl.innerHTML;
    }
    if (nextTicketsEl.innerHTML !== currentTickets) {
      document.querySelector(ordersSelector).innerHTML = nextTicketsEl.innerHTML;
      currentTickets = nextTicketsEl.innerHTML;
    }
  } catch (error) {
    console.warn('Chưa thể cập nhật đơn bếp.', error);
  }
}

setInterval(refreshOrders, 2500);
