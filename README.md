# Kaustav Cloud Storage App

This repository contains a minimal Android app prototype for the Kaustav concept. The app uploads files to Telegram (via the Bot API) instead of storing them offline on the device. It includes placeholders for the Telegram API credentials.

## Configure Telegram

1. Create a Telegram bot with BotFather and copy the bot token.
2. Start a chat with the bot and obtain the chat ID where files should be stored.
3. Enter the bot token and chat ID in the app, then connect.

## Build the APK

```bash
gradle assembleDebug
```

The debug APK will be at `app/build/outputs/apk/debug/app-debug.apk` after a successful build.
