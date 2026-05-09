# Pornhub-Country-Restriction-Bypass-
# HLS Native Player & Downloader
**Copyright © Dr. Dev || Dr. Hamza 2026**

A native Android application built with Java and Media3 (ExoPlayer). It bypasses strict CDN anti-hotlinking protections (CORS/Headers) to directly stream and download HLS (`.m3u8`) playlists.

## Features
* **Bypass CDN Protections**: Natively injects `Origin`, `Referer`, and `User-Agent` headers.
* **Auto Quality Selection**: Parses Master Playlists and auto-selects the best stream.
* **Mass Downloader**: Extracts and stitches `.ts` segments into a single offline file.
* **History Manager**: SQLite-backed history tracking (Play & Delete).
* **Offline Vault**: View, play, and delete downloaded `.ts` media files.
