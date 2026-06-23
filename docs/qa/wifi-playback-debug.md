# Wi-Fi Playback Debug & Fix

## Issue Description
Users reported that OmniTune playback frequently fails when connected to Wi-Fi, while succeeding on Mobile Data. The player shows a generic "No internet connection" error even though the device is online and other apps work perfectly.

## Root Cause
The problem was caused by two compounding issues related to YouTube CDN and ExoPlayer networking:
1. **IP-Bound Stream Caching (403 Forbidden):** `StreamUrlResolver` caches extracted stream URLs. YouTube binds these URLs to the IP address that requested them. When the device switches from Mobile Data to Wi-Fi, the IP address changes, causing the CDN to immediately reject the cached URL with a `403 Forbidden`.
2. **ExoPlayer HTTP Configuration:** `MusicService` was using `DefaultHttpDataSource` (backed by legacy `HttpURLConnection`). It lacked a robust `User-Agent` string and struggled with IPv4/IPv6 fallback, making it susceptible to unpredictable DNS timeouts on strict Wi-Fi routers.

## Fix Implemented
- **Robust HTTP Client:** Replaced `DefaultHttpDataSource` with `OkHttpDataSource` across `MusicService` and `DownloadUtil`. Injected `OkHttpClient` and configured a stable desktop `User-Agent` to prevent CDN blocking.
- **Network Change Detection:** Added a `ConnectivityManager.NetworkCallback` in `MusicService` to actively monitor transport capabilities (`TRANSPORT_WIFI` vs `TRANSPORT_CELLULAR`).
- **Cache Invalidation & Seamless Resume:** Upon detecting a network transport change, the app clears the `StreamUrlResolver` memory cache. If a track is actively playing, the player re-resolves the stream securely and resumes playback seamlessly from the current position.
- **Clear Error Messaging:** Updated `MusicService.onPlayerError`. If a network error occurs specifically while connected to Wi-Fi, the user now receives a clear instruction: *"Playback failed on this network. Try another Wi-Fi, disable VPN/Private DNS, or switch to mobile data."*

## Verification
- Switch from Mobile Data to Wi-Fi mid-playback. The track pauses briefly, invalidates the cache, fetches a fresh URL bound to the new IP, and resumes.
- Restrict OmniTune internet access and attempt playback on Wi-Fi. The specific "Playback failed on this network..." toast is shown instead of a confusing "No internet" toast.
- All downloads and background playback remain stable using `OkHttpDataSource`.
