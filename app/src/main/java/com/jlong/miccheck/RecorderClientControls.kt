package com.jlong.miccheck

import android.content.ContentUris
import android.content.ContentValues
import android.os.Build
import android.os.Bundle
import android.os.Message
import android.provider.MediaStore
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RecorderClientControls (
    val context: MainActivity
) {
    fun onStartRecord() {
        val msg = Message().apply {
            obj = RecorderActions.START
            data = Bundle().apply {
                putInt("sampleRate", context.viewModel.settings.sampleRate)
                putInt("encodingBitRate", context.viewModel.settings.encodingBitRate)
            }
        }
        context.mRecorderServiceMessenger?.apply {
            send(msg)
        }
        context.viewModel.recordingState = RecordingState.RECORDING
    }

    fun onPausePlayRecord() {
        if (context.viewModel.recordingState != RecordingState.RECORDING &&
            context.viewModel.recordingState != RecordingState.PAUSED
        )
            return
        val msg = Message().apply {
            if (context.viewModel.recordingState == RecordingState.RECORDING) {
                obj = RecorderActions.PAUSE
            } else if (context.viewModel.recordingState == RecordingState.PAUSED) {
                obj = RecorderActions.RESUME
            }
        }
        context.mRecorderServiceMessenger?.send(msg)
    }

    fun onStopRecord() {
        val lMsg = Message().apply {
            obj = RecorderActions.STOP
        }
        context.mRecorderServiceMessenger?.apply {
            send(lMsg)
        }
    }

    fun onRecordingFinalized(
        title: String,
        description: String,
        tags: List<Tag>
    ) {
        if (context.viewModel.currentRecordingUri == null)
            return
        val finalUri = context.viewModel.currentRecordingUri!!

        context.contentResolver.update(
            context.viewModel.currentRecordingUri!!,
            ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, "$title.m4a")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    put(MediaStore.Audio.Media.IS_RECORDING, 1)
                }
            },
            "${MediaStore.Audio.Media._ID} = ?",
            arrayOf(ContentUris.parseId(context.viewModel.currentRecordingUri!!).toString())
        )

        context.viewModel.onRecordingFinalized(title, description)

        context.lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                context.discoverAudioFiles()
                withContext(Dispatchers.Main) {
                    context.viewModel.addTagsToRecording(
                        context.viewModel.getRecordingData(
                            context.viewModel.getRecording(
                                finalUri
                            )!!
                        ), tags
                    )
                }
            }
        }


    }

    fun onCancelRecording() {
        if (context.viewModel.currentRecordingUri == null)
            return
        context.contentResolver.delete(context.viewModel.currentRecordingUri!!, null, null)
        context.viewModel.onCancelRecording()
    }

    fun onTrimRecording(title: String, description: String, tags: List<Tag>) {
        val startMilli = context.viewModel.currentPlaybackRec?.let { (it.first.duration*context.viewModel.loopRange.start).toLong() } ?: return
        val endMilli = context.viewModel.currentPlaybackRec?.let { (it.first.duration*context.viewModel.loopRange.endInclusive).toLong() } ?: return

        context.viewModel.currentPlaybackRec?.also {
            context.beginCrop(
                it.first,
                startMilli,
                endMilli,
                title,
                description,
                tags
            )
        }
    }

}