# Kotlin YouTube Extractor
## Android Library for extract YouTube Stream URL

[![Release](https://jitpack.io/v/maxrave-dev/kotlin-youtubeExtractor.svg)](https://jitpack.io/#maxrave-dev/kotlin-youtubeExtractor) ![Downloads](https://jitpack.io/v/maxrave-dev/kotlin-youtubeExtractor/month.svg)


A lightweight Android (Kotlin) library for extract YouTube streaming URL. Port from [HaarigerHarald/android-youtubeExtractor (Java)](https://github.com/HaarigerHarald/android-youtubeExtractor) to Kotlin.

## Features

- Get YouTube stream Url with all format and itag
- Get YouTube video metadata
- Using Kotlin Coroutines for best performance
## Using in your project
### Gradle

Add Jitpack.io to your setting.gradle file:

```kotlin
pluginManagement {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```

Then, add dependencies in app level build.gradle:

```kotlin
dependencies {
    implementation 'com.github.maxrave-dev:kotlin-youtubeExtractor:0.0.4'
}
```

## How to use

### Using with Kotlin Coroutines
>From version 0.0.4, this library change extract method and add more feature method.

Before start please add Kotlin Coroutines to your project

Call YTExtractor inside your activity or fragment
```kotlin
//If your YouTube link is "https://www.youtube.com/watch?v=IDwytT0wFRM" so this videoId is "IDwytT0wFRM"
var videoId = "IDwytT0wFRM"
val yt = YTExtractor(context)
var ytFiles: SparseArray<YtFile>? = null
var videoMeta: VideoMeta? = null
GlobalScope.launch {
            yt.extract(videoId)
            //Before get YtFile or VideoMeta, you need to check state of yt object
            if (yt.state == State.SUCCESS) {
                ytFiles = yt.getYTFiles()
                videoMeta = yt.getVideoMeta()
            }
        }
```
In above case, ytFiles is a map of available media files for one YouTube video, accessible by their itag value (in above code "251" is a audio itag) and videoMeta is an object that contains all metadata of this YouTube video (thumbnail, title, author, views, etc.).

#### YtFile
After use getYTFiles() function, you will get SparseArray<YtFile> object. This object contains all available media files for one YouTube video, accessible by their itag value. You can get YtFile object by itag value like this:
```kotlin
var ytFiles = yt.getYTFiles()
var ytFile = ytFiles.get(251) // 251 is itag of audio
//Get stream URL
var streamUrl = ytFile?.url
```
#### VideoMeta
After use getVideoMeta() function, you will get VideoMeta object. This object contains all metadata of this YouTube video (thumbnail, title, author, views, etc.). You can get all metadata like this:
```kotlin
var videoMeta = yt.getVideoMeta()
    //title
    var title = videoMeta?.title
    //author
    var author = videoMeta?.author
    //view count
    var views = videoMeta?.viewCount
    //video length in second
    var videoLength = videoMeta?.videoLength
    //video channel id
    var videoChannelId = videoMeta?.channelId

//Get thumbnail
    // Default resolution
    val thumbUrl = videoMeta?.thumbUrl
    // 320 x 180
    val mqImageUrl = videoMeta?.mqImageUrl
    // 480 x 360
    val hqImageUrl = videoMeta?.hqImageUrl
    // 640 x 480
    val sdImageUrl = videoMeta?.sdImageUrl
    // Max Res
    val maxResImageUrl = videoMeta?.maxResImageUrl
```
#### Filter
To get list of only video YtFile or only audio, you can call this function
```kotlin
    val ytFiles = yt.getYtFile()
    val videoYtFiles = ytFiles.getAudioOnly() // Return ArrayList<YtFile> of only video
    val audioYtFiles = ytFiles.getVideoOnly() // Return ArrayList<YtFile> of only audio
```
To get best quality of video or audio, you can call this function
```kotlin
    val ytFiles = yt.getYtFile()
    val videoYtFiles = ytFiles.getAudioOnly()?.bestQuality() // Return best quality video
    val audioYtFiles = ytFiles.getVideoOnly()?.bestQuality() // Return best quality audio
```

## Not working?
If you have any problem with this library, please create an issue.
In my case:
- Can't get 1080p video with audio, you need to merge video and audio by yourself

## List Itag and Format in this library:

| Itag | Format | Extension | Resolution | Video Codec | Audio Codec | Bitrate | Dash | HLS Live Stream | AUDIO ONLY |
|------|--------|-----------|------------|-------------|-------------|---------|------|----------------|------------|
| 17   | 3gp    | 144       |            | MPEG4       | AAC         | 24      |      |                |            |
| 36   | 3gp    | 240       |            | MPEG4       | AAC         | 32      |      |                |            |
| 5    | flv    | 240       |            | H263        | MP3         | 64      |      |                |            |
| 43   | webm   | 360       |            | VP8         | VORBIS      | 128     |      |                |            |
| 18   | mp4    | 360       |            | H264        | AAC         | 96      |      |                |            |
| 22   | mp4    | 720       |            | H264        | AAC         | 192     |      |                |            |
| 160  | mp4    | 144       |            | H264        |             |         | true |                |            |
| 133  | mp4    | 240       |            | H264        |             |         | true |                |            |
| 134  | mp4    | 360       |            | H264        |             |         | true |                |            |
| 135  | mp4    | 480       |            | H264        |             |         | true |                |            |
| 136  | mp4    | 720       |            | H264        |             |         | true |                |            |
| 137  | mp4    | 1080      |            | H264        |             |         | true |                |            |
| 264  | mp4    | 1440      |            | H264        |             |         | true |                |            |
| 266  | mp4    | 2160      |            | H264        |             |         | true |                |            |
| 298  | mp4    | 720       |            | H264        |             | 60      | true |                |            |
| 299  | mp4    | 1080      |            | H264        |             | 60      | true |                |            |
| 140  | m4a    |           |            |             | AAC         | 128     | true |                | true       |
| 141  | m4a    |           |            |             | AAC         | 256     | true |                | true       |
| 256  | m4a    |           |            |             | AAC         | 192     | true |                | true       |
| 258  | m4a    |           |            |             | AAC         | 384     | true |                | true       |
| 278  | webm   | 144       |            | VP9         |             |         | true |                |            |
| 242  | webm   | 240       |            | VP9         |             |         | true |                |            |
| 243  | webm   | 360       |            | VP9         |             |         | true |                |            |
| 244  | webm   | 480       |            | VP9         |             |         | true |                |            |
| 247  | webm   | 720       |            | VP9         |             |         | true |                |            |
| 248  | webm   | 1080      |            | VP9         |             |         | true |                |            |
| 271  | webm   | 1440      |            | VP9         |             |         | true |                |            |
| 313  | webm   | 2160      |            | VP9         |             |         | true |                |            |
| 302  | webm   | 720       |            | VP9         |             | 60      | true |                |            |
| 308  | webm   | 1440      |            | VP9         |             | 60      | true |                |            |
| 303  | webm   | 1080      |            | VP9         |             | 60      | true |                |            |
| 315  | webm   | 2160      |            | VP9         |             | 60      | true |                |            |
| 171  | webm   |           |            |             | VORBIS      | 128     | true |                | true       |
| 249  | webm   |           |            |             | OPUS        | 48      | true |                | true       |
| 250  | webm   |           |            |             | OPUS        | 64      | true |                | true       |
| 251  | webm   |           |            |             | OPUS        | 160     | true |                | true       |
| 91   | mp4    | 144       |            | H264        | AAC         | 48      |      | true           | true       |
| 92   | mp4    | 240       |            | H264        | AAC         | 48      |      | true           | true       |
| 93   | mp4    | 360       |            | H264        | AAC         | 128     |      | true           | true       |
| 94   | mp4    | 480       |            | H264        | AAC         | 128     |      | true           | true       |
| 95   | mp4    | 720       |            | H264        | AAC         | 256     |      | true           | true       |
| 96   | mp4    | 1080      |            | H264        | AAC         | 256     |      | true           | true       |

I am trying to add more feature to this library. Keep in touch!!!

## Dependency

- [JsEvaluator](https://github.com/evgenyneu/js-evaluator-for-android) (v6.0.0) - Running Javascript inside Android app

## Donate
|   ![Paypal](https://upload.wikimedia.org/wikipedia/commons/archive/b/b5/20230314142950%21PayPal.svg)         | paypal.me/maxraveofficial |
|-------|---------|
