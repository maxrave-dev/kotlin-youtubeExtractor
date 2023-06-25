package com.maxrave.exampleApp.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import com.maxrave.exampleApp.R
import com.maxrave.kotlinyoutubeextractor.State
import com.maxrave.kotlinyoutubeextractor.YTExtractor
import com.maxrave.kotlinyoutubeextractor.bestQuality
import com.maxrave.kotlinyoutubeextractor.getAudioOnly
import com.maxrave.kotlinyoutubeextractor.getVideoOnly
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //val videoId = "d40rzwlq8l4"
        val listVideoId = listOf<String>("d40rzwlq8l4", "Q2T8-q9fGSI", "UzvbmzVDCQ4", "aaMv6SJafPA")
        val tv = findViewById<TextView>(R.id.textView)
        val yt = YTExtractor(this@MainActivity)
        var text = ""
        GlobalScope.launch {
            listVideoId.forEach { videoId ->
                yt.extract(videoId)
                if (yt.state == State.SUCCESS) {
                    yt.getYTFiles().let { it ->
                        var a = it?.get(251).let { data ->
                            data?.url.toString()
                        }
                        text += a + "\n"
                        Log.d("Test get Stream URL", "Itag 22 URL: $a")
                        Log.d("Test Audio Filter", "Audio Only: ${it?.getAudioOnly()}")
                        Log.d("Test video filter", "Video Only: ${it?.getVideoOnly()}")

                        Log.d("Test get best quality of Audio", "Best Quality: ${it?.getAudioOnly()?.bestQuality()}")
                    }
//                    yt.getVideoMeta().let { meta ->
//                        Log.d("Test get Video Meta", "Video Meta: $meta")
//                        Log.d("Test get Video Meta", "Video Meta: ${meta?.maxResImageUrl}")
//                        Log.d("Test get Video Meta", "Video Meta: ${meta?.hqImageUrl}")
//                        Log.d("Test get Video Meta", "Video Meta: ${meta?.mqImageUrl}")
//                        Log.d("Test get Video Meta", "Video Meta: ${meta?.sdImageUrl}")
//                        Log.d("Test get Video Meta", "Video Meta: ${meta?.thumbUrl}")
//                    }
                }
            }
            withContext(Dispatchers.Main){
                tv.text = text
            }
        }
    }
}