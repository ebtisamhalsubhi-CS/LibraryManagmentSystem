const $ = s => document.querySelector(s);
const $$ = s => [...document.querySelectorAll(s)];

const state = { user: null, books: [], members: [], loans: [], searchBy: 'title', query: '', results: null, mode: 'login' };

const spineColors = [
    ['#1e3a2f', '#fbf7ee'], ['#8a2a2b', '#fbf7ee'], ['#2f4b6e', '#fbf7ee'], ['#d9c49a', '#2a2117', '#7a5434'],
    ['#5b3a5e', '#fbf7ee'], ['#a0522d', '#fbf7ee'], ['#3d5a40', '#fbf7ee'], ['#24323f', '#e9d6ae'],
    ['#efe6d2', '#1f1d1a', '#8a2a2b'], ['#7a6a3a', '#fbf7ee'],
];

function esc(value) {
    return String(value).replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}

async function api(path, method = 'GET', data) {
    const options = { method, headers: {}, credentials: 'same-origin' };
    if (data) {
        options.headers['Content-Type'] = 'application/x-www-form-urlencoded';
        options.body = new URLSearchParams(data).toString();
    }
    const res = await fetch('/api/' + path, options);
    const body = await res.json().catch(() => ({}));
    if (res.status === 401 && path !== 'login') {
        showAuth();
    }
    if (!res.ok) throw new Error(body.error || 'Something went wrong');
    return body;
}

let toastTimer;
function toast(message, bad = false) {
    const t = $('#toast');
    t.textContent = message;
    t.classList.toggle('bad', bad);
    t.classList.add('show');
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => t.classList.remove('show'), 2800);
}

function hash(text) {
    let h = 7;
    for (const ch of String(text)) h = (h * 31 + ch.charCodeAt(0)) >>> 0;
    return h;
}

function today() {
    const d = new Date();
    return new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().slice(0, 10);
}

function niceDate(iso) {
    const [y, m, d] = iso.split('-').map(Number);
    return new Date(y, m - 1, d).toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' });
}

function showAuth() {
    state.user = null;
    $('#app').hidden = true;
    $('#auth').hidden = false;
    $('#card-no').textContent = 'No. ' + String(1000 + Math.floor(Math.random() * 9000));
}

function setMode(mode) {
    state.mode = mode;
    $$('.tab').forEach(t => t.classList.toggle('on', t.dataset.mode === mode));
    $('#auth-submit').textContent = mode === 'login' ? 'Log in' : 'Create account';
    $('#auth-form').password.autocomplete = mode === 'login' ? 'current-password' : 'new-password';
    $('#auth-note').textContent = mode === 'login'
        ? 'New here? Create an account first, then you can log in any time.'
        : 'Your library starts with a few books and members so you can try it right away.';
    $('#auth-error').textContent = '';
}

$$('.tab').forEach(t => t.addEventListener('click', () => setMode(t.dataset.mode)));

$('#auth-form').addEventListener('submit', async e => {
    e.preventDefault();
    const form = e.target;
    const username = form.username.value.trim();
    const password = form.password.value;
    const err = $('#auth-error');
    if (username.length < 3) return err.textContent = 'Username must be at least 3 characters';
    if (password.length < 6) return err.textContent = 'Password must be at least 6 characters';
    const btn = $('#auth-submit');
    btn.disabled = true;
    try {
        const res = await api(state.mode, 'POST', { username, password });
        form.reset();
        await enter(res.user);
        toast(state.mode === 'signup' ? 'Account created. Welcome!' : 'Login successful');
    } catch (ex) {
        err.textContent = ex.message;
    } finally {
        btn.disabled = false;
    }
});

async function enter(user) {
    state.user = user;
    $('#user-name').textContent = '@' + user;
    $('#auth').hidden = true;
    $('#app').hidden = false;
    await refresh();
    route();
}

$('#logout').addEventListener('click', async () => {
    await api('logout', 'POST', {}).catch(() => {});
    showAuth();
    setMode('login');
    toast('ThankYou. See you next time');
});

async function refresh() {
    const [books, members, loans] = await Promise.all([api('books'), api('members'), api('loans')]);
    state.books = books;
    state.members = members;
    state.loans = loans;
    if (state.query) await runSearch(true);
    render();
}

function render() {
    renderStats();
    renderShelf();
    renderMembers();
    renderLoans();
}

function renderStats() {
    const out = state.books.filter(b => b.status === 'BORROWED').length;
    const items = [
        [state.books.length, 'Books'],
        [state.books.length - out, 'Available'],
        [out, 'Borrowed'],
        [state.members.length, 'Members'],
    ];
    $('#stats').innerHTML = items.map(([n, l]) => `<div class="stat"><b>${n}</b><span>${l}</span></div>`).join('');
}

function spineHTML(b) {
    const h = hash(b.title + b.id);
    const [c, t, band] = spineColors[h % spineColors.length];
    const len = b.title.length;
    const fs = len > 20 ? 11.5 : len > 14 ? 12.5 : 14;
    const width = Math.min(60, Math.max(38, 34 + (h % 4) * 6));
    const height = Math.round(Math.max(132, 60 + len * fs * .56 + (h % 3) * 8));
    const out = b.status === 'BORROWED';
    return `<div class="slot"><button type="button" class="spine${out ? ' out' : ''}" data-id="${b.id}"
        style="--c:${c};--t:${t};${band ? `--band:${band};` : ''}--w:${width}px;--h:${height}px;--fs:${fs}px"
        aria-label="${esc(b.title)} by ${esc(b.author)}, ${out ? 'borrowed' : 'available'}">
        ${out ? '<span class="tag">OUT</span>' : ''}
        <span class="t">${esc(b.title)}</span><span class="n">${b.id}</span></button></div>`;
}

function renderShelf() {
    const list = state.results ?? state.books;
    const box = $('#bookcase');
    if (!list.length) {
        box.innerHTML = state.results
            ? '<div class="empty"><div><b>Book not found</b>Try another word or search drawer.</div></div>'
            : '<div class="empty"><div><b>No books found</b>Add your first book to fill the shelf.</div></div>';
    } else {
        box.innerHTML = list.map(spineHTML).join('');
    }
    $('#result-note').textContent = state.results
        ? `${list.length} ${list.length === 1 ? 'book' : 'books'} found for “${state.query}”`
        : '';
}

function renderMembers() {
    const box = $('#members');
    if (!state.members.length) {
        box.innerHTML = '<p class="hint">No members found</p>';
        return;
    }
    box.innerHTML = state.members.map(m => `
        <article class="member">
            <div class="m-top"><span>Member</span><span class="mono">No. ${m.id}</span></div>
            <h3>${esc(m.name)}</h3>
            <span class="m-contact">${esc(m.contact)}</span>
            <div class="m-foot"><span class="m-loans">${m.borrowed ? `${m.borrowed} ${m.borrowed === 1 ? 'book' : 'books'} borrowed` : 'No books borrowed'}</span><span class="barcode"></span></div>
        </article>`).join('');
}

function renderLoans() {
    const box = $('#loans');
    if (!state.loans.length) {
        box.innerHTML = '<p class="none">No books borrowed yet. Borrow one from the shelf.</p>';
        return;
    }
    const now = today();
    box.innerHTML = `<div class="loan head"><span>Book</span><span>Member</span><span>Borrowed</span><span>Due</span><span></span></div>` +
        state.loans.map(l => {
            const late = !l.returned && l.due < now;
            return `<div class="loan">
                <span class="l-book">${esc(l.title)}<small>Book ${l.bookId}</small></span>
                <span class="l-member">${esc(l.member)}<small>Member ${l.memberId}</small></span>
                <span class="date"><i>Borrowed</i>${niceDate(l.borrowed)}</span>
                <span class="due-wrap"><i>Due</i><span class="due${late ? ' late' : ''}">${niceDate(l.due)}</span></span>
                <span class="act">${l.returned
                    ? `<span class="returned">Returned ${niceDate(l.returned)}</span>`
                    : `<button type="button" class="btn outline small" data-return="${l.bookId}">Return</button>`}</span>
            </div>`;
        }).join('');
}

function openModal(html) {
    $('#modal-body').innerHTML = html;
    $('#modal').hidden = false;
    document.body.style.overflow = 'hidden';
    const first = $('#modal-body').querySelector('input, select, .btn');
    if (first) setTimeout(() => first.focus(), 30);
}

function closeModal() {
    $('#modal').hidden = true;
    document.body.style.overflow = '';
}

$('#modal').addEventListener('click', e => {
    if (e.target.closest('[data-close]')) closeModal();
});

document.addEventListener('keydown', e => {
    if (e.key === 'Escape' && !$('#modal').hidden) closeModal();
});

function memberOptions() {
    return state.members.map(m => `<option value="${m.id}">${esc(m.name)} · No. ${m.id}</option>`).join('');
}

function bookCard(id) {
    const b = state.books.find(x => x.id === id);
    if (!b) return;
    const out = b.status === 'BORROWED';
    const loan = state.loans.find(l => l.bookId === id && !l.returned);
    openModal(`
        <div class="index-card">
            <div class="call"><span>Call No. ${b.id}</span><span>Printed book</span></div>
            <h3 id="modal-title">${esc(b.title)}</h3>
            <p><span class="k">Author:</span> ${esc(b.author)}</p>
            <p><span class="k">Pages:</span> ${b.pages}</p>
            ${loan ? `<p><span class="k">Due:</span> ${niceDate(loan.due)} · ${esc(loan.member)}</p>` : ''}
            <div class="stamp-box"><span class="stamp ${out ? 'out' : 'ok'}">${out ? 'BORROWED' : 'AVAILABLE'}</span></div>
        </div>
        <div id="card-panel"></div>
        <div class="actions" id="card-actions">
            ${out
                ? `<button type="button" class="btn primary" data-act="return">Return book</button>`
                : `<button type="button" class="btn primary" data-act="borrow">Borrow</button>`}
            <button type="button" class="btn outline" data-act="edit">Edit</button>
            <button type="button" class="btn danger" data-act="delete">Delete</button>
        </div>`);

    $('#card-actions').addEventListener('click', e => {
        const act = e.target.closest('[data-act]')?.dataset.act;
        const panel = $('#card-panel');
        if (act === 'return') return doReturn(b.id);
        if (act === 'borrow') {
            if (!state.members.length) return toast('Add a member first', true);
            panel.innerHTML = `<form class="sub-panel" id="borrow-form">
                <label class="field"><span>Borrow for member</span><select name="memberId">${memberOptions()}</select></label>
                <p>It will be due on <b>${niceDate(addDays(14))}</b>.</p>
                <button class="btn primary wide" type="submit">Confirm borrow</button></form>`;
            panel.querySelector('select').focus();
            $('#borrow-form').addEventListener('submit', ev => {
                ev.preventDefault();
                doBorrow(b.id, ev.target.memberId.value);
            });
        }
        if (act === 'edit') {
            panel.innerHTML = `<form class="sub-panel" id="edit-form" novalidate>
                <label class="field"><span>New title</span><input name="title" value="${esc(b.title)}" maxlength="120" required></label>
                <label class="field"><span>New author</span><input name="author" value="${esc(b.author)}" maxlength="120" required></label>
                <p class="form-error"></p>
                <button class="btn primary wide" type="submit">Update book</button></form>`;
            panel.querySelector('input').focus();
            $('#edit-form').addEventListener('submit', async ev => {
                ev.preventDefault();
                const f = ev.target;
                try {
                    const res = await api('books/' + b.id, 'PUT', { title: f.title.value, author: f.author.value });
                    toast(res.message);
                    closeModal();
                    await refresh();
                } catch (ex) {
                    f.querySelector('.form-error').textContent = ex.message;
                }
            });
        }
        if (act === 'delete') {
            panel.innerHTML = `<div class="sub-panel"><p>Delete <b>${esc(b.title)}</b> from the library?</p>
                <div class="actions"><button type="button" class="btn danger solid" id="yes-delete">Yes, delete</button>
                <button type="button" class="btn outline" data-close>Keep it</button></div></div>`;
            $('#yes-delete').addEventListener('click', async () => {
                try {
                    const res = await api('books/' + b.id, 'DELETE');
                    toast(res.message);
                    closeModal();
                    await refresh();
                } catch (ex) {
                    toast(ex.message, true);
                }
            });
        }
    });
}

function addDays(n) {
    const d = new Date();
    d.setDate(d.getDate() + n);
    return new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().slice(0, 10);
}

async function doBorrow(bookId, memberId) {
    try {
        const res = await api('loans', 'POST', { bookId, memberId });
        toast(res.message + ' · Due ' + niceDate(addDays(14)));
        closeModal();
        await refresh();
    } catch (ex) {
        toast(ex.message, true);
    }
}

async function doReturn(bookId) {
    try {
        const res = await api('returns', 'POST', { bookId });
        toast(res.message);
        closeModal();
        await refresh();
    } catch (ex) {
        toast(ex.message, true);
    }
}

$('#bookcase').addEventListener('click', e => {
    const spine = e.target.closest('.spine');
    if (spine) bookCard(Number(spine.dataset.id));
});

$('#loans').addEventListener('click', e => {
    const btn = e.target.closest('[data-return]');
    if (btn) doReturn(btn.dataset.return);
});

function formModal(title, fields, submitText, onSubmit) {
    openModal(`<h3 id="modal-title">${title}</h3>
        <form id="m-form" novalidate>${fields}<p class="form-error"></p>
        <button class="btn primary wide" type="submit">${submitText}</button></form>`);
    $('#m-form').addEventListener('submit', async e => {
        e.preventDefault();
        const f = e.target;
        const btn = f.querySelector('[type=submit]');
        btn.disabled = true;
        try {
            await onSubmit(Object.fromEntries(new FormData(f)));
        } catch (ex) {
            f.querySelector('.form-error').textContent = ex.message;
        } finally {
            btn.disabled = false;
        }
    });
}

function nextId(list, start) {
    return list.length ? Math.max(...list.map(x => x.id)) + 1 : start;
}

$('#add-book').addEventListener('click', () => formModal('Add a book', `
    <div class="row2">
        <label class="field"><span>Book ID</span><input name="id" type="number" inputmode="numeric" min="1" value="${nextId(state.books, 101)}" required></label>
        <label class="field"><span>Pages</span><input name="pages" type="number" inputmode="numeric" min="1" placeholder="e.g. 240" required></label>
    </div>
    <label class="field"><span>Title</span><input name="title" maxlength="120" required></label>
    <label class="field"><span>Author</span><input name="author" maxlength="120" required></label>`,
    'Add to shelf', async data => {
        if (!data.title.trim() || !data.author.trim()) throw new Error('Title and author are required');
        if (!(data.pages > 0)) throw new Error('Please enter the number of pages');
        const res = await api('books', 'POST', data);
        toast(res.message);
        closeModal();
        await refresh();
    }));

$('#add-member').addEventListener('click', () => formModal('Add a member', `
    <label class="field"><span>Member ID</span><input name="id" type="number" inputmode="numeric" min="1" value="${nextId(state.members, 202)}" required></label>
    <label class="field"><span>Name</span><input name="name" maxlength="120" autocomplete="off" required></label>
    <label class="field"><span>Phone</span><input name="phone" type="tel" inputmode="tel" maxlength="20" required></label>`,
    'Add member', async data => {
        if (!data.name.trim() || !data.phone.trim()) throw new Error('Name and phone are required');
        const res = await api('members', 'POST', data);
        toast(res.message);
        closeModal();
        await refresh();
    }));

$('#new-loan').addEventListener('click', () => {
    const available = state.books.filter(b => b.status === 'AVAILABLE');
    if (!available.length) return toast('Book is not available', true);
    if (!state.members.length) return toast('Add a member first', true);
    formModal('Borrow a book', `
        <label class="field"><span>Book</span><select name="bookId">${available.map(b => `<option value="${b.id}">${esc(b.title)} · ${b.id}</option>`).join('')}</select></label>
        <label class="field"><span>Member</span><select name="memberId">${memberOptions()}</select></label>
        <p class="hint" style="text-align:left;margin:0 0 12px">Due on <b>${niceDate(addDays(14))}</b></p>`,
        'Confirm borrow', async data => {
            const res = await api('loans', 'POST', data);
            toast(res.message + ' · Due ' + niceDate(addDays(14)));
            closeModal();
            await refresh();
        });
});

const placeholders = { title: 'Search by title…', author: 'Search by author…', id: 'Search by book ID…' };

$$('.drawer').forEach(d => d.addEventListener('click', () => {
    state.searchBy = d.dataset.by;
    $$('.drawer').forEach(x => x.classList.toggle('on', x === d));
    const input = $('#search-input');
    input.placeholder = placeholders[state.searchBy];
    input.inputMode = state.searchBy === 'id' ? 'numeric' : 'search';
    input.focus();
    runSearch();
}));

let searchTimer;
$('#search-input').addEventListener('input', () => {
    clearTimeout(searchTimer);
    searchTimer = setTimeout(runSearch, 220);
});

$('#search-form').addEventListener('submit', e => {
    e.preventDefault();
    runSearch();
});

$('#search-clear').addEventListener('click', () => {
    $('#search-input').value = '';
    runSearch();
    $('#search-input').focus();
});

async function runSearch(silent) {
    const q = $('#search-input').value.trim();
    state.query = q;
    $('#search-clear').hidden = !q;
    if (!q) {
        state.results = null;
    } else if (state.searchBy === 'id' && !/^\d+$/.test(q)) {
        state.results = [];
    } else {
        try {
            state.results = await api('books/search?' + new URLSearchParams({ by: state.searchBy, q }));
        } catch (ex) {
            state.results = [];
        }
    }
    if (!silent) renderShelf();
}

function route() {
    const view = ['shelf', 'members', 'loans'].includes(location.hash.slice(1)) ? location.hash.slice(1) : 'shelf';
    $$('.view').forEach(v => v.hidden = v.id !== 'view-' + view);
    $$('.nav a').forEach(a => a.classList.toggle('on', a.dataset.view === view));
}

window.addEventListener('hashchange', () => {
    route();
    window.scrollTo(0, 0);
});

(async () => {
    setMode('login');
    try {
        const me = await api('me');
        if (me.user) return enter(me.user);
    } catch (e) {}
    showAuth();
})();
