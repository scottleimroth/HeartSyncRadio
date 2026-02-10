# HrvXo Music Backend

FastAPI backend for YouTube Music integration. Provides search and playlist creation endpoints for the HrvXo Android app.

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/health` | Health check |
| POST | `/search` | Search YouTube Music for songs |
| POST | `/create-playlist` | Create a playlist with given songs |

## Local Development

```bash
# Install dependencies
pip install -r requirements.txt

# Place your oauth.json in this directory (get it via ytmusicapi oauth)
# Then run:
python main.py
```

Server starts at `http://localhost:8080`. API docs at `http://localhost:8080/docs`.

## Environment Variables

| Variable | Description |
|----------|-------------|
| `YTMUSIC_OAUTH_JSON` | YouTube Music OAuth credentials as a JSON string. Falls back to `oauth.json` file if not set. |

## Deploy to Fly.io

```bash
fly launch --no-deploy
fly secrets set YTMUSIC_OAUTH_JSON="$(cat oauth.json)"
fly deploy
```

## Tech Stack

- Python 3.12
- FastAPI + Uvicorn
- ytmusicapi (YouTube Music unofficial API)
- Fly.io (Sydney region)
