package com.focusflow.data.model

import com.focusflow.R

/**
 * Represents an audio track available for background sound during focus sessions
 */
data class AudioTrack(
    val id: String,
    val name: String,
    val resourceId: Int
)

/**
 * Available audio tracks in the application
 */
object AudioTracks {
    const val NATURE = "nature"
    const val BINAURAL = "binaural"
    const val WHITE_NOISE = "white_noise"
    const val NONE = "none"
    
    /**
     * Get all available audio tracks
     */
    fun getAll(): List<AudioTrack> = listOf(
        AudioTrack(
            id = NATURE,
            name = "Nature Sounds",
            resourceId = R.raw.nature_sounds
        ),
        AudioTrack(
            id = BINAURAL,
            name = "Binaural Beats",
            resourceId = R.raw.binaural_beats
        ),
        AudioTrack(
            id = WHITE_NOISE,
            name = "White Noise",
            resourceId = R.raw.white_noise
        ),
        AudioTrack(
            id = NONE,
            name = "None (Silent)",
            resourceId = R.raw.silence
        )
    )
    
    /**
     * Get audio track by ID
     */
    fun getById(id: String): AudioTrack {
        return getAll().find { it.id == id } ?: getAll().first()
    }
}
