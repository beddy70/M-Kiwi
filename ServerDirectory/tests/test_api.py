"""
Tests de l'API de l'annuaire VTML
Lancer : pytest tests/
"""
import json
import pytest
from fastapi.testclient import TestClient

import sys
import os
sys.path.insert(0, os.path.dirname(os.path.dirname(__file__)))

os.environ["API_KEY"] = "test-key"

from main import app, DATA_FILE, save_servers

HEADERS_AUTH = {"X-API-Key": "test-key"}

SAMPLE = {
    "url": "test.local",
    "port": 8080,
    "name": "Test Server",
    "description": "Serveur de test",
    "keywords": ["test"],
    "admin_email": "test@test.com",
    "website": "",
    "vtml_version": "1.0",
    "mkiwi_server_version": "1.0",
    "categories": ["test"],
    "enabled": True,
}


@pytest.fixture(autouse=True)
def clean_data():
    """Réinitialiser les données avant chaque test."""
    save_servers([])
    yield
    save_servers([])


@pytest.fixture
def client():
    return TestClient(app)


# ── Health ────────────────────────────────────────────────────────────────────

def test_health(client):
    r = client.get("/api/health")
    assert r.status_code == 200
    assert r.json()["status"] == "ok"


# ── CRUD ──────────────────────────────────────────────────────────────────────

def test_create_server(client):
    r = client.post("/api/servers", json=SAMPLE, headers=HEADERS_AUTH)
    assert r.status_code == 201
    data = r.json()
    assert data["name"] == "Test Server"
    assert data["id"] == 1


def test_create_requires_api_key(client):
    r = client.post("/api/servers", json=SAMPLE)
    assert r.status_code == 401  # Header absent


def test_create_wrong_key(client):
    r = client.post("/api/servers", json=SAMPLE, headers={"X-API-Key": "wrong"})
    assert r.status_code == 401


def test_list_servers(client):
    client.post("/api/servers", json=SAMPLE, headers=HEADERS_AUTH)
    client.post("/api/servers", json={**SAMPLE, "name": "Server 2"}, headers=HEADERS_AUTH)
    r = client.get("/api/servers")
    assert r.status_code == 200
    data = r.json()
    assert data["total"] == 2
    assert len(data["items"]) == 2


def test_pagination(client):
    for i in range(5):
        client.post("/api/servers", json={**SAMPLE, "name": f"Server {i}"}, headers=HEADERS_AUTH)
    r = client.get("/api/servers?page=1&limit=3")
    data = r.json()
    assert len(data["items"]) == 3
    assert data["pages"] == 2


def test_get_server(client):
    r = client.post("/api/servers", json=SAMPLE, headers=HEADERS_AUTH)
    sid = r.json()["id"]
    r2 = client.get(f"/api/servers/{sid}")
    assert r2.status_code == 200
    assert r2.json()["name"] == "Test Server"


def test_get_server_not_found(client):
    r = client.get("/api/servers/999")
    assert r.status_code == 404


def test_update_server(client):
    r = client.post("/api/servers", json=SAMPLE, headers=HEADERS_AUTH)
    sid = r.json()["id"]
    updated = {**SAMPLE, "name": "Updated"}
    r2 = client.put(f"/api/servers/{sid}", json=updated, headers=HEADERS_AUTH)
    assert r2.status_code == 200
    assert r2.json()["name"] == "Updated"


def test_delete_server(client):
    r = client.post("/api/servers", json=SAMPLE, headers=HEADERS_AUTH)
    sid = r.json()["id"]
    r2 = client.delete(f"/api/servers/{sid}", headers=HEADERS_AUTH)
    assert r2.status_code == 200
    assert client.get(f"/api/servers/{sid}").status_code == 404


def test_toggle_server(client):
    r = client.post("/api/servers", json=SAMPLE, headers=HEADERS_AUTH)
    sid = r.json()["id"]
    r2 = client.patch(f"/api/servers/{sid}/toggle", headers=HEADERS_AUTH)
    assert r2.json()["enabled"] is False
    r3 = client.patch(f"/api/servers/{sid}/toggle", headers=HEADERS_AUTH)
    assert r3.json()["enabled"] is True


# ── Search ────────────────────────────────────────────────────────────────────

def test_search_by_name(client):
    client.post("/api/servers", json=SAMPLE, headers=HEADERS_AUTH)
    client.post("/api/servers", json={**SAMPLE, "name": "GameKiwi"}, headers=HEADERS_AUTH)
    r = client.get("/api/servers?q=game")
    data = r.json()
    assert data["total"] == 1
    assert data["items"][0]["name"] == "GameKiwi"


def test_search_by_keyword(client):
    client.post("/api/servers", json={**SAMPLE, "keywords": ["retro", "jeu"]}, headers=HEADERS_AUTH)
    r = client.get("/api/servers?q=retro")
    assert r.json()["total"] == 1


def test_filter_by_category(client):
    client.post("/api/servers", json={**SAMPLE, "categories": ["jeu"]}, headers=HEADERS_AUTH)
    client.post("/api/servers", json={**SAMPLE, "name": "S2", "categories": ["info"]}, headers=HEADERS_AUTH)
    r = client.get("/api/servers?category=jeu")
    assert r.json()["total"] == 1


def test_disabled_server_hidden(client):
    client.post("/api/servers", json={**SAMPLE, "enabled": False}, headers=HEADERS_AUTH)
    r = client.get("/api/servers")
    assert r.json()["total"] == 0


# ── Validation ────────────────────────────────────────────────────────────────

def test_invalid_port(client):
    r = client.post("/api/servers", json={**SAMPLE, "port": 99999}, headers=HEADERS_AUTH)
    assert r.status_code == 422


def test_empty_name(client):
    r = client.post("/api/servers", json={**SAMPLE, "name": ""}, headers=HEADERS_AUTH)
    assert r.status_code == 422


def test_empty_url(client):
    r = client.post("/api/servers", json={**SAMPLE, "url": "  "}, headers=HEADERS_AUTH)
    assert r.status_code == 422
