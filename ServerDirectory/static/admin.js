"use strict";

const API = "";  // même origine

// ── Clé API ───────────────────────────────────────────────────────────────────

function getApiKey() {
  return localStorage.getItem("vtml_api_key") || "";
}

function saveApiKey() {
  const val = document.getElementById("api-key").value.trim();
  localStorage.setItem("vtml_api_key", val);
  showApiKeyStatus("Clé enregistrée ✓", "var(--success)");
}

function showApiKeyStatus(msg, color) {
  const el = document.getElementById("api-key-status");
  el.textContent = msg;
  el.style.color = color;
  setTimeout(() => { el.textContent = ""; }, 3000);
}

// ── Toast ─────────────────────────────────────────────────────────────────────

let _toastTimer = null;

function toast(msg, type = "ok") {
  const el = document.getElementById("toast");
  el.textContent = msg;
  el.className = "show " + type;
  clearTimeout(_toastTimer);
  _toastTimer = setTimeout(() => { el.className = ""; }, 3000);
}

// ── Fetch helpers ─────────────────────────────────────────────────────────────

async function apiFetch(path, opts = {}) {
  const headers = { "Content-Type": "application/json", ...opts.headers };
  const key = getApiKey();
  if (key) headers["X-API-Key"] = key;
  const res = await fetch(API + path, { ...opts, headers });
  if (!res.ok) {
    const err = await res.json().catch(() => ({ detail: res.statusText }));
    throw new Error(err.detail || res.statusText);
  }
  return res.status === 204 ? null : res.json();
}

// ── Chargement liste ──────────────────────────────────────────────────────────

let _allServers = [];
let _filteredServers = [];

async function loadServers() {
  try {
    const data = await apiFetch("/api/servers?limit=200");
    _allServers = data.items || [];
    applyFilter();
  } catch (e) {
    toast("Erreur chargement : " + e.message, "err");
    document.getElementById("server-list").innerHTML =
      `<tr><td colspan="8" class="empty-state">Erreur : ${e.message}</td></tr>`;
  }
}

function applyFilter() {
  const q = document.getElementById("global-search").value.toLowerCase();
  _filteredServers = q
    ? _allServers.filter(s =>
        (s.name || "").toLowerCase().includes(q) ||
        (s.url || "").toLowerCase().includes(q) ||
        (s.description || "").toLowerCase().includes(q) ||
        (s.categories || []).some(c => c.toLowerCase().includes(q)) ||
        (s.keywords || []).some(k => k.toLowerCase().includes(q))
      )
    : [..._allServers];
  renderTable();
}

function renderTable() {
  const tbody = document.getElementById("server-list");
  document.getElementById("count-label").textContent =
    `${_filteredServers.length} serveur${_filteredServers.length !== 1 ? "s" : ""}`;

  if (_filteredServers.length === 0) {
    tbody.innerHTML = `<tr><td colspan="8" class="empty-state">Aucun serveur trouvé.</td></tr>`;
    return;
  }

  tbody.innerHTML = _filteredServers.map(s => `
    <tr>
      <td class="td-id">${s.id}</td>
      <td class="td-name">${esc(s.name)}</td>
      <td class="td-url">${esc(s.url)}</td>
      <td class="td-port">${s.port}</td>
      <td class="td-desc">${esc(s.description || "")}</td>
      <td class="td-cats">${(s.categories || []).map(c => `<span class="badge badge-cat">${esc(c)}</span>`).join("")}</td>
      <td>
        <span class="badge ${s.enabled ? "badge-on" : "badge-off"}">${s.enabled ? "actif" : "désactivé"}</span>
      </td>
      <td>
        <span class="reach-unk" id="reach-${s.id}" title="Cliquer pour tester">—</span>
      </td>
      <td class="td-acts">
        <button class="btn-ghost btn-sm btn-icon" title="Tester" onclick="testServer(${s.id})">⚡</button>
        <button class="btn-ghost btn-sm btn-icon" title="${s.enabled ? "Désactiver" : "Activer"}" onclick="toggleServer(${s.id})">
          ${s.enabled ? "⏸" : "▶"}
        </button>
        <button class="btn-ghost btn-sm btn-icon" title="Éditer" onclick="openEditModal(${s.id})">✏</button>
        <button class="btn-danger btn-sm btn-icon" title="Supprimer" onclick="confirmDelete(${s.id}, '${esc(s.name)}')">✕</button>
      </td>
    </tr>
  `).join("");
}

function esc(str) {
  return String(str)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");
}

// ── Test accessibilité ────────────────────────────────────────────────────────

async function testServer(id) {
  const el = document.getElementById("reach-" + id);
  if (el) { el.textContent = "…"; el.className = "reach-unk"; }
  try {
    const data = await apiFetch(`/api/servers/${id}/test`);
    if (el) {
      el.textContent = data.reachable ? "✓" : "✗";
      el.className = data.reachable ? "reach-ok" : "reach-no";
      el.title = data.reachable ? "Accessible" : (data.error || "Inaccessible");
    }
  } catch (e) {
    if (el) { el.textContent = "✗"; el.className = "reach-no"; el.title = e.message; }
  }
}

// ── Toggle actif/inactif ──────────────────────────────────────────────────────

async function toggleServer(id) {
  try {
    const data = await apiFetch(`/api/servers/${id}/toggle`, { method: "PATCH" });
    toast(`Serveur ${data.enabled ? "activé" : "désactivé"} ✓`, "ok");
    await loadServers();
  } catch (e) {
    toast("Erreur : " + e.message, "err");
  }
}

// ── Suppression ───────────────────────────────────────────────────────────────

let _pendingDeleteId = null;

function confirmDelete(id, name) {
  _pendingDeleteId = id;
  document.getElementById("confirm-msg").textContent = `Supprimer "${name}" ?`;
  document.getElementById("confirm-box").classList.add("open");
}

function closeConfirm() {
  _pendingDeleteId = null;
  document.getElementById("confirm-box").classList.remove("open");
}

document.getElementById("confirm-ok-btn").addEventListener("click", async () => {
  if (_pendingDeleteId === null) return;
  closeConfirm();
  try {
    await apiFetch(`/api/servers/${_pendingDeleteId}`, { method: "DELETE" });
    toast("Serveur supprimé ✓", "ok");
    await loadServers();
  } catch (e) {
    toast("Erreur suppression : " + e.message, "err");
  }
});

// ── Modal Ajout / Édition ─────────────────────────────────────────────────────

function resetForm() {
  document.getElementById("f-id").value = "";
  document.getElementById("f-name").value = "";
  document.getElementById("f-url").value = "";
  document.getElementById("f-port").value = "8080";
  document.getElementById("f-vtml").value = "1.0";
  document.getElementById("f-mkiwi").value = "1.0";
  document.getElementById("f-desc").value = "";
  document.getElementById("f-categories").value = "";
  document.getElementById("f-keywords").value = "";
  document.getElementById("f-email").value = "";
  document.getElementById("f-website").value = "";
  document.getElementById("f-enabled").checked = true;
}

function openAddModal() {
  resetForm();
  document.getElementById("modal-title").textContent = "Ajouter un serveur";
  document.getElementById("modal-submit-btn").textContent = "Ajouter";
  document.getElementById("server-modal").classList.add("open");
}

function openEditModal(id) {
  const s = _allServers.find(x => x.id === id);
  if (!s) return;
  resetForm();
  document.getElementById("f-id").value = s.id;
  document.getElementById("f-name").value = s.name || "";
  document.getElementById("f-url").value = s.url || "";
  document.getElementById("f-port").value = s.port || 8080;
  document.getElementById("f-vtml").value = s.vtml_version || "1.0";
  document.getElementById("f-mkiwi").value = s.mkiwi_server_version || "1.0";
  document.getElementById("f-desc").value = s.description || "";
  document.getElementById("f-categories").value = (s.categories || []).join(", ");
  document.getElementById("f-keywords").value = (s.keywords || []).join(", ");
  document.getElementById("f-email").value = s.admin_email || "";
  document.getElementById("f-website").value = s.website || "";
  document.getElementById("f-enabled").checked = s.enabled !== false;
  document.getElementById("modal-title").textContent = `Modifier — ${s.name}`;
  document.getElementById("modal-submit-btn").textContent = "Enregistrer";
  document.getElementById("server-modal").classList.add("open");
}

function closeModal() {
  document.getElementById("server-modal").classList.remove("open");
}

function splitTags(str) {
  return str.split(",").map(s => s.trim()).filter(s => s.length > 0);
}

async function submitForm(e) {
  e.preventDefault();
  const id = document.getElementById("f-id").value;
  const body = {
    name: document.getElementById("f-name").value.trim(),
    url: document.getElementById("f-url").value.trim(),
    port: parseInt(document.getElementById("f-port").value, 10),
    description: document.getElementById("f-desc").value.trim(),
    categories: splitTags(document.getElementById("f-categories").value),
    keywords: splitTags(document.getElementById("f-keywords").value),
    admin_email: document.getElementById("f-email").value.trim(),
    website: document.getElementById("f-website").value.trim(),
    vtml_version: document.getElementById("f-vtml").value.trim(),
    mkiwi_server_version: document.getElementById("f-mkiwi").value.trim(),
    enabled: document.getElementById("f-enabled").checked,
  };

  try {
    if (id) {
      await apiFetch(`/api/servers/${id}`, { method: "PUT", body: JSON.stringify(body) });
      toast("Serveur mis à jour ✓", "ok");
    } else {
      await apiFetch("/api/servers", { method: "POST", body: JSON.stringify(body) });
      toast("Serveur ajouté ✓", "ok");
    }
    closeModal();
    await loadServers();
  } catch (e) {
    toast("Erreur : " + e.message, "err");
  }
}

// ── Init ──────────────────────────────────────────────────────────────────────

document.getElementById("global-search").addEventListener("input", applyFilter);

document.getElementById("server-modal").addEventListener("click", e => {
  if (e.target === document.getElementById("server-modal")) closeModal();
});

// Restaurer la clé API depuis localStorage
const savedKey = getApiKey();
if (savedKey) {
  document.getElementById("api-key").value = savedKey;
  showApiKeyStatus("Clé mémorisée", "var(--text-dim)");
}

loadServers();
