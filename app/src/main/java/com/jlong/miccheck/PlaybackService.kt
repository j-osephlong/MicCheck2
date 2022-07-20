package com.jlong.miccheck

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.drawable.Icon
import android.media.session.MediaSession
import android.net.Uri
import android.os.*
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.media.MediaBrowserServiceCompat
import androidx.media.session.MediaButtonReceiver
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import java.io.File

class PlaybackService : MediaBrowserServiceCompat() {

    private var mMediaSession: MediaSessionCompat? = null
    private lateinit var mStateBuilder: PlaybackStateCompat.Builder
    private var mExoPlayer: ExoPlayer? = null
    private var oldUri: Uri? = null
    private var currUri: Uri? = null
    private var mediaExtras: Bundle? = null
    private var playbackList: List<Pair<Uri, Bundle>>? = null
    private var currListIndex: Int = 0
    private var playbackSpeed: Float = 1f

    private var notificationId = 101
    private var channelId = "micCheckPlaybackServiceControls"
    private var notificationBuilder: Notification.Builder? = null

    private var playbackPosUpdateHandler: Handler? = null

    private val emptyRootMediaId = "micCheck_empty_root_media_id"

    private var loopMode: Boolean = false
    private var startRange: Float = 0f
    private var endRange: Float = 1f

    private val CUSTOM_ACTION_REPLAY = "micCheckCustomActionReplay"
    private val CUSTOM_ACTION_FORWARD = "micCheckCustomActionForward"

    private val mMediaSessionCallback = object : MediaSessionCompat.Callback() {

        override fun onPlayFromUri(uri: Uri?, extras: Bundle?) {
            super.onPlayFromUri(uri, extras)
            Log.i("play@PlaybackService", "Attempting playback from uri.")
            if (extras?.containsKey("playbackList") == true) {
                onPlayFromGroup(extras)
                return
            }
            else if (extras?.containsKey("isOfPlaybackList") == false)
                playbackList = null
            uri?.let {
                val mediaSource = extractMediaSourceFromUri(uri)
                currUri = uri
                mediaExtras = extras
                setMetadataFromExtras()
                if (uri != oldUri || mMediaSession!!.controller.playbackState.state == PlaybackStateCompat.STATE_STOPPED)
                    play(mediaSource)
                else play() // this song was paused so we don't need to reload it
                oldUri = uri
            }
            displayNotification()
            updateCurrentPosition()
        }

        /**
         * TODO: Group queue support
         */
        fun onPlayFromGroup (data: Bundle) {
            val list = data.getParcelableArrayList<Bundle>("playbackList")
            playbackList = list?.map {
                Pair(
                    Uri.parse(it.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI)),
                    it.apply { it.remove(MediaMetadataCompat.METADATA_KEY_MEDIA_URI) }
                )
            }
            Log.i("PlaybackService", "Playbacklist for group is size ${playbackList?.size}")
            if (playbackList == null)
                return
            currListIndex = data.getInt("listIndex")
            Log.i("PlaybackService", "Group playback starting at index $currListIndex")
            onPlayFromUri(playbackList!![currListIndex].first, playbackList!![currListIndex].second)
        }

        override fun onPlay() {
            super.onPlay()
            currUri?.let {
                onPlayFromUri(currUri, mediaExtras)
                displayNotification()
                updateCurrentPosition()
            }
        }

        override fun onSkipToNext() {
            super.onSkipToNext()
            if (currListIndex == playbackList?.size?.minus(1) ||
                playbackList == null
            )
                return
            currListIndex++
            onPlayFromUri(playbackList!![currListIndex].first, playbackList!![currListIndex].second)
        }

        override fun onSetPlaybackSpeed(speed: Float) {
            super.onSetPlaybackSpeed(speed)
            playbackSpeed = speed
            updatePlaybackState(null)
        }

        override fun onSkipToPrevious() {
            super.onSkipToPrevious()
            if (currListIndex == 0 ||
                playbackList == null
            )
                return
            currListIndex--
            onPlayFromUri(playbackList!![currListIndex].first, playbackList!![currListIndex].second)
        }

        override fun onSeekTo(pos: Long) {
            super.onSeekTo(pos)
            seek(pos)
        }

        override fun onPause() {
            Log.i("PlaybackService", "Paused.")
            super.onPause()
            pause()
            stopPlaybackStateUpdate()
            displayNotification()
            stopForeground(false)
        }

        override fun onRewind() {
            super.onRewind()

            val duration =
                mMediaSession?.controller?.metadata?.bundle?.getLong(MediaMetadataCompat.METADATA_KEY_DURATION)
                    ?: 0L
            val diff = if (duration < (1000 * 60 * 1.5))
                -5
            else if (duration > (1000 * 60 * 1.5) && duration < (1000 * 60 * 3))
                -10
            else
                -30

            seek(
                ((mExoPlayer?.currentPosition ?: 0L) + diff * 1000)
            )

            updateCurrentPosition()
        }

        override fun onCommand(command: String?, extras: Bundle?, cb: ResultReceiver?) {
            super.onCommand(command, extras, cb)
        }

        override fun onCustomAction(action: String?, extras: Bundle?) {
            super.onCustomAction(action, extras)
            if (action!=null && extras != null)
                when (action) {
                    "setLoopMode" -> {
                        extras.getBoolean("val").also {
                            loopMode = it
                        }
                    }
                    "setRange" -> {
                        extras.getFloat("startRange").let {
                            startRange = it
                        }
                        extras.getFloat("endRange").let {
                            endRange = it
                        }
                    }
                    CUSTOM_ACTION_REPLAY -> onRewind()
                    CUSTOM_ACTION_FORWARD -> onFastForward()
                }
        }

        override fun onFastForward() {
            super.onFastForward()
            val duration =
                mMediaSession?.controller?.metadata?.bundle?.getLong(MediaMetadataCompat.METADATA_KEY_DURATION)
                    ?: 0L
            val diff = if (duration < (1000 * 60 * 1.5))
                5
            else if (duration > (1000 * 60 * 1.5) && duration < (1000 * 60 * 3))
                10
            else
                30

            seek(
                ((mExoPlayer?.currentPosition ?: 0L) + diff * 1000)
            )

            updateCurrentPosition()
        }

        override fun onStop() {
            super.onStop()
            stop()
            stopPlaybackStateUpdate()
            stopForeground(true)
        }

        private fun updateCurrentPosition() {
            Log.i("PlaybackService", "UpdateCurrentPosition called.")
            if (mExoPlayer == null) {
                return
            }
            if (playbackPosUpdateHandler == null) {
                playbackPosUpdateHandler = Handler(Looper.getMainLooper())
            }
            playbackPosUpdateHandler?.postDelayed({
                updatePlaybackState(null)
                handleLoopFunctions()
                updateCurrentPosition()
            }, 100)
        }

        private fun handleLoopFunctions() {

            mExoPlayer?.also {
                Log.i("LoopFUNC",
                    "mode - $loopMode\n" +
                            "pos - ${it.currentPosition}\n" +
                            "startRange - ${(it.duration*startRange).toLong()}\n" +
                            "endRange - ${it.duration*endRange-100L}"
                )
                if (loopMode) {
                    if (it.currentPosition > it.duration*endRange-101L) {
                        Log.i("LOOP", "Case 1" )
                        onSeekTo((it.duration * startRange).toLong())
                    }

                    if (it.currentPosition < it.duration*startRange) {
                        Log.i("LOOP", "Case 2" )
                        onSeekTo((it.duration * startRange).toLong())
                    }
                } else {
                    if (it.duration*endRange - it.currentPosition > 500L && it.currentPosition > it.duration*endRange)
                        onSeekTo((it.duration*startRange).toLong())
                    else if (it.currentPosition > it.duration*endRange) {
                        if (endRange != 1f || playbackList == null) {
                            onSeekTo((it.duration*startRange).toLong())
                            onPause()
                        }
                    }

                    if (it.currentPosition < it.duration*startRange)
                        onSeekTo((it.duration*startRange).toLong())
                }

            }
        }

        override fun onSetRepeatMode(repeatMode: Int) {
            super.onSetRepeatMode(repeatMode)
            setRepeatMode(repeatMode)
        }
    }

    private fun stopPlaybackStateUpdate() {
        Log.i("PlaybackService", "Removed playbackPosUpdateHandler.")
        playbackPosUpdateHandler?.removeCallbacksAndMessages(null)
        playbackPosUpdateHandler = null
    }

    override fun onCreate() {
        super.onCreate()
        initializePlayer()
        initializeExtractor()
        initializeAttributes()
        mMediaSession = MediaSessionCompat(baseContext, "PlaybackService").apply {

            // Set initial PlaybackState with ACTION_PLAY, so media buttons can start the player
            mStateBuilder = PlaybackStateCompat.Builder()
                .setActions(
                    PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PAUSE
                            or PlaybackStateCompat.ACTION_REWIND or PlaybackStateCompat.ACTION_FAST_FORWARD
                            or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                )
                .addCustomAction(PlaybackStateCompat.CustomAction.Builder(
                    CUSTOM_ACTION_REPLAY,
                    "Replay",
                    R.drawable.ic_round_replay_24
                ).build())
//                .setState(PlaybackStateCompat.STATE_NONE, mExoPlayer!!.currentPosition, 1f)
            setPlaybackState(mStateBuilder.build())

            // methods that handle callbacks from a media controller
            setCallback(mMediaSessionCallback)

            // Set the session's token so that client activities can communicate with it
            setSessionToken(sessionToken)
            isActive = true
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            MediaButtonReceiver.handleIntent(mMediaSession, intent)
            Log.e(
                "PlaybackService",
                "onStartCommand(): received intent " + intent.action + " with flags " + flags + " and startId " + startId
            )
        }

        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        stop()
    }

    private fun setMetadataFromExtras() {
        mediaExtras?.let { mediaExtras ->
            mMediaSession?.let { mMediaSession ->
                mMediaSession.setMetadata(
                    MediaMetadataCompat.Builder().apply {
                        putString(
                            MediaMetadataCompat.METADATA_KEY_TITLE,
                            mediaExtras.getString(MediaMetadataCompat.METADATA_KEY_TITLE)
                        )
                        putString(
                            MediaMetadataCompat.METADATA_KEY_ALBUM,
                            mediaExtras.getString(MediaMetadataCompat.METADATA_KEY_ALBUM)
                        )
                        putString(
                            MediaMetadataCompat.METADATA_KEY_ARTIST,
                            mediaExtras.getString(MediaMetadataCompat.METADATA_KEY_ARTIST)
                        )
                        putLong(
                            MediaMetadataCompat.METADATA_KEY_DURATION,
                            mediaExtras.getLong(MediaMetadataCompat.METADATA_KEY_DURATION)
                        )
                        putString(
                            MediaMetadataCompat.METADATA_KEY_MEDIA_URI,
                            currUri.toString()
                        )
                        if (mediaExtras.containsKey(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI))
                            putString(
                                MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI,
                                mediaExtras.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI)
                            )
                    }.build()
                )
            }
        }
    }

    private fun play(mediaSource: MediaSource) {
        Log.i("play@PlaybackService", "Playing new.")
        if (mExoPlayer == null) initializePlayer()
        mExoPlayer?.apply {
            // AudioAttributes here from exoplayer package !!!
            mAttrs?.let { initializeAttributes() }
            // In 2.9.X you don't need to manually handle audio focus :D
            setAudioAttributes(mAttrs!!, true)
            setMediaSource(mediaSource)
            prepare()
            play()
            updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
        }
    }

    private fun play() {
        Log.i("play@PlaybackService", "Playing old.")
        mExoPlayer?.apply {
            mExoPlayer?.playWhenReady = true
            mMediaSession?.isActive = true
            updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
        }

    }

    private fun pause() {
        mExoPlayer?.apply {
            playWhenReady = false
            updatePlaybackState(PlaybackStateCompat.STATE_PAUSED)
        }
    }

    private fun setRepeatMode(mode: Int) {
        mExoPlayer?.apply {
            repeatMode = mode
        }
    }

    private fun seek(pos: Long) {
        Log.i("PlaybackService", "Seek $pos")
        mExoPlayer?.apply {
            seekTo(pos)
        }
    }

    private fun stop() {
        // release the resources when the service is destroyed
        mExoPlayer?.apply {
            playWhenReady = false
            release()
        }

        mExoPlayer = null
        mMediaSession?.isActive = false
        mMediaSession?.release()

        Log.i("PlaybackService", "Stopping playback.")
    }

    private fun updatePlaybackState(state: Int?) {
        val duration =
            mMediaSession?.controller?.metadata?.bundle?.getLong(MediaMetadataCompat.METADATA_KEY_DURATION)
                ?: 0L
        // You need to change the state because the action taken in the controller depends on the state !!!
        mMediaSession?.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setActions(
                    PlaybackStateCompat.ACTION_PAUSE or PlaybackStateCompat.ACTION_PLAY_PAUSE or PlaybackStateCompat.ACTION_PLAY or
                            PlaybackStateCompat.ACTION_FAST_FORWARD or PlaybackStateCompat.ACTION_REWIND or
                            PlaybackStateCompat.ACTION_STOP or PlaybackStateCompat.ACTION_SEEK_TO or
                            (
                                if (playbackList != null) PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                                else 0
                            )
                ).apply {
                    addCustomAction(
                        PlaybackStateCompat.CustomAction.Builder(
                            CUSTOM_ACTION_REPLAY,
                            "Replay",
                            if (duration < (1000 * 60 * 1.5))
                                R.drawable.ic_round_replay_5_24
                            else if (duration > (1000 * 60 * 1.5) && duration < (1000 * 60 * 3))
                                R.drawable.ic_round_replay_10_24
                            else
                                R.drawable.ic_round_replay_30_24
                        ).build()
                    )
                    addCustomAction(
                        PlaybackStateCompat.CustomAction.Builder(
                            CUSTOM_ACTION_FORWARD,
                            "Forward",
                            if (duration < (1000 * 60 * 1.5))
                                R.drawable.ic_round_forward_5_24
                            else if (duration > (1000 * 60 * 1.5) && duration < (1000 * 60 * 3))
                                R.drawable.ic_round_forward_10_24
                            else
                                R.drawable.ic_round_forward_30_24
                        ).build()
                    )
                }
                .setState(
                    state
                        ?: mMediaSession!!.controller.playbackState.state // this state is handled in the media controller
                    , mExoPlayer?.currentPosition ?: 0L, playbackSpeed // Speed playing
                ).build()
        )

        mExoPlayer?.setPlaybackSpeed(playbackSpeed)

        Log.i("PlaybackService", "Update state, speed $playbackSpeed, state $state")
    }

    private fun playbackListBehavior () : Boolean {
        if (playbackList == null || loopMode)
            return false
        //Handle end of track
        currListIndex++
        if (currListIndex == playbackList?.size)
        {
            currListIndex = 0
            playbackList = null
            updatePlaybackState(PlaybackStateCompat.STATE_STOPPED)
            mExoPlayer!!.stop()
            displayNotification()
        } else {
            mMediaSessionCallback.onPlayFromUri(playbackList!![currListIndex].first, playbackList!![currListIndex].second)
        }

        return true
    }

    private var mAttrs: AudioAttributes? = null

    private fun initializePlayer() {
        mExoPlayer = ExoPlayer.Builder(
            this
        ).build()

        mExoPlayer!!.addListener(
            object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    super.onIsPlayingChanged(isPlaying)
                    when (mExoPlayer!!.playbackState) {
                        Player.STATE_IDLE -> updatePlaybackState(PlaybackStateCompat.STATE_NONE)
                        Player.STATE_ENDED -> {
                            if (!playbackListBehavior())
                            {
                                updatePlaybackState(PlaybackStateCompat.STATE_STOPPED)
                                stopPlaybackStateUpdate()
                                mExoPlayer!!.stop()
                                displayNotification()
                            }
                        }
                        else ->
                            if (isPlaying)
                                updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
                            else
                                updatePlaybackState(PlaybackStateCompat.STATE_PAUSED)
                    }
                }
            }
        )
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot {
        return BrowserRoot(emptyRootMediaId, null)
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        if (parentId == emptyRootMediaId) {
            result.sendResult(null)
        }

    }

    private fun initializeAttributes() {
        mAttrs = AudioAttributes.Builder().setUsage(C.USAGE_MEDIA)
            .setContentType(C.CONTENT_TYPE_MUSIC)
            .build()
    }

    private lateinit var mExtractorFactory: ProgressiveMediaSource.Factory

    private fun initializeExtractor() {
        val userAgent = Util.getUserAgent(baseContext, "MicCheck")
        mExtractorFactory = ProgressiveMediaSource.Factory(
            DefaultDataSourceFactory(this, userAgent),
            DefaultExtractorsFactory()
        )
    }

    private fun extractMediaSourceFromUri(uri: Uri): MediaSource {

        return mExtractorFactory.createMediaSource(MediaItem.fromUri(uri))
    }

    private fun getPausePlayActions():
            Pair<Notification.Action, Notification.Action> {
        val pauseAction = Notification.Action.Builder(
            Icon.createWithResource(this, R.drawable.ic_pause), "Pause",
            MediaButtonReceiver.buildMediaButtonPendingIntent(
                this,
                PlaybackStateCompat.ACTION_PAUSE
            )
        ).build()

        val playAction = Notification.Action.Builder(
            Icon.createWithResource(this, R.drawable.ic_play_arrow), "Play",
            MediaButtonReceiver.buildMediaButtonPendingIntent(
                this,
                PlaybackStateCompat.ACTION_PLAY
            )
        ).build()

        return Pair(pauseAction, playAction)
    }

    private fun getSkipActions():
            Pair<Notification.Action, Notification.Action> {
        val prevAction = Notification.Action.Builder(
            Icon.createWithResource(this, R.drawable.ic_round_skip_previous_24), "Prev",
            MediaButtonReceiver.buildMediaButtonPendingIntent(
                this,
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
            )
        ).build()

        val nextAction = Notification.Action.Builder(
            Icon.createWithResource(this, R.drawable.ic_round_skip_next_24), "Next",
            MediaButtonReceiver.buildMediaButtonPendingIntent(
                this,
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT
            )
        ).build()

        return Pair(prevAction, nextAction)
    }

    private fun getReplayForwardActions():
            Pair<Notification.Action, Notification.Action> {
        val duration =
            mMediaSession?.controller?.metadata?.bundle?.getLong(MediaMetadataCompat.METADATA_KEY_DURATION)
                ?: 0L
        val replay = Notification.Action.Builder(
            Icon.createWithResource(
                this,
                if (duration < (1000 * 60 * 1.5))
                    R.drawable.ic_round_replay_5_24
                else if (duration > (1000 * 60 * 1.5) && duration < (1000 * 60 * 3))
                    R.drawable.ic_round_replay_10_24
                else
                    R.drawable.ic_round_replay_30_24
            ), "Skip Backward",
            MediaButtonReceiver.buildMediaButtonPendingIntent(
                this,
                PlaybackStateCompat.ACTION_REWIND
            )
        ).build()

        val forward = Notification.Action.Builder(
            Icon.createWithResource(
                this,
                if (duration < (1000 * 60 * 1.5))
                    R.drawable.ic_round_forward_5_24
                else if (duration > (1000 * 60 * 1.5) && duration < (1000 * 60 * 3))
                    R.drawable.ic_round_forward_10_24
                else
                    R.drawable.ic_round_forward_30_24
            ), "Skip Forward",
            MediaButtonReceiver.buildMediaButtonPendingIntent(
                this,
                PlaybackStateCompat.ACTION_FAST_FORWARD
            )
        ).build()

        return Pair(replay, forward)
    }

    private fun getNotificationIntent(): PendingIntent {
        val openActivityIntent = Intent(
            this,
            MainActivity::class.java
        )
        openActivityIntent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        return PendingIntent.getActivity(
            this@PlaybackService, 0, openActivityIntent,
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun initializeNotification(
        mediaDescription: MediaDescriptionCompat,
        bitmap: Bitmap?
    ) {

        val notificationIntent = getNotificationIntent()
        // 3
        val (pauseAction, playAction) = getPausePlayActions()
        val (replayAction, forwardAction) = getReplayForwardActions()
        val (prevAction, nextAction) = getSkipActions()
        // 4
        val notification = Notification.Builder(
            this@PlaybackService, channelId
        )

        notification
            .setStyle(
                Notification.MediaStyle()
                    .setMediaSession(mMediaSession!!.sessionToken.token as MediaSession.Token?)
                    .also {
                        if (mMediaSession!!.controller.playbackState.state != PlaybackStateCompat.STATE_STOPPED)
                            it.setShowActionsInCompactView(0, 1, 2)
                        else
                            it.setShowActionsInCompactView(0)
                    }
            )
            .setContentTitle(mediaDescription.title)
            .setContentText(mediaDescription.subtitle)
            .apply {
                if (bitmap != null)
                    setLargeIcon(bitmap)
            }
            .setContentIntent(notificationIntent)
            .setDeleteIntent(
                MediaButtonReceiver.buildMediaButtonPendingIntent
                    (this, PlaybackStateCompat.ACTION_STOP)
            )
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setSmallIcon(R.drawable.ic_notification)
            .also {
                if (mMediaSession!!.controller.playbackState.state != PlaybackStateCompat.STATE_STOPPED)
                {
                    if (playbackList == null)
                        it.addAction(replayAction)
                    else
                        it.addAction(prevAction)
                }
                it.addAction(
                    when (mMediaSession!!.controller.playbackState.state) {
                        PlaybackStateCompat.STATE_PLAYING -> pauseAction
                        else -> playAction
                    }
                )
                if (mMediaSession!!.controller.playbackState.state != PlaybackStateCompat.STATE_STOPPED)
                {
                    if (playbackList == null)
                        it.addAction(forwardAction)
                    else
                        it.addAction(nextAction)
                }

//                if (mediaExtras?.containsKey("CUSTOM_KEY_COLOR") == true)
//                {
//                    it.setColorized(true)
//                    it.setColor(mediaExtras?.getInt("CUSTOM_KEY_COLOR")?: Color.GRAY)
//                }
            }

        notificationBuilder = notification
    }

    private fun displayNotification() {
        // 1
        if (mMediaSession == null)
            return
        if (mMediaSession!!.controller.metadata == null) {
            return
        }

        // 3
        val mediaDescription =
            mMediaSession!!.controller.metadata.description
        // 4
//        GlobalScope.launch {
//            // 5


        val bitmap = mediaExtras!!.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI).let {
            if (it != null && it != "null" &&
                Build.VERSION.SDK_INT >= 28
            ) {
                val uri = Uri.parse(it)
                ImageDecoder.decodeBitmap(
                    ImageDecoder.createSource(
                        File(uri.path)
                    ),

                )
            }
            else null
//                BitmapFactory.decodeResource(resources, R.drawable.ic_notification)
        }
        Log.i("NotificationBuilder", "${bitmap?.width}, ${bitmap?.height}")
        // 7
        initializeNotification(
            mediaDescription,
            bitmap
        )
        // 8
        ContextCompat.startForegroundService(
            this@PlaybackService,
            Intent(
                this@PlaybackService,
                PlaybackService::class.java
            )
        )
        // 9
        startForeground(
            notificationId,
            notificationBuilder!!.build()
        )
//        }
    }

}