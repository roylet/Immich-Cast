# Immich Cast

<p align="center"><img src="docs/app-banner.png" width="640" alt="Immich Cast"></p>

A screensaver for **Chromecast with Google TV** (and other Android TV / Google TV devices) that shows your [Immich](https://immich.app) photos: **season by season**, **at random**, **recent** ones, or **on this day** in past years.

It shows photos from the current season across every year in your library: in spring you see every September–November from every year. You can also **exclude albums**. A photo in an excluded album never appears, even if it is also in another album.

<p align="center">
  <img src="docs/screenshots/screensaver-overlay.png" width="49%" alt="Screensaver with optional overlay">
  <img src="docs/screenshots/settings-photos.png" width="49%" alt="Settings">
</p>
<p align="center">
  <img src="docs/screenshots/exclude-albums.png" width="49%" alt="Excluding albums">
  <img src="docs/screenshots/setup.png" width="49%" alt="First-run setup">
</p>

## Features

- **Four slideshow modes:**
  - **Seasonal:** photos from the current season across every year (details below).
  - **Random:** your whole library, shuffled.
  - **Recent:** the last 30 days, 90 days, 6 months or 12 months.
  - **On this day:** the week around today's date in every past year. It changes daily.
- **Seasons** follow the Southern Hemisphere meteorological calendar:

  | Season | Months | Notes |
  |---|---|---|
  | Spring | Sep–Nov | |
  | Summer | Dec–Feb | Spans the new year |
  | Autumn | Mar–May | |
  | Winter | Jun–Aug | |

  The season switches automatically, or you can pin one. If the current season, recent span or day has no photos yet, the screensaver falls back to your whole library and says so.
- **Album exclusions.** Pick albums to hide (receipts, screenshots, work), and every photo in them is left out.
- **Everything configurable.** All of these are set with the remote:
  - time per photo
  - transition (crossfade, slow zoom, none)
  - image quality (fast previews, or full-quality originals for 4K)
  - fit (blurred background, crop to fill, black bars)
  - dimming
  - photos only, or photos and videos
  - favourites only
  - play order
  - overlay contents and position
- **No clutter by default.** The overlay is off. Turn on any of: clock, date taken, location, album name, description, season.
- **Fast and resilient.** The photo list is cached on the device, so the screensaver starts at once and keeps working through Wi-Fi blips. The next photo is preloaded and decoded before it is shown.
- **Private.** It talks only to your Immich server, nothing else.
- **Warm, remote-friendly design.** The look is inspired by [ImmichUp](https://github.com/roylet/immichup), softened for a dark TV room: subtle outlines and offset shadows, Archivo type and an orange accent. The focused item gets a warm tint and an orange outline, so you can always see where you are from the sofa.

## Requirements

- Chromecast with Google TV (4K or HD), or any Android TV / Google TV device running Android 12 or later
- An Immich server your TV can reach. Recent versions are supported, including 1.1xx and 2.x.
- A computer with [adb](https://developer.android.com/tools/releases/platform-tools), used once for setup

## 1. Get the APK

Download `immich-cast-x.y.z.apk` from the [latest release](../../releases), or from the **immich-cast-apk** artifact of the latest [Build workflow](../../actions/workflows/build.yml) run.

To build it yourself:

```sh
./gradlew assembleRelease
# app/build/outputs/apk/release/app-release.apk
```

## 2. Create an Immich API key

In Immich, go to **Account Settings → API Keys → New API Key**. If you restrict its permissions, it needs at least:

- `asset.read`, `asset.view`, `asset.download`
- `album.read`
- `timeline.read`

## 3. Install on the TV

1. On the TV: **Settings → System → About**, then select **Android TV OS build** 7 times to enable developer options.
2. **Settings → System → Developer options**: turn on **USB debugging** and **Wireless debugging** (or "Network debugging").
3. Find the TV's IP address under **Settings → Network & Internet**.
4. From your computer:

```sh
adb connect 192.168.1.50:5555      # your TV's IP; accept the prompt on the TV
adb install immich-cast-1.0.0.apk
```

## 4. Set it up

Open **Immich Cast** from the TV's apps row. The setup wizard asks for your server address and API key, then lets you pick albums to exclude.

Typing a long API key with a remote is painful. Two easier ways:

- Use the **Google TV app on your phone** as a keyboard and paste the key, or
- Send it straight from your computer:

  ```sh
  adb shell am start -n com.roylet.immichcast/.ui.SettingsActivity \
    --es url "http://192.168.1.20:2283" --es apiKey "YOUR_API_KEY"
  ```

## 5. Make it your screensaver

Google TV only lists its own screensaver ("Ambient mode") in settings. Pick Immich Cast with one adb command:

```sh
adb shell settings put secure screensaver_components com.roylet.immichcast/.dream.PhotoDreamService
```

Optionally, set how long the TV waits before starting the screensaver. The value is in milliseconds; this is 5 minutes:

```sh
adb shell settings put system screen_off_timeout 300000
```

The app's **About** page shows a tick once it is the active screensaver.

**To go back** to Google's Ambient mode:

```sh
adb shell settings put secure screensaver_components com.google.android.apps.tv.dreamx/.service.Backdrop
```

You may want to run `adb shell settings get secure screensaver_components` *before* changing it, so you know the exact original value on your device.

> Some Google TV updates reset this setting. If Ambient mode comes back, run the command again.

## Settings reference

| Setting | Options | Default |
|---|---|---|
| Show | Seasonal, Random, Recent, On this day | Seasonal |
| Season (Seasonal) | Current season, Spring, Summer, Autumn, Winter | Current season |
| Time span (Recent) | Last 30 days, 90 days, 180 days, 12 months | Last 90 days |
| Excluded albums | Any of your albums (owned or shared) | None |
| Media | Photos only, Photos and videos | Photos only |
| Favourites only | On / Off | Off |
| Play order (not shown for Random) | Shuffle, Oldest first, Newest first | Shuffle |
| Time per photo | 10 s – 5 min | 30 seconds |
| Transition | Crossfade, Slow zoom, None | Crossfade |
| Image quality | Standard (~1440 px preview), Full quality (original) | Standard |
| Image fit | Fit with blurred background, Fill (crop), Fit with black bars | Blurred background |
| Dim | Off – 60% | Off |
| Overlay | Clock, Date taken, Location, Album name, Description, Season | All off |
| Overlay position | Any corner | Bottom left |
| Clock format | System, 12-hour, 24-hour | System |
| Refresh library every | 6, 12, 24 hours | 12 hours |


## How it works

The app builds its photo list with Immich's search API:

1. It works out the date ranges for the chosen mode. For Seasonal and On this day, it reads your timeline so it only queries years that have photos. Random uses no date filter.
2. It queries each range with `POST /api/search/metadata`.
3. It removes every asset found in your excluded albums.

Archived, hidden, locked and trashed photos are never shown. The list is cached on the TV and rebuilt in the background on your refresh interval, when the season or day changes (for date-based modes), or when you change a setting that affects which photos are picked.

## Development

```sh
./gradlew testDebugUnitTest      # unit tests and screenshot rendering
./gradlew lintDebug
./gradlew recordPaparazziDebug   # re-render UI screenshots to app/src/test/snapshots
node art/render.js               # regenerate the TV banner and screensaver preview from art/banner.html
./gradlew assembleRelease
```

The code lives in `app/src/main/java/com/roylet/immichcast/`:

| Package | What it holds |
|---|---|
| `core/` | Season maths, options and playlist rules (plain Kotlin, fully unit tested) |
| `api/` | Small Immich REST client |
| `data/` | Settings storage and the cached playlist repository |
| `dream/` | The screensaver: `DreamService`, slideshow view, transitions, overlay |
| `ui/` | Settings and setup wizard, built with Compose for TV |

The app's TV banner is drawn in `art/banner.html` and rendered to PNGs by `art/render.js` (uses Playwright). The launcher icon is pure vector: `res/drawable/ic_launcher_*.xml`. The theme's colours and building blocks (outlined cards, offset shadows, focus states) live in `ui/theme/`. The Archivo font is bundled under the SIL Open Font License (`licenses/Archivo-OFL.txt`).

### Release signing

Release builds use the debug key unless one of these is provided:

- a `keystore.properties` file at the repo root, with `storeFile`, `storePassword`, `keyAlias` and `keyPassword`, or
- these repository secrets for CI:
  - `IMMICH_CAST_KEYSTORE_BASE64`
  - `IMMICH_CAST_KEYSTORE_PASSWORD`
  - `IMMICH_CAST_KEY_ALIAS`
  - `IMMICH_CAST_KEY_PASSWORD`

Push a tag such as `v1.0.0` to publish a GitHub release with the APK.

---

Immich Cast is an independent project and is not affiliated with Immich.
