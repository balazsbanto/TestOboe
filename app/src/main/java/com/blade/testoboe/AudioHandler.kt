package com.blade.testoboe

import android.content.Context
import android.media.*
import android.os.Build
import android.os.FileUtils
import android.util.Base64
import android.util.Base64OutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.*

class AudioHandler {

    private var audioFileBase64: String? = null
    private lateinit var audioSource: InputStream
    private var audioTrack: AudioTrack? = null


    suspend fun stopAudioRecording() {
        if (audioTrack != null) {
            try {
                audioTrack?.pause()
                audioTrack?.flush()
                audioTrack?.release()
            } catch (e: Exception) {
            }
            audioTrack = null
        }
    }

    suspend fun makeSound(
        context: Context,
        preferredAudioOutputDeviceType: Int,
        stimuliFileBase64: String,
        onPlaybackListener: PlaybackUpdateListener
    ) {
        // setting volume to the max
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        // force or making sure the sound is played from the loudspeaker and not via headphone
        //mAudioManager.setMode(AudioManager.MODE_IN_COMMUNICATION) // doesn't work on S8 after 8KhZ
        audioManager.mode = AudioManager.MODE_IN_CALL // works both in S8 and nexus4
        audioManager.isSpeakerphoneOn = true
        audioManager.setStreamVolume(
            AudioConfig.MODE,
            audioManager.getStreamMaxVolume(AudioConfig.MODE),
            0
        )
        // Logging if the speaker and the microphone supports emitting and recroding ultrasounds.
        val supportUltrasoundSpeaker =
            audioManager.getProperty(AudioManager.PROPERTY_SUPPORT_SPEAKER_NEAR_ULTRASOUND)
        val supportUltrasoundMic =
            audioManager.getProperty(AudioManager.PROPERTY_SUPPORT_MIC_NEAR_ULTRASOUND)
        Timber.d("Device speaker supports ultrasound: " + supportUltrasoundSpeaker)
        Timber.d("Device mic supports ultrasound: " + supportUltrasoundMic)

        audioFileBase64 = stimuliFileBase64
        val decodedString: ByteArray = Base64.decode(audioFileBase64, Base64.DEFAULT)
        audioSource = ByteArrayInputStream(decodedString)
        val initAudioResult = initAudioTrack(
            context,
            audioSource,
            AudioConfig.MODE,
            preferredAudioOutputDeviceType
        )
        if (initAudioResult is InitAudioTrackResult.InitAudioTrackSuccess) {
            audioTrack = initAudioResult.audioTrack
            val callback = object : AudioTrack.OnPlaybackPositionUpdateListener {
                override fun onPeriodicNotification(track: AudioTrack?) {
                    val percentagePlayed =
                        100f * track!!.playbackHeadPosition / initAudioResult.totalFrames.toFloat()
                    onPlaybackListener.onPlaybackUpdate(percentagePlayed)
                }

                override fun onMarkerReached(track: AudioTrack?) {
                    onPlaybackListener.onPlaybackFinished()
                }
            }
            audioTrack!!.setPlaybackPositionUpdateListener(callback)
        }
        try {
            playAudio(audioSource, audioTrack)
        } catch (e: IOException) {
            e.printStackTrace()
            audioSource.close()
            audioTrack = null
        }
    }

    companion object {

        internal fun initAudioTrack(
            context: Context,
            audioSource: InputStream,
            streamType: Int,
            preferredAudioOutputDeviceType: Int
        ): InitAudioTrackResult {
            val wh = WaveHeader()
            try {
                wh.read(audioSource)
                Timber.d("WaveHeader: " + wh)
            } catch (e: IOException) {
                e.printStackTrace()
                audioSource.close()
                return InitAudioTrackResult.InitAudioTrackFail
            }
            val bytesPerSample = wh.bitsPerSample.toInt() / 8
            val audioFormat = getPcmAudioFormat(wh.bitsPerSample.toInt())
            val channels = getChannelsFormat(wh.numChannels.toInt())
            val sampleRate = wh.sampleRate
            val totalFrames = wh.numBytes / wh.numChannels / bytesPerSample
            val length = totalFrames.toDouble() / sampleRate
            Timber.d("WaveHeader length in sec: " + length)
            Timber.d("WaveHeader channel: " + channels)

            val audioTrack = AudioTrack(
                streamType,
                sampleRate,
                channels,
                audioFormat,
                AudioConfig.sourceBufferSizeInBytes,
                AudioTrack.MODE_STREAM
            )

            findAudioOutputDevice(
                context,
                preferredAudioOutputDeviceType
            )?.let {
                audioTrack.setPreferredDevice(it)
            }
            audioTrack.positionNotificationPeriod = sampleRate / 20
            audioTrack.notificationMarkerPosition = totalFrames
            return InitAudioTrackResult.InitAudioTrackSuccess(
                audioTrack,
                totalFrames
            )
        }

        internal suspend fun playAudio(audioSource: InputStream, audioTrack: AudioTrack?) =
            withContext(Dispatchers.IO) {
                var i = 0
                val music = ByteArray(AudioConfig.sourceBufferSizeInBytes)
                audioTrack?.play()
                while (audioTrack != null &&
                    audioSource.read(music).also { i = it } != -1
                ) {
                    try {
                        audioTrack.write(music, 0, i)
                    } catch (e: Exception) {
                        Timber.e(e.toString())
                        break
                    }
                }
                audioSource.close()
            }

        internal fun findAudioOutputDevice(context: Context, deviceType: Int): AudioDeviceInfo? {
            return findAudioDevice(
                context,
                AudioManager.GET_DEVICES_OUTPUTS,
                deviceType
            )
        }

        private fun findAudioDevice(
            context: Context,
            deviceFlag: Int,
            deviceType: Int
        ): AudioDeviceInfo? {
            val manager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val adis = manager.getDevices(deviceFlag)
            for (adi in adis) {
                if (adi.type == deviceType) {
                    return adi
                }
            }
            return null
        }

        private fun getPcmAudioFormat(bytesPerSample: Int): Int =
            when (bytesPerSample) {
                1 -> AudioFormat.ENCODING_PCM_8BIT
                2 -> AudioFormat.ENCODING_PCM_16BIT
                else -> AudioFormat.ENCODING_DEFAULT
            }

        private fun getChannelsFormat(channels: Int): Int =
            when (channels) {
                2 -> AudioFormat.CHANNEL_OUT_STEREO
                else -> AudioFormat.CHANNEL_OUT_MONO
            }
    }
}