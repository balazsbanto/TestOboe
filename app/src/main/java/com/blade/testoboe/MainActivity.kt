package com.blade.testoboe

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import com.blade.testoboe.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Example of a call to a native method
        binding.sampleText.text = stringFromJNI()
    }

    /**
     * A native method that is implemented by the 'testoboe' native library,
     * which is packaged with this application.
     */
    external fun stringFromJNI(): String

//    external fun  st

    companion object {
        // Used to load the 'testoboe' library on application startup.
        init {
            System.loadLibrary("testoboe")
        }
    }
}