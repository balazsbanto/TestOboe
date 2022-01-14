package com.blade.testoboe

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.AudioManager.GET_DEVICES_INPUTS
import android.media.MediaScannerConnection
import android.media.MediaScannerConnection.OnScanCompletedListener
import android.os.Environment
import android.util.Base64
import android.util.Base64OutputStream
import android.util.Log
import android.widget.ArrayAdapter
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

    private var audioPlayback: AudioHandler = AudioHandler()
    private var fullPathToFile = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Example of a call to a native method
        oboelabel.text = stringFromJNI()

        buttonStartRecording.isEnabled = true

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

        populateSpinners()
    }

    fun populateSpinners() {
        ArrayAdapter.createFromResource(
            this,
            R.array.array_input_preset,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            // Specify the layout to use when the list of choices appears
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            // Apply the adapter to the spinner
            input_preset_spinner.adapter = adapter
        }

        ArrayAdapter.createFromResource(
            this,
            R.array.array_performance_mode,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            // Specify the layout to use when the list of choices appears
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            // Apply the adapter to the spinner
            performance_mode_spinner.adapter = adapter
        }
    }

    fun mapInputPresetToNdk(inputPreset: String): Int {
        return when (inputPreset) {
            "VoiceRecognition" -> 6
            "Unprocessed" -> 9
            "Camcorder" -> 5
            else -> throw Throwable("Unsupported")
        }
    }

    fun mapPerformanceModeToNdk(performanceMode: String): Int {
        return when (performanceMode) {
            "LowLatency" -> 12
            "None" -> 10
            else -> throw Throwable("Unsupported")
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

    fun processStartRecording() {

        playStimulus()
        initAndStartRecording()

        buttonStartRecording.isEnabled = false
    }

    private fun initAndStartRecording() {

        val inputPreset = input_preset_spinner.selectedItem.toString()
        val performanceMode = performance_mode_spinner.selectedItem.toString()

        val sdf = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.US)
//        val dir = this.getExternalFilesDir(null)
        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
        fullPathToFile = File(
            dir, "aud_${sdf.format(Date())}" +
                    "_preset-${inputPreset}_pMode-${performanceMode}.wav"
        ).absolutePath


        Thread(Runnable {
            startRecording(
                fullPathToFile, AudioConfig.RECORDER_SAMPLERATE,
                mapInputPresetToNdk(inputPreset),
                mapPerformanceModeToNdk(performanceMode)
            )
        }).start()
    }


    fun processStopRecording() {
        Thread.sleep(150)
        Thread(Runnable { stopRecording() }).start()
        buttonStartRecording.isEnabled = true
        scanFile(fullPathToFile)
    }

    private fun scanFile(path: String) {
        MediaScannerConnection.scanFile(
            this@MainActivity, arrayOf(path), null
        ) { p, uri -> Timber.i("Finished scanning " + p) }
    }


//    fun createTimeStampedFile(context: Context, prefix: String, extension: String): File {
//        val sdf = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SSS", Locale.US)
//        return File(context.getExternalFilesDir(null), "${prefix}_${sdf.format(Date())}.$extension")
//    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    external fun stringFromJNI(): String

    external fun startRecording(
        fullPathToFile: String, recordingFrequency: Int,
        inputPreset: Int, performanceMode: Int
    ): Boolean

    external fun stopRecording(): Boolean

    companion object {
        // Used to load the 'native-lib' library on application startup.
        init {
            System.loadLibrary("testoboe")
        }
    }
}
