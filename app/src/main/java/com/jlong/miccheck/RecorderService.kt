package com.jlong.miccheck

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.*
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
import android.database.Cursor
import android.graphics.drawable.Icon
import android.media.MediaRecorder
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import java.io.File
import java.io.IOException
import kotlin.random.Random


enum class RecorderActions {
    START, PAUSE, RESUME, STOP
}

enum class RecordingState {
    RECORDING, PAUSED, STOPPED, WAITING, ELAPSED_TIME, ERROR
}

class RecorderService : Service() {

    lateinit var mServiceMessenger: Messenger
    var mActivityMessenger: Messenger? = null

    private var recorder: MediaRecorder? = null
    private var currentOutputFileDescriptor: ParcelFileDescriptor? = null
    private var currentOutputFile: File? = null

    private val notificationChannelId = "micCheckRecordingControls"
    private val notificationId = 102
    private lateinit var notification: Notification.Builder

    var recordTimeHandler: Handler? = null
    var recordTime: Long = 0

    var sampleRate: Int = 0
    var encodingBitRate: Int = 0
    var outputFormat: OutputFormat = OutputFormat.M4A

    var isPaused = false

    @SuppressLint("HandlerLeak")
    inner class IncomingHandler : Handler(Looper.myLooper()!!) {
        override fun handleMessage(msg: Message) {
            when (msg.obj as RecorderActions) {
                RecorderActions.START -> onStartRecord(msg.data)
                RecorderActions.PAUSE -> onPause()
                RecorderActions.RESUME -> onResume()
                RecorderActions.STOP -> onStopRecord()
            }
        }
    }

    override fun onBind(intent: Intent): IBinder {
        Log.d("RecorderService", "onBind")
        return mServiceMessenger.binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("RecorderService", "onStartCommand")
        mActivityMessenger = intent?.getParcelableExtra("Messenger")
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onCreate() {
        super.onCreate()
        mServiceMessenger = Messenger(IncomingHandler())
        notification = Notification.Builder(this, notificationChannelId)
        registerReceiver(broadcastReceiver, IntentFilter("NOTIFICATION_ACTION"))
    }

    fun onStartRecord(params: Bundle) {
        sampleRate = params.getInt("sampleRate")
        encodingBitRate = params.getInt("encodingBitRate")
        outputFormat = params.getString("outputFormat")?.let {
            OutputFormat.valueOf(it)
        } ?: OutputFormat.M4A

        val uri = createRecordingFile()
        val failToast: () -> Unit = {
            Toast.makeText(
                applicationContext,
                "Recording failed.",
                Toast.LENGTH_LONG
            ).show()
        }

        if (uri == null) {
            Log.e("RecorderServer", "Uri is null.")
            failToast()
            return
        }

        if (recorder != null) {
            recorder?.apply {
                try {
                    stop()
                } catch (stopException: RuntimeException) {
                    Log.e("RecorderService", "Attempted to stop when start had not been called.")
                }
                release()
            }
            recorder = null
        }
        if (!prepareRecorder()) {
            Log.e("RecorderServer", "Recorder is null.")
            failToast()
            mActivityMessenger?.apply {
                send(Message().apply {
                    obj = RecordingState.ERROR
                })
            }

            return
        } else {
//                recorder!!.reset()
        }
        recorder!!.start()

        val lMsg = Message().apply {
            obj = RecordingState.RECORDING
            data = Bundle().apply {
                putString("URI", uri.toString())
            }
        }
        mActivityMessenger?.apply {
            send(lMsg)
        }
        postElapsed()

        isPaused = false
        moveToForeground()

        Log.i("RecorderService", "Recording started.")
    }

    private fun postElapsed() {
        if (recordTimeHandler == null) {
            recordTimeHandler = Handler(Looper.getMainLooper())
        }
        recordTimeHandler?.postDelayed({
            recordTime += 1000
            val lMsg = Message().apply {
                obj = RecordingState.ELAPSED_TIME
                data = Bundle().apply {
                    putLong("ELAPSED", recordTime)
                }
            }
            mActivityMessenger?.apply {
                send(lMsg)
            }
            postElapsed()
        }, 1000)
    }

    private fun stopPostElapsed() {
        recordTimeHandler?.removeCallbacksAndMessages(null)
        recordTimeHandler = null
    }

    fun onPause() {
        recorder?.apply {
            pause()
        }
        val lMsg = Message().apply {
            obj = RecordingState.PAUSED
        }
        mActivityMessenger?.apply {
            send(lMsg)
        }
        stopPostElapsed()

        isPaused = true
        moveToForeground()
        Log.i("RecorderService", "Recording paused.")
    }

    fun onResume() {
        recorder?.apply {
            resume()
        }
        val lMsg = Message().apply {
            obj = RecordingState.RECORDING
            data = Bundle().apply {
                putString("URI", "")
            }       // Tell the client we don't have a new URI
        }
        mActivityMessenger?.apply {
            send(lMsg)
        }
        postElapsed()

        isPaused = false
        moveToForeground()
        Log.i("RecorderService", "Recording resumed.")

    }

    fun onStopRecord() {
        try {
            recorder?.apply {
                stop()
                release()
            }
        } catch (e: RuntimeException) {
            Toast.makeText(this, "Recording failed, the microphone didn't send any data :/", Toast.LENGTH_LONG).show()
        }
        recorder = null
        currentOutputFileDescriptor?.close()
        currentOutputFileDescriptor = null

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            MediaScannerConnection.scanFile(
                this,
                arrayOf(currentOutputFile!!.absolutePath),
                arrayOf("audio/mp4"),
                null
            )
            currentOutputFile = null
        }

        stopForeground(true)

        val lMsg = Message().apply {
            obj = RecordingState.STOPPED
        }
        mActivityMessenger?.apply {
            send(lMsg)
        }
        stopPostElapsed()
        recordTime = 0

        isPaused = false
        Log.i("RecorderService", "Recording stopped.")

    }

    private fun prepareRecorder(): Boolean {
        try {
            recorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.DEFAULT) //TODO(#1 Crash source)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(encodingBitRate)
                setAudioSamplingRate(sampleRate)
                setOutputFile(currentOutputFileDescriptor!!.fileDescriptor)

                try {
                    prepare()
                    Log.e("MicCheck", "prepare() succeeded")
                } catch (e: IOException) {
                    Log.e("MicCheck", "prepare() failed")
                    return@prepareRecorder false
                }
            }
        } catch (e: Exception) {
            return false
        }
        return true
    }

    private fun createRecordingFile(): Uri? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            createRecordingFileQ()
        else
            createRecordingFileLegacy()

    private fun createRecordingFileQ(): Uri? {
        val values = ContentValues(4)
        values.put(MediaStore.Audio.Media.TITLE, "Untitled Recording")
        values.put(
            MediaStore.Audio.Media.DATE_ADDED,
            (System.currentTimeMillis() / 1000).toInt()
        )
        values.put(MediaStore.Audio.Media.MIME_TYPE, "audio/${
            when (outputFormat) {
                OutputFormat.M4A -> "mp4"
                OutputFormat.WAV -> "wav"
            }
        }")
        values.put(MediaStore.Audio.Media.ARTIST, "Me")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/micCheck/")
        } else {
            val directory = getExternalFilesDir(Environment.DIRECTORY_MUSIC)
            values.put(MediaStore.Audio.AudioColumns.DATA,
                "${directory}/Untitled Recording.${
                    when (outputFormat) {
                        OutputFormat.M4A -> "m4a"
                        OutputFormat.WAV -> "wav"
                    }
                }")
        }

        val audioUri = applicationContext.contentResolver.insert(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            values
        ) ?: return null

        currentOutputFileDescriptor = applicationContext.contentResolver.openFileDescriptor(audioUri, "w")

        return audioUri
    }

    private fun createRecordingFileLegacy() : Uri? {
        val tempTitle = Random.nextInt().toString()+".m4a"
        currentOutputFile = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
            tempTitle
        )

        currentOutputFile!!.createNewFile()

        MediaScannerConnection.scanFile(
            this,
            arrayOf(currentOutputFile!!.absolutePath),
            arrayOf("audio/mp4"),
            null
        )

        return getContentUri(
            currentOutputFile!!
        )?.also {
            contentResolver.update(
                it,
                ContentValues().apply {
                    put(MediaStore.Audio.Media.TITLE, tempTitle)
                    put(
                        MediaStore.Audio.Media.DATE_ADDED,
                        (System.currentTimeMillis() / 1000).toInt()
                    )
                    put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp4")
                    put(MediaStore.Audio.Media.ARTIST, "Me")
                },
                null,
                null
            )
            currentOutputFileDescriptor = contentResolver.openFileDescriptor(it, "w")
        }
    }

    private fun getContentUri(file: File): Uri? {
        val filePath = file.absolutePath
        val cursor: Cursor? = contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, arrayOf(MediaStore.Audio.Media._ID),
            MediaStore.Audio.Media.DATA + "=? ", arrayOf(filePath), null
        )
        return if (cursor != null && cursor.moveToFirst()) {
            val id: Int = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID))
            cursor.close()
            Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, "" + id)
        } else {
            if (file.exists()) {
                val values = ContentValues()
                values.put(MediaStore.Audio.Media.DATA, filePath)
                contentResolver.insert(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values
                )
            } else {
                null
            }
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(broadcastReceiver)
    }

    private fun getNotificationIntent(): PendingIntent {
        val openActivityIntent = Intent(
            this,
            MainActivity::class.java
        )
        openActivityIntent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        return PendingIntent.getActivity(
            this@RecorderService, 0, openActivityIntent,
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun getPauseIntent(): Notification.Action {
        val intentAction = Intent(this, ActionReceiver::class.java)
        intentAction.putExtra("action", "pauseResume")
        val pIntent = PendingIntent.getBroadcast(this,1,intentAction,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return Notification.Action.Builder(
            Icon.createWithResource(this, R.drawable.ic_pause),
            "Pause",
            pIntent
        ).build()
    }

    private fun getResumeIntent(): Notification.Action {
        val intentAction = Intent(this, ActionReceiver::class.java)
        intentAction.putExtra("action", "pauseResume")
        val pIntent = PendingIntent.getBroadcast(this,1,intentAction,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return Notification.Action.Builder(
            Icon.createWithResource(this, R.drawable.ic_round_mic_24),
            "Resume",
            pIntent
        ).build()
    }

    private fun getStopIntent(): PendingIntent {
        val intentAction = Intent(this, ActionReceiver::class.java)
        intentAction.putExtra("action", "stop")
        return PendingIntent.getBroadcast(this,2,intentAction,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun buildNotification (): Notification {
        notification = Notification.Builder(this, notificationChannelId)
            .setContentTitle("MicCheck Recording")
            .setContentText(if (isPaused) "Paused" else "Currently Recording")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(getNotificationIntent())
            .apply {
                if (isPaused)
                    addAction(getResumeIntent())
                else
                    addAction(getPauseIntent())
            }


        return notification.build()
    }

    private fun moveToForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) startForeground(
            notificationId,
            buildNotification(), FOREGROUND_SERVICE_TYPE_MICROPHONE
        )
        else
            startForeground(notificationId, buildNotification())
    }

    var broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, intent: Intent?) {
            if (intent == null)
                return
            when (intent.getStringExtra("action")) {
                "pauseResume" -> if (isPaused) onResume() else onPause()
                "stop" -> {
                    onStopRecord()
                }
            }
        }
    }
}

class ActionReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent == null || context == null)
            return
        context.sendBroadcast(
            Intent("NOTIFICATION_ACTION").apply {
                putExtra("action", intent.getStringExtra("action"))
            }
        )
    }

}