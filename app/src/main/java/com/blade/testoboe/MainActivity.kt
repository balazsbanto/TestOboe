package com.blade.testoboe

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import com.blade.testoboe.databinding.ActivityMainBinding
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.AudioManager.GET_DEVICES_INPUTS
import android.os.Environment
import android.util.Base64
import android.util.Base64OutputStream
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {

    var fullPathToFile = ""
    var recordingFrequency = 48000

    private var audioPlayback: AudioHandler = AudioHandler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Timber.d("timber")
        Log.d("logcat", "logcal")

        // Example of a call to a native method
        sample_text.text = stringFromJNI()

        buttonStartRecording.isEnabled = true
        buttonStopRecording.isEnabled = false

        editTextFreq.setText(recordingFrequency.toString())

        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val devices = audioManager.getDevices(GET_DEVICES_INPUTS)

        val permissionW =
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        if (permissionW != PackageManager.PERMISSION_GRANTED) {
            Log.i("OboeAudioRecorder", "Permission to write denied")
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                WRITE_REQUEST_CODE
            )
        }

        val permissionR =
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
        if (permissionR != PackageManager.PERMISSION_GRANTED) {
            Log.i("OboeAudioRecorder", "Permission to read denied")
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                READ_REQUEST_CODE
            )
        }


        // Full path that is going to be sent to C++ through JNI ("/storage/emulated/0/Recorders/record.wav")
        fullPathToFile = createTimeStampedFile(this, "aud", "wav").absolutePath

        buttonStartRecording.setOnClickListener {
            val permission =
                ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)

            if (permission != PackageManager.PERMISSION_GRANTED) {
                Log.i("OboeAudioRecorder", "Permission to record denied")
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.RECORD_AUDIO),
                    RECORD_REQUEST_CODE
                )
            } else {
                processStartRecording()
            }
        }

        buttonStopRecording.setOnClickListener {
            processStopRecording()
        }
    }

    fun playStimulus() {
        lifecycleScope.launch(Dispatchers.IO) {
            audioPlayback.makeSound(this@MainActivity, AudioDeviceInfo.TYPE_BUILTIN_EARPIECE,
                convertFileToBase64(
                    File(
                        getExternalFilesDir(null),
                        "stimulus_29_short_audible_chirp_wn_end.wav"
                    )
                ),
                object : PlaybackUpdateListener {
                    override fun onPlaybackUpdate(percentage: Float) {
                        Timber.d(percentage.toString())
                    }

                    override fun onPlaybackFinished() {
                        Timber.d("processStopRecording")
                        processStopRecording()
                    }

                })
        }
    }

    private fun convertFileToBase64(imageFile: File): String {
        return ByteArrayOutputStream().use { outputStream ->
            Base64OutputStream(outputStream, Base64.DEFAULT).use { base64FilterStream ->
                imageFile.inputStream().use { inputStream ->
                    inputStream.copyTo(base64FilterStream)
                }
            }
            return@use outputStream.toString()
        }
    }

    // Get the recording frequency entered by the user. If empty then default to 48000.
    fun getRecordingFreq(): Int {
        var freq = recordingFrequency
        if (!editTextFreq.text.toString().trim().isEmpty()) {
            freq = editTextFreq.text.toString().toInt()
        }
        return freq
    }

    val RECORD_REQUEST_CODE = 1234
    val WRITE_REQUEST_CODE = 1235
    val READ_REQUEST_CODE = 1236

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            RECORD_REQUEST_CODE -> {
                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Log.i("OboeAudioRecorder", "Permission has been denied by user")
                } else {
                    Log.i("OboeAudioRecorder", "Permission has been granted by user")

                    processStartRecording()
                }
            }
            WRITE_REQUEST_CODE -> {
                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Log.i("OboeAudioRecorder", "Permission has been denied by user")
                } else {
                    Log.i("OboeAudioRecorder", "Permission has been granted by user")
                }
            }
            READ_REQUEST_CODE -> {
                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Log.i("OboeAudioRecorder", "Permission has been denied by user")
                } else {
                    Log.i("OboeAudioRecorder", "Permission has been granted by user")
                }
            }
        }
    }

    var stopTimer = false
    var seconds = 0
    val maxSeconds = 10
    fun startTimer() {
        stopTimer = false
        val timer = Timer()
        val task = object : TimerTask() {
            override fun run() {
                if (!stopTimer) {
                    seconds++
                    textViewTimer.post {
                        textViewTimer.setText(seconds.toString())
                    }
                    if (seconds == maxSeconds) {
                        processStopRecording()
                    }
                } else {
                    this.cancel()
                }
            }
        }
        timer.schedule(task, 0, 1000)
    }

    fun stopTimer() {
        stopTimer = true
        seconds = 0
    }

    val enableTimer = false

    fun processStartRecording() {

        playStimulus()
        if (enableTimer) {
            startTimer()
        }
        Thread(Runnable {
            startRecording(fullPathToFile, getRecordingFreq())
        }).start()
        buttonStartRecording.isEnabled = false
        buttonStopRecording.isEnabled = true
    }

    fun processStopRecording() {
        if (enableTimer) {
            stopTimer()
        }
        Thread(Runnable { stopRecording() }).start()
        buttonStartRecording.isEnabled = true
        buttonStopRecording.isEnabled = false
    }

    fun createTimeStampedFile(context: Context, prefix: String, extension: String): File {
        val sdf = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SSS", Locale.US)
        return File(context.getExternalFilesDir(null), "${prefix}_${sdf.format(Date())}.$extension")
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    external fun stringFromJNI(): String

    external fun startRecording(fullPathToFile: String, recordingFrequency: Int): Boolean

    external fun stopRecording(): Boolean

    companion object {
        // Used to load the 'native-lib' library on application startup.
        init {
            System.loadLibrary("testoboe")
        }
    }
}


//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//class MainActivity : AppCompatActivity() {
//
//    private lateinit var binding: ActivityMainBinding
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//
//        binding = ActivityMainBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//
//        // Example of a call to a native method
//        binding.sampleText.text = stringFromJNI()
//    }
//
//    /**
//     * A native method that is implemented by the 'testoboe' native library,
//     * which is packaged with this application.
//     */
//    external fun stringFromJNI(): String
//
////    external fun  st
//
//    companion object {
//        // Used to load the 'testoboe' library on application startup.
//        init {
//            System.loadLibrary("testoboe")
//        }
//    }
//}