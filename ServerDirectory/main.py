"""
Annuaire de serveurs VTML — Backend FastAPI
"""
import json
import re
import os
import urllib.request
from pathlib import Path
from typing import List, Optional

from fastapi import FastAPI, HTTPException, Header, Depends, Query
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import FileResponse, RedirectResponse
from fastapi.staticfiles import StaticFiles
from pydantic import BaseModel, validator

# ── Configuration ──────────────────────────────────────────────────────────────

API_KEY = os.getenv("API_KEY", "changeme")
DATA_FILE = Path(__file__).parent / "data" / "servers.json"
STATIC_DIR = Path(__file__).parent / "static"

app = FastAPI(title="VTML Server Directory", version="1.0.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

app.mount("/static", StaticFiles(directory=str(STATIC_DIR)), name="static")

# ── Modèles ────────────────────────────────────────────────────────────────────

def clean_tag(v: str) -> str:
    return re.sub(r"[^a-zA-Z0-9\-_À-ÿ ]", "", v)[:30]


class ServerBase(BaseModel):
    url: str
    port: int = 80
    name: str
    description: str = ""
    keywords: List[str] = []
    admin_email: str = ""
    website: str = ""
    vtml_version: str = "1.0"
    mkiwi_server_version: str = "1.0"
    categories: List[str] = []
    enabled: bool = True

    @validator("port")
    def port_range(cls, v):
        if not 1 <= v <= 65535:
            raise ValueError("Le port doit être entre 1 et 65535")
        return v

    @validator("url")
    def url_not_empty(cls, v):
        v = v.strip()
        if not v:
            raise ValueError("L'URL est requise")
        return v

    @validator("name")
    def name_not_empty(cls, v):
        v = v.strip()
        if not v:
            raise ValueError("Le nom est requis")
        return v[:80]

    @validator("description")
    def desc_max(cls, v):
        return v[:255]

    @validator("keywords", "categories", each_item=True)
    def clean_list_item(cls, v):
        return clean_tag(v)


class ServerCreate(ServerBase):
    pass


class ServerEntry(ServerBase):
    id: int


# ── Persistance ────────────────────────────────────────────────────────────────

def load_servers() -> List[dict]:
    if not DATA_FILE.exists():
        return []
    with open(DATA_FILE, encoding="utf-8") as f:
        return json.load(f)


def save_servers(servers: List[dict]):
    DATA_FILE.parent.mkdir(parents=True, exist_ok=True)
    with open(DATA_FILE, "w", encoding="utf-8") as f:
        json.dump(servers, f, ensure_ascii=False, indent=2)


def next_id(servers: List[dict]) -> int:
    return max((s["id"] for s in servers), default=0) + 1


# ── Authentification ───────────────────────────────────────────────────────────

def require_api_key(x_api_key: Optional[str] = Header(None, alias="X-API-Key")):
    if not x_api_key or x_api_key != API_KEY:
        raise HTTPException(status_code=401, detail="Clé API invalide ou absente")


# ── Routes publiques ───────────────────────────────────────────────────────────

@app.get("/api/health")
def health():
    servers = load_servers()
    return {"status": "ok", "total": len(servers)}


@app.get("/api/servers")
def list_servers(
    page: int = Query(1, ge=1),
    limit: int = Query(10, ge=1, le=1000),
    q: str = Query(""),
    category: str = Query(""),
):
    servers = [s for s in load_servers() if s.get("enabled", True)]

    if q:
        ql = q.lower()
        servers = [
            s for s in servers
            if ql in s.get("name", "").lower()
            or ql in s.get("description", "").lower()
            or ql in s.get("url", "").lower()
            or any(ql in k.lower() for k in s.get("keywords", []))
            or any(ql in c.lower() for c in s.get("categories", []))
        ]

    if category:
        cl = category.lower()
        servers = [s for s in servers if any(cl == c.lower() for c in s.get("categories", []))]

    total = len(servers)
    pages = max(1, (total + limit - 1) // limit)
    offset = (page - 1) * limit

    return {
        "page": page,
        "limit": limit,
        "total": total,
        "pages": pages,
        "items": servers[offset : offset + limit],
    }


@app.get("/api/search")
def search_servers(q: str = Query("")):
    servers = [s for s in load_servers() if s.get("enabled", True)]
    if q:
        ql = q.lower()
        servers = [
            s for s in servers
            if ql in s.get("name", "").lower()
            or ql in s.get("description", "").lower()
            or ql in s.get("url", "").lower()
            or any(ql in k.lower() for k in s.get("keywords", []))
            or any(ql in c.lower() for c in s.get("categories", []))
        ]
    return {"total": len(servers), "items": servers}


@app.get("/api/categories")
def list_categories():
    cats = set()
    for s in load_servers():
        for c in s.get("categories", []):
            cats.add(c)
    return sorted(cats)


@app.get("/api/servers/{server_id}")
def get_server(server_id: int):
    for s in load_servers():
        if s["id"] == server_id:
            return s
    raise HTTPException(status_code=404, detail="Serveur non trouvé")


@app.get("/api/servers/{server_id}/test")
def test_server(server_id: int):
    servers = load_servers()
    entry = next((s for s in servers if s["id"] == server_id), None)
    if not entry:
        raise HTTPException(status_code=404, detail="Serveur non trouvé")
    url = f"http://{entry['url']}:{entry['port']}/"
    try:
        urllib.request.urlopen(url, timeout=3)
        return {"reachable": True, "url": url}
    except Exception as e:
        return {"reachable": False, "url": url, "error": str(e)}


# ── Routes protégées ───────────────────────────────────────────────────────────

@app.post("/api/servers", status_code=201, dependencies=[Depends(require_api_key)])
def create_server(server: ServerCreate):
    servers = load_servers()
    entry = server.dict()
    entry["id"] = next_id(servers)
    servers.append(entry)
    save_servers(servers)
    return entry


@app.put("/api/servers/{server_id}", dependencies=[Depends(require_api_key)])
def update_server(server_id: int, server: ServerCreate):
    servers = load_servers()
    for i, s in enumerate(servers):
        if s["id"] == server_id:
            updated = server.dict()
            updated["id"] = server_id
            servers[i] = updated
            save_servers(servers)
            return updated
    raise HTTPException(status_code=404, detail="Serveur non trouvé")


@app.delete("/api/servers/{server_id}", dependencies=[Depends(require_api_key)])
def delete_server(server_id: int):
    servers = load_servers()
    new_servers = [s for s in servers if s["id"] != server_id]
    if len(new_servers) == len(servers):
        raise HTTPException(status_code=404, detail="Serveur non trouvé")
    save_servers(new_servers)
    return {"deleted": server_id}


@app.patch(
    "/api/servers/{server_id}/toggle",
    dependencies=[Depends(require_api_key)],
)
def toggle_server(server_id: int):
    servers = load_servers()
    for s in servers:
        if s["id"] == server_id:
            s["enabled"] = not s.get("enabled", True)
            save_servers(servers)
            return {"id": server_id, "enabled": s["enabled"]}
    raise HTTPException(status_code=404, detail="Serveur non trouvé")


# ── Interface admin ────────────────────────────────────────────────────────────

@app.get("/")
def admin_ui():
    return FileResponse(str(STATIC_DIR / "admin.html"))

@app.get("/favicon.ico", include_in_schema=False)
def favicon():
    return RedirectResponse(url="/static/favicon.svg")
