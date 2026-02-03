# HeartSync Radio - Development Plan

This document outlines the development plan for the HeartSync Radio application.

## 1. Project Setup

- [x] Set up a new Compose Multiplatform project for Android and iOS.
- [x] Rename the project to "HeartSyncRadio".

## 2. Core Feature Implementation

- [x] **Polar H10 Integration:**
    - [x] Integrate the Polar SDK for Android and iOS.
    - [x] Implement logic to search for, connect to, and receive data from the H10 sensor.
- [ ] **HRV & Coherence Calculation:**
    - [ ] Process raw RR-intervals from the Polar SDK.
    - [ ] Implement artifact removal to clean the data.
    - [ ] Calculate the coherence score based on the power spectrum density.
- [ ] **Spotify Integration:**
    - [ ] Integrate the Spotify Web API.
    - [ ] Implement OAuth for user authentication.
    - [ ] Fetch user playlists and track information.
    - [ ] Create new playlists.

## 3. UI/UX Development

- [ ] **Onboarding:**
    - [ ] Create a screen to connect to Spotify.
- [x] **Home Screen:**
    - [x] Design a central hub to connect to the Polar H10 sensor and start a new "Coherence Session."
- [ ] **Session Screen:**
    - [ ] Display the currently playing track.
    - [ ] Show a real-time graph of the user's heart rate and coherence score.
- [ ] **Playlist Generation:**
    - [ ] Create a screen to display the generated "Coherence Playlists".

## 4. Backend/Data Storage

- [ ] Use a simple local database (e.g., SQLDelight) to store the relationship between songs and the coherence scores they produce for the user.

## 5. Playlist Generation Logic

- [ ] Analyze the stored data to find songs with the highest coherence scores.
- [ ] Use the Spotify API to create a new playlist with these songs.
