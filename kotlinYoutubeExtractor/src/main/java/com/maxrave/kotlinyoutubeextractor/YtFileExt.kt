package com.maxrave.kotlinyoutubeextractor

import android.util.SparseArray
import androidx.core.util.forEach

/**
 * @return a list (ArrayList) of audio only YtFile objects
 */
fun SparseArray<YtFile>.getAudioOnly(): ArrayList<YtFile> {
    val resultList: ArrayList<YtFile> = ArrayList()
    val listAudioItag = listOf<Int>(171,249,250,251)
    for (itag in listAudioItag) {
        if (this[itag] != null) {
            resultList.add(this[itag])
        }
    }
    return resultList
}
/**
 * @return a list (ArrayList) of video YtFile objects
 */
fun SparseArray<YtFile>.getVideoOnly(): ArrayList<YtFile> {
    val resultList: ArrayList<YtFile> = ArrayList()
    val listVideoItag = listOf<Int>(18,22,37,38,82,83,84,85,133,134,135,136,137,138,160,242,243,244,247,248,264,266,271,272,278,298,299,302,303,308,313,315,330,331,332,333,334,335,336,337,394,395,396,397,398,399,400,401,402,403,404,405,406,407,408,409,410)
    for (itag in listVideoItag) {
        if (this[itag] != null) {
            resultList.add(this[itag])
        }
    }
    return resultList
}
/**
 * Convert SparseArray to ArrayList
 * @return a list (ArrayList) of video and audio YtFile objects
 */
fun <T> SparseArray<T>.values(): ArrayList<T> {
    val list = ArrayList<T>()
    forEach { _, value ->
        list.add(value)
    }
    return list
}
fun ArrayList<YtFile>.bestQuality(): YtFile?{
    return this.maxByOrNull { it.meta!!.audioBitrate }
}