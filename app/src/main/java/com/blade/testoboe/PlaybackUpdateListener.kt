package com.blade.testoboe

interface PlaybackUpdateListener {
    fun onPlaybackUpdate(percentage: Float)
    fun onPlaybackFinished()
}