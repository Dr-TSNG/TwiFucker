# TwiFucker

## Yet Another Adkiller for Twitter

https://t.me/TwiFucker

This is an Xposed module. Support only API 93+.

You can find Beta version / Rootless integration (automatically embed latest Twitter with [LSPatch](https://github.com/LSPosed/LSPatch)) at our Telegram channel.

## Features

1. Remove promoted user, content, trends, who to follow and topics to follow module [\[1\]](app/src/main/java/icu/nullptr/twifucker/hook/TimelineEntryHook.kt) [\[2\]](app/src/main/java/icu/nullptr/twifucker/hook/TimelineModuleHook.kt) [\[3\]](app/src/main/java/icu/nullptr/twifucker/hook/TimelineUserHook.kt) [\[4\]](app/src/main/java/icu/nullptr/twifucker/hook/TimelineTrendHook.kt) [\[5\]](app/src/main/java/icu/nullptr/twifucker/hook/TimelineTweetHook.kt)
2. [Remove share link tracking](app/src/main/java/icu/nullptr/twifucker/hook/UrlHook.kt)
3. [Remove sensitive media warning](app/src/main/java/icu/nullptr/twifucker/hook/sensitiveMediaWarning.kt)
4. [Copyable alt text](app/src/main/java/icu/nullptr/twifucker/hook/AltTextHook.kt)
5. [Download media menu](app/src/main/java/icu/nullptr/twifucker/hook/DownloadHook.kt)

## Usage

- `Settings and privacy` > `Additional resources` > Tap version

## Known Issues

- Loaded promoted content before switching on TwiFucker is not removed because it's cached.
