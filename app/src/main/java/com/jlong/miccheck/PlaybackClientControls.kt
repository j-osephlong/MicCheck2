package com.jlong.miccheck

import android.app.RecoverableSecurityException
import android.content.ContentUris
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.annotation.RequiresApi
import androidx.compose.runtime.mutableStateListOf
import java.io.File
import java.lang.Float.min

class PlaybackClientControls (
    val context: MainActivity
) {
    fun openPermissions () {
        val i = Intent()
        i.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        i.addCategory(Intent.CATEGORY_DEFAULT)
        i.data = Uri.parse("package:" + context.packageName)
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
        i.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        context.startActivity(i)
        Toast.makeText(context, "Open permissions and enable any disabled items. You may need to restart the app after.", Toast.LENGTH_LONG).show()
        context.viewModel.deniedPermissions = mutableStateListOf()
    }

    fun play() {
        if ((context.viewModel.currentPlaybackState == PlaybackStateCompat.STATE_PAUSED ||
                    context.viewModel.currentPlaybackState == PlaybackStateCompat.STATE_STOPPED) &&
            context.viewModel.currentPlaybackRec != null)
            context.mediaController.transportControls.playFromUri(
                context.viewModel.currentPlaybackRec!!.first.uri,
                Pair(
                    context.viewModel.currentPlaybackRec!!.first,
                    context.viewModel.currentPlaybackRec!!.third
                ).toMetaData().apply {
                    if (context.viewModel.isGroupPlayback)
                        putBoolean("isOfPlaybackList", true)
                }
            )
        context.viewModel
    }

    fun play(recording: Triple<Recording, RecordingData, RecordingGroup?>) {
        Log.i("PlaybackClientControls", "Playing from recording.")

        context.viewModel.isGroupPlayback = false
//
//            context.viewModel.setCurrentPlayback(recording)
        context.mediaController.transportControls.playFromUri(
            recording.first.uri,
            Pair(recording.first, recording.third).toMetaData()
        )
    }

    fun play(group: RecordingGroup, index: Int) {
//        TODO("Not yet implemented - Waiting for groups support")

        val recordings = group.recordings.map { uriStr ->
            context.viewModel.recordings.find { it.uri.toString() == uriStr } ?: return
        }

        if (recordings.isNotEmpty()) {
            val bundleList = arrayListOf<Bundle>()
            recordings.forEach {
                bundleList += Pair(it, group).toMetaData().apply {
                    putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, it.uri.toString())
                    putBoolean("isOfPlaybackList", true)
                }
            }

            context.viewModel.isGroupPlayback = true

            context.mediaController.transportControls.playFromUri(
                recordings[0].uri,
                Bundle().apply {
                    putParcelableArrayList("playbackList", bundleList)
                    putInt("listIndex", index)
                }
            )
        }
    }

    fun playFromTimestamp(
        recording: Triple<Recording, RecordingData, RecordingGroup?>,
        timeStamp: TimeStamp
    ) {
        context.viewModel.setCurrentPlayback(recording)

        play(recording)
        if (recording.first.duration > 0)
            seek(
                timeStamp.timeMilli / recording.first.duration.toFloat()
            )
    }

    fun pause() {
        context.mediaController.transportControls.pause()
    }

    fun seek(percent: Float) {
        context.mediaController.transportControls.seekTo(
            (percent * (context.viewModel.currentPlaybackRec?.first?.duration ?: 0)).toLong()
        )
    }

    fun seekDiff(difference: Long) {
        context.mediaController.transportControls.seekTo(
            context.viewModel.playbackProgress + difference
        )
    }

    fun skipTrackNext() {
//        TODO("Not yet implemented - Waiting for groups support")
        context.mediaController.transportControls.skipToNext()
    }

    fun skipTrackPrevious() {
//        TODO("Not yet implemented - Waiting for groups support")
        context.mediaController.transportControls.skipToPrevious()
    }

    fun enableLoopPlayback() {
        context.mediaController.transportControls.sendCustomAction(
            "setLoopMode", Bundle().apply {
                putBoolean("val", true)
            }
        )

        context.viewModel.loopMode = true
    }

    fun disableLoopPlayback() {
        context.mediaController.transportControls.sendCustomAction(
            "setLoopMode", Bundle().apply {
                putBoolean("val", false)
            }
        )

        context.viewModel.loopMode = false
    }

    fun setLoopSelection(startPercent: Float, endPercent: Float) {
        val safetyMin = 500L * context.viewModel.playbackSpeed

        val startMilli = context.viewModel.currentPlaybackRec?.let { (it.first.duration*startPercent).toLong() } ?: return
        val endMilli = context.viewModel.currentPlaybackRec?.let { (it.first.duration*endPercent).toLong() } ?: return
        Log.i("LOOP", "startMilli $startMilli endMilli $endMilli")
        var _startPercent = startPercent
        var _endPercent = endPercent
        if (endMilli-startMilli < safetyMin) {
            if (safetyMin - (endMilli - startMilli) + endMilli <= (context.viewModel.currentPlaybackRec?.first?.duration?.toLong()
                    ?: return)) {
                _endPercent = (safetyMin - (endMilli - startMilli) + endMilli)/(context.viewModel.currentPlaybackRec?.first?.duration?.toFloat()
                    ?: return)
                Log.e("LOOP", "COND 1")
            } else {
                _endPercent = 1f
                _startPercent = java.lang.Float.max(
                    (startMilli - (safetyMin - ((context.viewModel.currentPlaybackRec?.first?.duration?.toLong()
                        ?: return) - startMilli))).toFloat() /
                            (context.viewModel.currentPlaybackRec?.first?.duration?.toFloat()
                                ?: return),
                    1f
                )
            }
        }

        context.mediaController.transportControls.sendCustomAction(
            "setRange", Bundle().apply {
                putFloat("startRange", _startPercent)
                putFloat("endRange", _endPercent)
            }
        )

        context.viewModel.loopRange = _startPercent.._endPercent
    }

    fun deleteRecording(recording: Recording) {
        if (recording == context.viewModel.currentPlaybackRec?.first) {
            context.mediaController.transportControls.stop()
        }

        try {
            context.contentResolver.delete(
                recording.uri, null, null
            )
            context.viewModel.onDeleteRecording(recording)
            context.discoverAudioFiles()
        } catch (securityException: RuntimeException) {
            if (Build.VERSION.SDK_INT >= 29 &&
                securityException is RecoverableSecurityException
            ) {
                val intentSender =
                    securityException.userAction.actionIntent.intentSender
                context.filePermissionRequestLauncher(IntentSenderRequest.Builder(intentSender).build()) {
                    context.contentResolver.delete(
                        recording.uri, null, null
                    )
                    context.viewModel.onDeleteRecording(recording)
                    context.discoverAudioFiles()
                }
            }
        }
    }

    fun deleteRecordings(recordings: List<Recording>) {
        val recording = recordings[0]
        Log.i("DELETE", recordings.toString())
        Log.i("DELETE", "${recordings.size}")


        try {
            if (recording == context.viewModel.currentPlaybackRec?.first) {
                context.mediaController.transportControls.stop()
            }
            Log.i("DELETE", "Deleting $recording")

            context.contentResolver.delete(
                recording.uri, null, null
            )
            context.viewModel.onDeleteRecording(recording)
            if (recordings.isNotEmpty())
                deleteRecordings(recordings)
        } catch (securityException: RuntimeException) {
            if (Build.VERSION.SDK_INT >= 29 &&
                securityException is RecoverableSecurityException
            ) {
                val intentSender =
                    securityException.userAction.actionIntent.intentSender
                Log.i("DELETE", "Requesting $recording")
                context.filePermissionRequestLauncher(IntentSenderRequest.Builder(intentSender).build()) {
                    Log.i("DELETE", "Deleting $recording (REQUESTED)")
                    context.contentResolver.delete(
                        recording.uri, null, null
                    )
                    context.viewModel.onDeleteRecording(recording)
                    if (recordings.isNotEmpty())
                        deleteRecordings(recordings)
                }
            }
        }
    }

    fun finishRecordingEdit(recording: Recording, title: String, description: String, onFinished: () -> Unit) {
        Log.i("edit", "title - $title")
        recording.name = title
        try {
            context.contentResolver.update(
                recording.uri,
                ContentValues().apply {
                    put(MediaStore.Audio.Media.DISPLAY_NAME, "$title.m4a")
                },
                "${MediaStore.Audio.Media._ID} = ?",
                arrayOf(ContentUris.parseId(recording.uri).toString())
            )
            onFinished()
        } catch (securityException: RuntimeException) {
            if (Build.VERSION.SDK_INT >= 29 &&
                securityException is RecoverableSecurityException
            ) {
                val intentSender =
                    securityException.userAction.actionIntent.intentSender
                context.filePermissionRequestLauncher(IntentSenderRequest.Builder(intentSender).build()) {
                    context.contentResolver.update(
                        recording.uri,
                        ContentValues().apply {
                            put(MediaStore.Audio.Media.DISPLAY_NAME, "$title.m4a")
                        },
                        "${MediaStore.Audio.Media._ID} = ?",
                        arrayOf(ContentUris.parseId(recording.uri).toString())
                    )
                    onFinished()
                }
            }
        }

        context.viewModel.finishRecordingEdit(recording, description)

//            context.discoverAudioFiles()
    }

    fun shareRecording(recordings: List<Recording>) {
        require(recordings.isNotEmpty()) { "Call to shareRecording contained an empty list." }

        fun shareOne(rec: Recording) {
            val shareIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, rec.uri)
                type = "audio/*"
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share your recording."))
        }
        if (recordings.size == 1) {
            shareOne(recordings[0])
            return
        }

        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND_MULTIPLE
            putParcelableArrayListExtra(
                Intent.EXTRA_STREAM,
                ArrayList<Uri>().also {
                    it.addAll(
                        recordings.map { it.uri })
                }
            )
            type = "audio/*"
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share your recordings."))
    }

    fun shareRecordingAsVideo(recording: Recording, loopVideo: Boolean, debugCommand: String? = null) {
        context.pickVideo(
            {
                Log.i("PlaybackClientControls", "onShareAsVideo received uri $it, type ${context.contentResolver.getType(it)}, scheme ${it.scheme}")
                val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(File(it.path).extension)?:return@pickVideo
                val isImage = mimeType.startsWith("image")
                Log.i("PlaybackClientControls", "selected file is image? $isImage ($mimeType)")
                context.beginShareAsVideo(
                    recording,
                    it,
                    loopVideo,
                    isImage,
                    debugCommand = debugCommand
                )
            },
            {
                return@pickVideo
            }
        )
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun setPlaybackSpeed (float: Float) {
        val speed = min (2.0f, float)
        context.viewModel.playbackSpeed = speed
        context.mediaController.transportControls.setPlaybackSpeed(speed)
        setLoopSelection(
            context.viewModel.loopRange.start, context.viewModel.loopRange.endInclusive
        )
    }
}