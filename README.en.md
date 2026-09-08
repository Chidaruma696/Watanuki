[🇪🇸 Español](README.md)

<div align="center">
  <br/>
  <img src="docs/icon.png" width="180" alt="Watanuki icon" />

# Watanuki

**渡 · An open-source anime viewer for Latin America, with a manga aesthetic.**

<br/>

![Android 8.0+](https://img.shields.io/badge/android-8.0%2B-3ddc84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/kotlin-2.2-7f52ff?style=for-the-badge&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/jetpack%20compose-ui-4285f4?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![libVLC](https://img.shields.io/badge/libVLC-3.6-ff8800?style=for-the-badge&logo=vlcmediaplayer&logoColor=white)
![Apache 2.0 License](https://img.shields.io/badge/license-Apache%202.0-1b150d?style=for-the-badge)

<br/>

[![Download APK](https://img.shields.io/github/v/release/Chidaruma696/Watanuki?label=%F0%9F%93%B2%20DOWNLOAD%20APK&style=for-the-badge&color=2b2140)](https://github.com/Chidaruma696/Watanuki/releases/latest)

<br/>

*59 Spanish-language sources compiled into the app · no extensions · no ads · no tracking*

</div>

---

> [!IMPORTANT]
> **Watanuki does not host, upload, or distribute any video.** It only reads public third-party sites, just like a browser does.
> If you can pay for a legal platform, do it. This project exists because of a gap, not as a replacement. Read [why it exists](#-why-watanuki-exists) and [how to support anime](#-support-anime-for-real).

<br/>

## 📲 Download

1. Go to the [latest release](https://github.com/Chidaruma696/Watanuki/releases/latest) and download the `Watanuki-x.y.z.apk` file.
2. Open it on your phone. Android will ask for permission to install apps from this source; accept it once.
3. Done. Later versions install on top without losing your settings or downloads.

Watanuki is not on the Play Store and never will be; it is distributed only from here. And as long as Android stays open, that is enough ([why it matters](#-keep-android-open)).

<br/>

## 🗺️ What it is

Watanuki is an Android app for watching anime from Spanish-language sites, made for people who live in a country where the legal offering arrives late, incomplete, or not at all. It takes the community sources from [Aniyomi](https://github.com/Kohi-den/extensions-source), **compiles them into the APK** (no loose extensions to install), wraps them in a manga-styled interface, and gives them a serious player.

| 📺 Watch | 📥 Save | 🎨 Live |
| --- | --- | --- |
| Home feed with recommendations, latest episodes, and most popular per source | Parallel downloads, per episode or full season, with resume support | Ten palettes inspired by Touhou characters, in light and dark |
| libVLC player: any codec, six scaling modes, quick skips | Queue with progress, retries, and free-space checks | "Manga" design system ported from Komi Store: paper, ink, stamps, and screentones |
| Automatic pick of the best server, or a manual list | Offline playback of everything you saved | Adult content filter **off by default** from the very first launch |

<br/>

## ✨ Komi aesthetic

Watanuki's interface is an adaptation of the **Manga** *personality* from [Komi Store](https://github.com/komi-store/komi-store), a design system that drops Material to look like a manga page instead:

- 📄 **Paper and ink**: cream background by day, ink black by night; no neutral grays.
- ▭ **Zero rounded corners**: panels with 3 dp borders and hard, offset shadows, no blur.
- 🔤 **Anton in uppercase** for headlines, Noto Sans for body text, JetBrains Mono for data.
- 🩹 **Stamps and screentones**: headers with a slanted marker, dot screentones in the corners, buttons that get "stamped" when pressed.
- 🎌 Japanese *kickers* in every section (今日 · FOR YOU, 保存 · DOWNLOADS).

The colors are not Komi's: each theme takes its palette from a Touhou Project character (Reimu, Marisa, Patchouli, Sakuya, Remilia, Flandre, Cirno, Youmu, Yuyuko, Alice), each with a day and a night version.

<br/>

## 💔 Why Watanuki exists

This is not an app "to avoid paying." It is a response to a concrete, documented problem: **in Latin America, legal anime is scarce, fragmented, and expensive relative to income.**

- 📉 **The legal offering doesn't cover what airs.** Every season there are relevant series that no platform licenses for the region, or that arrive weeks or months behind Japan. When there are no simulcast rights, a territory can wait six weeks, three months, or more than a year for legal access, if it ever comes ([CBR, fall 2025](https://www.cbr.com/fall-2025-anime-streaming-limbo/); [Vitrina](https://vitrina.ai/blog/anime-regional-licensing-restrictions/)).
- 🧩 **Catalogs break down by country.** An anime can be on Crunchyroll in the United States and not in Mexico because another company holds the regional exclusive; licenses expire and series vanish without warning ([Level Up](https://www.levelup.com/noticias/crunchyroll-elimina-sin-avisar-mas-de-5-animes-muy-queridos-de-su-catalogo-y-demuestra-los-peligros-del-formato-digital/)). HiDive left Latin America in 2024 and left titles with no legal path at all ([MyAnimeList](https://myanimelist.net/stacks/58905)).
- 🌎 **The region is already watching anime; just without a legal option.** Brazil is the second country in the world for anime piracy; Mexico, Colombia, Chile, and Argentina make up the bulk of the audience of the planet's largest anime piracy site ([CBR](https://www.cbr.com/hianime-biggest-piracy-streaming-america-government-threat/); [Advanced Television](https://advanced-television.com/2016/10/17/anime-hit-by-7-7bn-pirate-visits/)). According to MUSO, in 2024 Mexico racked up 4.4 billion visits to piracy sites and Brazil 4.1 billion ([MUSO 2024](https://www.muso.com/hubfs/MUSO%202024%20Piracy%20Trends%20and%20Insights.pdf)).
- ☠️ **And it does so on dangerous sites.** Anime piracy websites in Brazil turned out to be up to 80 times riskier than a legitimate site in terms of malware and fraud ([Advanced Television, 2026](https://www.advanced-television.com/2026/05/01/studies-highlight-latam-piracy-cybersecurity-risks/)). Watanuki at least removes that layer: no ads, no trackers, no pop-ups, nothing weird to install.

Watanuki doesn't fix any of that. It only makes the path people already use cleaner and safer while the industry fails to show up.

<br/>

## 🙇 To the authors, studios, and animators

We apologize. Sincerely, not as a formality.

Behind every episode there are people working under conditions the industry itself admits are unsustainable: young animators earning under two million yen a year (about 13,000 dollars), workdays past twelve hours, and between 50% and 70% of the trade working freelance with no labor protections ([Infobae](https://www.infobae.com/america/mundo/2025/02/17/japon-enfrenta-fuertes-denuncias-laborales-en-la-industria-del-anime/); [Nippon.com](https://www.nippon.com/es/in-depth/d01174/); [Somos Kudasai](https://somoskudasai.com/noticias/animador-japones-revela-salario-explotacion-industria-anime-pirateria/)).

Every view that doesn't go through an official channel is a view that never comes back to them. We know that. That is why this project has no advertising, makes no money, and doesn't try to compete with anyone: if tomorrow the whole catalog reached the region legally, Watanuki should disappear, and we would be fine with that.

<br/>

## 💚 Support anime for real

If you use Watanuki, **give something back**. It doesn't take much; it has to be real. In order of impact:

1. 💳 **Pay for a legal platform** even if it doesn't have everything. Crunchyroll, Netflix, Prime Video, and Pluto TV license for the region, and those audience numbers are what make a studio see Latin America as a market rather than a loss.
2. 📀 **Buy official Blu-rays, manga, and soundtracks** when they reach your country. It's the most direct income for the production committees.
3. 🧸 **Official merchandise, not knockoffs.** A licensed figure or artbook pays salaries; the imitation pays someone else.
4. 🎟️ **Go to the theater** when an anime film opens in your city. Local distributors decide what to bring next based on what filled seats last time.
5. 📣 **Ask for what you want to watch, through official channels.** Platforms read requests by region; your country's catalog is negotiated with that data.
6. 💬 **Talk about the anime, not the piracy site.** Recommend the work, the studio, the author. Let demand grow for the legal option, not for the shortcut.

And if an anime you watch on Watanuki shows up on a legal platform in your country, **switch**. That's the deal.

<br/>

## 🔧 How it works

```
Watanuki
├── app/              Compose UI (feed, catalog, details, downloads, settings), libVLC player, streaming proxy
└── anime-sources/    Aniyomi sources compiled as a module + runtime for the extensions-lib 14 API
    ├── upstream/     Copy of Kohi-den/extensions-source (src/es, lib, lib-multisrc), Apache 2.0
    └── src/          Runtime: networking, cookies, Cloudflare, JavaScript engine, models, generated registry
```

- 🧩 **Compiled sources, not extensions.** A Gradle script walks `upstream/src/es`, adds every source and every extractor as a *source set* of the module, and generates a registry of the classes. There are no APKs to install and no permissions needed to see them.
- 🌐 **Our own runtime for the Aniyomi API.** The sources are written against `extensions-lib` v14; Watanuki implements that API (`AnimeHttpSource`, models, networking) with code from Aniyomi and our own.
- 🎬 **Local proxy for the player.** libVLC only accepts User-Agent and Referer, so videos go through an HTTP server on `127.0.0.1` that forwards them with all of the source's headers and cookies and rewrites the HLS playlists.
- 📦 **Our own downloads.** OkHttp with range-based resume, HLS concatenated into a single file, a queue with multiple workers, and free-space checks.

### Build

```bash
git clone https://github.com/Chidaruma696/Watanuki.git
cd Watanuki
./gradlew :app:assembleDebug        # APK in app/build/outputs/apk/debug/
```

Requirements: JDK 17 or later and Android SDK 36. To update the sources to the community's latest state: `tools/update-sources.sh`.

<br/>

## 🗺️ Roadmap

- [ ] Library and history with per-episode progress
- [ ] Player gestures: pinch to zoom, brightness and volume, PiP
- [ ] Subtitles and audio tracks
- [ ] Downloading encrypted HLS streams
- [ ] More source languages (only `es` today)
- [ ] Sync with AniList and MyAnimeList

<br/>

## ⚖️ Legal

Watanuki is distributed under the [Apache 2.0 License](LICENSE). It does not contain or distribute copyrighted content; it accesses third-party sites chosen by the user. Theme names are Touhou Project characters, property of Team Shanghai Alice, with no affiliation whatsoever. All third-party code and libraries, with their licenses, are listed in [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

Watanuki uses **libVLC** (VideoLAN) under LGPL 2.1, unmodified; its source code is at [code.videolan.org](https://code.videolan.org/videolan/vlc-android).

<br/>

## 📢 Keep Android Open

> **Your phone is about to stop being yours.** [keepandroidopen.org](https://keepandroidopen.org/)

In 2025 Google announced **mandatory developer verification**, effective in 2027: anyone publishing an Android app will have to register in a central Google system, pay a fee, and hand over their government ID. Apps from anyone who doesn't register **will be blocked on every certified device in the world**, whether or not they are on the Play Store, F-Droid's included. Installing on your own will become a nine-step process with a 24-hour wait, controlled by Google Play Services and revocable at any time.

Let's be clear: **if that goes into effect, Watanuki ceases to exist.** So does Yuko, and any free app that doesn't go through Google's checkout. This project is only possible because Android is still open.

That is why the app shows a notice on the Home screen (it can be hidden) and a permanent link under Settings › About. The campaign is backed by 71 organizations from 23 countries (F-Droid, EFF, FSF, Nextcloud, Proton, KDE, Tor Project, LineageOS, GNOME, Brave…). What it asks for is simple:

- 📲 **Install F-Droid** on every Android device you own.
- ✍️ **Sign the petition** and share the page.
- 🏛️ **Write to your competition or consumer protection regulator.**
- 🧑‍💻 If you're a developer: **don't register**, and convince others not to.

<br/>

<div align="center">

渡 · わたぬき

</div>
