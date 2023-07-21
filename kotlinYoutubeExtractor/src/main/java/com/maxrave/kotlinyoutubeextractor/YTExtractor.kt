package com.maxrave.kotlinyoutubeextractor

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.SparseArray
import com.evgenii.jsevaluator.JsEvaluator
import com.evgenii.jsevaluator.interfaces.JsCallback
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.lang.ref.WeakReference
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import java.util.regex.Pattern

/**
 * @author maxrave-dev
 * A lightweight Android (Kotlin) library for extract YouTube streaming URL.
 * @param con Context is required for caching the deciphering function.
 * @param CACHING Enable caching of the deciphering function, default is false. When you extract multiple links, caching is not recommended because Caching will cause HTTP 403 Error.
 * @param LOGGING Enable logging, default is false.
 */

class YTExtractor(val con: Context, val CACHING: Boolean = false, val LOGGING: Boolean = false, val retryCount: Int = 1) {
    private val LOG_TAG = "Kotlin YouTube Extractor"
    private val CACHE_FILE_NAME = "decipher_js_funct"

    var ytFiles: SparseArray<YtFile>? = null
    var state: State = State.INIT


    private var refContext: WeakReference<Context>? = null
    private var videoID: String? = null
    private var videoMeta: VideoMeta? = null
    private var cacheDirPath: String? = null

    @Volatile
    private var decipheredSignature: String? = null

    private var decipherJsFileName: String? = null
    private var decipherFunctions: String? = null
    private var decipherFunctionName: String? = null

    private val lock: Lock = ReentrantLock()
    private val jsExecuting = lock.newCondition()

    private val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/113.0.0.0 Safari/537.36"
    //Old User Agent = "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/97.0.4692.98 Safari/537.36"

    private val patPlayerResponse =
        Pattern.compile("var ytInitialPlayerResponse\\s*=\\s*(\\{.+?\\})\\s*;")
    private val patSigEncUrl = Pattern.compile("url=(.+?)(\\u0026|$)")
    private val patSignature = Pattern.compile("s=(.+?)(\\u0026|$)")

    private val patVariableFunction =
        Pattern.compile("([{; =])([a-zA-Z$][a-zA-Z0-9$]{0,2})\\.([a-zA-Z$][a-zA-Z0-9$]{0,2})\\(")
    private val patFunction = Pattern.compile("([{; =])([a-zA-Z\$_][a-zA-Z0-9$]{0,2})\\(") //check $

    private val patDecryptionJsFile = Pattern.compile("\\\\/s\\\\/player\\\\/([^\"]+?)\\.js")
    private val patDecryptionJsFileWithoutSlash = Pattern.compile("/s/player/([^\"]+?).js")
    private val patSignatureDecFunction =
        Pattern.compile("(?:\\b|[^a-zA-Z0-9$])([a-zA-Z0-9$]{1,4})\\s*=\\s*function\\(\\s*a\\s*\\)\\s*\\{\\s*a\\s*=\\s*a\\.split\\(\\s*\"\"\\s*\\)")

    private val FORMAT_MAP = SparseArray<Format>()

    init {
        refContext = WeakReference(con)
        cacheDirPath = con.cacheDir.absolutePath
        FORMAT_MAP.put(
            17,
            Format(17, "3gp", 144, Format.VCodec.MPEG4, Format.ACodec.AAC, 24, false)
        )
        FORMAT_MAP.put(
            36,
            Format(36, "3gp", 240, Format.VCodec.MPEG4, Format.ACodec.AAC, 32, false)
        )
        FORMAT_MAP.put(5, Format(5, "flv", 240, Format.VCodec.H263, Format.ACodec.MP3, 64, false))
        FORMAT_MAP.put(
            43,
            Format(43, "webm", 360, Format.VCodec.VP8, Format.ACodec.VORBIS, 128, false)
        )
        FORMAT_MAP.put(18, Format(18, "mp4", 360, Format.VCodec.H264, Format.ACodec.AAC, 96, false))
        FORMAT_MAP.put(
            22,
            Format(22, "mp4", 720, Format.VCodec.H264, Format.ACodec.AAC, 192, false)
        )


        // Dash Video (no audio)
        FORMAT_MAP.put(160, Format(160, "mp4", 144, Format.VCodec.H264, Format.ACodec.NONE, true))
        FORMAT_MAP.put(133, Format(133, "mp4", 240, Format.VCodec.H264, Format.ACodec.NONE, true))
        FORMAT_MAP.put(134, Format(134, "mp4", 360, Format.VCodec.H264, Format.ACodec.NONE, true))
        FORMAT_MAP.put(135, Format(135, "mp4", 480, Format.VCodec.H264, Format.ACodec.NONE, true))
        FORMAT_MAP.put(136, Format(136, "mp4", 720, Format.VCodec.H264, Format.ACodec.NONE, true))
        FORMAT_MAP.put(137, Format(137, "mp4", 1080, Format.VCodec.H264, Format.ACodec.NONE, true))
        FORMAT_MAP.put(264, Format(264, "mp4", 1440, Format.VCodec.H264, Format.ACodec.NONE, true))
        FORMAT_MAP.put(266, Format(266, "mp4", 2160, Format.VCodec.H264, Format.ACodec.NONE, true))

        FORMAT_MAP.put(
            298,
            Format(298, "mp4", 720, Format.VCodec.H264, 60, Format.ACodec.NONE, true)
        )
        FORMAT_MAP.put(
            299,
            Format(299, "mp4", 1080, Format.VCodec.H264, 60, Format.ACodec.NONE, true)
        )

        // Dash Audio

        // Dash Audio
        FORMAT_MAP.put(140, Format(140, "m4a", Format.VCodec.NONE, Format.ACodec.AAC, 128, true))
        FORMAT_MAP.put(141, Format(141, "m4a", Format.VCodec.NONE, Format.ACodec.AAC, 256, true))
        FORMAT_MAP.put(256, Format(256, "m4a", Format.VCodec.NONE, Format.ACodec.AAC, 192, true))
        FORMAT_MAP.put(258, Format(258, "m4a", Format.VCodec.NONE, Format.ACodec.AAC, 384, true))

        // WEBM Dash Video

        // WEBM Dash Video (no audio)
        FORMAT_MAP.put(278, Format(278, "webm", 144, Format.VCodec.VP9, Format.ACodec.NONE, true))
        FORMAT_MAP.put(242, Format(242, "webm", 240, Format.VCodec.VP9, Format.ACodec.NONE, true))
        FORMAT_MAP.put(243, Format(243, "webm", 360, Format.VCodec.VP9, Format.ACodec.NONE, true))
        FORMAT_MAP.put(244, Format(244, "webm", 480, Format.VCodec.VP9, Format.ACodec.NONE, true))
        FORMAT_MAP.put(247, Format(247, "webm", 720, Format.VCodec.VP9, Format.ACodec.NONE, true))
        FORMAT_MAP.put(248, Format(248, "webm", 1080, Format.VCodec.VP9, Format.ACodec.NONE, true))
        FORMAT_MAP.put(271, Format(271, "webm", 1440, Format.VCodec.VP9, Format.ACodec.NONE, true))
        FORMAT_MAP.put(313, Format(313, "webm", 2160, Format.VCodec.VP9, Format.ACodec.NONE, true))

        FORMAT_MAP.put(
            302,
            Format(302, "webm", 720, Format.VCodec.VP9, 60, Format.ACodec.NONE, true)
        )
        FORMAT_MAP.put(
            308,
            Format(308, "webm", 1440, Format.VCodec.VP9, 60, Format.ACodec.NONE, true)
        )
        FORMAT_MAP.put(
            303,
            Format(303, "webm", 1080, Format.VCodec.VP9, 60, Format.ACodec.NONE, true)
        )
        FORMAT_MAP.put(
            315,
            Format(315, "webm", 2160, Format.VCodec.VP9, 60, Format.ACodec.NONE, true)
        )

        // WEBM Dash Audio

        // WEBM Dash Audio
        FORMAT_MAP.put(
            171,
            Format(171, "webm", Format.VCodec.NONE, Format.ACodec.VORBIS, 128, true)
        )

        FORMAT_MAP.put(249, Format(249, "webm", Format.VCodec.NONE, Format.ACodec.OPUS, 48, true))
        FORMAT_MAP.put(250, Format(250, "webm", Format.VCodec.NONE, Format.ACodec.OPUS, 64, true))
        FORMAT_MAP.put(251, Format(251, "webm", Format.VCodec.NONE, Format.ACodec.OPUS, 160, true))

        // HLS Live Stream

        // HLS Live Stream
        FORMAT_MAP.put(
            91,
            Format(91, "mp4", 144, Format.VCodec.H264, Format.ACodec.AAC, 48, false, true)
        )
        FORMAT_MAP.put(
            92,
            Format(92, "mp4", 240, Format.VCodec.H264, Format.ACodec.AAC, 48, false, true)
        )
        FORMAT_MAP.put(
            93,
            Format(93, "mp4", 360, Format.VCodec.H264, Format.ACodec.AAC, 128, false, true)
        )
        FORMAT_MAP.put(
            94,
            Format(94, "mp4", 480, Format.VCodec.H264, Format.ACodec.AAC, 128, false, true)
        )
        FORMAT_MAP.put(
            95,
            Format(95, "mp4", 720, Format.VCodec.H264, Format.ACodec.AAC, 256, false, true)
        )
        FORMAT_MAP.put(
            96,
            Format(96, "mp4", 1080, Format.VCodec.H264, Format.ACodec.AAC, 256, false, true)
        )
    }


    @Throws(
        IOException::class,
        InterruptedException::class,
        JSONException::class
    )
    private fun getStreamUrls(): SparseArray<YtFile>? {
        val pageHtml: String
        val encSignatures = SparseArray<String>()
        val ytFiles = SparseArray<YtFile>()
        var reader: BufferedReader? = null
        var urlConnection: HttpURLConnection? = null
        val getUrl = URL("https://youtube.com/watch?v=$videoID")
        try {
            urlConnection = getUrl.openConnection() as HttpURLConnection
            urlConnection.setRequestProperty("User-Agent", USER_AGENT)
            reader = BufferedReader(InputStreamReader(urlConnection.inputStream))
            val sbPageHtml = java.lang.StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                sbPageHtml.append(line)
            }
            pageHtml = sbPageHtml.toString()
        } finally {
            reader?.close()
            urlConnection?.disconnect()
        }
        var mat = patPlayerResponse.matcher(pageHtml)
        if (mat.find()) {
            val ytPlayerResponse = mat.group(1)?.let { JSONObject(it) }
            val streamingData = ytPlayerResponse?.getJSONObject("streamingData")
            val formats = streamingData?.getJSONArray("formats")
            if (formats != null) {
                for (i in 0 until formats.length()) {
                    val format = formats.getJSONObject(i)

                    // FORMAT_STREAM_TYPE_OTF(otf=1) requires downloading the init fragment (adding
                    // `&sq=0` to the URL) and parsing emsg box to determine the number of fragment that
                    // would subsequently requested with (`&sq=N`) (cf. youtube-dl)
                    val type = format.optString("type")
                    if (type == "FORMAT_STREAM_TYPE_OTF") continue
                    val itag = format.getInt("itag")
                    if (FORMAT_MAP[itag] != null) {
                        if (format.has("url")) {
                            val url = format.getString("url").replace("\\u0026", "&")
                            ytFiles.append(itag, YtFile(FORMAT_MAP[itag], url))
                        } else if (format.has("signatureCipher")) {
                            mat =
                                patSigEncUrl.matcher(format.getString("signatureCipher"))
                            val matSig =
                                patSignature.matcher(format.getString("signatureCipher"))
                            if (mat.find() && matSig.find()) {
                                val url = URLDecoder.decode(mat.group(1), "UTF-8")
                                val signature = URLDecoder.decode(matSig.group(1), "UTF-8")
                                ytFiles.append(itag, YtFile(FORMAT_MAP[itag], url))
                                encSignatures.append(itag, signature)
                            }
                        }
                    }
                }
            }
            val adaptiveFormats = streamingData?.getJSONArray("adaptiveFormats")
            if (adaptiveFormats != null) {
                for (i in 0 until adaptiveFormats.length()) {
                    val adaptiveFormat = adaptiveFormats.getJSONObject(i)
                    val type = adaptiveFormat.optString("type")
                    if (type == "FORMAT_STREAM_TYPE_OTF") continue
                    val itag = adaptiveFormat.getInt("itag")
                    if (FORMAT_MAP[itag] != null) {
                        if (adaptiveFormat.has("url")) {
                            val url = adaptiveFormat.getString("url").replace("\\u0026", "&")
                            ytFiles.append(itag, YtFile(FORMAT_MAP[itag], url))
                        } else if (adaptiveFormat.has("signatureCipher")) {
                            mat =
                                patSigEncUrl.matcher(adaptiveFormat.getString("signatureCipher"))
                            val matSig =
                                patSignature.matcher(adaptiveFormat.getString("signatureCipher"))
                            if (mat.find() && matSig.find()) {
                                val url = URLDecoder.decode(mat.group(1), "UTF-8")
                                val signature = URLDecoder.decode(matSig.group(1), "UTF-8")
                                ytFiles.append(itag, YtFile(FORMAT_MAP[itag], url))
                                encSignatures.append(itag, signature)
                            }
                        }
                    }
                }
            }
            val videoDetails = ytPlayerResponse?.getJSONObject("videoDetails")
            if (videoDetails != null) {
                Log.d(LOG_TAG, "videoDetails: $videoDetails")
                videoMeta = VideoMeta(
                    videoDetails.getString("videoId"),
                    videoDetails.getString("title"),
                    videoDetails.getString("author"),
                    videoDetails.getString("channelId"),
                    videoDetails.getString("lengthSeconds").toLong(),
                    videoDetails.getString("viewCount").toLong(),
                    videoDetails.getBoolean("isLiveContent"),
                    videoDetails.getString("shortDescription")
                )
            }
        } else {
            Log.d(LOG_TAG, "ytPlayerResponse was not found")
        }
        if (encSignatures.size() > 0) {
            val curJsFileName: String
            if (CACHING
                && (decipherJsFileName == null || decipherFunctions == null || decipherFunctionName == null)
            ) {
                readDecipherFunctFromCache()
            }
            mat = patDecryptionJsFile.matcher(pageHtml)
            if (!mat.find()) mat =
                patDecryptionJsFileWithoutSlash.matcher(pageHtml)
            if (mat.find()) {
                curJsFileName = mat.group(0)!!.replace("\\/", "/")
                if (decipherJsFileName == null || decipherJsFileName != curJsFileName) {
                    decipherFunctions = null
                    decipherFunctionName = null
                }
                decipherJsFileName = curJsFileName
            }
            if (LOGGING) Log.d(
                LOG_TAG,
                "Decipher signatures: " + encSignatures.size() + ", videos: " + ytFiles.size()
            )
            decipheredSignature = null
            if (decipherSignature(encSignatures)) {
                lock.lock()
                try {
                    jsExecuting.await(7, TimeUnit.SECONDS)
                } finally {
                    lock.unlock()
                }
            }
            val signature: String? = decipheredSignature
            if (signature == null) {
                return null
            } else {
                val sigs = signature.split("\n".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()
                var i = 0
                while (i < encSignatures.size() && i < sigs.size) {
                    val key = encSignatures.keyAt(i)
                    var url = ytFiles[key].url
                    url += "&sig=" + sigs[i]
                    val newFile = YtFile(FORMAT_MAP[key], url)
                    ytFiles.put(key, newFile)
                    i++
                }
            }
        }
        if (ytFiles.size() == 0) {
            if (LOGGING) Log.d(LOG_TAG, pageHtml)
            return null
        }
        return ytFiles
    }

    private fun testHttp403Code(url: String?): Boolean {
        var urlConnection: HttpURLConnection? = null
        try {
            val urlObj = URL(url)
            urlConnection = urlObj.openConnection() as HttpURLConnection
            urlConnection.setRequestProperty("User-Agent", USER_AGENT)
            urlConnection.connect()
            val responseCode = urlConnection.responseCode
            if (responseCode == 403) {
                return true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            urlConnection?.disconnect()
        }
        return false
    }

    @Throws(IOException::class)
    private fun decipherSignature(encSignatures: SparseArray<String>): Boolean {
        // Assume the functions don't change that much
        if (decipherFunctionName == null || decipherFunctions == null) {
            val decipherFunctUrl = "https://youtube.com$decipherJsFileName"
            var reader: BufferedReader? = null
            val javascriptFile: String
            val url = URL(decipherFunctUrl)
            val urlConnection = url.openConnection() as HttpURLConnection
            urlConnection.setRequestProperty("User-Agent", USER_AGENT)
            try {
                reader = BufferedReader(InputStreamReader(urlConnection.inputStream))
                val sb = java.lang.StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    sb.append(line)
                    sb.append(" ")
                }
                javascriptFile = sb.toString()
            } finally {
                reader?.close()
                urlConnection.disconnect()
            }
            if (LOGGING) Log.d(
                LOG_TAG,
                "Decipher FunctURL: $decipherFunctUrl"
            )
            var mat = patSignatureDecFunction.matcher(javascriptFile)
            if (mat.find()) {
                decipherFunctionName = mat.group(1)
                if (LOGGING) Log.d(
                    LOG_TAG,
                    "Decipher Functname: $decipherFunctionName"
                )
                val patMainVariable = Pattern.compile(
                    "(var |\\s|,|;)" + decipherFunctionName?.replace("$", "\\$") +
                            "(=function\\((.{1,3})\\)\\{)"
                )
                var mainDecipherFunct: String
                mat = patMainVariable.matcher(javascriptFile)
                if (mat.find()) {
                    mainDecipherFunct =
                        "var " + decipherFunctionName + mat.group(2)
                } else {
                    val patMainFunction = Pattern.compile(
                        ("function " + decipherFunctionName?.replace("$", "\\$") +
                                "(\\((.{1,3})\\)\\{)")
                    )
                    mat = patMainFunction.matcher(javascriptFile)
                    if (!mat.find()) return false
                    mainDecipherFunct =
                        "function " + decipherFunctionName + mat.group(2)
                }
                var startIndex = mat.end()
                var braces = 1
                var i = startIndex
                while (i < javascriptFile.length) {
                    if (braces == 0 && startIndex + 5 < i) {
                        mainDecipherFunct += javascriptFile.substring(startIndex, i) + ";"
                        break
                    }
                    if (javascriptFile[i] == '{') braces++ else if (javascriptFile[i] == '}') braces--
                    i++
                }
                decipherFunctions = mainDecipherFunct
                // Search the main function for extra functions and variables
                // needed for deciphering
                // Search for variables
                mat = patVariableFunction.matcher(mainDecipherFunct)
                while (mat.find()) {
                    val variableDef = "var " + mat.group(2) + "={"
                    if (decipherFunctions!!.contains(variableDef)) {
                        continue
                    }
                    startIndex = javascriptFile.indexOf(variableDef) + variableDef.length
                    var bracesVariable = 1
                    var i1 = startIndex
                    while (i1 < javascriptFile.length) {
                        if (bracesVariable == 0) {
                            decipherFunctions += variableDef + javascriptFile.substring(
                                startIndex,
                                i1
                            ) + ";"
                            break
                        }
                        if (javascriptFile[i1] == '{') bracesVariable++ else if (javascriptFile[i1] == '}') bracesVariable--
                        i1++
                    }
                }
                // Search for functions
                mat = patFunction.matcher(mainDecipherFunct)
                while (mat.find()) {
                    val functionDef = "function " + mat.group(2) + "("
                    if (decipherFunctions?.contains(functionDef)!!) {
                        continue
                    }
                    startIndex = javascriptFile.indexOf(functionDef) + functionDef.length
                    var bracesFunction = 0
                    var i2 = startIndex
                    while (i2 < javascriptFile.length) {
                        if (bracesFunction == 0 && startIndex + 5 < i2) {
                            decipherFunctions += functionDef + javascriptFile.substring(
                                startIndex,
                                i2
                            ) + ";"
                            break
                        }
                        if (javascriptFile[i2] == '{') bracesFunction++ else if (javascriptFile[i2] == '}') bracesFunction--
                        i2++
                    }
                }
                if (LOGGING) Log.d(
                    LOG_TAG,
                    "Decipher Function: $decipherFunctions"
                )
                decipherViaWebView(encSignatures)
                if (CACHING) {
                    writeDeciperFunctToChache()
                }
            } else {
                return false
            }
        } else {
            decipherViaWebView(encSignatures)
        }
        return true
    }

    private fun readDecipherFunctFromCache() {
        val cacheFile = File("$cacheDirPath/$CACHE_FILE_NAME")
        // The cached functions are valid for 2 weeks
        if (cacheFile.exists() && System.currentTimeMillis() - cacheFile.lastModified() < 1209600000) {
            var reader: BufferedReader? = null
            try {
                reader = BufferedReader(InputStreamReader(FileInputStream(cacheFile), "UTF-8"))
                decipherJsFileName = reader.readLine()
                decipherFunctionName = reader.readLine()
                decipherFunctions = reader.readLine()
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            } finally {
                if (reader != null) {
                    try {
                        reader.close()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private fun decipherViaWebView(encSignatures: SparseArray<String>) {
        val context = refContext!!.get() ?: return
        val stb = StringBuilder("$decipherFunctions function decipher(")
        stb.append("){return ")
        for (i in 0 until encSignatures.size()) {
            val key = encSignatures.keyAt(i)
            if (i < encSignatures.size() - 1) stb.append(decipherFunctionName)
                .append("('").append(
                    encSignatures[key]
                ).append("')+\"\\n\"+") else stb.append(decipherFunctionName)
                .append("('").append(
                    encSignatures[key]
                ).append("')")
        }
        stb.append("};decipher();")
        Handler(Looper.getMainLooper()).post {
            JsEvaluator(context).evaluate(stb.toString(), object : JsCallback {
                override fun onResult(result: String) {
                    lock.lock()
                    try {
                        decipheredSignature = result
                        jsExecuting.signal()
                    } finally {
                        lock.unlock()
                    }
                }

                override fun onError(errorMessage: String) {
                    lock.lock()
                    try {
                        if (LOGGING) Log.e(
                            LOG_TAG,
                            errorMessage
                        )
                        jsExecuting.signal()
                    } finally {
                        lock.unlock()
                    }
                }
            })
        }
    }

    private fun writeDeciperFunctToChache() {
        val cacheFile = File("$cacheDirPath/$CACHE_FILE_NAME")
        var writer: BufferedWriter? = null
        try {
            writer = BufferedWriter(OutputStreamWriter(FileOutputStream(cacheFile), "UTF-8"))
            writer.write(
                """
                $decipherJsFileName
                
                """.trimIndent()
            )
            writer.write(
                """
                $decipherFunctionName
                
                """.trimIndent()
            )
            writer.write(decipherFunctions)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            if (writer != null) {
                try {
                    writer.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }

    /**
     * Extract data from YouTube videoId
     * Using Kotlin Coroutines to call this function
     * @param videoId YouTube videoId
     */
    @OptIn(DelicateCoroutinesApi::class)
    suspend fun extract(videoId: String) {
        withContext(Dispatchers.IO) {
            ytFiles = async {
                state = State.LOADING
                var retry = 0
                while (state != State.SUCCESS && retry < retryCount) {
                    if (LOGGING) Log.d(
                        LOG_TAG,
                        "Retry: $retry"
                    )
                    videoID = videoId
                    try {
                        val temp = getStreamUrls()
                        try {
                            if (temp != null) {
                                val test = testHttp403Code(temp.getAudioOnly().bestQuality()?.url)
                                if (!test) {
                                    if (LOGGING) Log.d(
                                        LOG_TAG,
                                        "NO Error"
                                    )
                                    state = State.SUCCESS
                                    return@async temp
                                }
                                else {
                                    retry++
                                    state = State.ERROR
                                    Log.e(LOG_TAG, "Extraction failed cause 403 HTTP Error")
                                }
                            }
                        }
                        catch (e: IOException){
                            retry++
                            state = State.ERROR
                            Log.e(LOG_TAG, "Extraction failed cause 403 HTTP Error", e)
                        }
                    } catch (e: java.lang.Exception) {
                        retry++
                        state = State.ERROR
                        Log.e(LOG_TAG, "Extraction failed", e)
                    }
                }
                return@async null
            }.await()
        }
    }

    /**
     * After extract, you can get the video meta data if state is SUCCESS
     * Please check the state before call this function
     * @return videoMeta YouTube Video Metadata
     */
    fun getVideoMeta(): VideoMeta? {
        return videoMeta
    }
    /**
     * After extract, you can get the video stream URL data if state is SUCCESS
     * Please check the state before call this function
     * To get stream URL, use [.get(itag)]
     * @return ytFiles SparseArray of YtFile
     */
    fun getYTFiles(): SparseArray<YtFile>? {
        return ytFiles
    }
}


enum class State {
    SUCCESS,
    ERROR,
    LOADING,
    INIT,
}