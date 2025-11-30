package com.haaz.domain

enum class TtsModel(
    val id: String,
    val title: String,
    val subtitle: String,
    val quality: String,
    val badge: String
) {
    ElevenV3(
        id = "eleven_v3",
        title = "Eleven v3 (alpha)",
        subtitle = "Our most expressive model",
        quality = "High quality",
        badge = "v3"
    ),
    MultilingualV2(
        id = "eleven_multilingual_v2",
        title = "Multilingual v2",
        subtitle = "Great for content creation",
        quality = "High quality",
        badge = "v2"
    ),
    TurboV2(
        id = "eleven_turbo_v2",
        title = "Turbo v2",
        subtitle = "Lower latency",
        quality = "High quality",
        badge = "v2"
    );

    companion object {
        fun fromId(id: String?): TtsModel = entries.firstOrNull { it.id == id } ?: MultilingualV2
    }
}
