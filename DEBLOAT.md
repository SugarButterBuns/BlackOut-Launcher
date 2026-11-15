# Debloating a Google TV with Claude Code

This guide shows how to trim the pre-installed apps on a Google TV / Android TV **safely and reversibly**, either by asking [Claude Code](https://claude.com/claude-code) to do it or by running the commands yourself.

It was done on a **Sansui 4K Google TV** (`SANSUI_4K_RATV3`, Expressluck NGamer, Android 14, Realtek chipset, 1.7 GB RAM). Package names differ between brands, so **always check what's on your own TV first**.

## The rules that keep it safe

1. **Disable, never uninstall.** `pm disable-user --user 0 <package>` does the same thing as Settings → Apps → Disable. Any app comes back with `pm enable --user 0 <package>`.
2. **Save the full app list first**, plus a restore script, before changing anything.
3. **Never touch:**
   - **Google TV's launcher** (`com.google.android.apps.tv.launcherx`) and **`com.google.android.tungsten.setupwraith`**. Disabling them can crash-loop the Home button, and disabling setupwraith can delete the Wi-Fi network saved during setup.
   - **Play Store, Google Play services, Cast** (`mediashell`), **the remote service** and **system updates** (OTA).
   - **The chipset maker's services** (`com.realtek.*` tuner and system services on this TV) and **the TV maker's factory apps**.
4. **One package per command**: chained `pm` commands can quietly stop after the first.
5. **Keep the apps you use**: put them on a keep-list so they're skipped even if they're added by mistake.
6. **Test afterwards**: Home button, your apps, then a restart to confirm the changes stick.

## Option A: let Claude Code do it

Install [Claude Code](https://claude.com/claude-code), turn on **Network debugging** on the TV (**Settings → System → About**, press OK on **Android TV OS build** 7 times, then **Developer options**), and give it a prompt like this:

> Find my TV on the network and connect to it over ADB. Then debloat it safely: save the full package list and a restore script first, only use `pm disable-user --user 0`, never uninstall, and don't touch the Google TV launcher, setupwraith, Play Store, Play services, Cast, the remote service, system updates or chipset and factory services. Keep these apps: *(your apps)*. Show me the list you plan to disable, split into "recommended" and "optional", and wait for my OK before changing anything. Afterwards, check the Home button and restart the TV to confirm the changes stick.

What to expect:
- Claude asks before downloading ADB, and before every change to the TV.
- The TV shows an **"Allow USB debugging?"** prompt. Tick **Always allow from this computer**, then choose **Allow**. If you don't tick it, the TV forgets your PC after a restart.
- If the prompt stops showing up, go to **Developer options → Revoke USB debugging authorizations**, turn Network debugging off and on, and reconnect.

## Option B: do it by hand

```bash
adb connect TV_IP:5555

# 1. Back up: save the full list and see what's already disabled
adb -s TV_IP:5555 shell pm list packages -f -u > all-packages.txt
adb -s TV_IP:5555 shell pm list packages -d

# 2. Disable one package at a time
adb -s TV_IP:5555 shell pm disable-user --user 0 tv.anoki.acr.anokiacroptin

# 3. Undo any time
adb -s TV_IP:5555 shell pm enable --user 0 tv.anoki.acr.anokiacroptin
```

## What was disabled on the Sansui TV

### Recommended (low risk)
| Package | What it is |
|---|---|
| `tv.anoki.acr.anokiacroptin` | **Automatic content recognition (ACR):** tracks what you watch for ad targeting |
| `com.realtek.retailmode` | Store demo mode |
| `com.realtek.kara`, `com.realtek.karaoke.userguide` | Karaoke app and its guide |
| `com.google.android.play.games` | Google Play Games |
| `com.google.android.feedback` | Sends feedback reports to Google |
| `com.google.android.marvin.talkback` | Screen reader. **Keep it if anyone in your home uses it.** |
| `com.netflix.tokenmanager` | Netflix helper. Only disable it if you don't use Netflix. |

### Optional (each one removes a feature)
| Package | What you lose |
|---|---|
| `com.google.android.katniss` | **Voice search and Assistant** (the mic button). Frees about 70 MB, the biggest single saving. |
| `com.google.android.videos` | The Google TV Shop and your purchased movies |
| `com.seraphic.hbbtv.app` | HbbTV "red button" extras on antenna channels |
| `com.rtk.mediabrowser` | Playing media from a USB drive |
| `com.google.android.apps.tv.dreamx` | Google's ambient screensaver. **First switch to another screensaver:** `settings put secure screensaver_components com.android.dreams.basic/com.android.dreams.basic.Colors` |

YouTube (`com.google.android.youtube.tv`), YouTube Music (`...youtube.tvmusic`) and Netflix (`com.netflix.ninja`) can also be disabled if you use other apps. SmartTube, for example, replaces YouTube.

### Results
- **About 95 MB** of always-running background processes stopped: Assistant and voice search about 71 MB, Google's screensaver about 24 MB.
- With BlackOut Launcher replacing a heavier launcher (Monet, about 130–150 MB), the home screen dropped to about 55 MB.
- **All changes survived a restart.**

## Home-screen ads without replacing the launcher

If you want to keep Google TV's own home screen but get rid of the recommendations and ads, turn on **Settings → Accounts & Sign In → (your account) → Apps only mode**. It's built in and completely reversible.

## Notes
- **Multiple user profiles:** `--user 0` only changes the main profile. Check `adb shell pm list users`; other profiles keep their apps.
- **When you're done:** turn **Network debugging** off again. While it's on, any device on your network can request access.
- **Factory reset:** a factory reset re-enables everything.

## Credits

This guide builds on the approach and warnings from **[mihirgupta0900/tv-home](https://github.com/mihirgupta0900/tv-home)** and **[Gathin23/cleanyourtv](https://github.com/Gathin23/cleanyourtv/)**. Check them out for more device examples and their own launchers.
