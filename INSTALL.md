# Installing Immich Cast on a Chromecast with Google TV

This guide takes you from nothing to Immich Cast running as your Chromecast's screensaver. It takes about 15 minutes, and you only do it once.

You'll need:

- A **Chromecast with Google TV** (4K or HD). Other Google TV / Android TV devices running Android 12 or later work the same way.
- Your **Immich server** running and reachable from the TV, on the same home network or over the internet.
- A **computer** (Windows, Mac or Linux) on the same Wi-Fi network as the TV. It's used once to install the app and switch the screensaver. No cable is needed.

> **Why a computer?** Immich Cast isn't on the Play Store, and Google TV only offers Google's own screensaver in its settings. A free Google tool called **adb** installs the app and sets it as the screensaver. Every command is below, ready to copy and paste.

---

## Step 1: Download the app (APK)

1. Open the [latest release](https://github.com/roylet/Immich-Cast/releases/latest).
2. Under **Assets**, download `immich-cast-1.0.0.apk` (the version number may be newer).
3. Save it somewhere easy to find, like your Downloads folder.

<details>
<summary>Want a build that isn't released yet?</summary>

Every push is built by GitHub Actions. Open the [Build workflow](https://github.com/roylet/Immich-Cast/actions/workflows/build.yml) (sign in to GitHub), click the newest run with a green tick, download **immich-cast-apk** under **Artifacts**, and unzip it.
</details>

## Step 2: Install adb on your computer

**Windows**

1. Download [SDK Platform-Tools for Windows](https://developer.android.com/tools/releases/platform-tools) and unzip it, for example to `C:\platform-tools`.
2. Open that folder, click the address bar, type `cmd` and press Enter. A command window opens in that folder.
3. In that window, type commands as `adb ...`. If that doesn't work, use `.\adb ...` instead.

**Mac**

Install [Homebrew](https://brew.sh) if you don't have it, then run this in Terminal:

```sh
brew install android-platform-tools
```

**Linux (Debian/Ubuntu)**

```sh
sudo apt install adb
```

Check it works:

```sh
adb version
```

## Step 3: Turn on developer options on the Chromecast

Use the TV remote:

1. Go to **Settings** (your profile picture, top-right) → **System** → **About**.
2. Scroll to **Android TV OS build** and press select on it **7 times**. You'll see *"You are now a developer!"*.
3. Go back to **Settings** → **System** → **Developer options**.
4. Turn on **USB debugging**. Despite the name, this also allows debugging over Wi-Fi.
5. Find the TV's IP address under **Settings** → **System** → **About** → **Status** (or **Network & Internet** → your Wi-Fi network). It looks like `192.168.1.50`.

## Step 4: Connect to the Chromecast

On your computer, using your TV's IP address:

```sh
adb connect 192.168.1.50:5555
```

A prompt appears on the TV asking you to **Allow USB debugging?**. Tick **Always allow from this computer** and select **Allow**.

Check the connection:

```sh
adb devices
```

You should see your TV's address followed by `device`. If it says `unauthorized`, look at the TV for the prompt and run `adb devices` again.

<details>
<summary><strong>"Connection refused"? Use Wireless debugging instead</strong></summary>

Newer Google TV updates use pairing instead of port 5555:

1. On the TV: **Settings** → **System** → **Developer options** → **Wireless debugging** → turn it on → **Pair device with pairing code**.
2. The TV shows an IP address with a port (e.g. `192.168.1.50:37123`) and a 6-digit code. On your computer:
   ```sh
   adb pair 192.168.1.50:37123
   ```
   Enter the code when asked.
3. Back on the Wireless debugging screen, note the **IP address & Port** shown there (a different port, e.g. `192.168.1.50:41235`), then:
   ```sh
   adb connect 192.168.1.50:41235
   ```
</details>

## Step 5: Install Immich Cast

From the folder containing the APK (or give the full path):

```sh
adb install immich-cast-1.0.0.apk
```

You should see `Success`. **Immich Cast** now appears in the TV's apps.

## Step 6: Create an Immich API key

1. In Immich (web), click your avatar → **Account Settings** → **API Keys** → **New API Key**.
2. Name it something like *Living room TV*.
3. If you limit its permissions, it needs at least: `asset.read`, `asset.view`, `asset.download`, `album.read`, `timeline.read`.
4. Copy the key. Immich only shows it once.

## Step 7: Set up the app

Open **Immich Cast** on the TV. The setup asks for:

1. **Server address**: the address you use to open Immich in a browser, e.g. `http://192.168.1.20:2283` or `https://photos.example.com`.
2. **API key**: the key from step 6.
3. **Albums to hide**: select any albums whose photos should never appear (receipts, screenshots…). You can change this later.

**Tip: skip typing the key with the remote.** Since adb is already connected, you can send the address and key straight from your computer. Replace both values, then run:

```sh
adb shell am start -n com.roylet.immichcast/.ui.SettingsActivity --es url "http://192.168.1.20:2283" --es apiKey "PASTE_YOUR_KEY_HERE"
```

The app opens already connected and goes straight to Settings. You can also type with your phone's keyboard through the **Google TV app** on your phone.

Use **Start preview** to check that photos appear. Any button on the remote exits the preview.

## Step 8: Make it your screensaver

First, **save your current screensaver setting** so you can switch back later:

```sh
adb shell settings get secure screensaver_components
```

Copy what it prints somewhere safe. Then switch to Immich Cast:

```sh
adb shell settings put secure screensaver_components com.roylet.immichcast/.dream.PhotoDreamService
adb shell settings put secure screensaver_enabled 1
```

That's it. When the TV is idle, Immich Cast starts instead of Google's Ambient mode. The app's **About** page shows a tick once it's the active screensaver.

**How long until it starts?** The screensaver uses the TV's own idle timer. On most Google TV versions it's under **Settings** → **System** → **Energy** (or **Ambient mode**) → **When to start**, or similar. The menu names vary between updates.

You can now turn **USB debugging** off again if you like. It isn't needed for the screensaver to keep working.

---

## Everyday use

- **Change what's shown**: open Immich Cast from the apps row. Under **Photos → Show** you can pick Seasonal, Random, Recent or On this day, choose excluded albums, and more.
- **Change how it looks**: **Display** and **Overlay** cover time per photo (default 30 seconds), transitions, quality, clock/date overlay (off by default) and dimming.
- Changes apply the next time the screensaver starts.

## Updating to a new version

Download the new APK (step 1), reconnect (step 4), then install over the top. Your settings are kept:

```sh
adb install -r immich-cast-1.0.0.apk
```

> If Android refuses with `INSTALL_FAILED_UPDATE_INCOMPATIBLE`, the new APK was signed with a different key. Uninstall first (see below) and set the app up again.

## Switching back to Google's Ambient mode

Put back the value you saved in step 8:

```sh
adb shell settings put secure screensaver_components "THE_VALUE_YOU_SAVED"
```

If you didn't save it, this is the usual Google TV default. It may differ on some versions:

```sh
adb shell settings put secure screensaver_components com.google.android.apps.tv.dreamx/.service.Backdrop
```

To remove the app completely:

```sh
adb uninstall com.roylet.immichcast
```

---

## Troubleshooting

| Problem | What to try |
|---|---|
| `adb connect` times out | Check that the computer and TV are on the same Wi-Fi network and the IP address is correct. Some routers isolate devices on a guest network, so use your main network. |
| `adb devices` says `unauthorized` | Accept the prompt on the TV. If you missed it, toggle **USB debugging** off and on, then run `adb connect` again. |
| `Connection refused` | Use **Wireless debugging** pairing (see the box in step 4). |
| App says *Can't reach the server* | Use the same address that works in a browser on the same network, including `http://` and the port (usually `:2283`). Avoid `localhost`. |
| App says *The API key was rejected* | Create a new key in Immich and enter it again (**Settings → Connection → API key**). |
| App says *missing permissions* | Edit the key in Immich and add the permissions listed in step 6. |
| Google's Ambient mode came back | Some Google TV updates reset the setting. Repeat step 8 (re-enable USB debugging if you turned it off). |
| Screensaver shows *No photos to show* | Check **Photos → Show** and your excluded albums. If the current season, recent span or day has no photos yet, the app falls back to your whole library. |
| Want to test without waiting | Use **Start preview** in the app. |
