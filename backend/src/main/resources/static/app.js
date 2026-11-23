'use strict';

/**
 * Access token хранится только в памяти вкладки. Refresh token браузер получает
 * в HttpOnly/SameSite cookie.
 */
const state = {
    accessToken: null,
    user: null,
    centers: [],
    operators: [],
    userPage: 0, auditPage: 0, notes: [],
    appeals: { page: 0, size: 25, totalPages: 0, query: '', content: [] },
    selectedAppealIds: new Set(),
    activeAppealForNotes: null,
    refreshPromise: null
};

const $ = (id) => document.getElementById(id);
const escapeHtml = (value) => String(value ?? '').replace(/[&<>'"]/g, ch => ({'&':'&amp;','<':'&lt;','>':'&gt;',"'":'&#39;','"':'&quot;'}[ch]));
const roles = () => new Set((state.user?.roles || []).map(String));
const isAdmin = () => roles().has('ADMIN');
const canEdit = () => roles().has('EDITOR') || isAdmin();


/**
 * Коды ролей в API остаются английскими; в интерфейсе используются русские названия.
 */
function roleLabel(value) {
    return ({
        ADMIN: 'Администратор',
        EDITOR: 'Редактор',
        VIEWER: 'Наблюдатель'
    })[value] || value;
}

/** Русские имена полей для ошибок Bean Validation. */
function fieldLabel(value) {
    return ({
        externalId: 'внешний ID',
        topic: 'тема',
        clientName: 'клиент',
        category: 'категория',
        email: 'email',
        phone: 'телефон',
        dueDate: 'срок решения',
        centerId: 'колл-центр',
        status: 'статус',
        impactScore: 'влияние',
        urgent: 'срочность',
        tags: 'теги',
        version: 'версия'
    })[value] || value;
}

/** Приводит первую букву текста после двоеточия к нижнему регистру. */
function lowerFirst(value) {
    const text = String(value ?? '');
    return text ? text.charAt(0).toLocaleLowerCase('ru-RU') + text.slice(1) : text;
}

/**
 * Нормализует сообщения вида «поле: Ошибка» в «поле: ошибка».
 * Правило применяется и к сообщениям сервера.
 */
function normalizeAfterColon(value) {
    return String(value ?? '').replace(/:\s*([А-ЯЁA-Z])/g,
        (_, letter) => `: ${letter.toLocaleLowerCase('ru-RU')}`);
}

/**
 * Приводит ввод телефона к формату +7-999-999-99-99.
 * Если пользователь вставляет 8XXXXXXXXXX или 7XXXXXXXXXX, код страны
 * автоматически нормализуется к +7.
 */
function formatRussianPhone(value) {
    let digits = String(value ?? '').replace(/\D/g, '');

    if (digits.startsWith('8')) {
        digits = `7${digits.slice(1)}`;
    }

    if (digits.startsWith('7')) {
        digits = digits.slice(1);
    }

    digits = digits.slice(0, 10);

    let result = '+7-';
    if (!digits.length) return result;

    result += digits.slice(0, 3);
    if (digits.length > 3) result += `-${digits.slice(3, 6)}`;
    if (digits.length > 6) result += `-${digits.slice(6, 8)}`;
    if (digits.length > 8) result += `-${digits.slice(8, 10)}`;

    return result;
}

/**
 * Маска телефона:
 * - при фокусе сразу подставляет +7-;
 * - разрешает вводить только цифры и служебные клавиши;
 * - дефисы добавляет автоматически;
 * - вставленный текст очищает от букв и лишних знаков.
 */
function bindPhoneMask(input) {
    if (!input) return;

    input.addEventListener('focus', () => {
        if (!input.value) input.value = '+7-';
    });

    input.addEventListener('keydown', event => {
        const allowedKeys = new Set([
            'Backspace', 'Delete', 'ArrowLeft', 'ArrowRight',
            'Home', 'End', 'Tab', 'Enter'
        ]);

        if (event.ctrlKey || event.metaKey || allowedKeys.has(event.key) || /^\d$/.test(event.key)) {
            return;
        }

        event.preventDefault();
    });

    input.addEventListener('input', () => {
        input.value = formatRussianPhone(input.value);
    });

    input.addEventListener('blur', () => {
        if (input.value === '+7-') input.value = '';
    });
}

function toast(message, error = false) {
    const node = $('toast');
    node.textContent = message;
    node.className = `toast show${error ? ' error' : ''}`;
    clearTimeout(toast.timer);
    toast.timer = setTimeout(() => node.className = 'toast', 3500);
}


/**
 * Ошибки modal dialog выводятся внутри формы, а не через общий toast.
 */
function showFormError(form, message) {
    const box = form.querySelector('[data-form-error]');
    if (!box) {
        toast(message, true);
        return;
    }
    box.textContent = message;
    box.hidden = false;
}

/** Очищает локальное сообщение об ошибке перед новой отправкой формы. */
function clearFormError(form) {
    const box = form.querySelector('[data-form-error]');
    if (!box) return;
    box.textContent = '';
    box.hidden = true;
}

async function parseError(response) {
    try {
        const body = await response.json();
        const fields = body.validationErrors
            ? Object.entries(body.validationErrors)
                .map(([key, value]) => `${fieldLabel(key)}: ${lowerFirst(value)}`)
                .join('; ')
            : '';

        const message = `${body.message || response.statusText}${fields ? ` — ${fields}` : ''}${body.correlationId ? ` [${body.correlationId}]` : ''}`;
        return normalizeAfterColon(message);
    } catch (_) {
        return `${response.status} ${response.statusText}`;
    }
}

/**
 * Общая функция HTTP-вызова обрабатывает 401 и ограничивает повторный refresh.
 * Параллельные запросы используют один refreshPromise.
 */
async function api(path, options = {}, retry = true) {
    const headers = new Headers(options.headers || {});
    if (state.accessToken) headers.set('Authorization', `Bearer ${state.accessToken}`);
    if (options.body && !(options.body instanceof FormData) && !headers.has('Content-Type')) headers.set('Content-Type', 'application/json');
    const response = await fetch(`/api${path}`, {...options, headers, credentials: 'include'});
    if (response.status === 401 && retry) {
        const refreshed = await refreshAccessToken();
        if (refreshed) return api(path, options, false);
    }
    if (!response.ok) throw new Error(await parseError(response));
    if (response.status === 204) return null;
    const type = response.headers.get('content-type') || '';
    return type.includes('application/json') ? response.json() : response;
}

async function refreshAccessToken() {
    if (!state.refreshPromise) {
        state.refreshPromise = fetch('/api/auth/refresh', {
            method: 'POST', credentials: 'include', headers: {'Content-Type':'application/json'}, body: '{}'
        }).then(async response => {
            if (!response.ok) return false;
            const result = await response.json();
            state.accessToken = result.accessToken;
            state.user = result.user;
            return true;
        }).finally(() => state.refreshPromise = null);
    }
    return state.refreshPromise;
}

async function boot() {
    bindEvents();
    if (await refreshAccessToken()) {
        await enterApplication();
    } else {
        showLogin();
    }
}

function showLogin() {
    $('app-view').hidden = true;
    $('login-view').hidden = false;
    state.accessToken = null;
    state.user = null;
}

async function enterApplication() {
    $('login-view').hidden = true;
    $('app-view').hidden = false;
    applyRoleVisibility();
    $('current-user').innerHTML = `<strong>${escapeHtml(state.user.displayName)}</strong><br>${escapeHtml(state.user.email)}<br>${escapeHtml(state.user.roles.map(roleLabel).join(', '))}`;

    if (state.user.passwordChangeRequired) {
        toast('Для продолжения необходимо сменить временный пароль', true);
        const changed = await openPasswordDialog(true);
        if (!changed) return;
    }

    /*
     * Ошибка загрузки аналитики или справочников не считается ошибкой входа.
     * Пользователь остаётся в приложении и получает отдельное уведомление.
     */
    try {
        await Promise.all([loadCallCenters(), loadDashboard(), api('/operators').then(data=>state.operators=data)]);
    } catch (error) {
        console.error('[Колл-центр] Initial data loading failed', error);
        toast(`Вход выполнен, но часть данных не загрузилась: ${error.message}`, true);
    }
}

function applyRoleVisibility() {
    document.querySelectorAll('.admin-only').forEach(node => node.hidden = !isAdmin());
    document.querySelectorAll('.editor-only').forEach(node => node.hidden = !canEdit());
}

function switchTab(name) {
    document.querySelectorAll('.tab').forEach(tab => tab.classList.toggle('active', tab.id === `tab-${name}`));
    document.querySelectorAll('.nav-item').forEach(button => button.classList.toggle('active', button.dataset.tab === name));
    const titles = {
        dashboard: ['Аналитика','Сводная картина по обращениям'],
        appeals: ['Обращения','Умный поиск, карточки и массовые операции'],
        clients: ['Клиенты','Контакты клиентов и история обращений'],
        operators: ['Операторы','Сотрудники, колл-центры и активность'],
        centers: ['Колл-центры','Справочник колл-центров'],
        users: ['Пользователи','Роли, блокировка и временные пароли'],
        audit: ['Журнал аудита','История действий пользователей']
    };
    [$('page-title').textContent, $('page-subtitle').textContent] = titles[name];
    if (name === 'clients' || name === 'operators') loadDirectory(name);
    if (name === 'appeals') loadAppeals();
    if (name === 'centers') renderCallCenters();
    if (name === 'users' && isAdmin()) loadUsers();
    if (name === 'audit' && isAdmin()) loadAudit();
    if (name === 'dashboard') loadDashboard();
}

async function loadDashboard() {
    try {
        const data = await api('/analytics/summary');
        const metrics = [
            ['Всего', data.total], ['Активные', data.active], ['В архиве', data.archived],
            ['Срочные', data.withUrgency], ['Влияние', data.impactScore ?? '—']
        ];
        $('metric-grid').innerHTML = metrics.map(([name,value]) => `<article class="metric card"><span class="muted">${name}</span><strong>${value}</strong></article>`).join('');
        renderBars('chart-centers', data.byCallCenter);
        renderBars('chart-status', data.byStatus);
        renderBars('chart-priority', data.byPriority);
    } catch (error) { toast(error.message, true); }
}

function renderBars(id, values) {
    const max = Math.max(1, ...values.map(item => item.count));

    const displayName = value => {
        if (id === 'chart-status') return statusLabel(value);
        if (id === 'chart-priority') return priorityLabel(value);
        return value;
    };

    $(id).innerHTML = values.length
        ? values.map(item => `<div class="bar-row"><span>${escapeHtml(displayName(item.name))}</span><progress value="${item.count}" max="${max}"></progress><strong>${item.count}</strong></div>`).join('')
        : '<p class="muted">Нет данных</p>';
}

async function loadCallCenters() {
    try { state.centers = await api('/centers?activeOnly=false'); renderCallCenters(); }
    catch (error) { toast(error.message, true); }
}

function renderCallCenters() {
    const body = $('centers-body');
    if (!body) return;
    body.innerHTML = state.centers.map(center => `<tr>
        <td><strong>${escapeHtml(center.code)}</strong></td><td>${escapeHtml(center.name || '—')}</td><td>${center.slaHours ?? '—'}</td>
        <td><span class="badge ${center.active ? 'success' : ''}">${center.active ? 'Да' : 'Нет'}</span></td>
        <td class="admin-only"><div class="actions"><button class="secondary" data-center-edit="${center.id}">Изменить</button><button class="secondary" data-center-toggle="${center.id}">${center.active ? 'Отключить' : 'Включить'}</button></div></td>
    </tr>`).join('');
    applyRoleVisibility();
}

async function loadAppeals(page = state.appeals.page) {
    try {
        const q = encodeURIComponent(state.appeals.query);
        const result = await api(`/appeals?q=${q}&page=${page}&size=${state.appeals.size}&sort=${encodeURIComponent($('appeals-sort').value)}`);
        state.appeals = {...state.appeals, ...result};
        state.selectedAppealIds.clear();
        renderAppeals();
    } catch (error) { toast(error.message, true); }
}

function renderAppeals() {
    $('appeals-body').innerHTML = state.appeals.content.map(appeal => `<tr class="${appeal.archived ? 'archived' : ''}">
        <td class="editor-only"><input type="checkbox" data-appeal-select="${appeal.id}"></td>
        <td><strong>${escapeHtml(appeal.fullName)}</strong><br><small class="muted">${escapeHtml(appeal.externalId || '')}</small></td>
        <td>${escapeHtml(appeal.centerCode || '—')}</td><td>${escapeHtml(statusLabel(appeal.status))}</td>
        <td>${appeal.impactScore ?? '—'}</td><td>${appeal.urgent ? '<span class="badge warning">Да</span>' : 'Нет'}</td>
        <td><span class="badge priority-${appeal.priorityLevel}">${escapeHtml(priorityLabel(appeal.priorityLevel))}</span></td>
        <td>${appeal.tags.map(tag => `<span class="badge">${escapeHtml(tag)}</span>`).join(' ') || '—'}</td>
        <td><div class="actions">
            <button class="secondary" data-appeal-view="${appeal.id}">Карточка</button>
            <button class="secondary" data-appeal-notes="${appeal.id}">Заметки</button>
            ${isAdmin() && appeal.archived ? `<button class="danger-outline" data-appeal-permanent="${appeal.id}">Удалить навсегда</button>` : ''}
            ${canEdit() ? `<button class="secondary" data-appeal-edit="${appeal.id}">Изменить</button><button class="danger-outline" data-appeal-archive="${appeal.id}">${appeal.archived ? 'Восстановить' : 'В архив'}</button>` : ''}
        </div></td>
    </tr>`).join('') || '<tr><td colspan="9" class="muted">Обращения не найдены</td></tr>';
    $('appeals-page').textContent = `Страница ${state.appeals.page + 1} из ${Math.max(1,state.appeals.totalPages)} · записей ${state.appeals.totalElements}`;
    $('appeals-prev').disabled = state.appeals.first;
    $('appeals-next').disabled = state.appeals.last;
    $('select-all-appeals').checked = false;
    applyRoleVisibility();
}

function appealForm(appeal = null, readOnly = false) {
    const centerOptions = ['<option value="">Без колл-центра</option>', ...state.centers.filter(g => g.active || g.id === appeal?.centerId).map(g => `<option value="${g.id}" ${g.id === appeal?.centerId ? 'selected' : ''}>${escapeHtml(g.code)}</option>`)].join('');
    return `<div class="dialog-head"><h3>${appeal ? (readOnly ? 'Карточка обращения' : 'Изменение обращения') : 'Новое обращение'}</h3><button type="button" data-close-dialog="entity-dialog" class="icon-button">×</button></div>
    <div class="form-grid">
      <label>Клиент<input name="clientName" required maxlength="150" value="${escapeHtml(appeal?.clientName || '')}" ${readOnly?'disabled':''}></label>
      <label>Тема<input name="topic" required maxlength="300" value="${escapeHtml(appeal?.topic || '')}" ${readOnly?'disabled':''}></label>
      <label>Категория<select name="category" required ${readOnly?'disabled':''}>${['TECH','COMPLAINT','CONSULTATION','BILLING'].map(v=>`<option ${v===appeal?.category?'selected':''}>${v}</option>`).join('')}</select></label>
      <label>External ID<input name="externalId" required maxlength="100" value="${escapeHtml(appeal?.externalId || '')}" ${readOnly?'disabled':''}></label>
      <label>Email<input name="email" type="email" maxlength="254" value="${escapeHtml(appeal?.email || '')}" ${readOnly?'disabled':''}></label>
      <label>Телефон<input name="phone" required type="tel" inputmode="numeric" autocomplete="tel" maxlength="16" pattern="\\+7-[0-9]{3}-[0-9]{3}-[0-9]{2}-[0-9]{2}" title="Формат: +7-999-999-99-99" placeholder="+7-999-999-99-99" value="${escapeHtml(appeal?.phone ? formatRussianPhone(appeal.phone) : '')}" ${readOnly?'disabled':''}></label>
      <label>Срок решения<input name="dueDate" type="date" value="${appeal?.dueDate || ''}" ${readOnly?'disabled':''}></label>
      <label>Колл-центр<select name="centerId" required ${readOnly?'disabled':''}>${centerOptions}</select></label>
      <label>Оператор<select name="operatorId" ${readOnly?'disabled':''}><option value="">Не назначен</option>${state.operators.filter(o=>o.active || o.id===appeal?.operatorId).map(o=>`<option value="${o.id}" ${o.id===appeal?.operatorId?'selected':''}>${escapeHtml(o.fullName)}</option>`).join('')}</select></label>
      <label>Статус<select name="status" ${readOnly?'disabled':''}>${statusOptions(appeal?.status)}</select></label>
      <label>Влияние (0–100)<input name="impactScore" required type="number" min="0" max="100" step="0.01" inputmode="decimal" placeholder="Например, 68.50" value="${appeal?.impactScore ?? ''}" ${readOnly?'disabled':''}></label>
      <label class="inline"><input name="urgent" type="checkbox" ${appeal?.urgent?'checked':''} ${readOnly?'disabled':''}> Срочное обращение</label>
      <label class="span-2">Теги через запятую<input name="tags" maxlength="1000" value="${escapeHtml((appeal?.tags || []).join(', '))}" ${readOnly?'disabled':''}></label>
    </div>
    ${readOnly ? '' : '<div class="form-message error" data-form-error hidden></div><button class="primary" type="submit">Сохранить</button>'}`;
}

async function openAppeal(id = null, readOnly = false) {
    const appeal = id ? await api(`/appeals/${id}`) : null;
    const form = $('entity-form');
    form.innerHTML = appealForm(appeal, readOnly);

    const phoneInput = form.elements.phone;
    if (phoneInput && !readOnly) {
        bindPhoneMask(phoneInput);
    }

    form.onsubmit = readOnly ? null : async event => {
        event.preventDefault();
        clearFormError(form);

        /*
         * reportValidity() запускает нативную browser-валидацию до похода в backend.
         * Так пользователь сразу видит проблему в конкретном поле.
         */
        if (!form.reportValidity()) return;

        const data = new FormData(form);
        const payload = {
            externalId: empty(data.get('externalId')), topic: data.get('topic'), clientName: data.get('clientName'), category: empty(data.get('category')),
            email: empty(data.get('email')), phone: empty(data.get('phone')), dueDate: empty(data.get('dueDate')),
            centerId: empty(data.get('centerId')), status: data.get('status'), impactScore: empty(data.get('impactScore')) ? Number(data.get('impactScore')) : null,
            urgent: data.get('urgent') === 'on', tags: String(data.get('tags') || '').split(',').map(v => v.trim()).filter(Boolean), version: appeal?.version ?? null, operatorId: empty(data.get("operatorId"))?Number(data.get("operatorId")):null
        };
        try {
            await api(id ? `/appeals/${id}` : '/appeals', {method: id ? 'PUT' : 'POST', body: JSON.stringify(payload)});
            $('entity-dialog').close(); toast('Карточка сохранена'); await Promise.all([loadAppeals(), loadDashboard()]);
        } catch (error) {
            showFormError(form, error.message);
        }
    };
    $('entity-dialog').showModal();
}

async function archiveAppeal(id) {
    const appeal = state.appeals.content.find(item => item.id === id);
    const action = appeal?.archived ? 'восстановить' : 'переместить в архив';
    if (!confirm(`Действительно ${action} карточку?`)) return;
    try {
        await api(appeal.archived ? `/appeals/${id}/restore` : `/appeals/${id}`, {method: appeal.archived ? 'POST' : 'DELETE'});
        toast('Операция выполнена'); await Promise.all([loadAppeals(), loadDashboard()]);
    } catch (error) { toast(error.message, true); }
}

async function openNotes(appealId) {
    state.activeAppealForNotes = appealId;
    $('note-form').hidden=!canEdit();
    await renderNotes();
    $('notes-dialog').showModal();
}

async function renderNotes() {
    try {
        const list = await api(`/appeals/${state.activeAppealForNotes}/notes`);
        state.notes=list;
        $('notes-list').innerHTML = list.map(note => `<article class="note"><div class="note-meta"><span>${escapeHtml(note.authorName)} · ${formatDate(note.createdAt)}</span><span>${note.pinned ? '📌' : ''}</span></div><p>${escapeHtml(note.body).replace(/\n/g,'<br>')}</p>${canEdit() && (isAdmin() || state.user.id===note.authorId)?`<div class="actions"><button class="secondary" data-note-edit="${note.id}">Изменить</button><button class="danger-outline" data-note-delete="${note.id}">Удалить</button></div>`:''}</article>`).join('') || '<p class="muted">Заметок пока нет</p>';
    } catch (error) { toast(error.message, true); }
}

async function bulkAction() {
    if (!state.selectedAppealIds.size) return toast('Выберите хотя бы одного обращения', true);
    const action = prompt('Действие: ARCHIVE, RESTORE, SET_STATUS, ADD_TAG, REMOVE_TAG', 'ARCHIVE');
    if (!action) return;
    const payload = {ids: [...state.selectedAppealIds], action: action.toUpperCase(), status: null, tag: null};
    if (payload.action === 'SET_STATUS') payload.status = prompt('Статус: NEW, IN_PROGRESS, ESCALATED, RESOLVED, CLOSED', 'NEW');
    if (payload.action === 'ADD_TAG' || payload.action === 'REMOVE_TAG') payload.tag = prompt('Тег');
    try {
        const result = await api('/appeals/bulk', {method:'POST', body:JSON.stringify(payload)});
        toast(`Успешно: ${result.succeeded}; ошибок: ${result.failures.length}`); await Promise.all([loadAppeals(), loadDashboard()]);
    } catch (error) { toast(error.message, true); }
}

async function exportCsv() {
    try {
        const response = await api(`/appeals/export?q=${encodeURIComponent(state.appeals.query)}`);
        const blob = await response.blob();
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a'); link.href = url; link.download = `appeals-${new Date().toISOString().slice(0,10)}.csv`; link.click();
        URL.revokeObjectURL(url);
    } catch (error) { toast(error.message, true); }
}

function centerForm(center = null) {
    return `<div class="dialog-head"><h3>${center?'Изменение колл-центра':'Новый колл-центр'}</h3><button type="button" data-close-dialog="entity-dialog" class="icon-button">×</button></div>
    <label>Код<input name="code" required maxlength="50" value="${escapeHtml(center?.code || '')}"></label>
    <label>Название<input name="name" maxlength="200" value="${escapeHtml(center?.name || '')}"></label>
    <label>SLA, часы<input name="slaHours" type="number" min="1" max="720" value="${center?.slaHours ?? ''}"></label>
    <label class="inline"><input name="active" type="checkbox" ${center?.active !== false?'checked':''}> Активна</label>
    <button class="primary">Сохранить</button>`;
}

async function openCallCenter(id = null) {
    const center = id ? state.centers.find(g => g.id === id) : null;
    const form = $('entity-form'); form.innerHTML = centerForm(center);
    form.onsubmit = async event => {
        event.preventDefault(); const data = new FormData(form);
        const payload = {code:data.get('code'), name:empty(data.get('name')), slaHours:empty(data.get('slaHours'))?Number(data.get('slaHours')):null, active:data.get('active')==='on', version:center?.version??null};
        try { await api(id?`/centers/${id}`:'/centers',{method:id?'PUT':'POST',body:JSON.stringify(payload)}); $('entity-dialog').close(); toast('Колл-центр сохранён'); await loadCallCenters(); }
        catch (error) { toast(error.message,true); }
    }; $('entity-dialog').showModal();
}

async function toggleCallCenter(id) {
    const center = state.centers.find(g => g.id === id);
    try { await api(`/centers/${id}/active?active=${!center.active}&version=${center.version}`,{method:'PATCH'}); await loadCallCenters(); }
    catch (error) { toast(error.message,true); }
}

async function loadUsers() {
    try {
        const page = await api(`/admin/users?page=${state.userPage}&size=10&sort=${$('users-sort').value}`);
        $('users-body').innerHTML = page.content.map(user => `<tr><td>${escapeHtml(user.email)}</td><td>${escapeHtml(user.displayName)}</td><td>${user.roles.map(r=>`<span class="badge">${escapeHtml(roleLabel(r))}</span>`).join(' ')}</td><td>${user.enabled?'Да':'Нет'}</td><td>${user.passwordChangeRequired?'Да':'Нет'}</td><td><div class="actions"><button class="secondary" data-user-edit="${user.id}">Изменить</button><button class="secondary" data-user-reset="${user.id}">Сбросить пароль</button></div></td></tr>`).join('');
        state.users = page.content;
        $('users-page').textContent=`${page.page+1} / ${Math.max(1,page.totalPages)}`; $('users-prev').disabled=page.first; $('users-next').disabled=page.last;
    } catch (error) { toast(error.message,true); }
}

function userForm(user = null) {
    const checked = role => user?.roles.includes(role) || (!user && role==='VIEWER');
    return `<div class="dialog-head"><h3>${user?'Изменение':'Новый'} пользователь</h3><button type="button" data-close-dialog="entity-dialog" class="icon-button">×</button></div>
    <label>Email<input name="email" type="email" required maxlength="254" value="${escapeHtml(user?.email||'')}"></label>
    <label>Отображаемое имя<input name="displayName" required maxlength="160" value="${escapeHtml(user?.displayName||'')}"></label>
    ${user?'': '<label>Временный пароль<input name="temporaryPassword" type="password" required minlength="12" maxlength="128"></label>'}
    <fieldset><legend>Роли</legend>${['VIEWER','EDITOR','ADMIN'].map(r=>`<label class="inline"><input type="checkbox" name="roles" value="${r}" ${checked(r)?'checked':''}> ${escapeHtml(roleLabel(r))}</label>`).join('')}</fieldset>
    ${user?`<label class="inline"><input type="checkbox" name="enabled" ${user.enabled?'checked':''}> Активен</label>`:''}
    <button class="primary">Сохранить</button>`;
}

async function openUser(id = null) {
    const user = id ? state.users.find(u=>u.id===id) : null;
    const form=$('entity-form'); form.innerHTML=userForm(user);
    form.onsubmit=async event=>{event.preventDefault(); const data=new FormData(form); const payload={email:data.get('email'),displayName:data.get('displayName'),roles:data.getAll('roles'),...(user?{enabled:data.get('enabled')==='on',version:user.version}:{temporaryPassword:data.get('temporaryPassword')})};
        try{await api(id?`/admin/users/${id}`:'/admin/users',{method:id?'PUT':'POST',body:JSON.stringify(payload)});$('entity-dialog').close();toast('Пользователь сохранён');await loadUsers();}catch(error){toast(error.message,true);}};
    $('entity-dialog').showModal();
}

async function resetUserPassword(id) {
    const password=prompt('Новый временный пароль (не менее 12 символов, регистры, цифра и спецсимвол)'); if(!password)return;
    try{await api(`/admin/users/${id}/reset-password`,{method:'POST',body:JSON.stringify({temporaryPassword:password})});toast('Пароль сброшен');await loadUsers();}catch(error){toast(error.message,true);}
}

async function loadAudit() {
    try {
        const q=encodeURIComponent($('audit-search').value.trim()); const page=await api(`/admin/audit?q=${q}&page=${state.auditPage}&size=10&sort=${$('audit-sort').value}`);
        $('audit-page').textContent=`${page.page+1} / ${Math.max(1,page.totalPages)}`; $('audit-prev').disabled=page.first; $('audit-next').disabled=page.last;
        $('audit-body').innerHTML=page.content.map(item=>`<tr><td>${formatDate(item.occurredAt)}</td><td>${escapeHtml(item.actorEmail||'system')}</td><td>${escapeHtml(item.eventType)}</td><td>${escapeHtml(item.entityType||'—')}</td><td>${escapeHtml(item.summary)}</td><td><code>${escapeHtml(item.correlationId||'—')}</code></td></tr>`).join('');
    } catch(error){toast(error.message,true);}
}

function openPasswordDialog(required=false) {
    return new Promise(resolve => {
        const dialog = $('entity-dialog');
        const form = $('entity-form');

        const currentPasswordField = required
            ? ''
            : '<label>Текущий пароль<input name="current" type="password" required maxlength="128" autocomplete="current-password"></label>';

        form.innerHTML = `<div class="dialog-head"><h3>${required ? 'Установка постоянного пароля' : 'Смена пароля'}</h3>${required ? '' : '<button type="button" data-close-dialog="entity-dialog" class="icon-button">×</button>'}</div>
            ${currentPasswordField}
            <label>Новый пароль<input name="next" type="password" required minlength="12" maxlength="128" autocomplete="new-password"></label>
            <label>Повторите новый пароль<input name="confirm" type="password" required minlength="12" maxlength="128" autocomplete="new-password"></label>
            <p class="muted">Минимум 12 символов: верхний/нижний регистр, цифра и специальный символ.</p>
            <button class="primary">${required ? 'Установить пароль' : 'Изменить'}</button>`;

        let completed = false;

        const onClose = () => {
            dialog.removeEventListener('close', onClose);
            if (!completed) resolve(false);
        };
        dialog.addEventListener('close', onClose);

        form.onsubmit = async event => {
            event.preventDefault();

            const data = new FormData(form);
            const next = String(data.get('next') || '');
            const confirm = String(data.get('confirm') || '');

            if (next !== confirm) {
                toast('Новые пароли не совпадают', true);
                return;
            }

            try {
                if (required) {
                    await api('/auth/change-initial-password', {
                        method: 'POST',
                        body: JSON.stringify({newPassword: next})
                    });

                    /*
                     * Access JWT был выпущен до смены пароля и ещё содержит
                     * passwordChangeRequired=true. Refresh-cookie ротируется, после чего
                     * клиент получает новый access JWT с актуальным значением из БД.
                     */
                    const refreshed = await refreshAccessToken();
                    if (!refreshed) {
                        completed = true;
                        dialog.close();
                        showLogin();
                        toast('Пароль изменён, но сессию не удалось обновить. Войдите новым паролем.', true);
                        resolve(false);
                        return;
                    }

                    state.user.passwordChangeRequired = false;
                    completed = true;
                    dialog.close();
                    toast('Постоянный пароль установлен');
                    resolve(true);
                    return;
                }

                await api('/auth/change-password', {
                    method: 'POST',
                    body: JSON.stringify({
                        currentPassword: data.get('current'),
                        newPassword: next
                    })
                });

                completed = true;
                dialog.close();
                toast('Пароль изменён. Выполните вход заново.');
                resolve(true);
                await logout();
            } catch (error) {
                toast(error.message, true);
            }
        };

        dialog.showModal();
    });
}

async function logout() {
    try { await fetch('/api/auth/logout',{method:'POST',credentials:'include',headers:{'Content-Type':'application/json'},body:'{}'}); } catch (_) {}
    showLogin();
}

function bindEvents() {
    for (const kind of ['clients','operators']) {
        $(`${kind}-find`).onclick=()=>loadDirectory(kind,0);
        $(`${kind}-search`).onkeydown=e=>{if(e.key==='Enter')loadDirectory(kind,0);};
        $(`${kind}-add`).onclick=()=>openDirectory(kind);
        $(`${kind}-prev`).onclick=()=>loadDirectory(kind,directories[kind].page-1);
        $(`${kind}-next`).onclick=()=>loadDirectory(kind,directories[kind].page+1);
        $(`${kind}-body`).onclick=e=>{const b=e.target.closest('[data-directory-id]');if(!b)return;const row=directories[kind].content.find(x=>String(x.id)===b.dataset.directoryId);
            if(b.dataset.action==='delete')deleteDirectory(kind,row);else openDirectory(kind,row,b.dataset.action==='view');};
    }

    $('appeals-sort').addEventListener('change',()=>loadAppeals(0));
    $('users-prev').onclick=()=>{state.userPage--;loadUsers();}; $('users-next').onclick=()=>{state.userPage++;loadUsers();};
    $('audit-prev').onclick=()=>{state.auditPage--;loadAudit();}; $('audit-next').onclick=()=>{state.auditPage++;loadAudit();};
    $('users-sort').onchange=()=>{state.userPage=0;loadUsers();}; $('audit-sort').onchange=()=>{state.auditPage=0;loadAudit();};
    $('csv-file').onchange=$('csv-mode').onchange=()=>{$('csv-apply').disabled=true;};
    $('notes-list').addEventListener('click',async e=>{const b=e.target.closest('[data-note-edit]');if(!b)return;
      const n=state.notes.find(n=>n.id===b.dataset.noteEdit);const body=prompt('Текст заметки',n.body);if(body===null)return;
      const pinned=confirm('Закрепить заметку?');try{await api(`/appeals/${state.activeAppealForNotes}/notes/${n.id}`,{method:'PUT',body:JSON.stringify({body,pinned,version:n.version})});await renderNotes();}catch(e){toast(e.message,true);}});

    $('login-form').addEventListener('submit', async event => {
        event.preventDefault();

        const form = $('login-form');
        const button = form.querySelector('button[type="submit"]');
        const errorNode = $('login-error');

        errorNode.hidden = true;
        button.disabled = true;
        const originalCaption = button.textContent;
        button.textContent = 'Входим…';

        try {
            const response = await fetch('/api/auth/login', {
                method: 'POST',
                credentials: 'include',
                headers: {'Content-Type':'application/json'},
                body: JSON.stringify({
                    email: $('login-email').value,
                    password: $('login-password').value,
                    clientType: 'WEB'
                })
            });

            if (!response.ok) {
                throw new Error(await parseError(response));
            }

            const result = await response.json();
            state.accessToken = result.accessToken;
            state.user = result.user;
            $('login-password').value = '';

            await enterApplication();
        } catch (error) {
            /*
             * Если ошибка возникает после успешного HTTP login, экран входа снова
             * открывается и сообщение выводится в видимой форме.
             */
            console.error('[Колл-центр] Login flow failed', error);
            showLogin();
            errorNode.textContent = error.message || 'Не удалось выполнить вход';
            errorNode.hidden = false;
        } finally {
            button.disabled = false;
            button.textContent = originalCaption;
        }
    });
    $('navigation').addEventListener('click',event=>{const button=event.target.closest('[data-tab]');if(button&&!button.hidden)switchTab(button.dataset.tab);});
    $('logout').addEventListener('click',logout); $('change-password').addEventListener('click',()=>openPasswordDialog(false));
    $('search-appeals').addEventListener('click',()=>{state.appeals.query=$('appeal-search').value.trim();state.appeals.page=0;loadAppeals(0);});
    $('appeal-search').addEventListener('keydown',event=>{if(event.key==='Enter')$('search-appeals').click();});
    $('appeals-prev').addEventListener('click',()=>loadAppeals(state.appeals.page-1)); $('appeals-next').addEventListener('click',()=>loadAppeals(state.appeals.page+1));
    $('add-appeal').addEventListener('click',()=>openAppeal()); $('bulk-action').addEventListener('click',bulkAction); $('export-csv').addEventListener('click',exportCsv);
    $('import-csv').addEventListener('click',()=>{$('csv-result').textContent='';$('csv-apply').disabled=true;$('csv-dialog').showModal();});
    $('add-center').addEventListener('click',()=>openCallCenter()); $('add-user').addEventListener('click',()=>openUser()); $('search-audit').addEventListener('click',()=>{state.auditPage=0;loadAudit();});
    $('appeals-body').addEventListener('click',event=>handleAppealTableClick(event));
    $('appeals-body').addEventListener('change',event=>{const checkbox=event.target.closest('[data-appeal-select]');if(checkbox){checkbox.checked?state.selectedAppealIds.add(checkbox.dataset.appealSelect):state.selectedAppealIds.delete(checkbox.dataset.appealSelect);}});
    $('select-all-appeals').addEventListener('change',event=>{document.querySelectorAll('[data-appeal-select]').forEach(cb=>{cb.checked=event.target.checked;event.target.checked?state.selectedAppealIds.add(cb.dataset.appealSelect):state.selectedAppealIds.delete(cb.dataset.appealSelect);});});
    $('centers-body').addEventListener('click',event=>{const edit=event.target.closest('[data-center-edit]');const toggle=event.target.closest('[data-center-toggle]');if(edit)openCallCenter(edit.dataset.centerEdit);if(toggle)toggleCallCenter(toggle.dataset.centerToggle);});
    $('users-body').addEventListener('click',event=>{const edit=event.target.closest('[data-user-edit]');const reset=event.target.closest('[data-user-reset]');if(edit)openUser(edit.dataset.userEdit);if(reset)resetUserPassword(reset.dataset.userReset);});
    $('note-form').addEventListener('submit',async event=>{event.preventDefault();try{await api(`/appeals/${state.activeAppealForNotes}/notes`,{method:'POST',body:JSON.stringify({body:$('note-body').value,pinned:$('note-pinned').checked,version:null})});$('note-body').value='';$('note-pinned').checked=false;await renderNotes();}catch(error){toast(error.message,true);}});
    $('notes-list').addEventListener('click',async event=>{const button=event.target.closest('[data-note-delete]');if(button&&confirm('Удалить заметку?')){try{await api(`/appeals/${state.activeAppealForNotes}/notes/${button.dataset.noteDelete}`,{method:'DELETE'});await renderNotes();}catch(error){toast(error.message,true);}}});
    $('csv-preview').addEventListener('click',()=>runCsv(false)); $('csv-apply').addEventListener('click',()=>runCsv(true));
    document.body.addEventListener('click',event=>{const close=event.target.closest('[data-close-dialog]');if(close)$(close.dataset.closeDialog).close();});
}

function handleAppealTableClick(event) {
    const permanent=event.target.closest('[data-appeal-permanent]');
    if(permanent && confirm('Безвозвратно удалить обращение и заметки?')) api(`/appeals/${permanent.dataset.appealPermanent}/permanent`,{method:'DELETE',headers:{'X-Confirm-Delete':'PERMANENTLY_DELETE'}}).then(()=>loadAppeals()).catch(e=>toast(e.message,true));
    const view=event.target.closest('[data-appeal-view]'), edit=event.target.closest('[data-appeal-edit]'), notes=event.target.closest('[data-appeal-notes]'), archive=event.target.closest('[data-appeal-archive]');
    if(view)openAppeal(view.dataset.appealView,true); if(edit)openAppeal(edit.dataset.appealEdit,false); if(notes)openNotes(notes.dataset.appealNotes); if(archive)archiveAppeal(archive.dataset.appealArchive);
}

async function runCsv(apply) {
    const file=$('csv-file').files[0]; if(!file)return toast('Выберите CSV-файл',true);
    const form=new FormData();form.append('file',file);
    try{const result=await api(`/appeals/import/${apply?'apply':'preview'}?mode=${$('csv-mode').value}`,{method:'POST',body:form});$('csv-result').textContent=JSON.stringify(result,null,2);$('csv-apply').disabled=apply?true:result.invalidRows>0;if(apply){toast('Импорт завершён');await Promise.all([loadAppeals(),loadDashboard()]);}}catch(error){toast(error.message,true);$('csv-result').textContent=error.message;}
}

function statusOptions(selected='NEW'){return ['NEW','IN_PROGRESS','ESCALATED','RESOLVED','CLOSED'].map(v=>`<option value="${v}" ${v===selected?'selected':''}>${statusLabel(v)}</option>`).join('');}
function statusLabel(value){return ({NEW:'Новое',IN_PROGRESS:'В работе',ESCALATED:'Эскалировано',RESOLVED:'Решено',CLOSED:'Закрыто'})[value]||value;}
function priorityLabel(value){return ({NOT_APPLICABLE:'Не применяется',LOW:'Низкий',MEDIUM:'Средний',HIGH:'Высокий',CRITICAL:'Критический'})[value]||value;}
function formatDate(value){return value?new Intl.DateTimeFormat('ru-RU',{dateStyle:'short',timeStyle:'short'}).format(new Date(value)):'—';}
function empty(value){const normalized=String(value??'').trim();return normalized||null;}


const directories={clients:{page:0,content:[]},operators:{page:0,content:[]}};
async function loadDirectory(kind,page=directories[kind].page){
 try{
  const data=await api(`/directory/${kind}?page=${page}&size=25&q=${encodeURIComponent($(`${kind}-search`).value)}`);
  directories[kind]={page:data.number,content:data.content};
  const edit=kind==='clients'?canEdit():isAdmin();
  $(`${kind}-body`).innerHTML=data.content.map(row=>`<tr><td>${escapeHtml(row.fullName)}</td>${kind==='clients'?`<td>${escapeHtml(row.phone)}</td><td>${escapeHtml(row.email||'—')}</td>`:`<td>${escapeHtml(row.login)}</td><td>${escapeHtml(row.centerCode)}</td><td>${row.active?'Да':'Нет'}</td>`}<td><div class="actions"><button data-directory-id="${row.id}" data-action="view">Карточка</button>${edit?`<button data-directory-id="${row.id}" data-action="edit">Изменить</button>`:''}${isAdmin()?`<button data-directory-id="${row.id}" data-action="delete">Удалить</button>`:''}</div></td></tr>`).join('')||'<tr><td colspan="5">Записей нет</td></tr>';
  $(`${kind}-page`).textContent=`Страница ${data.number+1} из ${Math.max(1,data.totalPages)} · записей ${data.totalElements}`;
  $(`${kind}-prev`).disabled=data.first;$(`${kind}-next`).disabled=data.last;
 }catch(e){toast(e.message,true);}
}
async function openDirectory(kind,row=null,readOnly=false){
 const operator=kind==='operators';
 if(operator)await loadCallCenters();
 const form=$('entity-form');
 form.innerHTML=`<div class="dialog-head"><h3>${readOnly?'Карточка':row?'Изменение':'Создание'} ${operator?'оператора':'клиента'}</h3><button type="button" data-close-dialog="entity-dialog">×</button></div>
 <fieldset ${readOnly?'disabled':''}><label>Имя<input name="fullName" required maxlength="150" value="${escapeHtml(row?.fullName||'')}"></label>
 ${operator?`<label>Логин<input name="login" required pattern="[a-zA-Z0-9_.-]{3,50}" maxlength="50" value="${escapeHtml(row?.login||'')}"></label><p class="muted">Логин сотрудника — не учётная запись входа в приложение.</p>
 <label>Колл-центр<select name="centerId" required><option value="">Выберите колл-центр</option>${state.centers.filter(c=>c.active||c.id===row?.centerId).map(c=>`<option value="${c.id}" ${c.id===row?.centerId?'selected':''}>${escapeHtml(c.code)}</option>`).join('')}</select></label>
 <label class="inline"><input type="checkbox" name="active" ${row?.active!==false?'checked':''}>Активен</label>`:
 `<label>Телефон<input name="phone" required maxlength="30" value="${escapeHtml(row?.phone||'')}"></label><label>Email<input name="email" type="email" maxlength="150" value="${escapeHtml(row?.email||'')}"></label><p class="muted">Изменение контактов не меняет сохранённые снимки в прежних обращениях.</p>`}</fieldset>
 <p class="error" data-form-error hidden></p>${readOnly?'':'<button class="primary">Сохранить</button>'}`;
 form.onsubmit=async e=>{e.preventDefault();if(readOnly)return;clearFormError(form);const d=new FormData(form);const payload={fullName:d.get('fullName'),version:row?.version??null,...(operator?{login:d.get('login'),centerId:d.get('centerId'),active:d.get('active')==='on'}:{phone:d.get('phone'),email:empty(d.get('email'))})};
 try{await api(`/directory/${kind}${row?'/'+row.id:''}`,{method:row?'PUT':'POST',body:JSON.stringify(payload)});$('entity-dialog').close();await loadDirectory(kind);if(operator)state.operators=await api('/operators');toast('Запись сохранена');}catch(err){showFormError(form,err.message);}};
 $('entity-dialog').showModal();
}
async function deleteDirectory(kind,row){
 if(!confirm(`Удалить ${row.fullName}? Запись с обращениями удалить нельзя.`))return;
 try{await api(`/directory/${kind}/${row.id}?version=${row.version}`,{method:'DELETE'});await loadDirectory(kind,0);if(kind==='operators')state.operators=await api('/operators');toast('Запись удалена');}catch(e){toast(e.message,true);}
}

boot().catch(error => {
    console.error('[Колл-центр] Boot failed', error);
    showLogin();
    toast(error.message || 'Не удалось инициализировать приложение', true);
});
