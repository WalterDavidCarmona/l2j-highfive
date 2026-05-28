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
   CAPTCHA MATEMÁTICO (Login)
   ==================================================================== */
let currentCaptchaAnswer = 0;

function generateCaptcha() {
  const num1 = Math.floor(Math.random() * 20) + 1;
  const num2 = Math.floor(Math.random() * 20) + 1;
  currentCaptchaAnswer = num1 + num2;

  document.getElementById('captcha-question').textContent = `¿Cuánto es ${num1} + ${num2}?`;
  document.getElementById('login-captcha').value = '';
  document.getElementById('captcha-error').textContent = '';
  document.getElementById('captcha-error').classList.add('hidden');
}

function validateCaptcha() {
  const userAnswer = parseInt(document.getElementById('login-captcha').value.trim());
  const errorEl = document.getElementById('captcha-error');

  if (isNaN(userAnswer)) {
    errorEl.textContent = 'Por favor, ingresa un número';
    errorEl.classList.remove('hidden');
    return false;
  }

  if (userAnswer !== currentCaptchaAnswer) {
    errorEl.textContent = 'Respuesta incorrecta, intenta de nuevo';
    errorEl.classList.remove('hidden');
    generateCaptcha();
    return false;
  }

  return true;
}

/* ====================================================================
   CAPTCHA MATEMÁTICO (Registro)
   ==================================================================== */
let currentRegCaptchaAnswer = 0;

function generateRegCaptcha() {
  const ops = [
    () => { const a = Math.floor(Math.random()*15)+2, b = Math.floor(Math.random()*15)+2; return { q:`¿Cuánto es ${a} + ${b}?`, ans: a+b }; },
    () => { const a = Math.floor(Math.random()*10)+5, b = Math.floor(Math.random()*5)+1;  return { q:`¿Cuánto es ${a} - ${b}?`, ans: a-b }; },
    () => { const a = Math.floor(Math.random()*9)+2,  b = Math.floor(Math.random()*5)+2;  return { q:`¿Cuánto es ${a} × ${b}?`, ans: a*b }; },
  ];
  const { q, ans } = ops[Math.floor(Math.random() * ops.length)]();
  currentRegCaptchaAnswer = ans;
  document.getElementById('reg-captcha-question').textContent = q;
  document.getElementById('reg-captcha').value = '';
  const errEl = document.getElementById('reg-captcha-error');
  errEl.textContent = ''; errEl.classList.add('hidden');
}

function validateRegCaptcha() {
  const userAnswer = parseInt(document.getElementById('reg-captcha').value.trim());
  const errorEl = document.getElementById('reg-captcha-error');
  if (isNaN(userAnswer)) {
    errorEl.textContent = 'Por favor, ingresa un número';
    errorEl.classList.remove('hidden');
    return false;
  }
  if (userAnswer !== currentRegCaptchaAnswer) {
    errorEl.textContent = 'Respuesta incorrecta, intenta de nuevo';
    errorEl.classList.remove('hidden');
    generateRegCaptcha();
    return false;
  }
  return true;
}

/* ====================================================================
   REGISTRO — validación en tiempo real de nombre de usuario
   ==================================================================== */
function sanitizeLoginPreview(raw) {
  return raw.trim().toLowerCase().replace(/[^a-z0-9_]/g, '');
}

document.addEventListener('DOMContentLoaded', () => {
  const regLoginEl = document.getElementById('reg-login');
  if (!regLoginEl) return;

  regLoginEl.addEventListener('input', () => {
    const raw      = regLoginEl.value;
    const clean    = sanitizeLoginPreview(raw);
    const preview  = document.getElementById('reg-login-preview');
    const errorEl  = document.getElementById('reg-login-error');

    // Mostrar caracteres inválidos detectados
    const hasInvalid = raw !== raw.toLowerCase() || /[^a-z0-9_]/.test(raw.toLowerCase());
    if (hasInvalid && raw.length > 0) {
      preview.textContent = `Se usará: "${clean}" (los caracteres no válidos se omiten)`;
      preview.classList.remove('hidden');
    } else {
      preview.classList.add('hidden');
    }

    // Validar longitud
    if (clean.length > 0 && clean.length < 4) {
      errorEl.textContent = `Mínimo 4 caracteres (tienes ${clean.length})`;
      errorEl.classList.remove('hidden');
    } else if (clean.length > 14) {
      errorEl.textContent = 'Máximo 14 caracteres';
      errorEl.classList.remove('hidden');
    } else {
      errorEl.classList.add('hidden');
    }
  });
});

/* ====================================================================
   DESCARGA BLOC DE NOTAS — datos de cuenta
   isRecovery=true  → recuperación de contraseña
   isChange=true    → cambio de contraseña desde el panel
   ==================================================================== */
function downloadAccountCard(login, password, email = '', birthday = '', isRecovery = false, isChange = false) {
  const now        = new Date();
  const dateStr    = now.toLocaleDateString('es-ES', { year:'numeric', month:'2-digit', day:'2-digit' });
  const timeStr    = now.toLocaleTimeString('es-ES', { hour:'2-digit', minute:'2-digit' });
  const serverName = document.title || 'L2H5 Server';

  // Formatear fecha de nacimiento legible
  let birthdayStr = '';
  if (birthday) {
    const [y, m, d] = birthday.split('-');
    birthdayStr = d ? `${d}/${m}/${y}` : birthday;
  }

  const action = isChange   ? 'ACTUALIZACIÓN DE CONTRASEÑA'
               : isRecovery ? 'RECUPERACIÓN DE CONTRASEÑA'
               :              'DATOS DE REGISTRO';

  const lines = [
    '╔══════════════════════════════════════════════╗',
    `║  ${action.padEnd(44)}║`,
    `║  ${serverName.padEnd(44)}║`,
    '╚══════════════════════════════════════════════╝',
    '',
    `  Fecha : ${dateStr} a las ${timeStr}`,
    '',
    '  ┌─ CREDENCIALES DE ACCESO ──────────────────┐',
    `  │  Usuario      : ${login}`,
    `  │  Contraseña   : ${password}`,
    '  └───────────────────────────────────────────┘',
    '',
    '  ┌─ DATOS DE IDENTIDAD ───────────────────────┐',
    `  │  Correo       : ${email || '(no registrado)'}`,
    `  │  Nacimiento   : ${birthdayStr || '(no registrado)'}`,
    '  └───────────────────────────────────────────┘',
    '',
    '  INSTRUCCIONES:',
    '  1. Abre el launcher de Lineage 2.',
    '  2. Ingresa el Usuario y Contraseña exactamente',
    '     como aparecen arriba.',
    '  3. El Correo y la Fecha de Nacimiento son',
    '     necesarios para recuperar la cuenta.',
    '  4. Guarda este archivo en un lugar seguro.',
    '',
    '═══════════════════════════════════════════════',
    '  ⚠  NO compartas esta información con nadie.',
    '     El staff JAMÁS pedirá tu contraseña.',
    '═══════════════════════════════════════════════',
  ].join('\r\n');

  const suffix   = isChange ? '_nueva_pass' : isRecovery ? '_recuperacion' : '_registro';
  const blob     = new Blob(['﻿' + lines], { type: 'text/plain;charset=utf-8' });
  const url      = URL.createObjectURL(blob);
  const a        = document.createElement('a');
  a.href         = url;
  a.download     = `cuenta_${login}${suffix}.txt`;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
}

/* ====================================================================
   INACTIVITY MANAGER
   ==================================================================== */
class InactivityManager {
  constructor(timeoutMinutes = 10, warningMinutes = 8) {
    this.timeoutMs = timeoutMinutes * 60 * 1000;
    this.warningMs = warningMinutes * 60 * 1000;
    this.timeoutId = null;
    this.warningId = null;
    this.countdownId = null;
    this.isActive = false;
    this.lastActivityTime = Date.now();

    // Eventos a monitorear
    this.events = ['mousemove', 'keydown', 'click', 'touchstart'];
  }

  start() {
    if (this.isActive) return;
    this.isActive = true;
    this.lastActivityTime = Date.now();
    this.resetTimer();
    this.attachListeners();
  }

  stop() {
    if (!this.isActive) return;
    this.isActive = false;
    clearTimeout(this.timeoutId);
    clearTimeout(this.warningId);
    clearInterval(this.countdownId);
    this.detachListeners();
  }

  resetTimer() {
    if (!this.isActive) return;

    // Cerrar modal de advertencia si está abierto
    const warningModal = document.getElementById('modal-inactivity-warning');
    if (warningModal?.classList.contains('open')) {
      closeAllModals();
    }

    clearTimeout(this.timeoutId);
    clearTimeout(this.warningId);
    clearInterval(this.countdownId);

    this.lastActivityTime = Date.now();

    // Configurar advertencia en 8 minutos
    this.warningId = setTimeout(() => this.showWarning(), this.warningMs);

    // Configurar logout en 10 minutos
    this.timeoutId = setTimeout(() => this.logout(), this.timeoutMs);
  }

  showWarning() {
    openModal('inactivity-warning');
    let remainingSeconds = Math.ceil((this.timeoutMs - this.warningMs) / 1000);

    // Actualizar countdown cada segundo
    this.countdownId = setInterval(() => {
      remainingSeconds--;
      document.getElementById('inactivity-countdown').textContent = remainingSeconds;

      if (remainingSeconds <= 0) {
        clearInterval(this.countdownId);
      }
    }, 1000);
  }

  logout() {
    if (!this.isActive) return;
    this.stop();
    api.logout();
    currentUser = null;
    updateAuthUI();
    navigate('home');
    showToast('Tu sesión expiró por inactividad', 'warning');
  }

  attachListeners() {
    this.events.forEach(event => {
      document.addEventListener(event, () => this.resetTimer(), { passive: true });
    });
  }

  detachListeners() {
    this.events.forEach(event => {
      document.removeEventListener(event, () => this.resetTimer());
    });
  }
}

// Instancia global
window.inactivityManager = null;

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
    case 'admin':     loadAdmin();      break;
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
    if (target === 'admin') {
      if (!api.isLoggedIn) { openModal('login'); showToast('Inicia sesión para continuar', 'warning'); return; }
      if (!currentUser?.account || currentUser.account.accessLevel < 100) {
        showToast('Sin permisos de administrador', 'error'); return;
      }
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

  // Generar CAPTCHA según el modal abierto
  if (type === 'login')    { generateCaptcha(); }
  if (type === 'register') {
    generateRegCaptcha();
    document.getElementById('form-register')?.reset();
    ['reg-login-preview','reg-login-error','reg-pass-error','reg-confirm-error',
     'reg-email-error','reg-email-confirm-error','reg-birthday-error','reg-captcha-error']
      .forEach(id => { const el = document.getElementById(id); if (el) { el.textContent=''; el.classList.add('hidden'); } });
  }
  if (type === 'recover') { resetRecoverModal(); }
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
  // Mostrar/ocultar links que requieren sesión iniciada
  ['nav-bets-item', 'nav-recharge-item'].forEach(id => {
    document.getElementById(id)?.classList.toggle('hidden', !loggedIn);
  });

  // Mostrar/ocultar link de Admin según accessLevel
  const adminNav = document.getElementById('nav-admin-item');
  if (adminNav) {
    const isAdmin = loggedIn && currentUser?.account?.accessLevel >= 100;
    adminNav.classList.toggle('hidden', !isAdmin);
  }
}

async function fetchCurrentUser() {
  if (!api.isLoggedIn) return;
  try {
    currentUser = await api.getMe();
    updateAuthUI();

    // Iniciar monitoreo de inactividad cuando login es exitoso
    if (!window.inactivityManager) {
      window.inactivityManager = new InactivityManager(10, 8); // 10 min timeout, 8 min warning
    }
    window.inactivityManager.start();
  } catch (err) {
    // Token expirado o inválido
    api.logout();
    currentUser = null;
    updateAuthUI();

    // Detener monitoreo de inactividad
    if (window.inactivityManager) {
      window.inactivityManager.stop();
    }
  }
}

/* ────────── Helper inline de errores de campo ────────── */
function fieldErr(id, msg) {
  const el = document.getElementById(id);
  if (!el) return;
  el.textContent = msg; el.classList.remove('hidden');
}
function clearFieldErr(id) {
  const el = document.getElementById(id);
  if (el) { el.textContent = ''; el.classList.add('hidden'); }
}
function isValidEmail(e) { return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(e); }
function isValidBirthdayClient(str) {
  if (!str) return false;
  const d = new Date(str + 'T00:00:00');
  return !isNaN(d.getTime()) && d < new Date();
}

/* ────────── REGISTER ────────── */
document.getElementById('form-register')?.addEventListener('submit', async e => {
  e.preventDefault();
  const btn      = e.target.querySelector('button[type=submit]');
  const rawLogin = document.getElementById('reg-login').value.trim();
  const login    = sanitizeLoginPreview(rawLogin);
  const pass     = document.getElementById('reg-password').value;
  const confirm  = document.getElementById('reg-confirm').value;
  const email    = document.getElementById('reg-email').value.trim().toLowerCase();
  const emailCf  = document.getElementById('reg-email-confirm').value.trim().toLowerCase();
  const birthday = document.getElementById('reg-birthday').value;

  // ── Limpiar errores previos ───────────────────────────────────
  ['reg-login-error','reg-pass-error','reg-confirm-error',
   'reg-email-error','reg-email-confirm-error','reg-birthday-error'].forEach(clearFieldErr);

  let valid = true;
  const fail = (id, msg) => { fieldErr(id, msg); valid = false; };

  if (login.length < 4)  fail('reg-login-error', `Mínimo 4 caracteres (tienes ${login.length})`);
  if (login.length > 14) fail('reg-login-error', 'Máximo 14 caracteres');
  if (pass.length < 6)   fail('reg-pass-error',  'La contraseña debe tener al menos 6 caracteres');
  if (pass !== confirm)  fail('reg-confirm-error','Las contraseñas no coinciden');

  if (!isValidEmail(email))       fail('reg-email-error',         'Correo electrónico no válido');
  if (email !== emailCf)          fail('reg-email-confirm-error', 'Los correos no coinciden');
  if (!isValidBirthdayClient(birthday)) fail('reg-birthday-error', 'Ingresa una fecha de nacimiento válida');

  if (!validateRegCaptcha()) valid = false;
  if (!valid) return;

  btn.disabled = true; btn.textContent = 'Creando cuenta...';
  try {
    const res = await api.register(login, pass, email, birthday);
    api.setToken(res.token);
    await fetchCurrentUser();
    closeAllModals();
    showToast(`¡Cuenta "${login}" creada! Descargando tus datos... ⚔️`, 'success');
    navigate('panel');
    downloadAccountCard(login, pass, email, birthday);
  } catch (err) {
    showToast(err.message, 'error');
    generateRegCaptcha();
  } finally {
    btn.disabled = false; btn.textContent = 'CREAR CUENTA';
  }
});

/* ────────── CAPTCHA REFRESH (Registro) ────────── */
document.getElementById('btn-reg-captcha-refresh')?.addEventListener('click', e => {
  e.preventDefault();
  generateRegCaptcha();
  showToast('Nueva pregunta generada', 'info');
});

/* ====================================================================
   CAPTCHA MATEMÁTICO (Recuperación de contraseña)
   ==================================================================== */
let currentRecCaptchaAnswer = 0;

function generateRecCaptcha() {
  const ops = [
    () => { const a = Math.floor(Math.random()*15)+2, b = Math.floor(Math.random()*15)+2; return { q:`¿Cuánto es ${a} + ${b}?`, ans: a+b }; },
    () => { const a = Math.floor(Math.random()*10)+5, b = Math.floor(Math.random()*5)+1;  return { q:`¿Cuánto es ${a} - ${b}?`, ans: a-b }; },
    () => { const a = Math.floor(Math.random()*9)+2,  b = Math.floor(Math.random()*5)+2;  return { q:`¿Cuánto es ${a} × ${b}?`, ans: a*b }; },
  ];
  const { q, ans } = ops[Math.floor(Math.random() * ops.length)]();
  currentRecCaptchaAnswer = ans;
  document.getElementById('rec-captcha-question').textContent = q;
  document.getElementById('rec-captcha').value = '';
  clearFieldErr('rec-captcha-error');
}

function validateRecCaptcha() {
  const val = parseInt(document.getElementById('rec-captcha').value.trim());
  if (isNaN(val)) { fieldErr('rec-captcha-error', 'Por favor ingresa un número'); return false; }
  if (val !== currentRecCaptchaAnswer) {
    fieldErr('rec-captcha-error', 'Respuesta incorrecta, intenta de nuevo');
    generateRecCaptcha(); return false;
  }
  return true;
}

/* ────────── RECOVER MODAL: lógica de dos pasos ────────── */
// Almacena los datos verificados para pasarlos al paso 2
let _recoverVerified = { email: '', birthday: '' };

function resetRecoverModal() {
  _recoverVerified = { email: '', birthday: '' };
  document.getElementById('recover-step1').classList.remove('hidden');
  document.getElementById('recover-step2').classList.add('hidden');
  document.getElementById('form-recover-step1')?.reset();
  document.getElementById('form-recover-step2')?.reset();
  ['rec-email-error','rec-birthday-error','rec-newpass-error',
   'rec-confirm-error','rec-captcha-error'].forEach(clearFieldErr);
}

/* Paso 1 — verificar que email + birthday existen en BD */
document.getElementById('form-recover-step1')?.addEventListener('submit', async e => {
  e.preventDefault();
  const btn      = e.target.querySelector('button[type=submit]');
  const email    = document.getElementById('rec-email').value.trim().toLowerCase();
  const birthday = document.getElementById('rec-birthday').value;

  ['rec-email-error','rec-birthday-error'].forEach(clearFieldErr);
  let valid = true;
  if (!isValidEmail(email))             { fieldErr('rec-email-error',    'Correo no válido'); valid = false; }
  if (!isValidBirthdayClient(birthday)) { fieldErr('rec-birthday-error', 'Fecha no válida');  valid = false; }
  if (!valid) return;

  btn.disabled = true; btn.textContent = 'Verificando...';
  try {
    // Usamos recover-password con una contraseña placeholder para verificar — NO.
    // En realidad hacemos la petición completa en el paso 2. Aquí solo validamos en cliente
    // y guardamos los datos. La verificación real ocurre en el submit del paso 2.
    // Si el servidor devuelve error en paso 2, se mostrará ahí.
    _recoverVerified = { email, birthday };
    document.getElementById('recover-step1').classList.add('hidden');
    document.getElementById('recover-step2').classList.remove('hidden');
    generateRecCaptcha();
  } finally {
    btn.disabled = false; btn.textContent = 'Verificar identidad';
  }
});

/* Paso 2 — establecer nueva contraseña */
document.getElementById('form-recover-step2')?.addEventListener('submit', async e => {
  e.preventDefault();
  const btn     = e.target.querySelector('button[type=submit]');
  const newPass = document.getElementById('rec-newpass').value;
  const confirm = document.getElementById('rec-confirm').value;

  ['rec-newpass-error','rec-confirm-error'].forEach(clearFieldErr);
  let valid = true;
  if (newPass.length < 6)   { fieldErr('rec-newpass-error', 'Mínimo 6 caracteres'); valid = false; }
  if (newPass !== confirm)  { fieldErr('rec-confirm-error', 'Las contraseñas no coinciden'); valid = false; }
  if (!validateRecCaptcha()) valid = false;
  if (!valid) return;

  btn.disabled = true; btn.textContent = 'Guardando...';
  try {
    const res = await api.recoverPassword(_recoverVerified.email, _recoverVerified.birthday, newPass);
    closeAllModals();
    showToast(`Contraseña de "${res.login}" actualizada. Se descargará tu ficha. ✅`, 'success');
    downloadAccountCard(res.login, newPass, _recoverVerified.email, _recoverVerified.birthday, true);
    resetRecoverModal();
  } catch (err) {
    // Si el servidor dice que no coinciden los datos, volver al paso 1
    showToast(err.message, 'error');
    if (err.message.includes('no coinciden') || err.message.includes('ninguna cuenta')) {
      resetRecoverModal();
    } else {
      generateRecCaptcha();
    }
  } finally {
    btn.disabled = false; btn.textContent = 'Guardar nueva contraseña';
  }
});

document.getElementById('btn-rec-captcha-refresh')?.addEventListener('click', e => {
  e.preventDefault(); generateRecCaptcha();
});

/* ────────── LOGIN ────────── */
document.getElementById('form-login')?.addEventListener('submit', async e => {
  e.preventDefault();
  const btn = e.target.querySelector('button[type=submit]');
  const login = document.getElementById('login-user').value.trim();
  const pass  = document.getElementById('login-pass').value;

  // Validar CAPTCHA primero
  if (!validateCaptcha()) {
    return;
  }

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
    generateCaptcha(); // Refrescar CAPTCHA si hay error
  } finally {
    btn.disabled = false; btn.textContent = 'INICIAR SESIÓN';
  }
});

/* ────────── CAPTCHA REFRESH ────────── */
document.getElementById('btn-captcha-refresh')?.addEventListener('click', e => {
  e.preventDefault();
  generateCaptcha();
  showToast('Nueva pregunta generada', 'info');
});

/* ────────── LOGOUT ────────── */
document.getElementById('btn-logout')?.addEventListener('click', () => {
  api.logout();
  currentUser = null;
  updateAuthUI();
  navigate('home');
  showToast('Sesión cerrada correctamente', 'info');
  // Detener monitoreo de inactividad si está activo
  if (window.inactivityManager) {
    window.inactivityManager.stop();
  }
});

/* ====================================================================
   HOME — Server Status
   ==================================================================== */

// Countdown sincronizado para zona PvP
let _pvpCountdownInterval = null;

function startPvpZoneCountdown(el, secondsRemaining) {
  if (_pvpCountdownInterval) clearInterval(_pvpCountdownInterval);

  let secs = secondsRemaining;

  const update = () => {
    if (secs <= 0) {
      clearInterval(_pvpCountdownInterval);
      el.textContent = '00:00';
      el.classList.add('urgent');
      // Recargar status después de 3s para obtener la nueva zona
      setTimeout(() => loadHome(), 3000);
      return;
    }
    const mm = String(Math.floor(secs / 60)).padStart(2, '0');
    const ss = String(secs % 60).padStart(2, '0');
    el.textContent = `${mm}:${ss}`;
    // Último minuto → clase urgente (dorado parpadeante)
    el.classList.toggle('urgent', secs <= 60);
    secs--;
  };

  update();
  _pvpCountdownInterval = setInterval(update, 1000);
}

async function loadHome() {
  try {
    const status = await api.getServerStatus();
    const isOnline = status.gameOnline === true || status.status === 'online';

    document.getElementById('stat-online').textContent   = status.online?.toLocaleString() || '0';
    document.getElementById('stat-accounts').textContent = status.accounts?.toLocaleString() || '0';
    document.getElementById('stat-chars').textContent    = status.characters?.toLocaleString() || '0';

    // Badge del navbar (color + texto)
    const navBadge = document.getElementById('nav-online');
    const navDot   = document.getElementById('nav-status-dot');
    const navLabel = document.getElementById('nav-status-label');
    if (navDot && navLabel) {
      navDot.className   = 'server-status-dot ' + (isOnline ? 'dot-online' : 'dot-offline');
      navLabel.textContent = isOnline
        ? (status.online?.toLocaleString() || '0') + ' online'
        : 'Offline';
    }
    if (navBadge) {
      navBadge.classList.toggle('badge-offline', !isOnline);
    }

    // Indicador en hero stats
    const heroDot   = document.getElementById('hero-status-dot');
    const heroLabel = document.getElementById('hero-status-label');
    if (heroDot && heroLabel) {
      heroDot.className    = 'server-status-dot ' + (isOnline ? 'dot-online' : 'dot-offline');
      heroLabel.textContent = isOnline ? 'Online' : 'Offline';
    }

    // Zona PvP activa con countdown sincronizado al servidor
    const pvpZoneEl  = document.getElementById('home-pvpzone');
    const pvpTimerEl = document.getElementById('home-pvpzone-timer');
    if (pvpZoneEl && status.pvpZone) {
      pvpZoneEl.textContent = status.pvpZone.name || '—';
    }
    if (pvpTimerEl && status.pvpZone?.nextRotationIn > 0) {
      startPvpZoneCountdown(pvpTimerEl, status.pvpZone.nextRotationIn);
    }
  } catch (err) {
    console.warn('Server status:', err.message);
    // Error de red → mostrar offline
    ['nav-status-dot','hero-status-dot'].forEach(id => {
      const el = document.getElementById(id);
      if (el) el.className = 'server-status-dot dot-offline';
    });
    const navLbl = document.getElementById('nav-status-label');
    const heroLbl = document.getElementById('hero-status-label');
    if (navLbl)  navLbl.textContent  = 'Offline';
    if (heroLbl) heroLbl.textContent = 'Offline';
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

  // Limpiar panel en-zona si cambiamos de tab
  const inZonePanel = document.getElementById('pvpzone-in-zone-panel');
  if (inZonePanel) inZonePanel.style.display = (type === 'pvpzone') ? '' : 'none';

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

    if (type === 'pvpzone') {
      // Nueva respuesta: { zoneName, nextRotationIn, playersInZone, ranking }
      renderPvpZoneTab(data);
    } else {
      if (!data.length) {
        tbody.innerHTML = '<tr><td colspan="7" class="text-center text-muted" style="padding:2rem">Sin datos disponibles</td></tr>';
        return;
      }
      tbody.innerHTML = data.map(row => renderRankingRow(type, row)).join('');
    }
  } catch (err) {
    tbody.innerHTML = `<tr><td colspan="7" class="text-center text-red" style="padding:2rem">Error: ${err.message}</td></tr>`;
  }
}

function renderPvpZoneTab(data) {
  // Actualizar widget nombre zona + countdown
  const nameEl = document.getElementById('ranking-pvpzone');
  const timerEl = document.getElementById('ranking-pvpzone-players');
  if (nameEl) nameEl.textContent = data.zoneName || '—';
  if (timerEl && data.nextRotationIn > 0) startPvpZoneCountdown(timerEl, data.nextRotationIn);

  // Panel de jugadores actualmente en zona
  const inZonePanel = document.getElementById('pvpzone-in-zone-panel');
  if (inZonePanel) {
    inZonePanel.style.display = '';
    const countEl = document.getElementById('pvpzone-in-zone-count');
    const listEl  = document.getElementById('pvpzone-in-zone-list');
    if (countEl) countEl.textContent = data.playersInZoneCount || 0;

    if (listEl) {
      if (!data.playersInZone || data.playersInZone.length === 0) {
        listEl.innerHTML = `<div class="in-zone-empty">Sin jugadores en la zona ahora mismo</div>`;
      } else {
        listEl.innerHTML = data.playersInZone.map(p => `
          <div class="in-zone-player">
            <span class="online-dot online" style="margin-right:.4rem"></span>
            <span class="in-zone-name">${p.char_name}</span>
            <span class="in-zone-class">${p.className}</span>
            <span class="in-zone-lvl">Nv.${p.level}</span>
            ${p.clan_name ? `<span class="in-zone-clan">[${p.clan_name}]</span>` : ''}
          </div>`).join('');
      }
    }
  }

  // Tabla de ranking de kills
  const tbody = document.getElementById('ranking-tbody');
  const ranking = data.ranking || [];
  if (!ranking.length) {
    tbody.innerHTML = '<tr><td colspan="7" class="text-center text-muted" style="padding:2rem">Aún no hay kills registradas en esta zona</td></tr>';
    return;
  }
  tbody.innerHTML = ranking.map(row => renderRankingRow('pvpzone', row)).join('');
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

/* ── Cart state ─────────────────────────────────────────────────── */
// cart = Map<shopItemId, { item: shopItemObj, qty: number }>
const cart = new Map();

function cartCount() {
  let n = 0;
  cart.forEach(v => n += v.qty);
  return n;
}
function cartTotal() {
  let t = 0;
  cart.forEach(v => t += (v.item.price_coins || 0) * v.qty);
  return t;
}
function updateCartBadge() {
  const n = cartCount();
  const badge = document.getElementById('cart-badge');
  if (badge) { badge.textContent = n; badge.style.display = n ? 'inline-flex' : 'none'; }
}

async function addToCart(item) {
  if (!api.isLoggedIn) { showToast('Inicia sesión para comprar', 'warning'); return; }

  const maxQty   = item.stock !== null ? item.stock : 999;
  const current  = cart.get(item.id)?.qty || 0;
  const available = maxQty - current;

  if (available <= 0) { showToast(`Stock máximo alcanzado (${maxQty})`, 'warning'); return; }

  // ── Pedir cantidad ───────────────────────────────────────────────
  const qty = await showQtyModal(item, available);
  if (!qty) return; // canceló
  // ────────────────────────────────────────────────────────────────

  const entry = cart.get(item.id);
  if (entry) {
    entry.qty = Math.min(entry.qty + qty, maxQty);
  } else {
    cart.set(item.id, { item, qty });
  }
  updateCartBadge();
  showToast(`${item.name} x${qty} agregado al carrito`, 'success', '🛒');
  renderCart();
}
window.addToCart = addToCart;

/* ── Modal: pedir cantidad antes de agregar al carrito ───────────
   Devuelve Promise<number|null> (null = canceló)
───────────────────────────────────────────────────────────────── */
function showQtyModal(item, maxAvailable) {
  return new Promise(resolve => {
    const existing = document.getElementById('modal-qty-dynamic');
    if (existing) existing.remove();

    const price = item.price_coins || 0;
    const modal = document.createElement('div');
    modal.id = 'modal-qty-dynamic';
    modal.className = 'modal-overlay open';
    modal.innerHTML = `
      <div class="modal-box" style="max-width:360px">
        <h3 style="margin-bottom:.25rem;font-size:1.05rem">🛒 Agregar al Carrito</h3>
        <p style="color:var(--text-muted);font-size:.9rem;margin-bottom:1.2rem">${item.name}</p>
        <div style="display:flex;align-items:center;gap:.5rem;justify-content:center;margin-bottom:1rem">
          <button class="cart-qty-btn" id="qm-minus">−</button>
          <input id="qm-input" type="number" min="1" max="${maxAvailable}" value="1"
            class="cart-qty-input" style="width:64px;text-align:center;font-size:1.1rem">
          <button class="cart-qty-btn" id="qm-plus">+</button>
        </div>
        <div id="qm-subtotal" style="text-align:center;font-size:.95rem;color:var(--gold);margin-bottom:1.4rem">
          🪙 ${price.toLocaleString()} WebCoins
        </div>
        <div class="modal-actions" style="display:flex;gap:.75rem;justify-content:flex-end">
          <button class="btn btn-secondary" id="qm-cancel">Cancelar</button>
          <button class="btn btn-gold" id="qm-ok">✅ Agregar</button>
        </div>
      </div>`;

    document.body.appendChild(modal);

    const input    = modal.querySelector('#qm-input');
    const subtotal = modal.querySelector('#qm-subtotal');

    const updateSubtotal = () => {
      const q = Math.max(1, Math.min(parseInt(input.value)||1, maxAvailable));
      input.value = q;
      subtotal.textContent = `🪙 ${(price * q).toLocaleString()} WebCoins`;
    };

    modal.querySelector('#qm-minus').addEventListener('click', () => { input.value = Math.max(1, (parseInt(input.value)||1) - 1); updateSubtotal(); });
    modal.querySelector('#qm-plus').addEventListener('click',  () => { input.value = Math.min(maxAvailable, (parseInt(input.value)||1) + 1); updateSubtotal(); });
    input.addEventListener('input', updateSubtotal);

    // Enter confirma
    input.addEventListener('keydown', e => { if (e.key === 'Enter') modal.querySelector('#qm-ok').click(); });

    const cleanup = (result) => { modal.remove(); resolve(result); };
    modal.querySelector('#qm-ok').addEventListener('click', () => {
      const q = Math.max(1, Math.min(parseInt(input.value)||1, maxAvailable));
      cleanup(q);
    });
    modal.querySelector('#qm-cancel').addEventListener('click', () => cleanup(null));
    modal.addEventListener('click', e => { if (e.target === modal) cleanup(null); });

    // Foco automático
    setTimeout(() => input.focus(), 50);
  });
}

function removeFromCart(itemId) {
  cart.delete(itemId);
  updateCartBadge();
  renderCart();
}
window.removeFromCart = removeFromCart;

function setCartQty(itemId, qty) {
  const entry = cart.get(itemId);
  if (!entry) return;
  const q = Math.max(1, Math.min(999, parseInt(qty) || 1));
  const maxQty = entry.item.stock !== null ? entry.item.stock : 999;
  entry.qty = Math.min(q, maxQty);
  updateCartBadge();
  renderCart();
}
window.setCartQty = setCartQty;

function clearCart() {
  cart.clear();
  updateCartBadge();
  renderCart();
}
window.clearCart = clearCart;

function openCart() {
  renderCart();
  document.getElementById('cart-overlay').classList.add('open');
  document.getElementById('cart-drawer').classList.add('open');
}
window.openCart = openCart;

function closeCart() {
  document.getElementById('cart-overlay').classList.remove('open');
  document.getElementById('cart-drawer').classList.remove('open');
}
window.closeCart = closeCart;

function renderCart() {
  const body   = document.getElementById('cart-body');
  const total  = document.getElementById('cart-total');
  const balEl  = document.getElementById('cart-balance');
  const btn    = document.getElementById('btn-checkout');
  if (!body) return;

  // Balance actualizado
  const balance = parseInt(document.getElementById('shop-coins')?.textContent?.replace(/\D/g,'')) || 0;
  if (balEl) balEl.textContent = balance.toLocaleString();

  if (!cart.size) {
    body.innerHTML = '<div class="cart-empty">Tu carrito está vacío.<br>Agrega ítems desde la tienda.</div>';
    if (total) total.textContent = '0';
    if (btn)   btn.disabled = true;
    return;
  }

  let html = '';
  cart.forEach(({ item, qty }) => {
    const subtotal = (item.price_coins || 0) * qty;
    const maxQty   = item.stock !== null ? item.stock : 999;
    html += `
      <div class="cart-item">
        <div class="cart-item-info">
          <div class="cart-item-name">${escHtml(item.name)}</div>
          <div class="cart-item-price">🪙 ${(item.price_coins||0).toLocaleString()} c/u · Sub: ${subtotal.toLocaleString()}</div>
        </div>
        <div class="cart-item-controls">
          <button class="cart-qty-btn" onclick="setCartQty(${item.id}, ${qty-1})" ${qty<=1?'disabled':''}>−</button>
          <input class="cart-qty-input" type="number" min="1" max="${maxQty}" value="${qty}"
            onchange="setCartQty(${item.id}, this.value)"
            oninput="setCartQty(${item.id}, this.value)">
          <button class="cart-qty-btn" onclick="setCartQty(${item.id}, ${qty+1})" ${qty>=maxQty?'disabled':''}>+</button>
          <button class="cart-remove-btn" onclick="removeFromCart(${item.id})" title="Eliminar">🗑</button>
        </div>
      </div>`;
  });
  body.innerHTML = html;

  const t = cartTotal();
  if (total) total.textContent = t.toLocaleString();
  if (btn)   btn.disabled = (t > balance);
  if (btn)   btn.title = t > balance ? 'Saldo insuficiente' : '';
}

async function cartCheckout() {
  if (!cart.size) { showToast('El carrito está vacío', 'warning'); return; }

  selectedCharacter = document.getElementById('shop-char-select')?.value || selectedCharacter;
  if (!selectedCharacter) { showToast('Selecciona un personaje primero', 'warning'); return; }

  const total = cartTotal();
  const balance = parseInt(document.getElementById('shop-coins')?.textContent?.replace(/\D/g,'')) || 0;
  if (total > balance) { showToast('Saldo insuficiente', 'error'); return; }

  // ── Confirmación previa ──────────────────────────────────────────
  const lines = [];
  cart.forEach(({ item, qty }) => lines.push(`• ${item.name} x${qty} — 🪙 ${((item.price_coins||0)*qty).toLocaleString()}`));
  const confirmHtml = `
    <div style="margin-bottom:1rem;font-size:.95rem;color:var(--text-muted)">
      Personaje: <strong>${selectedCharacter}</strong>
    </div>
    <div style="max-height:180px;overflow-y:auto;margin-bottom:1rem;font-size:.9rem;line-height:1.8">
      ${lines.join('<br>')}
    </div>
    <div style="font-size:1.05rem;font-weight:700;color:var(--gold)">
      Total: 🪙 ${total.toLocaleString()} WebCoins
    </div>`;

  const confirmed = await showConfirmModal('¿Confirmar compra?', confirmHtml, '✅ Comprar', '❌ Cancelar');
  if (!confirmed) return;
  // ────────────────────────────────────────────────────────────────

  const items = [];
  cart.forEach(({ qty }, id) => items.push({ id, qty }));

  const btn = document.getElementById('btn-checkout');
  if (btn) { btn.disabled = true; btn.textContent = '⏳ Procesando...'; }

  try {
    const res = await api.cartCheckout(selectedCharacter, items);
    showToast(res.message, 'success', '🎁');
    clearCart();
    closeCart();
    loadShop(); // refrescar balance
  } catch (err) {
    showToast(err.message, 'error');
  } finally {
    if (btn) { btn.disabled = false; btn.textContent = '✅ Confirmar compra'; }
  }
}
window.cartCheckout = cartCheckout;

/* ── Helper: modal de confirmación genérico ──────────────────────
   Devuelve una Promise<boolean>. Reutiliza el modal #modal-confirm
   si existe, o crea uno temporal.
───────────────────────────────────────────────────────────────── */
function showConfirmModal(title, bodyHtml, okLabel = 'Confirmar', cancelLabel = 'Cancelar') {
  return new Promise(resolve => {
    // Crear modal dinámico
    const existing = document.getElementById('modal-confirm-dynamic');
    if (existing) existing.remove();

    const modal = document.createElement('div');
    modal.id = 'modal-confirm-dynamic';
    modal.className = 'modal-overlay open';
    modal.innerHTML = `
      <div class="modal-box" style="max-width:420px">
        <h3 style="margin-bottom:1.2rem;font-size:1.15rem">${title}</h3>
        <div>${bodyHtml}</div>
        <div class="modal-actions" style="margin-top:1.5rem;display:flex;gap:.75rem;justify-content:flex-end">
          <button class="btn btn-secondary" id="modal-confirm-cancel">${cancelLabel}</button>
          <button class="btn btn-gold" id="modal-confirm-ok">${okLabel}</button>
        </div>
      </div>`;

    document.body.appendChild(modal);

    const cleanup = (result) => { modal.remove(); resolve(result); };
    modal.querySelector('#modal-confirm-ok').addEventListener('click', () => cleanup(true));
    modal.querySelector('#modal-confirm-cancel').addEventListener('click', () => cleanup(false));
    modal.addEventListener('click', e => { if (e.target === modal) cleanup(false); });
  });
}

/* ── Shop loader ─────────────────────────────────────────────────── */
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

    // Balance
    const balEl = document.getElementById('shop-coins');
    if (balEl) balEl.textContent = (balance.coins || 0).toLocaleString();
    updateCartBadge();

    // Selector personaje
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
    grid.innerHTML = items.map(item => {
      const inCart = cart.get(item.id);
      return `
      <div class="shop-item ${item.featured ? 'featured' : ''}">
        <div class="shop-item-img">
          ${item.image_url
            ? `<img src="${escHtml(item.image_url)}" alt="${escHtml(item.name)}" style="max-height:100%;max-width:100%;object-fit:contain">`
            : (EMOJIS[item.category] || '⚔️')}
          ${item.featured ? '<span class="shop-featured-badge">DESTACADO</span>' : ''}
        </div>
        <div class="shop-item-body">
          <div class="shop-item-name">${escHtml(item.name)}</div>
          <div class="shop-item-desc">${escHtml(item.description || '')}</div>
          <div class="shop-item-footer">
            <div class="shop-price"><span class="coin-icon">🪙</span> ${(item.price_coins||0).toLocaleString()}</div>
            ${item.stock !== null ? `<span class="shop-item-stock">📦 ${item.stock} disp.</span>` : ''}
            <button class="btn btn-gold btn-sm shop-add-btn" onclick='addToCart(${JSON.stringify(item)})'>
              ${inCart ? `🛒 En carrito (${inCart.qty})` : '🛒 Agregar al Carrito'}
            </button>
          </div>
        </div>
      </div>`;
    }).join('');
  } catch (err) {
    grid.innerHTML = `<div class="text-center text-red" style="padding:3rem">Error: ${err.message}</div>`;
  }
}

// Mantener buyItem como alias para compatibilidad
async function buyItem(itemId, itemName, price) {
  showToast('Usa el carrito para comprar', 'info', '🛒');
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

    // Refrescar saldo en tiempo real desde el backend
    // (web_coins ya viene en /auth/me pero lo re-consultamos para tener el valor más fresco)
    api.getShopBalance().then(b => {
      const coins = b.coins || 0;
      // Sincronizar todos los elementos que muestran monedas
      ['panel-coins', 'shop-coins', 'recharge-coins'].forEach(id => {
        const el = document.getElementById(id);
        if (el) el.textContent = coins.toLocaleString();
      });
      // Actualizar también en currentUser para que renderPanelAccount use el valor fresco
      if (currentUser?.account) currentUser.account.web_coins = coins;
    }).catch(() => {});

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

  // Mostrar monedas que vienen en /auth/me (valor inicial; loadPanel las actualiza en vivo)
  if (acc.web_coins != null) {
    ['panel-coins', 'shop-coins'].forEach(id => {
      const el = document.getElementById(id);
      if (el) el.textContent = Number(acc.web_coins).toLocaleString();
    });
  }
}

function renderPanelChars() {
  const grid = document.getElementById('panel-chars');
  if (!grid) return;
  const chars = currentUser?.characters || [];

  if (!chars.length) {
    grid.innerHTML = '<div class="text-muted text-center" style="padding:2rem">Sin personajes. ¡Crea uno en el juego!</div>';
    return;
  }

  // Mapa de notificaciones pvp activas por char_name
  const pvpNotifs = {};
  (currentUser?.pvp_notifications || []).forEach(n => {
    pvpNotifs[n.char_name] = n;
  });

  const raceEmojis = { 0:'🧑', 1:'🧝', 2:'🧙', 3:'👹', 4:'⚒️', 5:'👁️' };
  grid.innerHTML = chars.map(c => {
    const notif = pvpNotifs[c.char_name];
    const notifHtml = notif ? `
      <div class="pvp-reward-notif" id="pvp-notif-${escHtml(c.char_name).replace(/\s/g,'_')}">
        <div class="pvp-reward-notif__inner">
          <div class="pvp-reward-notif__icon">🏆</div>
          <div class="pvp-reward-notif__text">
            <span class="pvp-reward-notif__title">¡Felicitaciones, <strong>${escHtml(c.char_name)}</strong>!</span>
            <span class="pvp-reward-notif__detail">
              Ganaste <strong class="pvp-reward-notif__coins">🪙 ${Number(notif.coins_awarded).toLocaleString()} WebCoins</strong>
              por ser Top en <em>${escHtml(notif.zone_name)}</em>
            </span>
          </div>
          <button class="pvp-reward-notif__close" onclick="dismissPvpNotif('${escHtml(c.char_name)}')" title="Cerrar">✕</button>
        </div>
      </div>` : '';

    return `
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
      ${notifHtml}
    </div>`;
  }).join('');
}

/** Cierra/descarta la notificación PvP de un personaje */
async function dismissPvpNotif(charName) {
  // Ocultar visualmente de inmediato
  const safeId = charName.replace(/\s/g,'_');
  const el = document.getElementById(`pvp-notif-${safeId}`);
  if (el) {
    el.style.transition = 'opacity .3s, max-height .4s';
    el.style.opacity    = '0';
    el.style.maxHeight  = '0';
    el.style.overflow   = 'hidden';
    setTimeout(() => el.remove(), 400);
  }

  // Marcar como leída en el servidor
  try {
    await api.dismissPvpNotif(charName);
    // Actualizar estado local
    if (currentUser?.pvp_notifications) {
      currentUser.pvp_notifications = currentUser.pvp_notifications.filter(
        n => n.char_name !== charName
      );
    }
  } catch { /* silencioso */ }
}
window.dismissPvpNotif = dismissPvpNotif;

/* ────────── Change password form (Panel) ────────── */
document.getElementById('form-change-pass')?.addEventListener('submit', async e => {
  e.preventDefault();
  const btn      = e.target.querySelector('button[type=submit]');
  const email    = document.getElementById('cp-email').value.trim().toLowerCase();
  const birthday = document.getElementById('cp-birthday').value;
  const cur      = document.getElementById('cp-current').value;
  const nw       = document.getElementById('cp-new').value;
  const cf       = document.getElementById('cp-confirm').value;

  // Limpiar errores
  ['cp-email-error','cp-birthday-error','cp-current-error','cp-new-error','cp-confirm-error'].forEach(clearFieldErr);
  let valid = true;
  const fail = (id, msg) => { fieldErr(id, msg); valid = false; };

  if (!isValidEmail(email))             fail('cp-email-error',    'Correo no válido');
  if (!isValidBirthdayClient(birthday)) fail('cp-birthday-error', 'Fecha no válida');
  if (!cur)                             fail('cp-current-error',  'Ingresa tu contraseña actual');
  if (nw.length < 6)                    fail('cp-new-error',      'Mínimo 6 caracteres');
  if (nw === cur)                       fail('cp-new-error',      'La nueva contraseña no puede ser igual a la actual');
  if (nw !== cf)                        fail('cp-confirm-error',  'Las contraseñas no coinciden');
  if (!valid) return;

  btn.disabled = true; btn.textContent = 'Verificando...';
  try {
    await api.changePassword(cur, email, birthday, nw);
    showToast('Contraseña actualizada correctamente ✅', 'success');
    e.target.reset();
    // Descargar ficha actualizada
    const login = currentUser?.account?.login || 'cuenta';
    downloadAccountCard(login, nw, email, birthday, false, true);
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
   ADMIN PANEL
   ==================================================================== */

// Estado del panel de admin
let adminCurrentPage = 0;
let adminCurrentStatus = 'all';
let adminCurrentCoinAction = 'add';
const ADMIN_PAGE_SIZE = 50;

/** Carga inicial del panel admin */
function loadAdmin() {
  if (!currentUser?.account || currentUser.account.accessLevel < 100) {
    showToast('Acceso denegado', 'error');
    navigate('home');
    return;
  }
  // Tab inicial: usuarios
  document.querySelectorAll('[data-admin-tab]').forEach(b => b.classList.remove('active'));
  document.querySelector('[data-admin-tab="users"]')?.classList.add('active');
  document.getElementById('admin-tab-users')?.classList.remove('hidden');
  document.getElementById('admin-tab-payments')?.classList.add('hidden');
  document.getElementById('admin-tab-shop')?.classList.add('hidden');
  document.getElementById('admin-tab-pvpreward')?.classList.add('hidden');
}

/** Tabs del panel admin */
document.querySelectorAll('[data-admin-tab]').forEach(btn => {
  btn.addEventListener('click', () => {
    const tab = btn.dataset.adminTab;
    document.querySelectorAll('[data-admin-tab]').forEach(b => b.classList.remove('active'));
    btn.classList.add('active');
    document.getElementById('admin-tab-users')?.classList.toggle('hidden', tab !== 'users');
    document.getElementById('admin-tab-payments')?.classList.toggle('hidden', tab !== 'payments');
    document.getElementById('admin-tab-shop')?.classList.toggle('hidden', tab !== 'shop');
    document.getElementById('admin-tab-pvpreward')?.classList.toggle('hidden', tab !== 'pvpreward');
    if (tab === 'payments') {
      adminCurrentPage = 0;
      loadAdminPayments(adminCurrentStatus);
    }
    if (tab === 'shop') {
      loadAdminShop();
    }
    if (tab === 'pvpreward') {
      loadAdminPvpReward();
    }
  });
});

/** Búsqueda de usuarios admin */
async function searchAdminUsers() {
  const q = document.getElementById('admin-search-input')?.value.trim();
  if (!q) { showToast('Escribe un nombre para buscar', 'warning'); return; }

  const tbody = document.getElementById('admin-users-tbody');
  tbody.innerHTML = `<tr><td colspan="7" class="text-center"><div class="spinner" style="margin:1.5rem auto"></div></td></tr>`;

  try {
    const { users } = await api.adminSearchUsers(q);
    if (!users.length) {
      tbody.innerHTML = `<tr><td colspan="7" class="text-center text-muted" style="padding:2rem">Sin resultados para "${escHtml(q)}"</td></tr>`;
      return;
    }
    tbody.innerHTML = users.map(u => {
      const banStatus = u.ban_until
        ? (parseInt(u.ban_until) >= 9999999990
          ? `<span class="ban-active">🔨 Permanente</span>`
          : `<span class="ban-active">🔨 ${new Date(parseInt(u.ban_until)*1000).toLocaleDateString('es-AR')}</span>`)
        : `<span class="ban-none">✅ Libre</span>`;
      const levelBadge = u.accessLevel >= 100
        ? `<span class="badge-status approved">${u.accessLevel} Admin</span>`
        : `<span style="color:var(--text-muted)">${u.accessLevel}</span>`;
      return `<tr>
        <td><strong>${escHtml(u.login)}</strong></td>
        <td style="color:var(--text-dim)">${escHtml(u.email||'—')}</td>
        <td>${levelBadge}</td>
        <td><strong style="color:var(--cyan)">🪙 ${(u.web_coins||0).toLocaleString()}</strong></td>
        <td>${banStatus}</td>
        <td style="color:var(--text-muted);font-size:.82rem">${formatDate(u.last_login)}</td>
        <td><button class="btn btn-sm btn-secondary" onclick="openAdminEditModal('${escHtml(u.login)}')">✏️ Editar</button></td>
      </tr>`;
    }).join('');
  } catch (err) {
    tbody.innerHTML = `<tr><td colspan="7" class="text-center" style="color:var(--red)">Error: ${escHtml(err.message)}</td></tr>`;
  }
}

// Enter key en el buscador
document.getElementById('admin-search-input')?.addEventListener('keydown', e => {
  if (e.key === 'Enter') searchAdminUsers();
});

/** Abre modal de edición con datos del usuario */
async function openAdminEditModal(login) {
  try {
    const { user } = await api.adminGetUser(login);
    document.getElementById('admin-edit-login-title').textContent = user.login;
    document.getElementById('admin-edit-login-value').value = user.login;
    document.getElementById('auc-coins').textContent = `🪙 ${(user.web_coins||0).toLocaleString()}`;
    document.getElementById('auc-level').textContent = user.accessLevel;

    const banEl = document.getElementById('auc-ban');
    if (!user.ban_until) {
      banEl.textContent = '✅ Sin ban';
      banEl.style.color = 'var(--green)';
    } else if (parseInt(user.ban_until) >= 9999999990) {
      banEl.textContent = '🔨 Ban permanente';
      banEl.style.color = 'var(--red)';
    } else {
      banEl.textContent = `🔨 Hasta ${new Date(parseInt(user.ban_until)*1000).toLocaleDateString('es-AR')}`;
      banEl.style.color = 'var(--red)';
    }

    document.getElementById('admin-edit-email').value = user.email || '';
    document.getElementById('admin-edit-password').value = '';
    document.getElementById('admin-coins-result').textContent = '';

    // Reset coin action
    selectCoinAction('add', document.querySelector('[data-action="add"]'));

    openModal('admin-edit');
  } catch (err) {
    showToast('Error al cargar usuario: ' + err.message, 'error');
  }
}

/** Selecciona la acción de coins (add/subtract/set) */
function selectCoinAction(action, btn) {
  adminCurrentCoinAction = action;
  document.querySelectorAll('.coin-action-btn').forEach(b => b.classList.remove('active'));
  btn?.classList.add('active');
}

/** Guarda el ajuste de coins */
async function adminSaveCoins() {
  const login  = document.getElementById('admin-edit-login-value').value;
  const amount = parseInt(document.getElementById('admin-coins-amount').value);
  const resultEl = document.getElementById('admin-coins-result');

  if (!login || isNaN(amount) || amount < 0) {
    resultEl.textContent = '⚠️ Cantidad inválida';
    return;
  }

  try {
    const { web_coins } = await api.adminUpdateCoins(login, adminCurrentCoinAction, amount);
    resultEl.textContent = `✅ Saldo actualizado: 🪙 ${web_coins.toLocaleString()} coins`;
    document.getElementById('auc-coins').textContent = `🪙 ${web_coins.toLocaleString()}`;
    showToast(`Coins de ${login} actualizados a ${web_coins}`, 'success');
    // Refrescar tabla si está visible
    const q = document.getElementById('admin-search-input')?.value.trim();
    if (q) searchAdminUsers();
  } catch (err) {
    resultEl.textContent = '❌ ' + err.message;
    showToast(err.message, 'error');
  }
}

/** Guarda email y/o contraseña */
async function adminSaveAccount() {
  const login    = document.getElementById('admin-edit-login-value').value;
  const email    = document.getElementById('admin-edit-email').value.trim();
  const password = document.getElementById('admin-edit-password').value;

  if (!email && !password) {
    showToast('No hay cambios que guardar', 'warning');
    return;
  }

  const payload = {};
  if (email)    payload.email    = email;
  if (password) payload.password = password;

  try {
    const { changes } = await api.adminUpdateUser(login, payload);
    showToast(`✅ Actualizado: ${changes.join(', ')}`, 'success');
    if (password) document.getElementById('admin-edit-password').value = '';
  } catch (err) {
    showToast(err.message, 'error');
  }
}

/** Banea al usuario */
async function adminBanUser() {
  const login = document.getElementById('admin-edit-login-value').value;
  const days  = parseInt(document.getElementById('admin-ban-days').value) || 0;
  const label = days === 0 ? 'permanente' : `${days} días`;

  if (!confirm(`¿Confirmas banear a "${login}" (${label})?`)) return;

  try {
    await api.adminUpdateUser(login, { ban: { days } });
    showToast(`🔨 ${login} baneado (${label})`, 'success');
    // Actualizar info card
    const banEl = document.getElementById('auc-ban');
    if (days === 0) {
      banEl.textContent = '🔨 Ban permanente'; banEl.style.color = 'var(--red)';
    } else {
      const d = new Date(Date.now() + days*86400000);
      banEl.textContent = `🔨 Hasta ${d.toLocaleDateString('es-AR')}`; banEl.style.color = 'var(--red)';
    }
    const q = document.getElementById('admin-search-input')?.value.trim();
    if (q) searchAdminUsers();
  } catch (err) {
    showToast(err.message, 'error');
  }
}

/** Desbanea al usuario */
async function adminUnbanUser() {
  const login = document.getElementById('admin-edit-login-value').value;
  if (!confirm(`¿Confirmas remover el ban de "${login}"?`)) return;

  try {
    await api.adminUpdateUser(login, { ban: null });
    showToast(`✅ Ban removido de ${login}`, 'success');
    const banEl = document.getElementById('auc-ban');
    banEl.textContent = '✅ Sin ban'; banEl.style.color = 'var(--green)';
    const q = document.getElementById('admin-search-input')?.value.trim();
    if (q) searchAdminUsers();
  } catch (err) {
    showToast(err.message, 'error');
  }
}

/** Carga la tabla de compras */
async function loadAdminPayments(status) {
  adminCurrentStatus = status || 'all';
  const tbody   = document.getElementById('admin-payments-tbody');
  const totalEl = document.getElementById('admin-payments-total');
  const pageEl  = document.getElementById('admin-payments-page');

  // Resaltar botón activo
  document.querySelectorAll('.admin-status-btn').forEach(b => {
    b.classList.toggle('active', b.dataset.status === adminCurrentStatus);
  });

  tbody.innerHTML = `<tr><td colspan="8" class="text-center"><div class="spinner" style="margin:2rem auto"></div></td></tr>`;

  try {
    const { payments, total } = await api.adminGetPayments(adminCurrentStatus, ADMIN_PAGE_SIZE, adminCurrentPage * ADMIN_PAGE_SIZE);

    totalEl.textContent = `Total: ${total} registro${total !== 1 ? 's' : ''}`;
    pageEl.textContent  = `Página ${adminCurrentPage + 1}`;

    document.getElementById('btn-admin-payments-prev').disabled = adminCurrentPage === 0;
    document.getElementById('btn-admin-payments-next').disabled = (adminCurrentPage + 1) * ADMIN_PAGE_SIZE >= total;

    if (!payments.length) {
      tbody.innerHTML = `<tr><td colspan="8" class="text-center text-muted" style="padding:2rem">Sin registros</td></tr>`;
      return;
    }

    const providerIcon = { mercadopago: '💳', paypal: '🅿️' };
    tbody.innerHTML = payments.map(p => {
      const statusCls = { approved:'approved', pending:'pending', rejected:'rejected', cancelled:'cancelled', refunded:'refunded' };
      const statusLabel = { approved:'✅ Aprobado', pending:'⏳ Pendiente', rejected:'❌ Rechazado', cancelled:'🚫 Cancelado', refunded:'↩️ Reembolsado' };
      return `<tr>
        <td style="color:var(--text-muted);font-size:.82rem">#${p.id}</td>
        <td><strong>${escHtml(p.account_name)}</strong></td>
        <td style="font-size:.85rem">${escHtml(p.package_name || '—')}</td>
        <td style="color:var(--cyan)">🪙 ${p.coins.toLocaleString()}</td>
        <td>${p.currency === 'USD' ? '$' : 'ARS$'} ${parseFloat(p.amount).toLocaleString()}</td>
        <td>${providerIcon[p.provider] || '?'} ${p.provider}</td>
        <td><span class="badge-status ${statusCls[p.status]||''}">${statusLabel[p.status]||p.status}</span></td>
        <td style="color:var(--text-muted);font-size:.8rem">${formatDate(p.created_at)}</td>
      </tr>`;
    }).join('');
  } catch (err) {
    tbody.innerHTML = `<tr><td colspan="8" class="text-center" style="color:var(--red)">Error: ${escHtml(err.message)}</td></tr>`;
  }
}

/** Paginación de compras */
function adminPaymentsPage(delta) {
  adminCurrentPage = Math.max(0, adminCurrentPage + delta);
  loadAdminPayments(adminCurrentStatus);
}

// Filtros de estado en tabla de compras
document.querySelectorAll('.admin-status-btn').forEach(btn => {
  btn.addEventListener('click', () => {
    adminCurrentPage = 0;
    loadAdminPayments(btn.dataset.status);
  });
});

/* ====================================================================
   ADMIN SHOP EDITOR
   ==================================================================== */

let adminShopEditingId = null;

/** Activa/desactiva el input de stock según el checkbox Ilimitado */
function toggleStockUnlimited(cb) {
  const input = document.getElementById('asi-stock');
  input.disabled = cb.checked;
  if (cb.checked) input.value = '';
  else { input.focus(); }
}
window.toggleStockUnlimited = toggleStockUnlimited;

/** Carga la tabla de items de la tienda en el panel admin */
async function loadAdminShop() {
  const tbody = document.getElementById('admin-shop-tbody');
  tbody.innerHTML = `<tr><td colspan="9" class="text-center"><div class="spinner" style="margin:2rem auto"></div></td></tr>`;
  try {
    const { items } = await api.adminGetShopItems();
    if (!items.length) {
      tbody.innerHTML = `<tr><td colspan="9" class="text-center text-muted" style="padding:2rem">No hay items en la tienda</td></tr>`;
      return;
    }
    tbody.innerHTML = items.map(item => {
      const statusBadge = item.active
        ? `<span class="badge-status approved">✅ Activo</span>`
        : `<span class="badge-status cancelled">⛔ Inactivo</span>`;
      const stockLabel = item.stock === null ? '∞' : item.stock;
      const featuredIcon = item.featured ? '⭐ ' : '';
      return `<tr>
        <td style="color:var(--text-muted);font-size:.82rem">${item.id}</td>
        <td><strong>${featuredIcon}${escHtml(item.name)}</strong></td>
        <td style="color:var(--cyan);font-family:monospace">${item.item_id}</td>
        <td style="text-align:center">${item.item_count}</td>
        <td style="color:var(--gold)">🪙 ${(item.price_coins||0).toLocaleString()}</td>
        <td style="color:var(--text-muted);font-size:.85rem">${escHtml(item.category||'general')}</td>
        <td style="text-align:center">${stockLabel}</td>
        <td>${statusBadge}</td>
        <td>
          <div style="display:flex;gap:.4rem">
            <button class="btn btn-sm btn-secondary" onclick="openAdminShopModal(${item.id})" title="Editar">✏️</button>
            <button class="btn btn-sm" style="background:rgba(0,212,255,.1);border:1px solid rgba(0,212,255,.3);color:var(--cyan)"
              onclick="toggleAdminShopItem(${item.id}, ${item.active})" title="${item.active ? 'Desactivar' : 'Activar'}">${item.active ? '👁' : '👁‍🗨'}</button>
            <button class="btn btn-sm btn-danger" onclick="deleteAdminShopItem(${item.id}, '${escHtml(item.name)}')" title="Eliminar">🗑</button>
          </div>
        </td>
      </tr>`;
    }).join('');
  } catch (err) {
    tbody.innerHTML = `<tr><td colspan="9" class="text-center" style="color:var(--red)">Error: ${escHtml(err.message)}</td></tr>`;
  }
}

/** Abre el modal para crear (id=null) o editar (id=number) un item */
async function openAdminShopModal(id) {
  adminShopEditingId = id;
  const title = document.getElementById('admin-shop-modal-title');

  if (id === null) {
    // Modo creación: limpiar campos
    title.textContent = '➕ Nuevo Item de Tienda';
    document.getElementById('asi-editing-id').value = '';
    document.getElementById('asi-name').value = '';
    document.getElementById('asi-description').value = '';
    document.getElementById('asi-item-id').value = '';
    document.getElementById('asi-item-count').value = '1';
    document.getElementById('asi-price-coins').value = '0';
    document.getElementById('asi-category').value = 'general';
    document.getElementById('asi-image-url').value = '';
    document.getElementById('asi-stock').value = '';
    document.getElementById('asi-stock-unlimited').checked = true;
    document.getElementById('asi-stock').disabled = true;
    document.getElementById('asi-featured').checked = false;
    document.getElementById('asi-active').checked = true;
    openModal('admin-shop-item');
  } else {
    // Modo edición: cargar datos
    try {
      const { items } = await api.adminGetShopItems();
      const item = items.find(i => i.id === id);
      if (!item) { showToast('Item no encontrado', 'error'); return; }

      title.textContent = `✏️ Editar: ${item.name}`;
      document.getElementById('asi-editing-id').value = id;
      document.getElementById('asi-name').value = item.name || '';
      document.getElementById('asi-description').value = item.description || '';
      document.getElementById('asi-item-id').value = item.item_id || '';
      document.getElementById('asi-item-count').value = item.item_count || 1;
      document.getElementById('asi-price-coins').value = item.price_coins || 0;
      document.getElementById('asi-category').value = item.category || 'general';
      document.getElementById('asi-image-url').value = item.image_url || '';
      const isUnlimited = item.stock === null;
      document.getElementById('asi-stock-unlimited').checked = isUnlimited;
      document.getElementById('asi-stock').disabled = isUnlimited;
      document.getElementById('asi-stock').value = isUnlimited ? '' : item.stock;
      document.getElementById('asi-featured').checked = !!item.featured;
      document.getElementById('asi-active').checked = !!item.active;
      openModal('admin-shop-item');
    } catch (err) {
      showToast('Error al cargar item: ' + err.message, 'error');
    }
  }
}

/** Guarda el item (crea o actualiza) */
async function saveAdminShopItem() {
  const editingId = document.getElementById('asi-editing-id').value;
  const name       = document.getElementById('asi-name').value.trim();
  const item_id    = parseInt(document.getElementById('asi-item-id').value);

  if (!name)         { showToast('El nombre es requerido', 'error'); return; }
  if (!item_id || item_id <= 0) { showToast('Item ID de L2 es requerido', 'error'); return; }

  const isUnlimited = document.getElementById('asi-stock-unlimited').checked;
  const stockVal = document.getElementById('asi-stock').value;
  const payload = {
    name,
    description:  document.getElementById('asi-description').value.trim(),
    item_id,
    item_count:   parseInt(document.getElementById('asi-item-count').value) || 1,
    price_coins:  parseInt(document.getElementById('asi-price-coins').value) || 0,
    category:     document.getElementById('asi-category').value,
    image_url:    document.getElementById('asi-image-url').value.trim(),
    stock:        isUnlimited ? null : (stockVal !== '' ? parseInt(stockVal) : null),
    featured:     document.getElementById('asi-featured').checked,
    active:       document.getElementById('asi-active').checked,
  };

  try {
    if (editingId) {
      await api.adminUpdateShopItem(parseInt(editingId), payload);
      showToast(`✅ Item "${name}" actualizado`, 'success');
    } else {
      await api.adminCreateShopItem(payload);
      showToast(`✅ Item "${name}" creado`, 'success');
    }
    closeAllModals();
    loadAdminShop(); // Refrescar tabla
  } catch (err) {
    showToast(err.message, 'error');
  }
}

/** Toggle rápido activo/inactivo sin abrir modal */
async function toggleAdminShopItem(id, currentActive) {
  try {
    // Necesitamos todos los campos requeridos para el PUT — cargamos el item primero
    const { items } = await api.adminGetShopItems();
    const item = items.find(i => i.id === id);
    if (!item) { showToast('Item no encontrado', 'error'); return; }
    await api.adminUpdateShopItem(id, { ...item, active: currentActive ? 0 : 1 });
    showToast(currentActive ? '⛔ Item desactivado' : '✅ Item activado', 'success');
    loadAdminShop();
  } catch (err) {
    showToast(err.message, 'error');
  }
}

/** Elimina un item con confirmación */
async function deleteAdminShopItem(id, name) {
  if (!confirm(`¿Eliminar permanentemente el item "${name}"?\nEsta acción no se puede deshacer.`)) return;
  try {
    await api.adminDeleteShopItem(id);
    showToast(`🗑 Item "${name}" eliminado`, 'success');
    loadAdminShop();
  } catch (err) {
    showToast(err.message, 'error');
  }
}

/* ====================================================================
   ADMIN — PvP ZONE REWARD
   ==================================================================== */

/** Carga la configuración y el historial de recompensas PvP */
async function loadAdminPvpReward() {
  try {
    const data = await api.adminGetPvpReward();

    // Actualizar toggle
    const toggle = document.getElementById('pvpr-enabled-toggle');
    const statusBadge = document.getElementById('pvpr-status-text');
    const coinsInput  = document.getElementById('pvpr-coins-input');

    if (toggle) {
      toggle.checked = data.enabled;
      _updatePvpRewardStatusBadge(data.enabled);
      toggle.onchange = () => _updatePvpRewardStatusBadge(toggle.checked);
    }
    if (coinsInput) coinsInput.value = data.coins_per_kill ?? 5;

    // Tabla de totales por personaje
    const tbody = document.getElementById('pvpr-totals-tbody');
    if (tbody) {
      if (!data.totals || !data.totals.length) {
        tbody.innerHTML = `<tr><td colspan="5" class="text-center text-muted" style="padding:1.5rem">Sin recompensas entregadas aún</td></tr>`;
      } else {
        tbody.innerHTML = data.totals.map((r, i) => `
          <tr>
            <td><span class="rank-badge">${i + 1}</span></td>
            <td><strong>${escHtml(r.char_name)}</strong></td>
            <td>${r.kills_rewarded.toLocaleString()}</td>
            <td><span class="coins-badge">🪙 ${r.coins_total.toLocaleString()}</span></td>
            <td class="text-muted" style="font-size:.82rem">${r.last_reward_at ? new Date(r.last_reward_at).toLocaleString() : '—'}</td>
          </tr>`).join('');
      }
    }

    // Tabla de historial de eventos
    const hbody = document.getElementById('pvpr-history-tbody');
    if (hbody) {
      if (!data.history || !data.history.length) {
        hbody.innerHTML = `<tr><td colspan="5" class="text-center text-muted" style="padding:1rem">Sin eventos registrados</td></tr>`;
      } else {
        hbody.innerHTML = data.history.map(r => `
          <tr>
            <td><strong>${escHtml(r.char_name)}</strong></td>
            <td class="text-muted" style="font-size:.82rem">${escHtml(r.account_name)}</td>
            <td>+${r.kills_new}</td>
            <td><span class="coins-badge">🪙 +${r.coins_awarded}</span></td>
            <td class="text-muted" style="font-size:.82rem">${new Date(r.rewarded_at).toLocaleString()}</td>
          </tr>`).join('');
      }
    }
  } catch (err) {
    showToast('Error cargando configuración: ' + err.message, 'error');
  }
}

function _updatePvpRewardStatusBadge(enabled) {
  const badge = document.getElementById('pvpr-status-text');
  if (!badge) return;
  badge.textContent = enabled ? 'ACTIVADO' : 'DESACTIVADO';
  badge.className = 'pvpr-status-badge ' + (enabled ? 'pvpr-status-on' : 'pvpr-status-off');
}

/** Guarda la configuración de recompensa PvP */
async function savePvpRewardConfig() {
  const enabled = document.getElementById('pvpr-enabled-toggle')?.checked ?? false;
  const coinsRaw = parseInt(document.getElementById('pvpr-coins-input')?.value || '0');

  if (isNaN(coinsRaw) || coinsRaw < 0) {
    showToast('El valor de coins debe ser >= 0', 'error'); return;
  }

  const btn = document.getElementById('pvpr-save-btn');
  const msg = document.getElementById('pvpr-save-msg');
  if (btn) btn.disabled = true;

  try {
    await api.adminSetPvpReward(enabled, coinsRaw);
    showToast(`✅ Configuración guardada — ${enabled ? coinsRaw + ' 🪙 por kill (ACTIVO)' : 'DESACTIVADO'}`, 'success');
    if (msg) {
      msg.textContent = `Guardado: ${enabled ? coinsRaw + ' WebCoins por kill — ACTIVO' : 'Recompensas desactivadas'}`;
      msg.classList.remove('hidden');
      setTimeout(() => msg.classList.add('hidden'), 4000);
    }
  } catch (err) {
    showToast('Error: ' + err.message, 'error');
  } finally {
    if (btn) btn.disabled = false;
  }
}

/** Reinicia el historial de recompensas con confirmación */
async function confirmResetPvpLog() {
  if (!confirm('¿Reiniciar TODO el historial de recompensas PvP?\n\nEsto borra los contadores de kills premiados y el historial de eventos.\nLos kills existentes volverán a ser premiados en el próximo ciclo.\n\nEsta acción no se puede deshacer.')) return;
  try {
    const r = await api.adminResetPvpRewardLog();
    showToast('🗑 ' + r.message, 'success');
    loadAdminPvpReward();
  } catch (err) {
    showToast('Error: ' + err.message, 'error');
  }
}

window.savePvpRewardConfig  = savePvpRewardConfig;
window.confirmResetPvpLog   = confirmResetPvpLog;
window.loadAdminPvpReward   = loadAdminPvpReward;

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
