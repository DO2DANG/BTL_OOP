
function handleCardSpotlight(e, card) {
  const rect = card.getBoundingClientRect();
  card.style.setProperty('--mx', (e.clientX - rect.left) + 'px');
  card.style.setProperty('--my', (e.clientY - rect.top) + 'px');
}

function spawnRipple(e, btn) {
  if (!btn) return;
  const rect = btn.getBoundingClientRect();
  const wave = document.createElement('span');
  const size = Math.max(rect.width, rect.height);
  wave.style.width = wave.style.height = size + 'px';
  wave.style.left = ((e.clientX || rect.left + rect.width / 2) - rect.left - size / 2) + 'px';
  wave.style.top = ((e.clientY || rect.top + rect.height / 2) - rect.top - size / 2) + 'px';
  wave.className = 'ripple-wave';
  btn.appendChild(wave);
  setTimeout(() => wave.remove(), 620);
}

function launchFlyOrb(sourceEl) {
  if (!sourceEl || window.matchMedia('(prefers-reduced-motion: reduce)').matches) return;
  const targetEl = window.innerWidth <= 960 ? document.getElementById('mobile-cart-bar') : document.getElementById('cart-box');
  if (!targetEl) return;
  const sRect = sourceEl.getBoundingClientRect();
  const tRect = targetEl.getBoundingClientRect();
  const orb = document.createElement('div');
  orb.className = 'fly-orb';
  orb.textContent = '+1 phần';
  orb.style.left = (sRect.left + sRect.width / 2 - 30) + 'px';
  orb.style.top = (sRect.top + sRect.height / 2 - 14) + 'px';
  document.body.appendChild(orb);
  requestAnimationFrame(() => {
    const dx = (tRect.left + tRect.width / 2) - (sRect.left + sRect.width / 2);
    const dy = (tRect.top + 28) - (sRect.top + sRect.height / 2);
    orb.style.transform = `translate3d(${dx}px, ${dy}px, 0) scale(0.55)`;
    orb.style.opacity = '0.15';
  });
  setTimeout(() => orb.remove(), 640);
}

function filterCategory(cat, btn, ev) {
  if (ev) spawnRipple(ev, btn);
  document.querySelectorAll('.cat-pill').forEach(b => b.classList.remove('active'));
  btn.classList.add('active');
  btn.scrollIntoView({ behavior: 'smooth', inline: 'center', block: 'nearest' });
  let visibleIndex = 0;
  document.querySelectorAll('.food-card').forEach(card => {
    if (cat === 'all' || card.getAttribute('data-category') === cat) {
      card.style.display = 'flex';
      card.style.animation = 'none';
      card.offsetHeight;
      card.style.animation = `cardReveal 0.42s var(--ease-spring) ${visibleIndex * 40}ms backwards`;
      visibleIndex++;
    } else {
      card.style.display = 'none';
    }
  });
}

function toggleMobileCart(open) {
  const box = document.getElementById('cart-box');
  const backdrop = document.getElementById('cart-backdrop');
  if (open) {
    box.classList.add('drawer-open');
    backdrop.classList.add('open');
  } else {
    box.classList.remove('drawer-open');
    backdrop.classList.remove('open');
  }
}

function syncMobileBar() {
  const header = document.querySelector('#cart-box .cart-header');
  if (!header) return;
  const qty = header.getAttribute('data-total-qty') || '0';
  const price = header.getAttribute('data-total-price') || '0 đ';
  document.getElementById('mobile-bar-qty').textContent = qty + ' phần trong giỏ';
  document.getElementById('mobile-bar-price').textContent = price;
}

function showToast(msg) {
  const t = document.getElementById('toast-popup');
  const label = document.createElement('span');
  label.textContent = msg;
  const progress = document.createElement('div');
  progress.className = 'toast-progress';
  t.replaceChildren(label, progress);
  t.classList.add('show');
  clearTimeout(window.toastTimer);
  window.toastTimer = setTimeout(() => { t.classList.remove('show'); }, 2300);
}

let cartRequestPending = false;

async function submitAjax(event, form) {
  event.preventDefault();
  if (cartRequestPending) return;

  const path = new URL(form.action).pathname;
  const cartBox = document.getElementById('cart-box');
  const checkoutForm = cartBox.querySelector('.checkout-form');
  const draft = checkoutForm ? new FormData(checkoutForm) : null;
  const body = new URLSearchParams(new FormData(form));
  const submitButton = form.querySelector('button[type=submit]');
  cartRequestPending = true;
  if (submitButton) submitButton.disabled = true;

  try {
    const response = await fetch(path, { method: 'POST', body });
    const html = await response.text();
    if (!response.ok && !html.startsWith("<div class='cart-header'")) {
      showToast('Chưa thể thực hiện yêu cầu. Vui lòng thử lại.');
      return;
    }

    cartBox.innerHTML = html;
    const newCheckoutForm = cartBox.querySelector('.checkout-form');
    if (draft && newCheckoutForm) {
      for (const [name, value] of draft) {
        const field = newCheckoutForm.elements.namedItem(name);
        if (field) field.value = value;
      }
    }
    cartBox.classList.remove('cart-bump');
    void cartBox.offsetWidth;
    cartBox.classList.add('cart-bump');
    syncMobileBar();

    if (!response.ok) {
      const message = cartBox.querySelector('.order-banner')?.textContent;
      showToast(message || 'Thông tin gửi lên chưa hợp lệ.');
      return;
    }

    if (path === '/cart/add') {
      launchFlyOrb(submitButton);
      if (submitButton) {
        const originalText = submitButton.textContent;
        submitButton.classList.add('added-state');
        submitButton.textContent = 'ĐÃ THÊM VÀO ĐƠN';
        setTimeout(() => {
          submitButton.classList.remove('added-state');
          submitButton.textContent = originalText;
        }, 900);
      }
      showToast('Đã thêm món vào phiếu đặt của bạn');
      const noteInput = form.querySelector('input[name=note]');
      if (noteInput) noteInput.value = '';
    } else if (path === '/cart/submit') {
      showToast('Đơn đặt món đã được gửi tới bếp');
    }
  } catch (error) {
    showToast('Kết nối bị gián đoạn. Vui lòng thử lại.');
    console.error(error);
  } finally {
    cartRequestPending = false;
    if (submitButton) submitButton.disabled = false;
  }
}
