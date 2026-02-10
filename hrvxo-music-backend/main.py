import base64
import json
import os
import tempfile

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from ytmusicapi import YTMusic

app = FastAPI(title="HrvXo Music Backend", version="1.0.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Lazy-init so the app starts even if credentials are missing (health check works)
_ytmusic: YTMusic | None = None


def get_ytmusic() -> YTMusic:
    """Initialise YTMusic client from base64 env var or local file."""
    global _ytmusic
    if _ytmusic is not None:
        return _ytmusic

    oauth_b64 = os.environ.get("YTMUSIC_OAUTH_B64")
    if oauth_b64:
        # Decode base64 → JSON string → validate → write to temp file
        try:
            oauth_json_str = base64.b64decode(oauth_b64).decode("utf-8")
            json.loads(oauth_json_str)  # validate it's real JSON
        except Exception as e:
            raise RuntimeError(f"YTMUSIC_OAUTH_B64 decode failed: {e}")

        tmp_path = os.path.join(tempfile.gettempdir(), "ytmusic_oauth.json")
        with open(tmp_path, "w") as f:
            f.write(oauth_json_str)
        _ytmusic = YTMusic(tmp_path)
        return _ytmusic

    # Local dev fallback
    if os.path.exists("oauth.json"):
        _ytmusic = YTMusic("oauth.json")
        return _ytmusic

    raise RuntimeError(
        "No YouTube Music credentials found. "
        "Set YTMUSIC_OAUTH_B64 env var or provide oauth.json file."
    )


# --- Request / Response models ---


class SearchRequest(BaseModel):
    query: str


class SearchResult(BaseModel):
    videoId: str
    title: str
    artist: str
    album: str | None = None
    duration: str | None = None


class CreatePlaylistRequest(BaseModel):
    title: str
    description: str = ""
    song_ids: list[str]


class CreatePlaylistResponse(BaseModel):
    playlistId: str


# --- Endpoints ---


@app.get("/health")
def health():
    return {"status": "ok"}


@app.post("/search", response_model=list[SearchResult])
def search(req: SearchRequest):
    if not req.query.strip():
        raise HTTPException(status_code=400, detail="Query cannot be empty")
    try:
        yt = get_ytmusic()
        results = yt.search(req.query, filter="songs", limit=10)
    except RuntimeError as e:
        raise HTTPException(status_code=503, detail=str(e))
    except Exception as e:
        raise HTTPException(status_code=502, detail=f"YouTube Music API error: {e}")

    out: list[SearchResult] = []
    for item in results:
        artists = ", ".join(a["name"] for a in item.get("artists", []))
        album = item.get("album")
        out.append(
            SearchResult(
                videoId=item["videoId"],
                title=item.get("title", ""),
                artist=artists,
                album=album["name"] if album else None,
                duration=item.get("duration"),
            )
        )
    return out


@app.post("/create-playlist", response_model=CreatePlaylistResponse)
def create_playlist(req: CreatePlaylistRequest):
    if not req.title.strip():
        raise HTTPException(status_code=400, detail="Playlist title cannot be empty")
    if not req.song_ids:
        raise HTTPException(
            status_code=400, detail="Must provide at least one song ID"
        )
    try:
        yt = get_ytmusic()
        playlist_id = yt.create_playlist(
            title=req.title,
            description=req.description,
            video_ids=req.song_ids,
        )
    except RuntimeError as e:
        raise HTTPException(status_code=503, detail=str(e))
    except Exception as e:
        raise HTTPException(status_code=502, detail=f"YouTube Music API error: {e}")

    if not playlist_id or not isinstance(playlist_id, str):
        raise HTTPException(status_code=502, detail="Failed to create playlist")

    return CreatePlaylistResponse(playlistId=playlist_id)


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(app, host="0.0.0.0", port=8080)
