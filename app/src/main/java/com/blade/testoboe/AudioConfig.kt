package com.blade.testoboe

import android.media.AudioFormat
import android.media.AudioManager
import android.media.MediaRecorder

object AudioConfig {
    const val RECORDER_SAMPLERATE = 48000
    const val RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO
    const val RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT
    const val AUDIO_SOURCE = MediaRecorder.AudioSource.UNPROCESSED

    const val MODE = AudioManager.STREAM_VOICE_CALL

    const val sourceBufferSizeInBytes = 512


}