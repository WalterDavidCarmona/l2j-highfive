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

  // Cerrar SSE si salimos de apuestas
  if (sectionId !== 'bets' && betsSSE) { betsSSE.close(); betsSSE = null; }

  // Lazy load del contenido
  switch (sectionId) {
    case 'rankings':  loadRankings();   break;
    case 'news':      loadNews();       break;
    case 'shop':      loadShop();       break;
    case 'bets':      loadBets();       break;
    case 'recharge':  loadRecharge();   break;
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
    if ((target === 'panel' || target === 'shop' || target === 'recharge') && !api.isLoggedIn) {
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
   APUESTAS OLIMPIADA — Tiempo Real via SSE
   ==================================================================== */
let betsSeasonData   = null;   // { season, candidates, myBet, totals }
let betSelectedChar  = null;   // candidato elegido para apostar
let betSelectedCoins = 50;     // monedas a apostar (default 50)
let betsSSE          = null;   // EventSource activo
let currentBetsTab   = 'candidates';

/* ── Tabs ── */
document.querySelectorAll('[data-bets-tab]').forEach(btn => {
  btn.addEventListener('click', () => {
    document.querySelectorAll('[data-bets-tab]').forEach(b => b.classList.remove('active'));
    btn.classList.add('active');
    currentBetsTab = btn.dataset.betsTab;
    ['candidates','my-bets','history'].forEach(t =>
      document.getElementById(`bets-tab-${t}`)?.classList.toggle('hidden', t !== currentBetsTab)
    );
    if (currentBetsTab === 'my-bets')  loadMyBets();
    if (currentBetsTab === 'history')  loadBetsHistory();
  });
});

/* ── Cargar sección principal ── */
async function loadBets() {
  try {
    const data = await api.getBetSeason();
    betsSeasonData = data;
    renderBetsSeason(data);
    renderBetsCandidates(data.candidates || [], data.myBet, data.totals);
    startBetsSSE();

    // Admin panel
    const adminPanel = document.getElementById('bets-admin-panel');
    if (adminPanel && currentUser?.account?.accessLevel >= 100) {
      adminPanel.classList.remove('hidden');
    }
  } catch (err) {
    document.getElementById('bets-candidates-grid').innerHTML =
      `<p class="text-red" style="grid-column:1/-1;padding:2rem">Error: ${err.message}</p>`;
  }
}

/* ── Banner de temporada ── */
function renderBetsSeason({ season, myBet, totals }) {
  if (!season) {
    document.getElementById('bets-season-name').textContent   = 'Sin temporada activa';
    document.getElementById('bets-season-status').textContent = 'Esperá la próxima temporada.';
    return;
  }

  document.getElementById('bets-season-name').textContent = season.name;

  const statusMap = { open:'🟢 Apuestas abiertas', closed:'🔴 Apuestas cerradas', resolved:'✅ Resuelta' };
  document.getElementById('bets-season-status').textContent = statusMap[season.status] || season.status;

  document.getElementById('bets-total-bets').textContent = (totals?.total_bets || 0).toLocaleString();
  document.getElementById('bets-total-pool').textContent = `${(totals?.total_pool || 0).toLocaleString()} 🪙`;

  // Mi apuesta activa
  const myCard = document.getElementById('bets-my-bet-card');
  if (myBet && myCard) {
    myCard.classList.remove('hidden');
    document.getElementById('bmb-char-name').textContent = myBet.char_bet;
    document.getElementById('bmb-detail').textContent =
      `${myBet.coins_bet} 🪙 apostadas · ${myBet.won === null ? 'Pendiente' : myBet.won ? '¡Ganaste!' : 'Perdiste'}`;
    document.getElementById('bets-my-pick').textContent = myBet.char_bet;

    // Payout estimado
    const total   = totals?.total_bets || 1;
    const onPick  = (betsSeasonData?.candidates || []).find(c => c.char_name === myBet.char_bet)?.bets_count || 1;
    const est     = Math.min(20, Math.max(1, Math.floor(total / onPick) * myBet.coins_bet));
    document.getElementById('bmb-pay-val').textContent = `🪙 ${est}`;
  } else if (myCard) {
    myCard.classList.add('hidden');
    document.getElementById('bets-my-pick').textContent = '—';
  }
}

/* ── Candidatos ── */
function renderBetsCandidates(candidates, myBet, totals) {
  const grid = document.getElementById('bets-candidates-grid');
  if (!grid) return;

  if (!candidates.length) {
    grid.innerHTML = '<p class="text-muted text-center" style="grid-column:1/-1;padding:3rem">No hay datos de Olimpiada disponibles.</p>';
    return;
  }

  const totalBets   = parseInt(totals?.total_bets) || 0;
  const maxBets     = Math.max(...candidates.map(c => parseInt(c.bets_count) || 0), 1);
  const season      = betsSeasonData?.season;
  const isOpen      = season?.status === 'open';
  const alreadyBet  = !!myBet;

  grid.innerHTML = candidates.map(c => {
    const betsCount  = parseInt(c.bets_count) || 0;
    const barPct     = maxBets > 0 ? Math.round((betsCount / maxBets) * 100) : 0;
    const isHero     = !!c.is_current_hero;
    const isMyPick   = myBet?.char_bet === c.char_name;
    const odds       = totalBets > 0 && betsCount > 0 ? Math.floor(totalBets / betsCount) : '∞';
    const oddsClass  = typeof odds === 'number' ? (odds <= 2 ? 'hot' : odds <= 5 ? 'mid' : 'cold') : 'cold';
    const oddsLabel  = typeof odds === 'number' ? (odds <= 2 ? '🔥 Favorito' : odds <= 5 ? '⚡ Popular' : '💎 Underdog') : '💎 Underdog';
    const estPayout  = typeof odds === 'number' ? Math.min(20, Math.max(1, odds * 2)) : 20;
    const classIcon  = getClassIcon(c.classid);
    const titleColor = c.title_color ? '#' + parseInt(c.title_color).toString(16).padStart(6,'0') : '#aaa';

    let btnHtml = '';
    if (isMyPick) {
      btnHtml = `<button class="btn-bet my-pick-btn" disabled>✅ Tu apuesta</button>`;
    } else if (alreadyBet || !isOpen) {
      btnHtml = `<button class="btn-bet" disabled>${isOpen ? '(Ya apostaste)' : '(Cerrado)'}</button>`;
    } else {
      btnHtml = `<button class="btn-bet" onclick="openBetModal(${JSON.stringify(c).replace(/"/g,'&quot;')})">⚔️ Apostar</button>`;
    }

    return `
    <div class="bet-card ${isHero ? 'is-hero' : ''} ${isMyPick ? 'my-pick' : ''}" id="bet-card-${escHtml(c.char_name).replace(/\s/g,'_')}">
      ${isHero  ? '<div class="bet-hero-crown">👑</div>'    : ''}
      ${isMyPick? '<div class="bet-my-pick-badge">Mi pick</div>' : ''}
      <div class="bet-card-header">
        <div class="bet-card-avatar">${classIcon}</div>
        <div>
          <div class="bet-card-name">${escHtml(c.char_name)}</div>
          <div class="bet-card-class">${CLASS_NAMES[c.classid] || `Clase ${c.classid}`}</div>
          ${c.title ? `<div class="bet-card-title" style="color:${titleColor}">${escHtml(c.title)}</div>` : ''}
        </div>
      </div>

      <div class="bet-card-stats">
        <span class="bet-stat-chip">🏅 ${(c.olympiad_points||0).toLocaleString()} pts</span>
        <span class="bet-stat-chip">⚔️ ${c.competitions_won||0}W</span>
        ${c.clan_name ? `<span class="bet-stat-chip">🛡️ ${escHtml(c.clan_name)}</span>` : ''}
        ${isHero ? '<span class="bet-stat-chip" style="color:var(--gold)">👑 Héroe actual</span>' : ''}
      </div>

      <div>
        <div class="bet-bar-row">
          <span style="font-size:.82rem;color:var(--text-dim)">
            <strong id="bet-count-${escHtml(c.char_name).replace(/\s/g,'_')}">${betsCount}</strong> apuestas
          </span>
          <span class="bet-odds-badge ${oddsClass}">${oddsLabel} · x${odds}</span>
        </div>
        <div class="bet-bar-wrap">
          <div class="bet-bar-fill" id="bet-bar-${escHtml(c.char_name).replace(/\s/g,'_')}"
               style="width:${barPct}%"></div>
        </div>
        <div style="font-size:.78rem;color:var(--text-muted);margin-top:.3rem">
          Payout estimado: <strong style="color:var(--gold)">🪙 ${estPayout}</strong> por 50 monedas
        </div>
      </div>

      ${btnHtml}
    </div>`;
  }).join('');
}

/* ── Abrir modal de apuesta ── */
function openBetModal(candidate) {
  if (!api.isLoggedIn) { openModal('login'); return; }
  betSelectedChar  = candidate;
  betSelectedCoins = 50;

  // Rellenar resumen
  document.getElementById('bcs-class-icon').textContent = getClassIcon(candidate.classid);
  document.getElementById('bcs-char-name').textContent  = candidate.char_name;
  document.getElementById('bcs-class-name').textContent = CLASS_NAMES[candidate.classid] || `Clase ${candidate.classid}`;
  document.getElementById('bcs-stats').textContent =
    `🏅 ${(candidate.olympiad_points||0).toLocaleString()} pts · ⚔️ ${candidate.competitions_won||0}W`;

  updateBetModal();
  // Reset selector de monedas
  const customInput = document.getElementById('bet-coins-custom');
  if (customInput) customInput.value = '';
  document.querySelectorAll('.bet-coin-btn').forEach(b =>
    b.classList.toggle('active', parseInt(b.dataset.coins) === 50)
  );
  openModal('bet');
}
window.openBetModal = openBetModal;

/* ── Actualizar estimaciones en el modal ── */
function updateBetModal() {
  if (!betSelectedChar || !betsSeasonData) return;
  const totalBets  = parseInt(betsSeasonData.totals?.total_bets) || 0;
  const betsOnChar = parseInt(betSelectedChar.bets_count) || 0;
  const odds       = betsOnChar > 0 ? Math.floor(totalBets / betsOnChar) : totalBets || 1;
  const payout     = Math.min(20, Math.max(1, odds * betSelectedCoins));

  document.getElementById('bcs-odds').textContent     = `x${odds || '∞'}`;
  document.getElementById('bpe-coins').textContent    = `${betSelectedCoins} 🪙`;
  document.getElementById('bpe-payout').textContent   = `≈ ${payout} 🪙`;
  document.getElementById('bpe-bets-on').textContent  = betsOnChar;
  document.getElementById('bpe-total-bets').textContent = totalBets;
}

/* ── Selector de monedas en modal ── */
document.querySelectorAll('.bet-coin-btn').forEach(btn => {
  btn.addEventListener('click', () => {
    document.querySelectorAll('.bet-coin-btn').forEach(b => b.classList.remove('active'));
    btn.classList.add('active');
    betSelectedCoins = parseInt(btn.dataset.coins);
    const customInput = document.getElementById('bet-coins-custom');
    if (customInput) customInput.value = '';
    updateBetModal();
  });
});

// Input manual 1-1000
document.getElementById('bet-coins-custom')?.addEventListener('input', function () {
  const val = parseInt(this.value);
  if (isNaN(val) || val < 1) return;
  betSelectedCoins = Math.min(1000, val);
  document.querySelectorAll('.bet-coin-btn').forEach(b => b.classList.remove('active'));
  updateBetModal();
});

/* ── Confirmar apuesta ── */
async function confirmBet() {
  if (!betSelectedChar) return;
  const btn = document.getElementById('btn-confirm-bet');
  btn.disabled = true;
  btn.textContent = 'Apostando...';
  try {
    const res = await api.placeBet(betSelectedChar.char_name, betSelectedCoins);
    closeAllModals();
    showToast(res.message, 'success', '🎯');
    loadBets();  // refrescar
  } catch (err) {
    showToast(err.message, 'error');
  } finally {
    btn.disabled = false;
    btn.textContent = '⚔️ Confirmar Apuesta';
  }
}
window.confirmBet = confirmBet;

/* ── Mis apuestas ── */
async function loadMyBets() {
  const el = document.getElementById('bets-my-list');
  if (!el || !api.isLoggedIn) {
    if (el) el.innerHTML = '<p class="text-muted" style="padding:1rem">Iniciá sesión para ver tus apuestas.</p>';
    return;
  }
  el.innerHTML = '<div class="flex-center" style="padding:2rem"><div class="spinner"></div></div>';
  try {
    const bets = await api.getMyBets();
    if (!bets.length) { el.innerHTML = '<p class="text-muted" style="padding:1rem">No tenés apuestas aún.</p>'; return; }
    el.innerHTML = bets.map(b => {
      const isWon    = b.won === 1;
      const isLost   = b.won === 0;
      const isPending= b.won === null;
      return `
      <div class="my-bet-item">
        <div class="mbi-icon">${isPending ? '⏳' : isWon ? '🏆' : '💀'}</div>
        <div class="mbi-body">
          <div class="mbi-season">${escHtml(b.season_name)}</div>
          <div class="mbi-char">Aposté por: <strong>${escHtml(b.char_bet)}</strong></div>
          <div class="mbi-detail">${b.coins_bet} 🪙 apostadas · ${formatDate(b.created_at)}</div>
          ${b.winner_char ? `<div class="mbi-detail">Héroe ganador: <strong>${escHtml(b.winner_char)}</strong></div>` : ''}
        </div>
        <div class="mbi-result">
          ${isPending ? `<div class="mbi-pend">⏳ Pendiente</div>` : ''}
          ${isWon     ? `<div class="mbi-won">+${b.payout} 🪙</div><div style="font-size:.75rem;color:var(--green)">¡Ganaste!</div>` : ''}
          ${isLost    ? `<div class="mbi-lost">−${b.coins_bet} 🪙</div><div style="font-size:.75rem;color:var(--red)">Perdiste</div>` : ''}
        </div>
      </div>`;
    }).join('');
  } catch (err) {
    el.innerHTML = `<p class="text-red" style="padding:1rem">Error: ${err.message}</p>`;
  }
}

/* ── Historial de temporadas ── */
async function loadBetsHistory() {
  const el = document.getElementById('bets-history-list');
  if (!el) return;
  el.innerHTML = '<div class="flex-center" style="padding:2rem"><div class="spinner"></div></div>';
  try {
    const seasons = await api.getBetHistory();
    if (!seasons.length) { el.innerHTML = '<p class="text-muted" style="padding:1rem">Sin temporadas resueltas aún.</p>'; return; }
    el.innerHTML = seasons.map(s => `
    <div class="bets-history-season">
      <div>
        <div class="bhs-name">${escHtml(s.name)}</div>
        <div class="bhs-winner">👑 Héroe: <strong>${escHtml(s.winner_char || '—')}</strong>
          ${s.winner_class_id ? ` · ${CLASS_NAMES[s.winner_class_id] || ''}` : ''}
        </div>
        <div style="font-size:.78rem;color:var(--text-muted);margin-top:.25rem">
          Resuelta: ${formatDate(s.resolved_at)}
        </div>
      </div>
      <div class="bhs-stats">
        <div class="bhs-stat">
          <div class="bhs-stat-v">${(s.total_bets||0).toLocaleString()}</div>
          <div class="bhs-stat-l">Apuestas</div>
        </div>
        <div class="bhs-stat">
          <div class="bhs-stat-v text-gold">${(s.total_pool||0).toLocaleString()} 🪙</div>
          <div class="bhs-stat-l">En juego</div>
        </div>
      </div>
    </div>`).join('');
  } catch (err) {
    el.innerHTML = `<p class="text-red" style="padding:1rem">Error: ${err.message}</p>`;
  }
}

/* ── SSE — Tiempo real ── */
function startBetsSSE() {
  if (betsSSE) { betsSSE.close(); betsSSE = null; }

  const token = api.token ? `?token=${api.token}` : '';
  betsSSE = new EventSource(`/api/bets/live`);

  betsSSE.onmessage = (e) => {
    try {
      const msg = JSON.parse(e.data);
      handleBetsSSE(msg);
    } catch {}
  };

  betsSSE.onerror = () => {
    // Reconectar en 5s
    setTimeout(() => {
      if (document.getElementById('section-bets')?.classList.contains('active')) {
        startBetsSSE();
      }
    }, 5000);
  };
}

function handleBetsSSE(msg) {
  if (msg.type === 'new_bet') {
    // Actualizar contador del candidato en tiempo real
    const charKey  = msg.char_bet?.replace(/\s/g, '_');
    const countEl  = document.getElementById(`bet-count-${charKey}`);
    if (countEl) countEl.textContent = msg.bets_count;

    // Animar barra
    const barEl = document.getElementById(`bet-bar-${charKey}`);
    if (barEl && betsSeasonData?.candidates) {
      const maxBets = Math.max(...betsSeasonData.candidates.map(c => parseInt(c.bets_count)||0), msg.bets_count);
      barEl.style.width = Math.round((msg.bets_count / maxBets) * 100) + '%';
    }

    // Flash visual en la tarjeta
    const card = document.getElementById(`bet-card-${charKey}`);
    if (card) {
      card.classList.add('flash');
      setTimeout(() => card.classList.remove('flash'), 700);
    }

    // Actualizar totales
    document.getElementById('bets-total-bets').textContent = (msg.total_bets||0).toLocaleString();
    document.getElementById('bets-total-pool').textContent = `${(msg.total_pool||0).toLocaleString()} 🪙`;

    // Actualizar betsSeasonData localmente
    if (betsSeasonData?.candidates) {
      const cand = betsSeasonData.candidates.find(c => c.char_name === msg.char_bet);
      if (cand) cand.bets_count = msg.bets_count;
      if (betsSeasonData.totals) {
        betsSeasonData.totals.total_bets = msg.total_bets;
        betsSeasonData.totals.total_pool = msg.total_pool;
      }
    }

    showToast(`Nueva apuesta por ${msg.char_bet}!`, 'info', '🎯');

  } else if (msg.type === 'season_resolved') {
    showToast(`¡Temporada resuelta! 🏆 Héroe: ${msg.winner_char}`, 'success', '👑');
    loadBets();

  } else if (msg.type === 'season_closed') {
    showToast('Las apuestas están cerradas. ¡Esperá el resultado!', 'warning', '🔒');
    loadBets();

  } else if (msg.type === 'new_season') {
    showToast('¡Nueva temporada de apuestas abierta!', 'success', '⚔️');
    loadBets();
  }
}

/* ── Admin helpers ── */
async function adminNewSeason() {
  const name = document.getElementById('admin-season-name')?.value?.trim();
  try {
    const res = await api.adminNewSeason(name || undefined);
    showToast(res.message, 'success');
    loadBets();
  } catch (err) { showToast(err.message, 'error'); }
}
async function adminCloseSeason() {
  const seasonId = betsSeasonData?.season?.id;
  if (!seasonId) { showToast('No hay temporada activa', 'warning'); return; }
  if (!confirm('¿Cerrar apuestas? Ya no se aceptarán nuevas apuestas.')) return;
  try {
    const res = await api.adminCloseSeason(seasonId);
    showToast(res.message, 'success');
    loadBets();
  } catch (err) { showToast(err.message, 'error'); }
}
async function adminResolve() {
  const seasonId   = betsSeasonData?.season?.id;
  const winnerChar = document.getElementById('admin-winner-char')?.value?.trim();
  if (!seasonId)   { showToast('No hay temporada activa', 'warning'); return; }
  if (!winnerChar) { showToast('Ingresá el nombre del Héroe ganador', 'warning'); return; }
  if (!confirm(`¿Declarar a "${winnerChar}" como Héroe y pagar a los ganadores?`)) return;
  try {
    const res = await api.adminResolveBets(seasonId, winnerChar);
    showToast(res.message, 'success', '👑');
    loadBets();
  } catch (err) { showToast(err.message, 'error'); }
}
window.adminNewSeason    = adminNewSeason;
window.adminCloseSeason  = adminCloseSeason;
window.adminResolve      = adminResolve;

/* ── Helpers de clase/icono (reutiliza CLASS_NAMES de main.js) ── */
function getClassIcon(classId) {
  const icons = {
    88:'⚔️',89:'🛡️',90:'🛡️',91:'🗡️',92:'🏹',93:'🗡️',
    94:'🔥',95:'💀',96:'👁️',97:'✨',98:'☀️',
    99:'🛡️',100:'🎵',101:'💨',102:'🏹',103:'🔮',104:'🌊',105:'🌿',
    106:'⚡',107:'💃',108:'👻',109:'🏹',110:'🌩️',111:'👁️',112:'🌸',
    113:'💪',114:'🥊',115:'🌀',116:'📣',117:'💰',118:'🔧'
  };
  return icons[classId] || '⚔️';
}

const CLASS_NAMES = {
  0:'Human Fighter',1:'Warrior',2:'Gladiator',3:'Warlord',4:'Human Knight',
  5:'Paladin',6:'Dark Avenger',7:'Rogue',8:'Treasure Hunter',9:'Hawkeye',
  10:'Human Mystic',11:'Human Wizard',12:'Sorcerer',13:'Necromancer',14:'Warlock',
  15:'Cleric',16:'Bishop',17:'Prophet',
  18:'Elven Fighter',19:'Elven Knight',20:'Temple Knight',21:'Swordsinger',
  22:'Elven Scout',23:'Plainswalker',24:'Silver Ranger',
  25:'Elven Mystic',26:'Elven Wizard',27:'Spellsinger',28:'Elemental Summoner',
  29:'Elven Oracle',30:'Elven Elder',
  31:'Dark Fighter',32:'Palus Knight',33:'Shillien Knight',34:'Bladedancer',
  35:'Assassin',36:'Abyss Walker',37:'Phantom Ranger',
  38:'Dark Elven Mystic',39:'Dark Wizard',40:'Spellhowler',41:'Phantom Summoner',
  42:'Shillien Oracle',43:'Shillien Elder',
  44:'Orc Fighter',45:'Orc Raider',46:'Destroyer',47:'Monk',48:'Tyrant',
  49:'Orc Mystic',50:'Orc Shaman',51:'Overlord',52:'Warcryer',
  53:'Dwarven Fighter',54:'Scavenger',55:'Bounty Hunter',56:'Artisan',57:'Warsmith',
  88:'Duelist',89:'Dreadnought',90:'Phoenix Knight',91:'Hell Knight',
  92:'Sagittarius',93:'Adventurer',94:'Archmage',95:'Soultaker',
  96:'Arcana Lord',97:'Cardinal',98:'Hierophant',
  99:"Eva's Templar",100:'Sword Muse',101:'Wind Rider',102:'Moonlight Sentinel',
  103:'Mystic Muse',104:'Elemental Master',105:"Eva's Saint",
  106:'Shillien Templar',107:'Spectral Dancer',108:'Ghost Hunter',
  109:'Ghost Sentinel',110:'Storm Screamer',111:'Spectral Master',112:'Shillien Saint',
  113:'Titan',114:'Grand Khavatari',115:'Dominator',116:'Doomcryer',
  117:'Fortune Seeker',118:'Maestro'
};

/* ====================================================================
   RECHARGE — Paquetes de Monedas
   ==================================================================== */
let selectedPackage = null;

async function loadRecharge() {
  // Mostrar/ocultar según login
  const guest   = document.getElementById('recharge-guest');
  const content = document.getElementById('recharge-content');
  if (!api.isLoggedIn) {
    guest?.classList.remove('hidden');
    content?.classList.add('hidden');
    return;
  }
  guest?.classList.add('hidden');
  content?.classList.remove('hidden');

  // Balance actualizado
  try {
    const bal = await api.getShopBalance();
    const el  = document.getElementById('recharge-coins');
    if (el) el.textContent = (bal.coins || 0).toLocaleString();
  } catch {}

  // Cargar paquetes
  const grid = document.getElementById('packages-grid');
  if (!grid) return;
  grid.innerHTML = '<div class="flex-center" style="padding:3rem;grid-column:1/-1"><div class="spinner"></div></div>';

  try {
    const packages = await api.getCoinPackages();
    if (!packages.length) {
      grid.innerHTML = '<p class="text-muted text-center" style="grid-column:1/-1;padding:2rem">No hay paquetes disponibles.</p>';
      return;
    }
    grid.innerHTML = packages.map(pkg => renderPackageCard(pkg)).join('');
  } catch (err) {
    grid.innerHTML = `<p class="text-red text-center" style="grid-column:1/-1;padding:2rem">Error: ${err.message}</p>`;
  }
}

function renderPackageCard(pkg) {
  const coins = (pkg.coins || 0).toLocaleString();
  const hasBonus = pkg.bonus_pct > 0;
  const priceArs = pkg.price_ars ? `$${parseFloat(pkg.price_ars).toLocaleString('es-AR')} ARS` : null;
  const priceUsd = pkg.price_usd ? `U$D ${parseFloat(pkg.price_usd).toFixed(2)}` : null;

  return `
  <div class="package-card ${pkg.featured ? 'featured' : ''}" onclick="openPaymentModal(${JSON.stringify(pkg).replace(/"/g,'&quot;')})">
    ${pkg.featured ? '<div class="pkg-badge">⭐ Popular</div>' : ''}
    <div class="pkg-icon">🪙</div>
    <div class="pkg-name">${escHtml(pkg.name)}</div>
    ${pkg.description ? `<div class="pkg-desc">${escHtml(pkg.description)}</div>` : ''}
    <div class="pkg-coins">🪙 ${coins}</div>
    <div class="pkg-coins-label">WebCoins</div>
    ${hasBonus ? `<div class="pkg-bonus">🎁 +${pkg.bonus_pct}% BONUS</div>` : ''}
    <div class="pkg-prices">
      ${priceArs ? `
      <div class="pkg-price-row">
        <div class="pkg-price-provider">🇦🇷 MercadoPago</div>
        <div class="price-val">${priceArs}</div>
      </div>` : ''}
      ${priceUsd ? `
      <div class="pkg-price-row">
        <div class="pkg-price-provider">🌎 PayPal</div>
        <div class="price-val">${priceUsd}</div>
      </div>` : ''}
    </div>
    <button class="btn-buy-package">Comprar ahora →</button>
  </div>`;
}

function openPaymentModal(pkg) {
  if (!api.isLoggedIn) { openModal('login'); return; }
  selectedPackage = pkg;

  // Rellenar resumen
  document.getElementById('pay-pkg-coins').textContent = (pkg.coins || 0).toLocaleString();
  document.getElementById('pay-pkg-name').textContent  = pkg.name;

  const bonusWrap = document.getElementById('pay-pkg-bonus-wrap');
  if (pkg.bonus_pct > 0) {
    document.getElementById('pay-pkg-bonus').textContent = pkg.bonus_pct;
    bonusWrap?.classList.remove('hidden');
  } else {
    bonusWrap?.classList.add('hidden');
  }

  const arsEl = document.getElementById('pay-price-ars');
  const usdEl = document.getElementById('pay-price-usd');
  if (arsEl) arsEl.textContent = pkg.price_ars ? `$${parseFloat(pkg.price_ars).toLocaleString('es-AR')} ARS` : 'No disp.';
  if (usdEl) usdEl.textContent = pkg.price_usd ? `U$D ${parseFloat(pkg.price_usd).toFixed(2)}` : 'No disp.';

  // Deshabilitar botón si no hay precio
  const btnMp = document.getElementById('btn-pay-mp');
  const btnPp = document.getElementById('btn-pay-pp');
  if (btnMp) btnMp.disabled = !pkg.price_ars;
  if (btnPp) btnPp.disabled = !pkg.price_usd;

  openModal('payment');
}
window.openPaymentModal = openPaymentModal;

async function startPayment(provider) {
  if (!selectedPackage) return;
  closeAllModals();

  try {
    if (provider === 'mp') {
      // MercadoPago: redirige al checkout de MP
      const res = await api.createMpPayment(selectedPackage.id);
      if (res.initPoint) {
        window.location.href = res.initPoint;
      } else {
        showToast('Error al crear la preferencia de pago', 'error');
      }

    } else if (provider === 'paypal') {
      // PayPal: abre la URL de aprobación
      const res = await api.createPaypalOrder(selectedPackage.id);
      if (res.approveUrl) {
        // Abrir PayPal en popup (o mismo tab si falla el popup)
        const popup = window.open(res.approveUrl, 'paypal_checkout',
          'width=500,height=700,scrollbars=yes');

        if (popup) {
          // Esperar que el popup cierre (retorno via URL) y luego capturar
          document.getElementById('modal-processing')?.classList.add('open');
          document.getElementById('processing-msg').textContent =
            'Completá el pago en la ventana de PayPal y esperá la confirmación.';

          const interval = setInterval(async () => {
            if (popup.closed) {
              clearInterval(interval);
              closeAllModals();
              // Intentar capturar con los datos guardados
              try {
                const capture = await api.capturePaypalOrder(res.paypalOrderId, res.orderId);
                showPaymentResult(capture.status === 'approved' ? 'success' : 'failure',
                  capture.coins, capture.message);
              } catch {
                showPaymentResult('failure');
              }
            }
          }, 1000);
        } else {
          // Popup bloqueado → redirigir
          window.location.href = res.approveUrl;
        }
      } else {
        showToast('Error al crear la orden PayPal', 'error');
      }
    }
  } catch (err) {
    showToast(err.message, 'error');
  }
}
window.startPayment = startPayment;

function showPaymentResult(type, coins = 0, msg = '') {
  // Ocultar todos los resultados
  ['success','failure','pending'].forEach(t =>
    document.getElementById(`result-${t}`)?.classList.add('hidden')
  );

  const el = document.getElementById(`result-${type}`);
  if (!el) return;

  if (type === 'success') {
    document.getElementById('result-coins-amount').textContent = (coins || 0).toLocaleString();
    document.getElementById('result-success-msg').textContent =
      msg || '¡Tus WebCoins fueron acreditadas automáticamente!';
    // Refrescar balance en navbar
    api.getShopBalance().then(b => {
      document.getElementById('recharge-coins').textContent = (b.coins || 0).toLocaleString();
      document.getElementById('shop-coins').textContent     = (b.coins || 0).toLocaleString();
      document.getElementById('panel-coins').textContent    = (b.coins || 0).toLocaleString();
    }).catch(() => {});
  }

  el.classList.remove('hidden');
}
window.showPaymentResult = showPaymentResult;

function closePaymentResult() {
  ['success','failure','pending'].forEach(t =>
    document.getElementById(`result-${t}`)?.classList.add('hidden')
  );
  navigate('shop');
}
window.closePaymentResult = closePaymentResult;

async function loadPaymentHistory() {
  const el = document.getElementById('payment-history');
  if (!el || !api.isLoggedIn) return;
  el.innerHTML = '<div class="flex-center" style="padding:1rem"><div class="spinner"></div></div>';
  try {
    const rows = await api.getPaymentHistory();
    if (!rows.length) { el.innerHTML = '<p class="text-muted" style="padding:1rem">Sin recargas aún.</p>'; return; }

    const statusLabel = { approved:'Aprobado', pending:'Pendiente', rejected:'Rechazado', cancelled:'Cancelado' };
    el.innerHTML = `<div class="table-wrap" style="margin-top:1rem">
      <table class="ranking-table payment-history-table"><thead><tr>
        <th>Paquete</th><th>Monedas</th><th>Monto</th><th>Método</th><th>Estado</th><th>Fecha</th>
      </tr></thead><tbody>
      ${rows.map(r => `<tr>
        <td>${escHtml(r.package_name)}</td>
        <td class="text-gold">🪙 ${(r.coins||0).toLocaleString()}</td>
        <td><strong>${r.currency === 'ARS' ? '$' : 'U$D'} ${parseFloat(r.amount).toLocaleString()}</strong></td>
        <td><span class="ph-provider-badge ${r.provider === 'mercadopago' ? 'mp' : 'paypal'}">
          ${r.provider === 'mercadopago' ? '🇦🇷 MP' : '🌎 PayPal'}</span></td>
        <td><span class="ph-status-badge ${r.status}">${statusLabel[r.status] || r.status}</span></td>
        <td class="text-muted">${formatDate(r.created_at)}</td>
      </tr>`).join('')}
      </tbody></table></div>`;
  } catch (err) {
    el.innerHTML = `<p class="text-red" style="padding:1rem">Error: ${err.message}</p>`;
  }
}
window.loadPaymentHistory = loadPaymentHistory;

/* ── Manejar retorno de MercadoPago (redirect de vuelta) ── */
function checkPaymentReturn() {
  const params = new URLSearchParams(window.location.search);
  const provider = params.get('provider');
  if (!provider) return;

  // Limpiar URL
  window.history.replaceState({}, '', '/');

  if (provider === 'mp') {
    const status = params.get('status') || params.get('collection_status');
    if (status === 'approved') {
      navigate('recharge');
      showPaymentResult('success', 0, 'Pago aprobado. Las monedas serán acreditadas en instantes.');
    } else if (status === 'pending' || status === 'in_process') {
      navigate('recharge');
      showPaymentResult('pending');
    } else {
      navigate('recharge');
      showPaymentResult('failure');
    }
  }
}

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
  checkPaymentReturn();   // detectar retorno de MP/PayPal
  navigate('home');
}

init();
