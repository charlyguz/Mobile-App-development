# City Explorer Challenge

City Explorer Challenge is an Android application that works as a personal exploration assistant. It uses the user's real location, nearby OpenStreetMap places, local history, and time of day to generate dynamic city exploration missions.

The app is not a static CRUD list. Missions are generated from live context and saved locally so the user can complete them, review past missions, and track progress.

## Final Project Video

[Open the final project video](./Final%20project%20Video.mp4)

<video controls src="./Final%20project%20Video.mp4" title="City Explorer Challenge final project video"></video>

If the embedded player does not render on GitHub, use the link above to open or download the video.

## Implemented Features

- **Dynamic mission generation**: Missions are generated from current location, nearby places, previous missions, category history, and time of day.
- **Location awareness**: Uses Google Play Services `FusedLocationProviderClient` with runtime permission handling, GPS settings checks, fresh location requests, last-known-location fallback, and basic invalid-location rejection.
- **OpenStreetMap / Overpass integration**: Fetches nearby places from the Overpass API across four categories: Nature, Culture, Food, and Sport.
- **Interactive map**: Uses osmdroid to show the map, user marker, destination marker, straight-line direction, live distance, and the completion radius.
- **Manual completion verification**: The user taps **Check Completion**. The app verifies whether the user is within **150 meters** of the target.
- **Local persistence**: Uses Room to store active, completed, and expired missions.
- **Mission history**: Shows completed and expired missions with event dates.
- **Statistics**: Tracks total completed missions, explored distance, category breakdown, most explored category, and daily/weekly progress.
- **Notifications**: Shows local notifications for newly generated and completed missions when notification permission is granted.
- **Unit tests**: Includes tests for the main mission generation rules.

## Mission Generation Rules

The current generator implements five rules:

1. **No recent repetition**: Places used in the last 10 missions are excluded when alternatives exist.
2. **Category diversity**: If the last 3 missions used the same category, the next mission prefers a different category.
3. **Time-of-day awareness**:
   - Morning, 6-11: prefers Food places.
   - Afternoon, 12-17: prefers Nature or Culture places.
   - Evening, 18-22: prefers Food or Sport places.
4. **Difficulty evolution**: Users with more than 5 completed missions receive a slightly farther target.
5. **New-user proximity**: New users receive the nearest reachable target.

## Main User Flow

1. The user opens the app.
2. The app requests location permission.
3. If permission is granted, the app starts location tracking and gets the current location.
4. Nearby places are downloaded from OpenStreetMap through Overpass.
5. The generator selects a suitable mission using the rules above.
6. The active mission appears on the Home screen.
7. The user opens the Map screen and moves toward the target.
8. The user taps **Check Completion**.
9. If the user is within 150 meters, the mission is marked as completed.
10. Completed and expired missions appear in History, and Statistics are updated.

## Screens

1. **Home**: Active mission, category, distance, status, location status, and progress summary.
2. **Map**: osmdroid map, user marker, target marker, straight-line route indicator, live distance, and completion check.
3. **Mission Details**: Mission description, generation reasoning, place name, category, distance, and coordinates.
4. **History**: Completed and expired missions with status indicators and dates.
5. **Statistics**: Completed count, distance explored, category totals, most explored category, and daily/weekly progress.

## Architecture

```text
com.example.cityexplorerchallenge/
|-- data/
|   |-- local/          Room database, DAO, and ChallengeEntity
|   `-- remote/         Overpass API, DTOs, and PlacesRepository
|-- domain/             ChallengeGenerator and mission rules
|-- location/           LocationTracker wrapper around FusedLocationProvider
|-- notifications/      NotificationHelper for mission updates
`-- ui/
    |-- MainViewModel   Shared ViewModel for app state and orchestration
    |-- main/           Home screen
    |-- map/            osmdroid map screen
    |-- details/        Mission details screen
    |-- history/        RecyclerView history screen
    `-- statistics/     Progress and statistics screen
```

## Tech Stack

- Kotlin
- Android SDK 36 target, min SDK 26
- View Binding
- Material Design 3 components
- Jetpack Navigation with BottomNavigationView
- Room with KSP
- Retrofit 2 and Moshi
- OpenStreetMap Overpass API
- osmdroid
- Google Play Services Location
- Kotlin Coroutines and StateFlow
- JUnit local unit tests

## API Information

The app uses the Overpass API:

```text
https://overpass-api.de/api/
```

No API key is required. The app searches OpenStreetMap tags for nearby Nature, Culture, Food, and Sport places. OpenStreetMap does not provide ratings in the way Google Places does, so ratings are not part of the current implementation.

## Permissions

The app uses:

- `ACCESS_FINE_LOCATION`: precise GPS-based location.
- `ACCESS_COARSE_LOCATION`: approximate location fallback.
- `INTERNET`: Overpass API and map tiles.
- `ACCESS_NETWORK_STATE`: network state awareness.
- `POST_NOTIFICATIONS`: mission update notifications on Android 13+.

## Build and Run

### Requirements

- Android Studio
- JDK configured through Android Studio or `JAVA_HOME`
- Android SDK installed
- Emulator or physical Android device with:
  - Google Play Services
  - Location services enabled
  - Internet access

### Run

Open the project in Android Studio, let Gradle sync, then run the `app` configuration.

From terminal:

```bash
./gradlew assembleDebug
```

Run tests:

```bash
./gradlew testDebugUnitTest
```

## Author

Carlos Guzman  
AGH University of Science and Technology  
Mobile App Development - Final Project 2026
