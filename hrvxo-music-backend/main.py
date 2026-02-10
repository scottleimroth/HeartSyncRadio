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


def get_ytmusic() -> YTMusic:
    """Initialise YTMusic client from env var or local file."""
    oauth_json_str = os.environ.get("YTMUSIC_OAUTH_JSON")
    if oauth_json_str:
        # Write the JSON string to a temp file — ytmusicapi expects a file path
        tmp = tempfile.NamedTemporaryFile(
            mode="w", suffix=".json", delete=False
        )
        tmp.write(oauth_json_str)
        tmp.flush()
        return YTMusic(tmp.name)
    # Local dev fallback
    if os.path.exists("oauth.json"):
        return YTMusic("oauth.json")
    raise RuntimeError(
        "No YouTube Music credentials found. "
        "Set YTMUSIC_OAUTH_JSON env var or provide oauth.json file."
    )


ytmusic = get_ytmusic()


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
        results = ytmusic.search(req.query, filter="songs", limit=10)
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
        playlist_id = ytmusic.create_playlist(
            title=req.title,
            description=req.description,
            video_ids=req.song_ids,
        )
    except Exception as e:
        raise HTTPException(status_code=502, detail=f"YouTube Music API error: {e}")

    if not playlist_id or not isinstance(playlist_id, str):
        raise HTTPException(status_code=502, detail="Failed to create playlist")

    return CreatePlaylistResponse(playlistId=playlist_id)


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(app, host="0.0.0.0", port=8080)
