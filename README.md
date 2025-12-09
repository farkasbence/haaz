# Haaz Text-to-Speech App

## Overview
Haaz is a simple ElevenLabs-powered text-to-speech Android app built with Jetpack Compose. Users can enter text, generate audio, and play it back from the home screen. A settings sheet lets users configure the model, voice, speed and stability. A history page allows users to see their past prompts and select them to be used in the main text-to-speech generation flow.

## Setup
1) Add your ElevenLabs API key to `local.properties`:
```
ELEVENLABS_API_KEY=your_api_key_here
```
2) Build/Run the app.

3) Open the app and enter text, then tap Generate.

## Tech Stack
- Kotlin, Jetpack Compose, Material3
- Retrofit/OkHttp
- [ElevenLabs TTS API](https://elevenlabs.io/docs/api-reference/text-to-speech/stream)
- Media3 ExoPlayer for playback
- DataStore for settings persistence

## Screenshots
| Home | Playback | Settings | History | E2E flow                             |
| --- | --- | --- | --- |--------------------------------------|
| ![home.png](docs/screenshots/home.png) | ![playback.png](docs/screenshots/playback.png) | ![settings.png](docs/screenshots/settings.png) | ![history.png](docs/screenshots/history.png) | ![e2e.gif](docs/screenshots/e2e.gif) |
