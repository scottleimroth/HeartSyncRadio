# HeartSyncRadio: Music API Research Report

**Date:** February 10, 2026  
**Context:** Evaluate music streaming APIs for playlist generation based on heart rate coherence scores

---

## Executive Summary

**RECOMMENDATION: Deezer API**
- Zero friction setup (same day)
- Full playlist generation on free tier
- No approval process
- 90M+ track catalog
- Production-ready MVP in 7 days

---

## 1. DEEZER API (🥇 RECOMMENDED)

### Overview
- **Official API:** Yes, fully documented
- **Approval Process:** None - register app, get API keys immediately
- **Free Tier:** Full playlist creation available
- **Requires Subscription:** NO - works with free Deezer accounts
- **User Base:** 90M+ songs, strong in Europe/Latin America
- **Setup Time:** 15 minutes (registration) + 1-2 days (integration)

### Features
✅ Create playlists  
✅ Add tracks to playlists  
✅ Search library  
✅ User auth via OAuth  
✅ Track metadata (duration, artist, BPM, mood)  
✅ User profile access  

### Authentication Flow
```
1. User logs in via OAuth
2. App receives access token
3. Make API calls on user's behalf
4. Create/modify playlists in user's library
```

### Pricing
- **Free:** $0/month (includes API access)
- **Premium:** €10.99/month (optional, adds offline/quality)
- **Developer:** Free tier (unlimited API calls for free users)

### Implementation Effort
- **Complexity:** Low-Medium
- **Time to MVP:** 7 days (with existing Android codebase)
- **Code Changes:** ~500 lines (OAuth + playlist creation)

### Links
- **API Docs:** https://developers.deezer.com/
- **Android SDK:** https://github.com/deezer/android-sdk
- **OAuth Guide:** https://developers.deezer.com/login

### Why Deezer Wins
1. **Zero approval friction** - no waiting for partnership
2. **Free user support** - playlist gen works without paid subscription
3. **No infrastructure** - direct API calls from Android app
4. **Fast integration** - API is straightforward, good documentation
5. **Good catalog** - 90M+ tracks covers most use cases

### Limitations
- Smaller US user base than Spotify/Apple Music
- Playlist discovery features limited vs Spotify
- Regional focus on Europe/Latin America

---

## 2. YOUTUBE MUSIC (ytmusicapi Backend) (🥈 ALTERNATIVE)

### Overview
- **Official API:** NO (unofficial reverse-engineered API)
- **Approach:** Backend service (Python) + Android app communication
- **Catalog:** 100M+ tracks (YouTube's entire library)
- **No Subscription:** Works with free YouTube accounts
- **User Base:** 70M+ users, growing in US/Asia

### Why Backend Service Needed
YouTube doesn't have official API. Solution: run **ytmusicapi** (Python) on your server:
```
Android App → Your Backend → YouTube Music (via ytmusicapi)
```

### Features
✅ Create playlists  
✅ Add tracks  
✅ Full search  
✅ Get recommendations (mood-based)  
✅ Access user library  

### Infrastructure Requirements

#### Option A: DigitalOcean (RECOMMENDED)
- **Cost:** $5/month (1GB RAM droplet)
- **Setup:** 30 minutes
- **Maintenance:** Low (Python 3.9+)
- **Downtime:** 99.9% uptime SLA
- **Setup Link:** https://www.digitalocean.com/products/droplets

#### Option B: AWS Lambda + RDS
- **Cost:** $0-50/month (based on traffic)
- **Setup:** 2-3 hours
- **Maintenance:** Managed, but complex
- **Best For:** Scaling to 10k+ users

#### Option C: Heroku
- **Cost:** $5-7/month (free tier deprecated)
- **Setup:** 15 minutes
- **Maintenance:** Minimal
- **Best For:** Rapid prototyping

#### Option D: Self-Hosted (Raspberry Pi/VPS)
- **Cost:** $3-5/month (Linode/Hetzner)
- **Setup:** 45 minutes
- **Maintenance:** Manual updates required

### Backend Architecture
```
┌─────────────────────────────────────────┐
│  HeartSyncRadio Android App             │
│  ├─ Heart Rate Input (Polar H10)        │
│  ├─ Coherence Calculation               │
│  └─ API Calls to Backend                │
└────────────┬────────────────────────────┘
             │
┌────────────▼────────────────────────────┐
│  Python Backend (DigitalOcean)          │
│  ├─ FastAPI server                      │
│  ├─ ytmusicapi client                   │
│  └─ Playlist generation logic           │
└────────────┬────────────────────────────┘
             │
┌────────────▼────────────────────────────┐
│  YouTube Music (via reverse API)        │
│  ├─ Search tracks by mood/artist        │
│  ├─ Create playlists                    │
│  └─ Manage user library                 │
└─────────────────────────────────────────┘
```

### Code Example: FastAPI Backend
```python
from fastapi import FastAPI
from ytmusicapi import YTMusic

app = FastAPI()
ytmusic = YTMusic("oauth.json")  # Auth file from setup

@app.post("/create-playlist")
async def create_coherence_playlist(
    playlist_name: str,
    songs: list[dict]  # [{"title": "...", "artist": "..."}]
):
    # Create playlist in user's YouTube Music library
    playlist_id = ytmusic.create_playlist(
        title=playlist_name,
        description="Generated by HeartSyncRadio"
    )
    
    # Add songs based on coherence scores
    for song in songs:
        results = ytmusic.search(
            f"{song['title']} {song['artist']}"
        )
        if results:
            ytmusic.add_playlist_items(
                playlist_id,
                [results[0]['videoId']]
            )
    
    return {"playlist_id": playlist_id}
```

### Android Integration
```kotlin
// In HeartSyncRadio, call backend
suspend fun generatePlaylist(coherenceScores: List<Double>) {
    val client = HttpClient()
    val response = client.post("https://your-backend.com/create-playlist") {
        contentType(ContentType.Application.Json)
        setBody(mapOf(
            "playlist_name" to "My Coherence Playlist",
            "songs" to coherenceScores.map { /* transform to songs */ }
        ))
    }
    // Open playlist in YouTube Music app
    openYouTubeMusicPlaylist(response.playlist_id)
}
```

### Setup Time
- **Total:** 10-14 days (including testing)
- **Day 1-2:** DigitalOcean setup + server config
- **Day 3-5:** ytmusicapi installation & auth setup
- **Day 6-10:** Backend API development
- **Day 11-14:** Android integration + testing

### Links
- **ytmusicapi GitHub:** https://github.com/sigma67/ytmusicapi
- **DigitalOcean Droplets:** https://www.digitalocean.com/products/droplets
- **FastAPI Docs:** https://fastapi.tiangolo.com/
- **ytmusicapi Setup Guide:** https://ytmusicapi.readthedocs.io/

### Why YouTube Music Works
1. **Largest catalog** - 100M+ songs
2. **No subscription required** - free accounts work
3. **Full control** - you own the backend
4. **Offline capable** - can cache playlists locally

### Limitations
- **Unofficial API** - risk of breaking if YouTube changes API
- **Infrastructure overhead** - server to maintain
- **No real support** - community-driven library
- **Authentication fragile** - may need cookie refresh periodically

---

## 3. SPOTIFY (❌ NOT VIABLE)

### Why It's Out
- **November 2024 Policy Change:** Spotify restricted **recommendation endpoints** for new apps
- **Playlist Creation:** Now requires 250,000 Monthly Active Users
- **Approval:** Application likely to be rejected
- **For New Apps:** Not worth pursuing

### Historical Context
Previously this was the best option, but policy changes made it inaccessible to indie developers.

---

## 4. APPLE MUSIC API

### Overview
- **Official API:** Yes (MusicKit)
- **Approval Process:** Required partnership with Apple
- **Requires Subscription:** YES - Apple Music subscription mandatory
- **Setup Time:** 2-4 weeks (partnership approval)

### Features
✅ Playlist creation  
✅ Track search  
✅ User auth  
✅ Analytics  

### Approval Requirements
1. Apply at https://developer.apple.com/musickit/
2. Apple reviews application (2-4 weeks)
3. Likely requirement: "substantial iOS app with user base"
4. Agreement signing ($99/year developer account)

### Pricing
- **Developer Account:** $99/year
- **User Subscription:** Users must have Apple Music ($10.99/month)

### Why Not Recommended
1. **Approval timeline too long** - 2-4 weeks vs Deezer same-day
2. **Requires user subscription** - limits addressable market
3. **iOS-centric** - less friction for iOS than Android
4. **Overkill for MVP** - enterprise-level complexity

### Viable Only If
- You're building iOS app
- Users already have Apple Music
- You need premium UX polish

---

## 5. TIDAL API

### Overview
- **Official API:** Yes, documented
- **Approval Process:** Simple - register, get keys (24-48 hours)
- **Free Tier:** Limited (some features restricted)
- **Premium Required:** For full playlist features
- **Setup Time:** 2-3 days

### Features
✅ Playlist creation  
✅ Search  
✅ User auth  
✅ High-fidelity audio metadata  

### Approval
- Register developer account: https://developer.tidal.com/
- Submit app details
- Approval: Usually within 24-48 hours

### Pricing
- **API:** Free (with limits)
- **User Subscription:** Tidal Premium required (Users: €11.99/month)

### Why Not First Choice
1. **Smaller user base** - 3M users vs Deezer's 90M
2. **Premium requirement** - limits addressable market
3. **Slower than Deezer** - 24-48h approval vs same-day

### Good Alternative If
- Users already subscribe to Tidal
- You want lossless audio support
- Need European market reach

---

## 6. OTHER OPTIONS (Limited)

### SoundCloud API
- **Pros:** Easier approval than Spotify
- **Cons:** 100M tracks (smaller catalog), declining user base
- **Verdict:** Skip it - Deezer is better

### Last.fm
- **Pros:** Scrobbling + recommendations
- **Cons:** Not a music source (metadata only)
- **Verdict:** Use as secondary (mood/recommendation layer)

### MusicBrainz
- **Pros:** Open, no approval
- **Cons:** Not a music source (metadata database)
- **Verdict:** Supplement with Deezer

---

## 7. COMPARISON TABLE

| Criteria | Deezer | YouTube Music | Spotify | Apple Music | Tidal |
|----------|--------|---------------|---------|-------------|-------|
| **Approval** | Same-day | N/A | Rejected now | 2-4 weeks | 24-48h |
| **Setup Time** | 1 day | 10-14 days | N/A | 2-4 weeks | 2-3 days |
| **Catalog** | 90M | 100M | 100M+ | 100M+ | 80M |
| **Free Tier Works** | ✅ Yes | ✅ Yes | ❌ No | ❌ No | ⚠️ Limited |
| **Infrastructure** | ❌ None | ✅ $5/mo | N/A | N/A | ❌ None |
| **User Subscription** | Optional | No | Required | Required | Required |
| **Cost to Launch** | $0 | $5/mo | N/A | $99 + dev | $0 |
| **MVP Timeline** | 7 days | 14 days | N/A | 4-6 weeks | 7-10 days |
| **Maintenance** | Low | Medium | N/A | Medium | Low |

---

## FINAL RECOMMENDATION

### For MVP Launch: **DEEZER API**
1. **Register:** https://developers.deezer.com/
2. **Integration:** 1-2 days (existing Android codebase)
3. **Launch:** Week 1
4. **Cost:** $0
5. **Time:** 7 days to production

### For Future (Month 2): **Add YouTube Music Backend**
- Keep Deezer as primary
- Add ytmusicapi backend as secondary option
- Gives users choice
- Staggered development = lower risk

### Development Timeline
- **Week 1:** Deezer API integration + UI
- **Week 2:** Testing + beta launch
- **Month 2-3:** Add YouTube Music backend (optional)
- **Month 3+:** Tidal/other services if demand

---

## Implementation Checklist: Deezer

### Phase 1: Setup (Day 1)
- [ ] Register at https://developers.deezer.com/
- [ ] Create test app
- [ ] Get API credentials (app_id, app_secret)
- [ ] Read OAuth documentation

### Phase 2: Android Integration (Day 2-3)
- [ ] Add Deezer OAuth library to gradle
- [ ] Implement OAuth login flow
- [ ] Store user access token securely
- [ ] Test user authentication

### Phase 3: Playlist Creation (Day 4-5)
- [ ] Implement search endpoint
- [ ] Add playlist creation logic
- [ ] Map coherence scores to songs
- [ ] Test playlist generation

### Phase 4: UI/UX (Day 6-7)
- [ ] Add "Generate Playlist" button
- [ ] Show playlist results
- [ ] Open in Deezer app
- [ ] Testing + bug fixes

### Phase 5: Launch (Week 2)
- [ ] Beta testing
- [ ] Gather feedback
- [ ] Production deployment
- [ ] Play store release

---

## Code Example: Deezer Integration

### 1. Android OAuth Flow
```kotlin
import com.deezer.sdk.network.connect.DeezerConnect
import com.deezer.sdk.network.request.DeezerRequest
import com.deezer.sdk.network.request.DeezerRequestFactory

class HeartSyncDeezer {
    private val connect = DeezerConnect(
        context = this,
        appId = "YOUR_APP_ID"
    )
    
    fun loginWithDeezer() {
        val permissions = listOf("manage_library", "create_playlists")
        connect.authorize(
            activity = this,
            permissions = permissions,
            listener = object : DeezerAuthListener {
                override fun onComplete() {
                    // Access token available
                    val accessToken = connect.accessToken
                }
                override fun onError(error: String) {}
                override fun onCancel() {}
            }
        )
    }
    
    fun createPlaylist(
        playlistName: String,
        coherenceScores: Map<String, Double>
    ) {
        val request = DeezerRequestFactory.requestBuilder(
            "https://api.deezer.com/user/me/playlists"
        )
            .addParameter("title", playlistName)
            .addParameter("description", "Generated by HeartSyncRadio")
            .build()
        
        connect.executeRequest(request, DeezerRequestListener { response ->
            val playlistId = response.optString("id")
            // Add songs to playlist
            addSongsToPlaylist(playlistId, coherenceScores)
        })
    }
    
    private fun addSongsToPlaylist(
        playlistId: String,
        coherenceScores: Map<String, Double>
    ) {
        // Search for each song by coherence (high->low)
        coherenceScores
            .entries
            .sortedByDescending { it.value }
            .forEach { (song, score) ->
                searchAndAddSong(playlistId, song)
            }
    }
}
```

### 2. API: Search & Add Track
```kotlin
fun searchAndAddSong(
    playlistId: String,
    query: String
) {
    val request = DeezerRequestFactory.requestBuilder(
        "https://api.deezer.com/search/track"
    )
        .addParameter("q", query)
        .build()
    
    connect.executeRequest(request, DeezerRequestListener { response ->
        val tracks = response.optJSONArray("data")
        if (tracks.length() > 0) {
            val trackId = tracks.getJSONObject(0).optString("id")
            // Add to playlist
            addTrackToPlaylist(playlistId, trackId)
        }
    })
}

fun addTrackToPlaylist(playlistId: String, trackId: String) {
    val request = DeezerRequestFactory.requestBuilder(
        "https://api.deezer.com/playlist/$playlistId/tracks"
    )
        .addParameter("songs", trackId)
        .build()
    
    connect.executeRequest(request, DeezerRequestListener { response ->
        Log.d("HeartSync", "Track added: $trackId")
    })
}
```

---

## Cost Breakdown

### Deezer Path
| Item | Cost | One-time |
|------|------|----------|
| Developer Account | $0 | No |
| Hosting | $0 | No |
| User Subscription | User pays | Optional |
| **Total Cost** | **$0/month** | |

### YouTube Music Path
| Item | Cost | One-time |
|------|------|----------|
| DigitalOcean Droplet | $5/month | No |
| Domain (optional) | $12/year | Yes |
| SSL Certificate | $0 (Let's Encrypt) | No |
| **Total Cost** | **$5/month + $1/mo domain** | |

### Difference
- **Deezer:** $0 (no infrastructure overhead)
- **YouTube Music:** $5-6/month (but larger catalog)

---

## Final Recommendation Matrix

### If You Want...
- **Fastest MVP** → Deezer
- **Largest Catalog** → YouTube Music Backend
- **Easiest User Onboarding** → Deezer
- **Most Control** → YouTube Music Backend
- **Zero Infrastructure** → Deezer
- **iOS Support** → Apple Music (but 4-6 week approval)

### Sweet Spot for HeartSyncRadio
**Phase 1 (Week 1-2): Launch with Deezer**
- Deezer API + Android integration
- 7-day MVP
- $0 cost
- User-friendly

**Phase 2 (Month 2-3): Add YouTube Music as Alternative**
- Backend service infrastructure
- Give power users choice
- Expand catalog to 100M tracks

---

## Resources & Links

### Deezer
- API Docs: https://developers.deezer.com/
- SDK: https://github.com/deezer/android-sdk
- OAuth: https://developers.deezer.com/login
- Dashboard: https://www.deezer.com/developers/

### YouTube Music (ytmusicapi)
- GitHub: https://github.com/sigma67/ytmusicapi
- Docs: https://ytmusicapi.readthedocs.io/
- FastAPI: https://fastapi.tiangolo.com/

### Hosting
- DigitalOcean: https://www.digitalocean.com/
- Heroku: https://www.heroku.com/
- AWS Lambda: https://aws.amazon.com/lambda/

### Alternatives
- Tidal: https://developer.tidal.com/
- Apple Music: https://developer.apple.com/musickit/

---

## Conclusion

**Deezer API is the clear winner for HeartSyncRadio MVP:**
1. ✅ Zero approval friction
2. ✅ Same-day setup
3. ✅ Full playlist features on free tier
4. ✅ 90M+ tracks (sufficient)
5. ✅ $0 to launch

**Proceed with Deezer for Week 1 launch, plan YouTube Music backend for Month 2 if needed.**

---

*Report compiled: February 10, 2026*
*Context: HeartSyncRadio - Android app syncing music to heart rate coherence*
