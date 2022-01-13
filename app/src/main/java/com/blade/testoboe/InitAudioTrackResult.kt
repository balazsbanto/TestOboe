package com.blade.testoboe

import android.media.AudioTrack

sealed class InitAudioTrackResult {
    object InitAudioTrackFail : InitAudioTrackResult()

    class InitAudioTrackSuccess(val audioTrack: AudioTrack, val totalFrames: Int) :
        InitAudioTrackResult()
}