/**
 * L2H5 API Client
 * Centraliza todas las llamadas al backend Node.js
 */
const API_BASE = '/api';

class L2Api {
  constructor() {
    this._token = localStorage.getItem('l2_token');
  }

  get token()      { return this._token; }
  get isLoggedIn() { return !!this._token; }

  setToken(t) {
    this._token = t;
    if (t) localStorage.setItem('l2_token', t);
    else   localStorage.removeItem('l2_token');
  }

  async _fetch(path, options = {}) {
    const headers = { 'Content-Type': 'application/json', ...options.headers };
    if (this._token) headers['Authorization'] = `Bearer ${this._token}`;

    const res = await fetch(API_BASE + path, { ...options, headers });
    const data = await res.json().catch(() => ({ error: 'Respuesta inválida del servidor' }));

    if (!res.ok) throw new Error(data.error || `HTTP ${res.status}`);
    return data;
  }

  // ── Auth ──────────────────────────────────────────────────────────
  register(login, password, email, birthday)  {
    return this._fetch('/auth/register', {
      method: 'POST', body: JSON.stringify({ login, password, email, birthday })
    });
  }
  login(login, password) {
    return this._fetch('/auth/login', {
      method: 'POST', body: JSON.stringify({ login, password })
    });
  }
  getMe() { return this._fetch('/auth/me'); }

  /** Recuperación sin login: verifica email + birthday, actualiza contraseña */
  recoverPassword(email, birthday, newPassword) {
    return this._fetch('/auth/recover-password', {
      method: 'POST', body: JSON.stringify({ email, birthday, newPassword })
    });
  }

  /** Cambio de contraseña autenticado: verifica contraseña actual + email + birthday */
  changePassword(currentPassword, email, birthday, newPassword) {
    return this._fetch('/auth/change-password', {
      method: 'POST', body: JSON.stringify({ currentPassword, email, birthday, newPassword })
    });
  }
  logout() { this.setToken(null); }

  /** Marca como leída la notificación PvP de un personaje */
  dismissPvpNotif(charName) {
    return this._fetch(`/auth/dismiss-pvp-notif/${encodeURIComponent(charName)}`, { method: 'POST' });
  }

  // ── Server ────────────────────────────────────────────────────────
  getServerStatus() { return this._fetch('/server/status'); }
  getPvpZoneInfo()  { return this._fetch('/server/pvpzone'); }

  // ── Rankings ──────────────────────────────────────────────────────
  getRankingPvp(limit = 50)     { return this._fetch(`/rankings/pvp?limit=${limit}`); }
  getRankingPk(limit = 50)      { return this._fetch(`/rankings/pk?limit=${limit}`); }
  getRankingPvpZone(limit = 25) { return this._fetch(`/rankings/pvpzone?limit=${limit}`); }
  getRankingClans(limit = 25)   { return this._fetch(`/rankings/clans?limit=${limit}`); }
  getRankingOlympiad(limit = 25){ return this._fetch(`/rankings/olympiad?limit=${limit}`); }
  getOnlinePlayers()            { return this._fetch('/rankings/online'); }

  // ── News ──────────────────────────────────────────────────────────
  getNews(limit = 10, offset = 0, type = '') {
    let q = `/news?limit=${limit}&offset=${offset}`;
    if (type) q += `&type=${type}`;
    return this._fetch(q);
  }
  getNewsItem(id) { return this._fetch(`/news/${id}`); }
  createNews(data) {
    return this._fetch('/news', { method: 'POST', body: JSON.stringify(data) });
  }

  // ── Shop ──────────────────────────────────────────────────────────
  getShopItems(category = '') {
    return this._fetch(`/shop/items${category ? '?category=' + category : ''}`);
  }
  getShopBalance() { return this._fetch('/shop/balance'); }
  purchase(itemShopId, charName, qty = 1) {
    return this._fetch('/shop/purchase', {
      method: 'POST', body: JSON.stringify({ itemShopId, charName, qty })
    });
  }
  cartCheckout(charName, items) {
    return this._fetch('/shop/cart-checkout', {
      method: 'POST', body: JSON.stringify({ charName, items })
    });
  }
  getShopHistory() { return this._fetch('/shop/history'); }

  // ── Payments ──────────────────────────────────────────────────────
  getCoinPackages()             { return this._fetch('/payments/packages'); }
  createMpPayment(packageId)    { return this._fetch('/payments/mp/create',     { method:'POST', body: JSON.stringify({ packageId }) }); }
  getMpOrderStatus(orderId)     { return this._fetch(`/payments/mp/status/${orderId}`); }
  createPaypalOrder(packageId)  { return this._fetch('/payments/paypal/create',  { method:'POST', body: JSON.stringify({ packageId }) }); }
  capturePaypalOrder(paypalOrderId, orderId) {
    return this._fetch('/payments/paypal/capture', { method:'POST', body: JSON.stringify({ paypalOrderId, orderId }) });
  }
  getPaymentHistory()           { return this._fetch('/payments/history'); }

  // ── Admin ─────────────────────────────────────────────────────────
  adminSearchUsers(q)                          { return this._fetch(`/admin/users/search?q=${encodeURIComponent(q)}`); }
  adminGetUser(login)                          { return this._fetch(`/admin/users/${encodeURIComponent(login)}`); }
  adminUpdateCoins(login, action, amount)      { return this._fetch(`/admin/users/${encodeURIComponent(login)}/coins`, { method:'POST', body: JSON.stringify({ action, amount }) }); }
  adminUpdateUser(login, data)                 { return this._fetch(`/admin/users/${encodeURIComponent(login)}`, { method:'PUT', body: JSON.stringify(data) }); }
  adminGetPayments(status = 'all', limit = 50, offset = 0) { return this._fetch(`/admin/payments?status=${status}&limit=${limit}&offset=${offset}`); }
  adminGetShopItems()              { return this._fetch('/admin/shop-items'); }
  adminCreateShopItem(data)        { return this._fetch('/admin/shop-items',     { method:'POST',   body: JSON.stringify(data) }); }
  adminUpdateShopItem(id, data)    { return this._fetch(`/admin/shop-items/${id}`, { method:'PUT',  body: JSON.stringify(data) }); }
  adminDeleteShopItem(id)          { return this._fetch(`/admin/shop-items/${id}`, { method:'DELETE' }); }
  adminGetShopHistory(page=1, limit=50, account='', char='') {
    const q = new URLSearchParams({ page, limit });
    if (account) q.set('account', account);
    if (char)    q.set('char',    char);
    return this._fetch(`/admin/shop-history?${q}`);
  }

  // ── Daily Reward ──────────────────────────────────────────────────
  getDailyStatus()          { return this._fetch('/daily/status'); }
  claimDaily(charId)        { return this._fetch('/daily/claim', { method: 'POST', body: JSON.stringify({ charId }) }); }

  // ── Admin PvP Reward ──────────────────────────────────────────────
  adminGetPvpReward()              { return this._fetch('/admin/pvpzone-reward'); }
  adminSetPvpReward(enabled, coins_per_kill) {
    return this._fetch('/admin/pvpzone-reward', { method:'PUT', body: JSON.stringify({ enabled, coins_per_kill }) });
  }
  adminResetPvpRewardLog()         { return this._fetch('/admin/pvpzone-reward/log', { method:'DELETE' }); }

  // ── Bets ──────────────────────────────────────────────────────────
  getBetSeason()                       { return this._fetch('/bets/season'); }
  placeBet(charBet, coinsBet)          { return this._fetch('/bets/place', { method:'POST', body: JSON.stringify({ charBet, coinsBet }) }); }
  getMyBets()                          { return this._fetch('/bets/my'); }
  getBetHistory()                      { return this._fetch('/bets/history'); }
  adminResolveBets(seasonId, winnerChar){ return this._fetch('/bets/admin/resolve',    { method:'POST', body: JSON.stringify({ seasonId, winnerChar }) }); }
  adminNewSeason(name)                 { return this._fetch('/bets/admin/new-season',  { method:'POST', body: JSON.stringify({ name }) }); }
  adminCloseSeason(seasonId)           { return this._fetch('/bets/admin/close',       { method:'POST', body: JSON.stringify({ seasonId }) }); }
}

window.api = new L2Api();
