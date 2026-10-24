'use strict';

/**
 * Inventrack 動作確認用画面のスクリプト（開発用）。
 * フレームワークは使わず、同一オリジンの API（/api/v1）を fetch で呼ぶ。
 * 画面に出す文字列は textContent だけで組み立て、HTML として解釈させない（XSS 対策）。
 */

const API = '/api/v1';
const MAX_LOG = 30;

const SORT_FIELDS = [
  ['centerId', 'センターID'],
  ['centerName', 'センター名'],
  ['address', '住所'],
  ['operationalStatus', '稼働状況'],
  ['maxStorageCapacity', '最大保管容量'],
  ['currentStorageCapacity', '現在保管容量'],
  ['createDate', '作成日時'],
  ['updateDate', '更新日時'],
];

const STATUS_LABEL = { 0: '稼働中', 1: '稼働停止' };

/** 入力例（エラー確認用）。名称の重複を避けるため、正常な入力は時刻を付ける。 */
const PRESETS = {
  ok: () => ({
    centerName: 'テスト' + new Date().toTimeString().slice(0, 8).replace(/:/g, ''),
    postCode: '530-0001', address: '大阪府大阪市北区梅田1-1-1', phoneNumber: '06-1234-5678',
    managerName: '田中一郎', operationalStatus: '0', maxStorageCapacity: '800', currentStorageCapacity: '0', notes: '',
  }),
  dup: () => ({ ...PRESETS.ok(), centerName: '東京センター' }),
  longName: () => ({ ...PRESETS.ok(), centerName: 'あ'.repeat(21) }),
  badPost: () => ({ ...PRESETS.ok(), postCode: '5300001' }),
  badPhone: () => ({ ...PRESETS.ok(), phoneNumber: 'abc' }),
  over: () => ({ ...PRESETS.ok(), maxStorageCapacity: '100', currentStorageCapacity: '200' }),
  script: () => ({ ...PRESETS.ok(), centerName: '<script>' }),
};

const state = {
  accessToken: sessionStorage.getItem('accessToken') || null,
  refreshToken: sessionStorage.getItem('refreshToken') || null,
  email: sessionStorage.getItem('email') || null,
  role: sessionStorage.getItem('role') || null,
  /** false にすると、ログイン中でもトークンを付けずに API を呼ぶ（未ログイン時の挙動を確認するため）。 */
  sendToken: sessionStorage.getItem('sendToken') !== 'false',
  page: 1,
  totalPages: 0,
  editingId: null,
  logs: [],
};

const $ = (id) => document.getElementById(id);

/** 通常の API 呼び出しに付けるトークン。ログインしていない、またはスイッチが OFF のときは null。 */
function effectiveToken() {
  return state.accessToken && state.sendToken ? state.accessToken : null;
}

// ---------------------------------------------------------------- 通信

/** API を呼び、ステータスと本文（JSON）を返す。結果は通信ログに残す。 */
async function request(method, path, opts = {}) {
  const url = new URL(API + path, window.location.origin);
  Object.entries(opts.params || {}).forEach(([k, v]) => {
    if (v !== '' && v !== null && v !== undefined) url.searchParams.set(k, v);
  });
  const headers = {};
  if (opts.body !== undefined) headers['Content-Type'] = 'application/json';
  const token = opts.token !== undefined ? opts.token : effectiveToken();
  if (token) headers.Authorization = 'Bearer ' + token;
  const authLabel = token
    ? (opts.token !== undefined ? '認証: リフレッシュトークンあり' : '認証: トークンあり')
    : '認証: トークンなし';

  const started = performance.now();
  let response;
  let text = '';
  try {
    response = await fetch(url, {
      method,
      headers,
      body: opts.body !== undefined ? JSON.stringify(opts.body) : undefined,
    });
    text = await response.text();
  } catch (e) {
    addLog(method, url, null, 'ネットワークエラー: ' + e.message, performance.now() - started, opts.body, authLabel);
    throw e;
  }
  let json = null;
  try { json = text ? JSON.parse(text) : null; } catch (e) { /* JSON ではない本文はそのまま扱う */ }
  addLog(method, url, response.status, json ? JSON.stringify(json, null, 2) : text,
    performance.now() - started, opts.body, authLabel);
  return { status: response.status, json, text };
}

/** 通信ログに 1 件追加する（新しい順）。パスワードとトークンは表示しない。 */
function addLog(method, url, status, body, ms, requestBody, authLabel) {
  const lines = [];
  lines.push('[' + new Date().toLocaleTimeString() + '] [' + authLabel + '] ' + method + ' ' + url.pathname
    + url.search + '  →  ' + (status === null ? '失敗' : status) + '  (' + Math.round(ms) + ' ms)');
  if (requestBody !== undefined) {
    lines.push('  リクエスト本文: ' + JSON.stringify(redact(requestBody)));
  }
  lines.push(redactResponse(body));
  state.logs.unshift(lines.join('\n'));
  state.logs.length = Math.min(state.logs.length, MAX_LOG);
  $('log').textContent = state.logs.join('\n\n');
}

function redact(obj) {
  const copy = { ...obj };
  if ('password' in copy) copy.password = '********';
  return copy;
}

function redactResponse(text) {
  return String(text).replace(/("(?:accessToken|refreshToken)"\s*:\s*)"[^"]*"/g, '$1"(省略)"');
}

// ---------------------------------------------------------------- 画面の更新

function showMessage(text, kind) {
  const el = $('message');
  el.textContent = text;
  el.className = 'message ' + kind;
  el.hidden = !text;
}

/** エラー応答（共通形式）から、表示用のメッセージを作る。 */
function describeError(r) {
  const err = r.json && r.json.error;
  if (err) return r.status + ' ' + err.code + ': ' + err.message;
  return String(r.status) + ' ' + (r.text || '応答の本文がありません');
}

/** アクセストークンの有効期限（ミリ秒）。JWT の payload（公開情報）の exp を読む。読めなければ null。 */
function tokenExpiry(token) {
  try {
    const payload = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
    const exp = JSON.parse(atob(payload)).exp;
    return exp ? exp * 1000 : null;
  } catch (e) {
    return null;
  }
}

function setChip(el, on, text) {
  el.textContent = text;
  el.classList.toggle('on', on);
  el.classList.toggle('off', !on);
}

/** ログイン状態、トークンの有無、有効期限を、画面上部のバーと各区画に反映する。状態が変わるたびに呼ぶ。 */
function renderAuth() {
  const loggedIn = Boolean(state.accessToken);
  const sending = Boolean(effectiveToken());
  const expiry = loggedIn ? tokenExpiry(state.accessToken) : null;
  const expired = expiry !== null && expiry <= Date.now();

  // 画面上部のバー
  const bar = $('auth-bar');
  const badge = $('auth-badge');
  let barKind = 'off';
  let badgeText = '未ログイン';
  if (loggedIn) {
    barKind = sending && !expired ? 'on' : 'warn';
    badgeText = expired ? 'ログイン中（トークン期限切れ）' : 'ログイン中';
  }
  bar.className = 'auth-bar ' + barKind;
  badge.className = 'badge ' + barKind;
  badge.textContent = badgeText;
  $('auth-user').textContent = loggedIn
    ? (state.email || '(不明)') + (state.role ? '（' + state.role + '）' : '')
    : '（認証が必要な API は 401 になります）';
  $('send-token').checked = state.sendToken;
  $('send-token').disabled = !loggedIn;

  // 各 API 操作が、実際にトークンを付けて送られるか
  const sendText = sending ? '送信: トークンあり' : '送信: トークンなし';
  setChip($('auth-effective'), sending, sendText);
  document.querySelectorAll('[data-auth-chip]').forEach((el) => setChip(el, sending, sendText));

  // 認証セクションの表示とボタン
  const session = $('session');
  session.textContent = loggedIn ? 'ログイン中: ' + (state.email || '(不明)') : '未ログイン';
  session.className = 'status' + (loggedIn ? ' logged-in' : '');
  $('btn-register').disabled = loggedIn;
  $('btn-login').disabled = loggedIn;
  $('btn-refresh').disabled = !loggedIn;
  $('btn-logout').disabled = !loggedIn;

  // 本文のクラス（区画の左線の色）
  document.body.classList.toggle('authed', loggedIn);
  document.body.classList.toggle('anon', !loggedIn);
  document.body.classList.toggle('token-off', loggedIn && !state.sendToken);

  renderExpiry();
}

/** 有効期限の残り時間を表示する（1 秒ごとに更新）。 */
function renderExpiry() {
  const el = $('auth-expiry');
  const expiry = state.accessToken ? tokenExpiry(state.accessToken) : null;
  if (expiry === null) {
    el.textContent = '';
    el.classList.remove('expired');
    return;
  }
  const left = Math.round((expiry - Date.now()) / 1000);
  if (left <= 0) {
    el.textContent = 'アクセストークンは期限切れ（API は 401 になります。トークン更新を試せます）';
    el.classList.add('expired');
  } else {
    el.textContent = 'アクセストークンの残り ' + Math.floor(left / 60) + ' 分 ' + String(left % 60).padStart(2, '0') + ' 秒';
    el.classList.remove('expired');
  }
}

function setSession(json, email) {
  state.accessToken = json.accessToken;
  state.refreshToken = json.refreshToken;
  if (json.user && json.user.email) state.email = json.user.email;
  else if (email) state.email = email;
  if (json.user && json.user.role) state.role = json.user.role;
  sessionStorage.setItem('accessToken', state.accessToken || '');
  sessionStorage.setItem('refreshToken', state.refreshToken || '');
  sessionStorage.setItem('email', state.email || '');
  sessionStorage.setItem('role', state.role || '');
  renderAuth();
}

function clearSession() {
  state.accessToken = null;
  state.refreshToken = null;
  state.email = null;
  state.role = null;
  sessionStorage.clear();
  state.sendToken = true;
  renderAuth();
}

function cell(text, className) {
  const td = document.createElement('td');
  td.textContent = text === null || text === undefined ? '' : String(text);
  if (className) td.className = className;
  return td;
}

function button(label, className, handler) {
  const b = document.createElement('button');
  b.type = 'button';
  b.textContent = label;
  if (className) b.className = className;
  b.addEventListener('click', handler);
  return b;
}

function renderRows(page) {
  const tbody = $('rows');
  tbody.replaceChildren();
  if (!page.data.length) {
    const tr = document.createElement('tr');
    const td = cell('該当する在庫センターはありません。', 'empty');
    td.colSpan = 9;
    tr.appendChild(td);
    tbody.appendChild(tr);
  }
  page.data.forEach((c) => {
    const tr = document.createElement('tr');
    tr.appendChild(cell(c.centerId));
    tr.appendChild(cell(c.centerName));
    tr.appendChild(cell(c.address));
    tr.appendChild(cell(c.phoneNumber));
    tr.appendChild(cell(c.managerName));
    tr.appendChild(cell(c.operationalStatus + ': ' + (STATUS_LABEL[c.operationalStatus] || '?')));
    tr.appendChild(cell(c.maxStorageCapacity, 'num'));
    tr.appendChild(cell(c.currentStorageCapacity, 'num'));
    const actions = cell('', 'actions');
    actions.appendChild(button('編集', 'secondary', () => startEdit(c)));
    actions.appendChild(button('削除', 'danger', () => removeCenter(c)));
    tr.appendChild(actions);
    tbody.appendChild(tr);
  });
  const m = page.meta;
  state.page = m.page;
  state.totalPages = m.totalPages;
  $('page-info').textContent = '全 ' + m.total + ' 件 / ' + m.page + ' ページ目（全 ' + m.totalPages
    + ' ページ・' + m.size + ' 件ずつ）';
  $('page-input').value = m.page;
  $('btn-prev').disabled = m.page <= 1;
  $('btn-next').disabled = m.page >= m.totalPages;
}

// ---------------------------------------------------------------- 認証

function credentials() {
  return { email: $('email').value, password: $('password').value };
}

async function register() {
  const body = {
    firstname: $('firstname').value, lastname: $('lastname').value, ...credentials(),
  };
  const r = await request('POST', '/auth/register', { body, token: null });
  if (r.status === 200) {
    setSession(r.json);
    showMessage('登録してログインしました。', 'success');
  } else {
    showMessage(describeError(r), 'error');
  }
}

async function login() {
  const r = await request('POST', '/auth/authenticate', { body: credentials(), token: null });
  if (r.status === 200) {
    setSession(r.json);
    showMessage('ログインしました。', 'success');
  } else {
    showMessage(describeError(r), 'error');
  }
}

async function refreshToken() {
  if (!state.refreshToken) {
    showMessage('リフレッシュトークンがありません。先にログインしてください。', 'error');
    return;
  }
  const r = await request('POST', '/auth/refresh-token', { token: state.refreshToken });
  if (r.status === 200) {
    setSession(r.json);
    showMessage('トークンを更新しました。', 'success');
  } else {
    showMessage(describeError(r), 'error');
  }
}

// ---------------------------------------------------------------- 一覧

function searchParams(page) {
  return {
    centerName: $('s-name').value,
    address: $('s-address').value,
    operationalStatus: $('s-status').value,
    sort: $('s-sort').value,
    order: $('s-order').value,
    size: $('s-size').value,
    page,
  };
}

async function search(page) {
  const r = await request('GET', '/centers', { params: searchParams(page) });
  if (r.status === 200) {
    showMessage('', 'success');
    renderRows(r.json);
  } else {
    showMessage(describeError(r), 'error');
  }
}

// ---------------------------------------------------------------- 登録・編集

const form = () => $('center-form');

function readForm() {
  const f = form().elements;
  const num = (v) => (v.trim() === '' ? null : Number(v));
  return {
    centerName: f.centerName.value,
    postCode: f.postCode.value,
    address: f.address.value,
    phoneNumber: f.phoneNumber.value,
    managerName: f.managerName.value,
    operationalStatus: Number(f.operationalStatus.value),
    maxStorageCapacity: num(f.maxStorageCapacity.value),
    currentStorageCapacity: num(f.currentStorageCapacity.value),
    notes: f.notes.value,
  };
}

function fillForm(values) {
  const f = form().elements;
  Object.keys(values).forEach((k) => {
    if (f[k]) f[k].value = values[k] === null || values[k] === undefined ? '' : String(values[k]);
  });
}

function resetForm() {
  form().reset();
  state.editingId = null;
  $('form-mode').textContent = '新規登録';
  $('btn-save').textContent = '登録';
  $('preset').value = '';
}

function startEdit(c) {
  state.editingId = c.centerId;
  fillForm(c);
  $('form-mode').textContent = '編集中（ID ' + c.centerId + '）';
  $('btn-save').textContent = '更新';
  $('h-form').scrollIntoView({ behavior: 'smooth' });
}

async function save(event) {
  event.preventDefault();
  const body = readForm();
  const editing = state.editingId !== null;
  const r = editing
    ? await request('PUT', '/centers/' + state.editingId, { body })
    : await request('POST', '/centers', { body });
  if (r.status === 200) {
    showMessage((editing ? '更新しました。' : '登録しました。') + ' ID: ' + r.json.centerId, 'success');
    resetForm();
    await search(state.page || 1);
  } else {
    showMessage(describeError(r), 'error');
  }
}

async function removeCenter(c) {
  if (!window.confirm('「' + c.centerName + '」を削除しますか？（論理削除）')) return;
  const r = await request('DELETE', '/centers/' + c.centerId);
  if (r.status === 200) {
    showMessage('削除しました。 ID: ' + c.centerId, 'success');
    await search(state.page || 1);
  } else {
    showMessage(describeError(r), 'error');
  }
}

// ---------------------------------------------------------------- 初期化

function init() {
  SORT_FIELDS.forEach(([value, label]) => {
    const o = document.createElement('option');
    o.value = value;
    o.textContent = value + '（' + label + '）';
    $('s-sort').appendChild(o);
  });

  $('btn-register').addEventListener('click', register);
  $('btn-login').addEventListener('click', login);
  $('btn-refresh').addEventListener('click', refreshToken);
  $('btn-logout').addEventListener('click', () => { clearSession(); showMessage('ログアウトしました。', 'success'); });

  $('search-form').addEventListener('submit', (e) => { e.preventDefault(); search(1); });
  $('btn-clear').addEventListener('click', () => {
    $('search-form').reset();
    $('s-sort').value = 'centerId';
  });
  $('btn-prev').addEventListener('click', () => search(state.page - 1));
  $('btn-next').addEventListener('click', () => search(state.page + 1));
  $('btn-goto').addEventListener('click', () => search($('page-input').value));

  form().addEventListener('submit', save);
  $('btn-reset').addEventListener('click', resetForm);
  $('preset').addEventListener('change', (e) => {
    const make = PRESETS[e.target.value];
    if (make) fillForm(make());
  });
  $('btn-clear-log').addEventListener('click', () => { state.logs = []; $('log').textContent = ''; });

  $('btn-prev').disabled = true;
  $('btn-next').disabled = true;
  $('send-token').addEventListener('change', (e) => {
    state.sendToken = e.target.checked;
    sessionStorage.setItem('sendToken', String(state.sendToken));
    renderAuth();
    showMessage(state.sendToken ? 'API にトークンを付けて送ります。' : 'ログイン中でも、トークンを付けずに API を送ります（401 の確認用）。', 'success');
  });
  renderAuth();
  setInterval(renderExpiry, 1000);
}

init();
