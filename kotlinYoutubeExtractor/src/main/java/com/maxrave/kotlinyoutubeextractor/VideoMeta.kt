package com.maxrave.kotlinyoutubeextractor

/**
 * VideoMeta contains all the information available for a YouTube video such as title, author, thumbnail, view count, etc.
 */
class VideoMeta(
    val videoId: String?, val title: String?, val author: String?, val channelId: String?,
    /**
     * The video length in seconds.
     */
    val videoLength: Long,
    val viewCount: Long, val isLiveStream: Boolean, val shortDescription: String?
) {

    // 120 x 90
    val thumbUrl: String
        get() = IMAGE_BASE_URL + videoId + "/default.jpg"

    // 320 x 180
    val mqImageUrl: String
        get() = IMAGE_BASE_URL + videoId + "/mqdefault.jpg"

    // 480 x 360
    val hqImageUrl: String
        get() = IMAGE_BASE_URL + videoId + "/hqdefault.jpg"

    // 640 x 480
    val sdImageUrl: String
        get() = IMAGE_BASE_URL + videoId + "/sddefault.jpg"

    // Max Res
    val maxResImageUrl: String
        get() = IMAGE_BASE_URL + videoId + "/maxresdefault.jpg"

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val videoMeta = o as VideoMeta
        if (videoLength != videoMeta.videoLength) return false
        if (viewCount != videoMeta.viewCount) return false
        if (isLiveStream != videoMeta.isLiveStream) return false
        if (if (videoId != null) videoId != videoMeta.videoId else videoMeta.videoId != null) return false
        if (if (title != null) title != videoMeta.title else videoMeta.title != null) return false
        if (if (author != null) author != videoMeta.author else videoMeta.author != null) return false
        return if (channelId != null) channelId == videoMeta.channelId else videoMeta.channelId == null
    }

    override fun hashCode(): Int {
        var result = videoId?.hashCode() ?: 0
        result = 31 * result + (title?.hashCode() ?: 0)
        result = 31 * result + (author?.hashCode() ?: 0)
        result = 31 * result + (shortDescription?.hashCode() ?: 0)
        result = 31 * result + (channelId?.hashCode() ?: 0)
        result = 31 * result + (videoLength xor (videoLength ushr 32)).toInt()
        result = 31 * result + (viewCount xor (viewCount ushr 32)).toInt()
        result = 31 * result + if (isLiveStream) 1 else 0
        return result
    }

    override fun toString(): String {
        return "VideoMeta{" +
                "videoId='" + videoId + '\'' +
                ", title='" + title + '\'' +
                ", author='" + author + '\'' +
                ", shortDescription='" + shortDescription + '\'' +
                ", channelId='" + channelId + '\'' +
                ", videoLength=" + videoLength +
                ", viewCount=" + viewCount +
                ", isLiveStream=" + isLiveStream +
                '}'
    }

    companion object {
        private const val IMAGE_BASE_URL = "http://i.ytimg.com/vi/"
    }
}