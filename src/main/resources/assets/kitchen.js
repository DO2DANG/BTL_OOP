const totalSelector = '.metric-tile:nth-child(3) .metric-value';
const currentTotal = Number(document.querySelector(totalSelector).textContent);
const currentTickets = document.querySelector('.orders-grid').innerHTML;

async function refreshOrders() {
  try {
    const response = await fetch('/');
    if (!response.ok) return;

    const html = await response.text();
    const page = new DOMParser().parseFromString(html, 'text/html');
    const total = page.querySelector(totalSelector);
    const tickets = page.querySelector('.orders-grid');
    if (!total || !tickets) return;

    if (Number(total.textContent) > currentTotal) {
      new Audio('https://assets.mixkit.co/active_storage/sfx/2869/2869-preview.mp3').play().catch(() => {});
    }
    if (tickets.innerHTML !== currentTickets) {
      location.reload();
    }
  } catch (error) {
    console.warn('Chưa thể cập nhật đơn bếp.', error);
  }
}

setInterval(refreshOrders, 2500);
