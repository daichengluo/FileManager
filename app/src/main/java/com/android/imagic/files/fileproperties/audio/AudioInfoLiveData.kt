package com.android.imagic.files.fileproperties.audio

import android.media.MediaMetadataRetriever
import android.os.AsyncTask
import android.os.Build
import com.android.imagic.files.compat.METADATA_KEY_SAMPLERATE
import com.android.imagic.files.fileproperties.PathObserverLiveData
import com.android.imagic.files.fileproperties.extractMetadataNotBlank
import com.android.imagic.files.util.Failure
import com.android.imagic.files.util.Loading
import com.android.imagic.files.util.Stateful
import com.android.imagic.files.util.Success
import com.android.imagic.files.util.setDataSource
import com.android.imagic.files.util.valueCompat
import java8.nio.file.Path
import java.time.Duration

class AudioInfoLiveData(path: Path) : PathObserverLiveData<Stateful<AudioInfo>>(path) {
    init {
        loadValue()
        observe()
    }

    override fun loadValue() {
        value = Loading(value?.value)
        AsyncTask.THREAD_POOL_EXECUTOR.execute {
            val value = try {
                val audioInfo = MediaMetadataRetriever().use { retriever ->
                    retriever.setDataSource(path)
                    val title = retriever.extractMetadataNotBlank(
                        MediaMetadataRetriever.METADATA_KEY_TITLE
                    )
                    val artist = retriever.extractMetadataNotBlank(
                        MediaMetadataRetriever.METADATA_KEY_ARTIST
                    )
                    val album = retriever.extractMetadataNotBlank(
                        MediaMetadataRetriever.METADATA_KEY_ALBUM
                    )
                    val albumArtist = retriever.extractMetadataNotBlank(
                        MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST
                    )
                    val composer = retriever.extractMetadataNotBlank(
                        MediaMetadataRetriever.METADATA_KEY_COMPOSER
                    )
                    val discNumber = retriever.extractMetadataNotBlank(
                        MediaMetadataRetriever.METADATA_KEY_DISC_NUMBER
                    )
                    val trackNumber = retriever.extractMetadataNotBlank(
                        MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER
                    )
                    val year = retriever.extractMetadataNotBlank(
                        MediaMetadataRetriever.METADATA_KEY_YEAR
                    )
                    val genre = retriever.extractMetadataNotBlank(
                        MediaMetadataRetriever.METADATA_KEY_GENRE
                    )
                    val duration = retriever.extractMetadataNotBlank(
                        MediaMetadataRetriever.METADATA_KEY_DURATION
                    )?.toLongOrNull()?.let { Duration.ofMillis(it) }
                    val bitRate = retriever.extractMetadataNotBlank(
                        MediaMetadataRetriever.METADATA_KEY_BITRATE
                    )?.toIntOrNull()
                    val sampleRate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        retriever.extractMetadataNotBlank(
                            MediaMetadataRetriever::class.METADATA_KEY_SAMPLERATE
                        )?.toIntOrNull()
                    } else {
                        null
                    }
                    AudioInfo(
                        title, artist, album, albumArtist, composer, discNumber, trackNumber, year,
                        genre, duration, bitRate, sampleRate
                    )
                }
                Success(audioInfo)
            } catch (e: Exception) {
                Failure(valueCompat.value, e)
            }
            postValue(value)
        }
    }
}
