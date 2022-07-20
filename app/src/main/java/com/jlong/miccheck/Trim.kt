package com.jlong.miccheck

import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.arthenica.mobileffmpeg.Config
import com.arthenica.mobileffmpeg.ExecuteCallback
import com.arthenica.mobileffmpeg.FFmpeg
import kotlinx.coroutines.*
import java.io.File

enum class FFMPEGState {
    None, Running, Failed, Finished
}

var MainActivity.ffmpegExecId: Long?
    get() = null
    set(value) {
        value
    }

fun MainActivity.beginCrop(
    recording: Recording,
    startMilli: Long,
    endMilli: Long,
    title: String,
    description: String,
    tags: List<Tag>
) {
    val startInSec = startMilli / 1000f
    val endInSec = endMilli / 1000f

    //FFMPEG Execution
    val path = applicationContext.filesDir.absolutePath + "/$title.m4a"
    val command = "-i \"${recording.path}\" -vn -acodec copy -ss $startInSec -t $endInSec \"$path\""

    viewModel.ffmpegState = FFMPEGState.Running
    ffmpegExecId = FFmpeg.executeAsync(
        command
    ) { _, returnCode ->
        when (returnCode) {
            Config.RETURN_CODE_SUCCESS -> {
                viewModel.ffmpegState = FFMPEGState.Finished
                onCropFinished(recording, title, description, tags)
            }
            else -> {
                viewModel.ffmpegState = FFMPEGState.Failed
                File(path).also {
                    if (it.exists())
                        it.delete()
                }
                Toast.makeText(this, "Failed to crop recording.", Toast.LENGTH_LONG).show()
            }
        }
        lifecycleScope.launch {
            delay(3000)
            viewModel.ffmpegState = FFMPEGState.None
        }
    }
}

private fun MainActivity.onCropFinished(
    recording: Recording,
    title: String,
    description: String,
    tags: List<Tag>
) {
    // MediaStore Setup
    val values = ContentValues(4)
    values.put(MediaStore.Audio.Media.TITLE, title)
    values.put(MediaStore.Audio.Media.DISPLAY_NAME, title)
    values.put(
        MediaStore.Audio.Media.DATE_ADDED,
        (System.currentTimeMillis() / 1000).toInt()
    )
    values.put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp4")
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        values.put(MediaStore.Audio.Media.IS_PENDING, 1)
        values.put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/micCheck/")
    }

    val uri = applicationContext.contentResolver.insert(
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
        values
    ) ?: return

    //Transfer from temp local file
    val path = applicationContext.filesDir.absolutePath + "/$title.m4a"

    val localFile = File(path).inputStream()
    val mediaStoreFile = applicationContext.contentResolver.openOutputStream(uri) ?: return
    localFile.copyTo(mediaStoreFile)
    localFile.close()
    mediaStoreFile.close()

    File(path).also {
        if (it.exists())
            it.delete()
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        applicationContext.contentResolver.update(
            uri,
            ContentValues().also {
                it.put(MediaStore.Audio.Media.IS_PENDING, 0)
            },
            null, null
        )
    }

    viewModel.recordingsData.add(
        RecordingData(
            recordingUri = uri.toString(),
            description = description,
            clipParentUri = recording.uri.toString()
        ).also {
            viewModel.addTagsToRecording(
                it,
                tags
            )
        }
    )
    lifecycleScope.launch {
        withContext(Dispatchers.IO) {
            discoverAudioFiles()
        }
    }

}

fun MainActivity.beginShareAsVideo (
    recording: Recording,
    visuals: Uri,
    loopVideo: Boolean,
    isImage: Boolean
) {

    val loopString = if (loopVideo)
        "-stream_loop -1 " else ""

    val tempPath = applicationContext.filesDir.absolutePath + "/$title.mp4"
    val command = if (!isImage)
        "$loopString-i \"${visuals.path}\" -i \"${recording.path}\" -y -c:v copy -c:a aac -map 0:v:0 -map 1:a:0 -shortest \"$tempPath\""
    else
        "-r 1 -loop 1 -y -i \"${visuals.path}\" -i \"${recording.path}\" -r 1 -shortest \"$tempPath\""

    viewModel.ffmpegState = FFMPEGState.Running
    ffmpegExecId = FFmpeg.executeAsync(
        command
    ) { _, returnCode ->
        when (returnCode) {
            Config.RETURN_CODE_SUCCESS -> {
                viewModel.ffmpegState = FFMPEGState.Finished
                Log.w("Trim", "Export finished.")
                finishShareAsVideo(recording)
            }
            else -> {
                viewModel.ffmpegState = FFMPEGState.Failed
                File(tempPath).also {
                    if (it.exists())
                        it.delete()
                }
                Toast.makeText(
                    this,
                    "Failed to create video. Error $returnCode.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        lifecycleScope.launch {
            delay(3000)
            viewModel.ffmpegState = FFMPEGState.None
        }
    }
}

fun MainActivity.finishShareAsVideo(
    recording: Recording
) {
    val values = ContentValues(4)
    values.put(MediaStore.Video.Media.TITLE, recording.name)
    values.put(MediaStore.Video.Media.DISPLAY_NAME, recording.name)
    values.put(
        MediaStore.Video.Media.DATE_ADDED,
        (System.currentTimeMillis() / 1000).toInt()
    )
    values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        values.put(MediaStore.Video.Media.IS_PENDING, 1)
        values.put(MediaStore.Video.Media.RELATIVE_PATH, "Pictures/micCheck/")
    }

    val uri = applicationContext.contentResolver.insert(
        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
        values
    ) ?: return

    //Transfer from temp local file
    val path = applicationContext.filesDir.absolutePath + "/$title.mp4"

    val localFile = File(path).inputStream()
    val mediaStoreFile = applicationContext.contentResolver.openOutputStream(uri) ?: return
    localFile.copyTo(mediaStoreFile)
    localFile.close()
    mediaStoreFile.close()

    File(path).also {
        if (it.exists())
            it.delete()
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        applicationContext.contentResolver.update(
            uri,
            ContentValues().also {
                it.put(MediaStore.Video.Media.IS_PENDING, 0)
            },
            null, null
        )
    }

    Toast.makeText(this, "Video created! Check your gallery app.", Toast.LENGTH_LONG).show()
}