@file:Suppress("DEPRECATION")

package com.jlong.miccheck

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.*
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.media.MediaMetadata.METADATA_KEY_MEDIA_URI
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.text.format.Formatter
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.core.app.ActivityCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.google.accompanist.insets.ProvideWindowInsets
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.jlong.miccheck.billing.Billing
import com.jlong.miccheck.billing.PRO_SKU
import com.jlong.miccheck.ui.compose.*
import com.jlong.miccheck.ui.theme.MicCheckTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.time.ZoneId


class MainActivity : ComponentActivity() {
    val viewModel by viewModels<MicCheckViewModel>()

    //Recording service variables
    private var mRecorderActivityMessenger: Messenger? = null
    var mRecorderServiceMessenger: Messenger? = null
    private var recorderServiceConnection: RecorderServiceConnection? = null

    //Playback service variables
    private lateinit var mMediaBrowserCompat: MediaBrowserCompat

    private val permissions = arrayOf(
        Manifest.permission.RECORD_AUDIO
    ).let {
        var perApi = it
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            perApi += arrayOf(Manifest.permission.FOREGROUND_SERVICE)
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            perApi += arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            perApi += arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            perApi += arrayOf(Manifest.permission.READ_MEDIA_AUDIO)

        perApi
    }

    //region lifecycle functions
    @OptIn(ExperimentalAnimationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val builder = StrictMode.VmPolicy.Builder()
        StrictMode.setVmPolicy(builder.build())

        createNotificationChannel()

        viewModel.inDebugMode = 0 != applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE
        viewModel.serializeAndSave = this::serializeAndSaveData
        loadData()
        if (!viewModel.settings.firstLaunch) {
            if (!permissions.all {
                    ActivityCompat.checkSelfPermission(
                        this,
                        it
                    ) == PackageManager.PERMISSION_GRANTED
                }) {
                requestPermissions.launch(permissions)
            }
        }

        //setup recording service
        mRecorderActivityMessenger = Messenger(RecorderHandler())
        val lIntent = Intent(this@MainActivity, RecorderService::class.java)
        lIntent.putExtra("Messenger", mRecorderActivityMessenger)
        startService(lIntent)

        //setup playback service
        val componentName = ComponentName(this, PlaybackService::class.java)
        // initialize the browser
        mMediaBrowserCompat = MediaBrowserCompat(
            this, componentName, //Identifier for the service
            connectionCallback,
            null
        )

        if (!viewModel.settings.firstLaunch) {
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    discoverAudioFiles()
                }
            }
        }
        Log.i("MAIN_A", viewModel.recordings.toList().toString())

        Billing.getInstance(application, GlobalScope, arrayOf(PRO_SKU)) {
            getSharedPreferences("micCheck", MODE_PRIVATE).edit().putBoolean("is_pro", it).apply()
            viewModel.isPro = it
        }

        setContent {

            MicCheckTheme (
                darkTheme = when (viewModel.settings.theme) {
                    ThemeOptions.Light -> false
                    ThemeOptions.Dark -> true
                    ThemeOptions.System -> isSystemInDarkTheme()
                },
                dynamicColor = true
            ) {
                ProvideWindowInsets {
                    val systemUiController = rememberSystemUiController()
                    val useDarkIcons = !isSystemInDarkTheme() && viewModel.settings.theme != ThemeOptions.Dark
                    SideEffect {
                        systemUiController.setSystemBarsColor(Color.Transparent, darkIcons = useDarkIcons)
                    }
                    if (viewModel.settings.firstLaunch)
                        FirstLaunchScreen(
                            viewModel = viewModel,
                            requestPermissions = {
                                if (!permissions.all {
                                        ActivityCompat.checkSelfPermission(
                                            this,
                                            it
                                        ) == PackageManager.PERMISSION_GRANTED
                                    }) {
                                    requestPermissions.launch(permissions)
                                }
                            },
                            onComplete = {
                                lifecycleScope.launch {
                                    withContext(Dispatchers.IO) {
                                        discoverAudioFiles()
                                    }
                                }
                                viewModel.clearFirstLaunch()
                            }
                        ) {
                            lifecycleScope.launch {
                                withContext(Dispatchers.IO) {
                                    discoverAudioFiles()
                                }
                            }
                            viewModel.clearFirstLaunch()
                            viewModel.firstStartShowProScreen = true
                        }
                    else
                        App(
                            viewModel,
                            recorderClientControls,
                            playbackClientControls,
                            this::pickImage,
                            { callback -> pickFile(null) { callback(it) } },
                            this::launchUri,
                            this::startExportData,
                            this::startImportData
                        )
                }
            }
        }

        when {
            intent?.action == Intent.ACTION_SEND && intent?.type?.startsWith("audio/") == true -> {
                Log.i("MAIN", "Intent found !! ${intent}")
                importExternalFileFromIntent(intent)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        mMediaBrowserCompat.connect()
    }

    override fun onResume() {
        super.onResume()
        val lIntent = Intent(this@MainActivity, RecorderService::class.java)
        if (recorderServiceConnection == null)
            recorderServiceConnection = RecorderServiceConnection()
        bindService(
            lIntent,
            recorderServiceConnection!!,
            0
        )
    }

    override fun onPause() {
        super.onPause()
        if (recorderServiceConnection != null)
            unbindService(recorderServiceConnection!!)
    }

    override fun onStop() {
        super.onStop()

        val controllerCompat = MediaControllerCompat.getMediaController(this)
        controllerCompat?.unregisterCallback(mControllerCallback)
        mMediaBrowserCompat.disconnect()
    }
    //endregion

    //region RecordingService related
    inner class RecorderServiceConnection : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            mRecorderServiceMessenger = Messenger(service)
            // where mServiceMessenger is used to send messages to Service
            // service is the binder returned from onBind method in the Service
        }

        override fun onServiceDisconnected(name: ComponentName) {
            mRecorderServiceMessenger = null
            unbindService(this)
        }
    }

    private val recorderClientControls = RecorderClientControls(this)

    @SuppressLint("HandlerLeak")
    inner class RecorderHandler : Handler(Looper.myLooper()!!) {
        override fun handleMessage(msg: Message) {
            when (msg.obj as RecordingState) {
                RecordingState.RECORDING -> {
                    val uri = msg.data.getString("URI")
                    if ((uri ?: "").isNotBlank())
                        viewModel.currentRecordingUri = Uri.parse(uri)
                    viewModel.recordingState = RecordingState.RECORDING
                }
                RecordingState.ELAPSED_TIME -> {
                    val time = msg.data.getLong("ELAPSED")
                    viewModel.recordTime = time
                }
                RecordingState.ERROR -> {
                    Log.i("RecordeHandler", "Received error from recording service after attempting to record.")
                    viewModel.recordingState = RecordingState.WAITING

                    if (viewModel.deniedPermissions.contains(Manifest.permission.RECORD_AUDIO)) {
                        viewModel.showLatePermissionsDialog = true
                    }
                }
                else -> viewModel.recordingState = msg.obj as RecordingState
            }
        }
    }
    //endregion

    //region PlaybackService related

    private val playbackClientControls = PlaybackClientControls(this)

    private val mControllerCallback = object : MediaControllerCompat.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            super.onPlaybackStateChanged(state)
            viewModel.currentPlaybackState = state?.state ?: PlaybackStateCompat.STATE_NONE
            viewModel.playbackProgress = state?.position ?: 0L
            viewModel.playbackSpeed = state?.playbackSpeed ?: 1f
        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            super.onMetadataChanged(metadata)
            val recording = viewModel.recordings.find {
                it.uri.toString() == metadata?.getString(METADATA_KEY_MEDIA_URI)
            } ?: return

            if (
                recording.uri != viewModel.currentPlaybackRec?.first?.uri
            ) {
                viewModel.loopMode = false
                viewModel.loopRange = 0f..1f
                playbackClientControls.disableLoopPlayback()
                playbackClientControls.setLoopSelection(0f, 1f)
            }

            viewModel.setCurrentPlayback(
                Triple(
                    recording,
                    viewModel.getRecordingData(recording),
                    viewModel.getGroups(recording).elementAtOrNull(0)
                )
            )
        }

    }

    private val connectionCallback: MediaBrowserCompat.ConnectionCallback =
        object : MediaBrowserCompat.ConnectionCallback() {
            override fun onConnected() {

                // The browser connected to the session successfully, use the token to create the controller
                super.onConnected()
                mMediaBrowserCompat.sessionToken.also { token ->
                    val mediaController = MediaControllerCompat(this@MainActivity, token)
                    MediaControllerCompat.setMediaController(this@MainActivity, mediaController)
                    mediaController.registerCallback(
                        mControllerCallback
                    )
                    mControllerCallback.onPlaybackStateChanged(mediaController.playbackState)
                }
                Log.d("onConnected", "Controller Connected")
            }

            override fun onConnectionFailed() {
                super.onConnectionFailed()
                Log.d("onConnectionFailed", "Connection Failed")

            }

        }
    //endregion

    private fun createNotificationChannel() {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        notificationManager.apply {
            createNotificationChannel(
                NotificationChannel(
                    "micCheckPlaybackServiceControls",
                    "MicCheck PlaybackService Controls",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Playback controls"
                }
            )
            createNotificationChannel(
                NotificationChannel(
                    "micCheckRecordingControls",
                    "MicCheck Recording Controls",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Recording controls"
                }
            )
        }

    }

    private val requestPermissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        viewModel.deniedPermissions = mutableStateListOf()
        permissions.forEach {
            Log.i("MainActivity", "Permission ${it.key} granted? ${it.value}")
            if (!it.value) {
                viewModel.deniedPermissions += it.key
                Toast.makeText(
                    this,
                    "This app needs the permission ${it.key}.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        if (permissions.toList().fold(true) { acc, t ->
            acc and t.second
        }) {
            discoverAudioFiles()
        }
    }

    private var imagePickerOnError: (() -> Unit)? = null
    private var imagePickerOnSuccess: ((Uri) -> Unit)? = null

    private fun pickImage(onSuccess: (Uri) -> Unit, onError: () -> Unit) {
        Log.i("pickImage", "called.")
        imagePickerOnError = onError
        imagePickerOnSuccess = onSuccess

        val intent = if (Build.VERSION.SDK_INT >= 33) {
            Intent(MediaStore.ACTION_PICK_IMAGES).apply {
//                putExtra(MediaStore.EXTRA_PICK_IMAGES_MAX, 1)
                type = "image/*"
            }
        } else {
            Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "image/*"
            }
        }

        pickImageHandler.launch(intent)
    }

    fun pickVideo(onSuccess: (Uri) -> Unit, onError: () -> Unit) {
        Log.i("pickImage", "called.")
        imagePickerOnError = onError
        imagePickerOnSuccess = onSuccess

        val intent = if (Build.VERSION.SDK_INT >= 33) {
            Intent(MediaStore.ACTION_PICK_IMAGES).apply {
//                putExtra(MediaStore.EXTRA_PICK_IMAGES_MAX, 1)
//                type = "video/*"
            }
        } else {
            Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "video/*"
            }
        }

        pickImageHandler.launch(intent)
    }

    private val pickImageHandler = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        Log.i("pickImage", "call back called.")
        val cleanUpCallbacks = {
            imagePickerOnSuccess = null
            imagePickerOnError = null
        }

        if (it.resultCode != Activity.RESULT_OK) {
            imagePickerOnError?.invoke()
            cleanUpCallbacks()
            return@registerForActivityResult
        }
        else {
            val uri: Uri? = it.data?.data
            if (uri == null) {
                imagePickerOnError?.invoke()
                cleanUpCallbacks()
                return@registerForActivityResult
            }
            Log.i("PickVideo", "Received $uri, type ${contentResolver.getType(uri)}")

            val destFile = File(
                applicationContext.filesDir.absolutePath,
                uri.lastPathSegment!! + "." + MimeTypeMap.getSingleton().getExtensionFromMimeType(contentResolver.getType(uri))
            ).also { file ->
                file.createNewFile()
            }

            val outStream = FileOutputStream(destFile)
            val inStream = contentResolver.openInputStream(uri)?.also { stream ->
                stream.copyTo(outStream)
            }
            inStream?.close()
            outStream.close()

            imagePickerOnSuccess?.invoke(Uri.fromFile(destFile))
            cleanUpCallbacks()
            return@registerForActivityResult
        }
    }

    private var filePermissionRequestCallback: () -> Unit = {}
    fun filePermissionRequestLauncher (request: IntentSenderRequest, callback: () -> Unit) {
        filePermissionRequestCallback = callback
        filePermissionRequest.launch(request)
    }
    private val filePermissionRequest = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
        if (it.resultCode == RESULT_OK)
            filePermissionRequestCallback()
    }


    private var createFileOnSuccess: (Uri) -> Unit = { }
    fun createFile(defaultTitle: String, onSuccess: (Uri) -> Unit) {
        createFileOnSuccess = onSuccess

        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
            putExtra(Intent.EXTRA_TITLE, defaultTitle)
        }
        createFileRequest.launch(intent)
    }
    private val createFileRequest = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK)
            it.data?.data?.also {
                createFileOnSuccess(it)
            }
    }

    private var pickFileOnSuccess: (Uri) -> Unit = {}
    fun pickFile(typeString: String? = "json", onSuccess: (Uri) -> Unit) {
        pickFileOnSuccess = onSuccess

        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)

            if (typeString != null)
                type = "application/$typeString"
            else
                type = "*/*"
        }

        pickFileRequest.launch(intent)
    }
    private val pickFileRequest = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        try {
            if (it.resultCode == RESULT_OK)
                it.data?.data?.also {
                    contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    pickFileOnSuccess(it)
                }
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
            Toast.makeText(this, "File type not supported.", Toast.LENGTH_LONG).show()
        }
    }

    fun launchUri (typeString: String, uri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION

//        val candidateApps = packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
//        candidateApps.forEach {
//            Log.i("PM", it.resolvePackageName)
//        }
//        Log.i("PM", candidateApps.size.toString())
//        Log.i("PM", uri.toString())
//        if (true)
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            e.printStackTrace()
            Toast.makeText(this, "No app found to open this attachment.", Toast.LENGTH_LONG).show()
        }
    }
}


fun MainActivity.discoverAudioFiles () {
    Log.i("MC VM", "loadRecordings !!")

    val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Audio.Media.getContentUri(
            MediaStore.VOLUME_EXTERNAL
        )
    } else {
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    }
    val projection = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.DISPLAY_NAME,
        MediaStore.Audio.Media.DURATION,
        MediaStore.Audio.Media.SIZE,
        MediaStore.Audio.Media.DATE_MODIFIED,
        MediaStore.Audio.Media.DATA
    )
    val selection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Audio.Media.RELATIVE_PATH + " like ?"
    } else {
        ""
    }
    val selectionArgs =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        arrayOf("%micCheck%")
    } else {
        arrayOf()
    }
    val sortOrder = MediaStore.Audio.Media.DATE_MODIFIED + " DESC"

    val query = this.contentResolver.query(
        collection,
        projection,
        selection,
        selectionArgs,
        sortOrder
    )

    query?.use { cursor ->
        // Cache column indices.
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
        val nameColumn =
            cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
        val displayColumn =
            cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
        val durationColumn =
            cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
        val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
        val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
        val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

        viewModel.recordings.removeRange(0, viewModel.recordings.size)

        while (cursor.moveToNext()) {
            // Get values of columns for a given Audio.
            val id = cursor.getLong(idColumn)
            val name = cursor.getString(nameColumn)
            val duration = cursor.getInt(durationColumn)
            val size = cursor.getInt(sizeColumn)
            val date = cursor.getInt(dateColumn) * 1000L
            val dName = cursor.getString(displayColumn)
            val path = cursor.getString(pathColumn)

            if (duration == 0)
                continue

            val contentUri: Uri = ContentUris.withAppendedId(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                id
            )

            // Stores column values and the contentUri in a local object
            // that represents the media file.
            Log.e("VM", "Found file $contentUri - $name - $dName")
            lifecycleScope.launch(Dispatchers.Main){
                viewModel.recordings.add(
                    Recording(
                        contentUri,
                        dName.removeSuffix(".m4a").removeSuffix(".wav"),
                        duration,
                        size,
                        Formatter.formatShortFileSize(this@discoverAudioFiles, size.toLong()).toString(),
                        date = Instant.ofEpochMilli(date).atZone(ZoneId.systemDefault())
                            .toLocalDateTime(),
                        path
                    )
                )

                if (viewModel.recordingsData.find { contentUri == Uri.parse(it.recordingUri) } == null)
                    viewModel.recordingsData.add(
                        RecordingData(
                            contentUri.toString()
                        )
                    )
            }
        }
    }

    lifecycleScope.launch {
        withContext(Dispatchers.IO) {
            serializeAndSaveData()
        }
    }
}