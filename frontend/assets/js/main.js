/**
 * L2H5 Web Panel — Main Application
 */

/* ====================================================================
   TOAST SYSTEM
   ==================================================================== */
function showToast(message, type = 'info', icon = null) {
  const icons = { success: '✅', error: '❌', warning: '⚠️', info: '💡' };
  const container = document.getElementById('toast-container');
  const toast = document.createElement('div');
  toast.className = `toast ${type}`;
  toast.innerHTML = `<span>${icon || icons[type] || '💡'}</span><span>${message}</span>`;
  container.appendChild(toast);
  setTimeout(() => toast.remove(), 4200);
}

/* ====================================================================
   NAVIGATION (SPA)
   ==================================================================== */
function navigate(sectionId) {
  document.querySelectorAll('.section').forEach(s => s.classList.remove('active'));
  document.querySelectorAll('.navbar-links a').forEach(a => a.classList.remove('active'));

  const sec = document.getElementById(`section-${sectionId}`);
  if (sec) sec.classList.add('active');

  const link = document.querySelector(`[data-nav="${sectionId}"]`);
  if (link) link.classList.add('active');

  window.scrollTo({ top: 0 });

  // Lazy load del contenido
  switch (sectionId) {
    case 'rankings':  loadRankings();   break;
    case 'news':      loadNews();       break;
    case 'shop':      loadShop();       break;
    case 'panel':     loadPanel();      break;
    case 'home':      loadHome();       break;
  }
}

// Navbar
document.querySelectorAll('[data-nav]').forEach(el => {
  el.addEventListener('click', e => {
    e.preventDefault();
    const target = el.getAttribute('data-nav');

    // Secciones protegidas
    if ((target === 'panel' || target === 'shop') && !api.isLoggedIn) {
      openModal('login');
      showToast('Inicia sesión para continuar', 'warning');
      return;
    }
    navigate(target);
    // Cerrar menú mobile
    document.querySelector('.navbar-links').classList.remove('open');
  });
});

// Hamburger
document.querySelector('.hamburger')?.addEventListener('click', () => {
  document.querySelector('.navbar-links').classList.toggle('open');
});

/* ====================================================================
   MODALS
   ==================================================================== */
function openModal(type) {
  closeAllModals();
  document.getElementById(`modal-${type}`)?.classList.add('open');
}
function closeAllModals() {
  document.querySelectorAll('.modal-overlay').forEach(m => m.classList.remove('open'));
}
document.querySelectorAll('.modal-overlay').forEach(overlay => {
  overlay.addEventListener('click', e => {
    if (e.target === overlay) closeAllModals();
  });
});
document.querySelectorAll('.modal-close').forEach(btn => {
  btn.addEventListener('click', closeAllModals);
});
document.addEventListener('keydown', e => {
  if (e.key === 'Escape') closeAllModals();
});

/* ====================================================================
   AUTH STATE
   ==================================================================== */
let currentUser = null;

function updateAuthUI() {
  const loggedIn = api.isLoggedIn;
  document.querySelectorAll('[data-auth-show]').forEach(el => {
    const required = el.dataset.authShow;
    el.classList.toggle('hidden', required === 'logged' ? !loggedIn : loggedIn);
  });
  if (currentUser) {
    document.getElementById('nav-username')?.textContent &&
      (document.getElementById('nav-username').textContent = currentUser.account?.login || '');
  }
}

async function fetchCurrentUser() {
  if (!api.isLoggedIn) return;
  try {
    currentUser = await api.getMe();
    updateAuthUI();
  } catch {
    api.logout();
    currentUser = null;
    updateAuthUI();
  }
}

/* ────────── REGISTER ────────── */
document.getElementById('form-register')?.addEventListener('submit', async e => {
  e.preventDefault();
  const btn = e.target.querySelector('button[type=submit]');
  const login    = document.getElementById('reg-login').value.trim();
  const pass     = document.getElementById('reg-password').value;
  const confirm  = document.getElementById('reg-confirm').value;

  if (pass !== confirm) { showToast('Las contraseñas no coinciden', 'error'); return; }

  btn.disabled = true; btn.textContent = 'Creando cuenta...';
  try {
    const res = await api.register(login, pass);
    api.setToken(res.token);
    await fetchCurrentUser();
    closeAllModals();
    showToast('¡Cuenta creada exitosamente! Bienvenido/a ⚔️', 'success');
    navigate('panel');
  } catch (err) {
    showToast(err.message, 'error');
  } finally {
    btn.disabled = false; btn.textContent = 'CREAR CUENTA';
  }
});

/* ────────── LOGIN ────────── */
document.getElementById('form-login')?.addEventListener('submit', async e => {
  e.preventDefault();
  const btn = e.target.querySelector('button[type=submit]');
  const login = document.getElementById('login-user').value.trim();
  const pass  = document.getElementById('login-pass').value;

  btn.disabled = true; btn.textContent = 'Iniciando sesión...';
  try {
    const res = await api.login(login, pass);
    api.setToken(res.token);
    await fetchCurrentUser();
    closeAllModals();
    showToast(`¡Bienvenido/a de vuelta, ${login}! 🗡️`, 'success');
    navigate('panel');
  } catch (err) {
    showToast(err.message, 'error');
  } finally {
    btn.disabled = false; btn.textContent = 'INICIAR SESIÓN';
  }
});

/* ────────── LOGOUT ────────── */
document.getElementById('btn-logout')?.addEventListener('click', () => {
  api.logout();
  currentUser = null;
  updateAuthUI();
  navigate('home');
  showToast('Sesión cerrada correctamente', 'info');
});

/* ====================================================================
   HOME — Server Status
   ==================================================================== */
async function loadHome() {
  try {
    const status = await api.getServerStatus();
    document.getElementById('stat-online').textContent  = status.online?.toLocaleString() || '0';
    document.getElementById('stat-accounts').textContent = status.accounts?.toLocaleString() || '0';
    document.getElementById('stat-chars').textContent   = status.characters?.toLocaleString() || '0';

    const pvpZoneEl = document.getElementById('home-pvpzone');
    if (pvpZoneEl && status.pvpZone) {
      pvpZoneEl.textContent = status.pvpZone.name;
    }
  } catch (err) {
    console.warn('Server status:', err.message);
  }
}

/* ====================================================================
   RANKINGS
   ==================================================================== */
let currentRankingTab = 'pvp';

document.querySelectorAll('[data-ranking-tab]').forEach(btn => {
  btn.addEventListener('click', () => {
    document.querySelectorAll('[data-ranking-tab]').forEach(b => b.classList.remove('active'));
    btn.classList.add('active');
    currentRankingTab = btn.dataset.rankingTab;
    renderRanking(currentRankingTab);
  });
});

async function loadRankings() {
  renderRanking(currentRankingTab);
}

async function renderRanking(type) {
  const tbody = document.getElementById('ranking-tbody');
  if (!tbody) return;

  tbody.innerHTML = `<tr><td colspan="7" class="text-center" style="padding:2rem">
    <div class="spinner" style="margin:0 auto"></div></td></tr>`;

  try {
    let data;
    switch (type) {
      case 'pvp':      data = await api.getRankingPvp(50);       break;
      case 'pk':       data = await api.getRankingPk(50);        break;
      case 'pvpzone':  data = await api.getRankingPvpZone(25);   break;
      case 'clans':    data = await api.getRankingClans(25);      break;
      case 'olympiad': data = await api.getRankingOlympiad(25);  break;
      default:         data = await api.getRankingPvp(50);
    }

    if (!data.length) {
      tbody.innerHTML = '<tr><td colspan="7" class="text-center text-muted" style="padding:2rem">Sin datos disponibles</td></tr>';
      return;
    }

    tbody.innerHTML = data.map(row => renderRankingRow(type, row)).join('');
  } catch (err) {
    tbody.innerHTML = `<tr><td colspan="7" class="text-center text-red" style="padding:2rem">Error: ${err.message}</td></tr>`;
  }
}

function getRankBadge(rank) {
  const cls = rank === 1 ? 'rank-1' : rank === 2 ? 'rank-2' : rank === 3 ? 'rank-3' : 'rank-n';
  return `<span class="rank-badge ${cls}">${rank}</span>`;
}

function getOnlineDot(online) {
  return `<span class="online-dot ${online ? 'online' : ''}"></span>`;
}

function renderRankingRow(type, row) {
  if (type === 'clans') {
    return `<tr>
      <td>${getRankBadge(row.rank)}</td>
      <td><strong>${escHtml(row.clan_name)}</strong></td>
      <td><span class="text-cyan">Lv ${row.clan_level}</span></td>
      <td>${escHtml(row.leader_name || '-')}</td>
      <td><span class="kills-badge kills-pvp">⚔ ${(row.total_pvp||0).toLocaleString()}</span></td>
      <td>${row.member_count || 0} miembros</td>
      <td><span class="text-gold">${(row.reputation_score||0).toLocaleString()} pts</span></td>
    </tr>`;
  }

  const title = row.title ? `<div style="font-size:.72rem;color:${row.titleColor||'#FFFF77'}">${escHtml(row.title)}</div>` : '';
  const clanTag = row.clan_name ? `<span style="font-size:.75rem;color:var(--text-muted)"> [${escHtml(row.clan_name)}]</span>` : '';

  const classEmojis = {
    88:'⚔️', 89:'🛡️', 90:'🛡️', 91:'🗡️', 92:'🏹', 93:'🗡️',
    94:'🔥', 95:'💀', 96:'👁️', 97:'✨', 98:'☀️',
    99:'🛡️',100:'🎵',101:'💨',102:'🏹',103:'🔮',104:'🌊',105:'🌿',
    106:'⚡',107:'💃',108:'👻',109:'🏹',110:'🌩️',111:'👁️',112:'🌸',
    113:'💪',114:'🥊',115:'🌀',116:'📣',117:'💰',118:'🔧'
  };
  const classIcon = classEmojis[row.classid] || '⚔️';

  let killsCell;
  if (type === 'pvp' || type === 'pvpzone') {
    killsCell = `<span class="kills-badge ${type === 'pvpzone' ? 'kills-zone' : 'kills-pvp'}">
      ${type === 'pvpzone' ? '🏆' : '⚔'} ${(row.kills || row.pvpkills || 0).toLocaleString()}</span>`;
  } else if (type === 'pk') {
    killsCell = `<span class="kills-badge kills-pk">💀 ${(row.pkkills||0).toLocaleString()}</span>`;
  } else if (type === 'olympiad') {
    killsCell = `<span class="kills-badge kills-pvp">🏅 ${(row.olympiad_points||0).toLocaleString()}</span>`;
  }

  return `<tr>
    <td>${getRankBadge(row.rank)}</td>
    <td>
      <div class="char-name-cell">
        <div class="class-icon">${classIcon}</div>
        <div>
          <div><strong>${escHtml(row.char_name)}</strong>${clanTag}</div>
          ${title}
        </div>
      </div>
    </td>
    <td><span class="text-cyan">Lv ${row.level || '?'}</span></td>
    <td>${escHtml(row.className || '-')}</td>
    <td>${killsCell}</td>
    <td>${type === 'olympiad' ? `<span class="text-muted">${row.competitions_won||0}W / ${row.competitions_lost||0}L</span>`
                               : `<span class="kills-badge kills-pk">💀 ${(row.pkkills||0).toLocaleString()}</span>`}</td>
    <td>${getOnlineDot(row.online)}</td>
  </tr>`;
}

/* ====================================================================
   SHOP
   ==================================================================== */
let selectedCharacter = null;
let currentShopCategory = '';

document.querySelectorAll('[data-shop-cat]').forEach(btn => {
  btn.addEventListener('click', () => {
    document.querySelectorAll('[data-shop-cat]').forEach(b => b.classList.remove('active'));
    btn.classList.add('active');
    currentShopCategory = btn.dataset.shopCat === 'all' ? '' : btn.dataset.shopCat;
    loadShop();
  });
});

async function loadShop() {
  if (!api.isLoggedIn) {
    document.getElementById('shop-grid').innerHTML =
      '<div class="text-center text-muted" style="padding:3rem">Inicia sesión para ver la tienda 🔒</div>';
    return;
  }

  const grid = document.getElementById('shop-grid');
  grid.innerHTML = '<div class="flex-center" style="padding:3rem"><div class="spinner"></div></div>';

  try {
    const [items, balance] = await Promise.all([
      api.getShopItems(currentShopCategory),
      api.getShopBalance()
    ]);

    // Actualizar balance
    const balEl = document.getElementById('shop-coins');
    if (balEl) balEl.textContent = (balance.coins || 0).toLocaleString();

    // Selector de personaje
    if (currentUser?.characters?.length) {
      const sel = document.getElementById('shop-char-select');
      if (sel) {
        sel.innerHTML = currentUser.characters.map(c =>
          `<option value="${escHtml(c.char_name)}">${escHtml(c.char_name)} (Lv ${c.level})</option>`
        ).join('');
        selectedCharacter = sel.value;
        sel.onchange = () => { selectedCharacter = sel.value; };
      }
    }

    if (!items.length) {
      grid.innerHTML = '<div class="text-center text-muted" style="padding:3rem">No hay ítems en esta categoría</div>';
      return;
    }

    const EMOJIS = { scrolls:'📜', skills:'✨', adena:'💰', premium:'⭐', boxes:'📦', general:'⚔️' };
    grid.innerHTML = items.map(item => `
      <div class="shop-item ${item.featured ? 'featured' : ''}">
        <div class="shop-item-img">
          ${EMOJIS[item.category] || '⚔️'}
          ${item.featured ? '<span class="shop-featured-badge">DESTACADO</span>' : ''}
        </div>
        <div class="shop-item-body">
          <div class="shop-item-name">${escHtml(item.name)}</div>
          <div class="shop-item-desc">${escHtml(item.description || '')}</div>
          <div class="shop-item-footer">
            <div class="shop-price"><span class="coin-icon">🪙</span> ${(item.price_coins||0).toLocaleString()}</div>
            <button class="btn btn-gold btn-sm" onclick="buyItem(${item.id}, '${escHtml(item.name).replace(/'/g,"\\'")}', ${item.price_coins})">
              Comprar
            </button>
          </div>
          ${item.stock !== null ? `<div class="form-hint" style="margin-top:.5rem">Stock: ${item.stock}</div>` : ''}
        </div>
      </div>
    `).join('');
  } catch (err) {
    grid.innerHTML = `<div class="text-center text-red" style="padding:3rem">Error: ${err.message}</div>`;
  }
}

async function buyItem(itemId, itemName, price) {
  if (!selectedCharacter && currentUser?.characters?.length) {
    selectedCharacter = document.getElementById('shop-char-select')?.value;
  }
  if (!selectedCharacter) {
    showToast('Selecciona un personaje primero', 'warning');
    return;
  }
  if (!confirm(`¿Comprar "${itemName}" por ${price.toLocaleString()} 🪙 para ${selectedCharacter}?`)) return;

  try {
    const res = await api.purchase(itemId, selectedCharacter);
    showToast(res.message, 'success', '🎁');
    loadShop(); // refrescar balance
  } catch (err) {
    showToast(err.message, 'error');
  }
}

window.buyItem = buyItem;

/* ====================================================================
   NEWS
   ==================================================================== */
let newsOffset = 0;
const NEWS_LIMIT = 6;

async function loadNews(reset = true) {
  if (reset) newsOffset = 0;

  const mainGrid = document.getElementById('news-main');
  const sideList = document.getElementById('news-sidebar');

  if (reset && mainGrid) {
    mainGrid.innerHTML = '<div class="flex-center" style="padding:3rem"><div class="spinner"></div></div>';
  }

  try {
    const res = await api.getNews(NEWS_LIMIT, newsOffset);
    const items = res.items || [];

    if (reset) {
      mainGrid.innerHTML = '';
    }

    items.forEach((item, i) => {
      const el = document.createElement('div');
      el.className = `news-card ${item.pinned ? 'pinned' : ''}`;

      const typeIcons = { news:'📰', event:'⚔️', update:'🔧', maintenance:'🛠️' };
      el.innerHTML = `
        <div class="news-img">
          ${typeIcons[item.type] || '📰'}
          <span class="news-type-badge ${item.type}">${item.type}</span>
        </div>
        <div class="news-body">
          <div class="news-title">${item.pinned ? '📌 ' : ''}${escHtml(item.title)}</div>
          <div class="news-excerpt">${escHtml((item.content||'').substring(0, 140))}${item.content?.length > 140 ? '...' : ''}</div>
          <div class="news-meta">
            <span>👤 ${escHtml(item.author || 'Admin')}</span>
            <span>📅 ${formatDate(item.created_at)}</span>
          </div>
        </div>
      `;
      mainGrid.appendChild(el);

      // Sidebar (primeros 5)
      if (reset && i < 5 && sideList) {
        const typeEmojis = { news:'📰', event:'⚔️', update:'🔧', maintenance:'🛠️' };
        const sideEl = document.createElement('div');
        sideEl.className = 'news-side-item';
        sideEl.innerHTML = `
          <div class="news-side-icon">${typeEmojis[item.type]||'📰'}</div>
          <div>
            <div class="news-side-title">${escHtml(item.title)}</div>
            <div class="news-side-date">${formatDate(item.created_at)}</div>
          </div>
        `;
        sideList.appendChild(sideEl);
      }
    });

    newsOffset += items.length;

    const loadMoreBtn = document.getElementById('news-load-more');
    if (loadMoreBtn) {
      loadMoreBtn.classList.toggle('hidden', newsOffset >= res.total);
    }
  } catch (err) {
    if (mainGrid) mainGrid.innerHTML = `<div class="text-red" style="padding:2rem">Error: ${err.message}</div>`;
  }
}

document.getElementById('news-load-more')?.addEventListener('click', () => loadNews(false));

/* ====================================================================
   PANEL (User Dashboard)
   ==================================================================== */
async function loadPanel() {
  if (!api.isLoggedIn) return;

  try {
    if (!currentUser) await fetchCurrentUser();
    renderPanelAccount();
    renderPanelChars();
  } catch (err) {
    showToast('Error cargando panel: ' + err.message, 'error');
  }
}

function renderPanelAccount() {
  if (!currentUser?.account) return;
  const acc = currentUser.account;

  const el = document.getElementById('panel-login');
  if (el) el.textContent = acc.login;

  const lastIP = document.getElementById('panel-lastip');
  if (lastIP) lastIP.textContent = acc.lastIP || '-';

  const lastActive = document.getElementById('panel-lastactive');
  if (lastActive) lastActive.textContent = acc.lastactive ? formatDate(new Date(parseInt(acc.lastactive))) : '-';
}

function renderPanelChars() {
  const grid = document.getElementById('panel-chars');
  if (!grid) return;
  const chars = currentUser?.characters || [];

  if (!chars.length) {
    grid.innerHTML = '<div class="text-muted text-center" style="padding:2rem">Sin personajes. ¡Crea uno en el juego!</div>';
    return;
  }

  const raceEmojis = { 0:'🧑', 1:'🧝', 2:'🧙', 3:'👹', 4:'⚒️', 5:'👁️' };
  grid.innerHTML = chars.map(c => `
    <div class="char-card ${c.online ? 'online-char' : ''}">
      <div class="char-header">
        <div class="char-avatar">${raceEmojis[c.race] || '⚔️'}</div>
        <div>
          <div class="char-name">${escHtml(c.char_name)}</div>
          <div class="char-title">${c.title ? escHtml(c.title) : ''}</div>
        </div>
        <div style="margin-left:auto">${c.online ? '<span class="online-dot online"></span>' : ''}</div>
      </div>
      <div class="char-stats">
        <span class="char-stat">Lv ${c.level}</span>
        <span class="char-stat pvp">⚔ ${(c.pvpkills||0).toLocaleString()} PvP</span>
        <span class="char-stat pk">💀 ${(c.pkkills||0).toLocaleString()} PK</span>
      </div>
    </div>
  `).join('');
}

/* ────────── Change password form ────────── */
document.getElementById('form-change-pass')?.addEventListener('submit', async e => {
  e.preventDefault();
  const btn = e.target.querySelector('button[type=submit]');
  const cur = document.getElementById('cp-current').value;
  const nw  = document.getElementById('cp-new').value;
  const cf  = document.getElementById('cp-confirm').value;

  if (nw !== cf) { showToast('Las contraseñas nuevas no coinciden', 'error'); return; }

  btn.disabled = true; btn.textContent = 'Cambiando...';
  try {
    await api.changePassword(cur, nw);
    showToast('Contraseña actualizada correctamente ✅', 'success');
    e.target.reset();
  } catch (err) {
    showToast(err.message, 'error');
  } finally {
    btn.disabled = false; btn.textContent = 'Cambiar Contraseña';
  }
});

/* ====================================================================
   PARTICLES (canvas background)
   ==================================================================== */
function initParticles() {
  const canvas = document.getElementById('particles-canvas');
  if (!canvas) return;
  const ctx = canvas.getContext('2d');

  function resize() {
    canvas.width  = window.innerWidth;
    canvas.height = window.innerHeight;
  }
  resize();
  window.addEventListener('resize', resize);

  const PARTICLE_COUNT = 60;
  const particles = Array.from({ length: PARTICLE_COUNT }, () => ({
    x: Math.random() * canvas.width,
    y: Math.random() * canvas.height,
    vx: (Math.random() - .5) * .4,
    vy: (Math.random() - .5) * .4,
    r: Math.random() * 1.5 + .5,
    alpha: Math.random() * .5 + .1,
    color: Math.random() > .5 ? '0,212,255' : '124,58,237'
  }));

  function draw() {
    ctx.clearRect(0, 0, canvas.width, canvas.height);
    particles.forEach(p => {
      p.x += p.vx; p.y += p.vy;
      if (p.x < 0) p.x = canvas.width;
      if (p.x > canvas.width) p.x = 0;
      if (p.y < 0) p.y = canvas.height;
      if (p.y > canvas.height) p.y = 0;

      ctx.beginPath();
      ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2);
      ctx.fillStyle = `rgba(${p.color},${p.alpha})`;
      ctx.fill();
    });

    // Líneas de conexión
    for (let i = 0; i < particles.length; i++) {
      for (let j = i + 1; j < particles.length; j++) {
        const dx = particles[i].x - particles[j].x;
        const dy = particles[i].y - particles[j].y;
        const dist = Math.sqrt(dx * dx + dy * dy);
        if (dist < 120) {
          ctx.beginPath();
          ctx.moveTo(particles[i].x, particles[i].y);
          ctx.lineTo(particles[j].x, particles[j].y);
          ctx.strokeStyle = `rgba(0,212,255,${0.06 * (1 - dist / 120)})`;
          ctx.lineWidth = .5;
          ctx.stroke();
        }
      }
    }
    requestAnimationFrame(draw);
  }
  draw();
}

/* ====================================================================
   UTILITIES
   ==================================================================== */
function escHtml(str) {
  if (!str) return '';
  return String(str)
    .replace(/&/g,'&amp;')
    .replace(/</g,'&lt;')
    .replace(/>/g,'&gt;')
    .replace(/"/g,'&quot;');
}

function formatDate(d) {
  if (!d) return '-';
  const date = new Date(d);
  return isNaN(date) ? String(d).substring(0,10) :
    date.toLocaleDateString('es-AR', { day:'2-digit', month:'short', year:'numeric' });
}

/* ====================================================================
   INIT
   ==================================================================== */
async function init() {
  initParticles();
  updateAuthUI();
  await fetchCurrentUser();
  navigate('home');
}

init();
