package com.maxrave.exampleApp.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import com.maxrave.exampleApp.R
import com.maxrave.kotlinyoutubeextractor.YTExtractor
import com.maxrave.kotlinyoutubeextractor.bestQuality
import com.maxrave.kotlinyoutubeextractor.getAudioOnly
import com.maxrave.kotlinyoutubeextractor.getVideoOnly
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val tv = findViewById<TextView>(R.id.textView)
        GlobalScope.launch {
            var a = ""
            YTExtractor(this@MainActivity).getYtFile("lO816281lJQ").let { it ->
                a = it?.get(22).let { data ->
                    data?.url.toString()
                }
                Log.d("Test get Stream URL", "Itag 22 URL: $a")
                Log.d("Test Audio Filter", "Audio Only: ${it?.getAudioOnly()}")
                Log.d("Test video filter", "Video Only: ${it?.getVideoOnly()}")

                Log.d("Test get best quality of Audio", "Best Quality: ${it?.getAudioOnly()?.bestQuality()}")



                withContext(Dispatchers.Main){
                    tv.text = it.toString()
                }
            }
        }
    }
}