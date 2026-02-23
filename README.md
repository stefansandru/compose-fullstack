# Compose Fullstack

A full-stack Android application built with Jetpack Compose, demonstrating offline-first architecture with real-time synchronization, Google Maps integration, and device sensor access.

## Overview

This project implements a Master-Detail CRUD application with a Node.js/Express backend and an Android client. The app supports full offline operation -- data is persisted locally using Room and automatically synchronized with the server when connectivity is restored.

## Features

- **JWT Authentication** -- Login/logout with token-based session management via DataStore.
- **Master-Detail CRUD** -- Browse, create, edit, and delete items through a clean Compose UI.
- **Offline-First Architecture** -- All data is cached in a local Room database. The app remains fully functional without network access.
- **Automatic Sync** -- Dirty-flagged records (created, updated, or deleted offline) are synced to the server when connectivity returns, powered by a `ConnectivityObserver`.
- **Real-Time Updates** -- Socket.IO WebSocket connection pushes server-side changes (adds, updates, deletes) to all connected clients instantly.
- **Background Sync** -- WorkManager `SyncWorker` and a foreground `WebSocketService` ensure data stays up to date even when the app is in the background.
- **Push Notifications** -- Users are notified when new items arrive via WebSocket.
- **Google Maps** -- Interactive map screen using Maps Compose with fused location provider.
- **Device Sensors** -- Dedicated sensor screen that reads hardware sensor data.
- **Network Status Banner** -- Animated banner displayed when the device goes offline.
- **Bottom Navigation** -- Tab-based navigation between Items, Map, and Sensor screens.

## Architecture

```
UI Layer                 Data Layer                   Backend
-----------              ----------------             --------
LoginScreen              ItemRepository               Express REST API
ListScreen       <-->    +-- ApiInterface (Retrofit)   JWT Auth Middleware
EditScreen               +-- ItemDao (Room)            Socket.IO Server
MapScreen                +-- ItemWebSocket (Socket.IO)
SensorScreen             +-- UserPreferencesRepository (DataStore)
                         +-- ConnectivityObserver
```

The `ItemRepository` serves as the single source of truth. It coordinates between the remote API, WebSocket events, and the local Room database. ViewModels expose repository data as observable state to Compose screens.

### Data Flow

1. The UI observes `itemDao.getAllItems()` as a `Flow`, so local database changes appear instantly.
2. On refresh, the repository fetches from the REST API and updates the local database.
3. If the API call fails (no server or no internet), the app continues showing cached data.
4. Items modified offline are flagged as dirty and synced when connectivity is restored.
5. WebSocket events from other clients are written directly to Room, updating the UI in real time.

## Tech Stack

### Android Client

| Category | Library |
|---|---|
| UI | Jetpack Compose, Material 3 |
| Navigation | Navigation Compose |
| Networking | Retrofit 2, OkHttp, Socket.IO Client |
| Database | Room |
| Preferences | DataStore |
| Background | WorkManager, Foreground Service |
| Maps | Google Maps SDK, Maps Compose |
| Permissions | Accompanist Permissions |
| Build | Gradle KTS, Version Catalog, KSP |

**Min SDK:** 24 | **Target SDK:** 34 | **Compile SDK:** 35 | **Language:** Kotlin

### Backend

| Category | Library |
|---|---|
| Framework | Express.js |
| Auth | JSON Web Tokens (jsonwebtoken) |
| Real-Time | Socket.IO |
| Middleware | CORS, body-parser |

## Project Structure

```
app/src/main/java/com/example/composetutorial/
    MainActivity.kt                  -- Entry point, navigation host, network banner
    ComposeTutorialApplication.kt    -- Application class, dependency container
    connectivity/
        ConnectivityObserver.kt      -- Observes network state changes
    data/
        ItemRepository.kt           -- Single source of truth, sync logic
        model/
            Item.kt                  -- Room entity with dirty/sync flags
            AuthModels.kt            -- Login request/response models
        local/
            AppDatabase.kt           -- Room database definition
            ItemDao.kt               -- Data access object
            UserPreferencesRepository.kt -- DataStore wrapper for auth token
        remote/
            ApiInterface.kt          -- Retrofit service interface
            ItemWebSocket.kt         -- Socket.IO client wrapper
    service/
        WebSocketService.kt         -- Foreground service for persistent connection
    worker/
        SyncWorker.kt               -- WorkManager periodic sync task
    sensor/
        SensorHelper.kt             -- Hardware sensor reader
    util/
        NotificationHelper.kt       -- Notification channel and display
    ui/
        login/                       -- Login screen + ViewModel
        list/                        -- Item list screen + ViewModel
        edit/                        -- Item edit/create screen + ViewModel
        map/                         -- Google Maps screen
        sensor/                      -- Sensor data screen
        theme/                       -- Material 3 theming

server/
    server.js                        -- Express + Socket.IO server
    package.json                     -- Node.js dependencies
```

## Getting Started

### Prerequisites

- Android Studio (Hedgehog or later)
- Node.js (v16+)
- Android Emulator or physical device (API 24+)

### 1. Start the Backend

```bash
cd server
npm install
npm start
```

The server runs on `http://localhost:3000`. The Android emulator reaches it via `http://10.0.2.2:3000`.

**Test credentials:** username `a`, password `a`

### 2. Run the Android App

1. Open the project in Android Studio.
2. In the project root `local.properties` file (same level as `settings.gradle.kts`), add your Maps key:
   ```properties
   MAPS_API_KEY=your_api_key_here
   ```
3. If `local.properties` already exists (it usually contains `sdk.dir=...`), keep existing lines and append `MAPS_API_KEY=...`.
4. Sync Gradle.
5. Build and run on an emulator or connected device.

### 3. Google Maps Configuration Notes

- `local.properties` is gitignored, so the API key is not committed.
- The app reads `MAPS_API_KEY` during build and injects it into `AndroidManifest.xml` as `com.google.android.geo.API_KEY`.
- Without a valid key, the map screen will not load map tiles.

## API Endpoints

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/auth/login` | No | Returns JWT token |
| POST | `/auth/logout` | Yes | Invalidates session |
| GET | `/item` | Yes | List all items |
| POST | `/item` | Yes | Create a new item |
| PUT | `/item/:id` | Yes | Update an item |
| DELETE | `/item/:id` | Yes | Delete an item |

WebSocket events: `itemAdded`, `itemUpdated`, `itemDeleted`

## Offline Sync Strategy

Each `Item` entity carries three sync flags:

- `isDirty` -- The item has been modified locally and needs to be pushed to the server.
- `isDeleted` -- The item was deleted locally and the deletion needs to be synced.
- `isCreatedLocally` -- The item was created offline and doesn't yet exist on the server.

When connectivity is restored, `ItemRepository.sync()` iterates over all dirty items and reconciles them with the server. If a sync operation fails, the item remains flagged for retry on the next attempt.

## License

This project is available for educational and portfolio purposes.
