package com.maxrave.kotlinyoutubeextractor

class YtFile internal constructor(
    /**
     * Format data for the specific file.
     */
    val meta: Format?,
    /**
     * The url to download the file.
     */
    val url: String?
) {
    /**
     * Format data for the specific file.
     */

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val ytFile = other as YtFile
        if (if (meta != null) meta != ytFile.meta else ytFile.meta != null) return false
        return if (url != null) url == ytFile.url else ytFile.url == null
    }

    override fun hashCode(): Int {
        var result = meta?.hashCode() ?: 0
        result = 31 * result + (url?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "YtFile{" +
                "format=" + meta +
                ", url='" + url + '\'' +
                '}'
    }
}