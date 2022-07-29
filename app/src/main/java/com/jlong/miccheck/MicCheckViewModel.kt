package com.jlong.miccheck

import android.net.Uri
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jlong.miccheck.ui.compose.SortType
import com.jlong.miccheck.ui.compose.starredGroupUUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class MicCheckViewModel : ViewModel() {
    lateinit var serializeAndSave: () -> Unit

    var isPro by mutableStateOf(false)

    var deniedPermissions = mutableStateListOf<String>()

    var settings by mutableStateOf(UserAndSettings())
    var stats by mutableStateOf(AppStats())

    var recordingState by mutableStateOf(RecordingState.WAITING)
    var currentRecordingUri: Uri? = null
    var recordTime by mutableStateOf(0L)

    var currentPlaybackState by mutableStateOf(PlaybackStateCompat.STATE_NONE)
    var currentPlaybackRec by mutableStateOf<Triple<Recording, RecordingData, RecordingGroup?>?>(null)
    var playbackProgress by mutableStateOf(0L)
    var isGroupPlayback by mutableStateOf(false)

    var recordings = mutableStateListOf<Recording>()
    var recordingsData = mutableStateListOf<RecordingData>()
    var tags = mutableStateListOf<Tag>()
    var groups = mutableStateListOf<RecordingGroup>()

    var currentRecordingInfoScreen by mutableStateOf<Triple<Recording, RecordingData, RecordingGroup?>?>(null)
    var currentGroupScreen by mutableStateOf<RecordingGroup?>(null)

    var showingGroupsList by mutableStateOf(false)

    var queuedTimestamps = mutableStateListOf<TimeStamp>()

    var loopMode by mutableStateOf(false)
    var loopRange by mutableStateOf(0f..1f)
    var playbackSpeed by mutableStateOf(1f)

    var selectedRecordings = mutableStateListOf<Recording>()

    var searchScreenInSelectMode by mutableStateOf(false)

    var currentSearchString by mutableStateOf("")

    var recordingsSortType by mutableStateOf(SortType.DateNewest)

    var ffmpegState by mutableStateOf(FFMPEGState.None)

    fun getRecording(uri: Uri) =
        recordings.find { it.uri == uri }

    fun getRecording(timeStamp: TimeStamp) =
        recordings.find {
            getRecordingData(it).timeStamps.contains(timeStamp)
        }

    fun getRecordingData(recording: Recording) =
        recordingsData.find {it.recordingUri == recording.uri.toString()} ?: RecordingData(
            "", listOf(), "", listOf()
        )

    fun getGroups(recording: Recording) =
        groups.filter { it.recordings.contains(recording.uri.toString()) }

    fun getGroup(uuid: String) = groups.find { it.uuid == uuid }

    fun getGroupRecordings(group: RecordingGroup) = recordings.filter {
        it.uri.toString() in group.recordings
    }

    fun addTagsToRecording(
        recordingData: RecordingData,
        tags: List<Tag>
    ) {
        tags.forEach { tag ->
            if (this.tags.find { it.name == tag.name } == null)
                this.tags.add(tag)
        }

        recordingData.tags = recordingData.tags + tags
        Log.i("addTags", recordingData.tags.toString())
        this.tags.forEach { tag -> if (tags.find{it.name == tag.name} != null) tag.useCount += 1}

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                serializeAndSave()
            }
        }
    }

    fun removeTagFromRecording(
        recordingData: RecordingData,
        tag: Tag
    ) {
        recordingData.tags = recordingData.tags - listOf(tag)

        this.tags.forEach {
            if (it.name == tag.name) it.useCount -= 1
        }

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                serializeAndSave()
            }
        }
    }

    fun timestampRecording(
        recording: Triple<Recording, RecordingData , RecordingGroup?>,
        timeMilli: Long,
        title: String,
        description: String
    ) {
        var uuid = UUID.randomUUID().toString()
        val timeStamps = mutableListOf<String>()
        recordingsData.forEach { data ->
            timeStamps += data.timeStamps.map { it.uuid }
        }
        while (timeStamps.contains(uuid)) uuid = UUID.randomUUID().toString()

        recording.second.timeStamps =
            recording.second.timeStamps + listOf(
                TimeStamp(
                    timeMilli,
                    title,
                    description,
                    uuid
                )
            )

        serializeAndSave()
    }

    fun queueTimestamp (
        timeMilli: Long,
        title: String,
        description: String
    ) {
        var uuid = UUID.randomUUID().toString()
        val timeStamps = mutableListOf<String>()
        recordingsData.forEach { data ->
            timeStamps += data.timeStamps.map { it.uuid }
        }
        while (timeStamps.contains(uuid)) uuid = UUID.randomUUID().toString()

        queuedTimestamps += TimeStamp(
            timeMilli,
            title,
            description,
            uuid
        )
    }

    fun editTimestamp(
        recording: Triple<Recording, RecordingData, RecordingGroup?>,
        timeStamp: TimeStamp,
        title: String,
        description: String
    ) {
        recording.second.timeStamps.find {it.uuid == timeStamp.uuid}!!.also {
            it.name = title
            it.description = description
        }

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                serializeAndSave()
            }
        }
    }

    fun deleteTimestamp(
        recording: Triple<Recording, RecordingData, RecordingGroup?>,
        timeStamp: TimeStamp
    ) {
        recording.second.timeStamps-=timeStamp

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                serializeAndSave()
            }
        }
    }

    fun onRecordingFinalized (title: String, description: String) {
        recordTime = 0L

        if (currentRecordingUri == null)
            return

        recordingsData.add(
            RecordingData(
                recordingUri = currentRecordingUri.toString(),
                description = description
            ).apply {
                queuedTimestamps.forEach {
                    timeStamps+=it
                }
            }
        )

        currentRecordingUri = null
        queuedTimestamps = mutableStateListOf()
        recordingState = RecordingState.WAITING

        viewModelScope.launch{
            withContext(Dispatchers.IO) {
                serializeAndSave()
            }
        }
    }

    fun onCancelRecording () {
        recordTime = 0L
        currentRecordingUri = null
        recordingState = RecordingState.WAITING

        queuedTimestamps = mutableStateListOf()
    }

    fun setCurrentPlayback(rec: Triple<Recording, RecordingData, RecordingGroup?>?) {
        currentPlaybackRec = rec
        Log.e("VM", rec?.first.toString())
    }

    fun onDeleteRecording (recording: Recording) {
        val data = getRecordingData(recording)
        val groups = getGroups(recording)

        if (recording == currentPlaybackRec?.first) {
            currentPlaybackState = PlaybackStateCompat.STATE_NONE
            setCurrentPlayback(null)
            playbackProgress = 0L
        }

        if (selectedRecordings.contains(recording))
            selectedRecordings.remove(recording)

        val recData = getRecordingData(recording)
        groups.forEach { group ->

            group.recordings -= recData.recordingUri
        }
        recordings.remove(recording)
        recordingsData.remove(data)

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                serializeAndSave()
            }
        }
    }

    fun createGroup(
        title: String,
        imageUri: Uri?
    ) : RecordingGroup {
        var uuid = UUID.randomUUID().toString()
        val existingUUIDs = groups.map { it.uuid }
        while (existingUUIDs.contains(uuid)) uuid = UUID.randomUUID().toString()

        groups += RecordingGroup(
            title,
            imageUri?.toString(),
            uuid
        )

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                serializeAndSave()
            }
        }

        return groups.find{it.uuid == uuid}!!
    }

    fun addRecordingToGroup (
        group: RecordingGroup,
        recording: Recording
    ) {
        if (recording.uri.toString() in group.recordings) return

//        groups.add(
//            groups.indexOf(group), groups.removeAt(groups.indexOf(group)).copy(
//                recordings = group.recordings + recording.uri.toString()
//            )
//        )

        groups.replace(group, group.copy(
            recordings = group.recordings + recording.uri.toString()
        ))

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                serializeAndSave()
            }
        }
    }

    fun editGroupName(
        name: String,
        group: RecordingGroup
    ) {
        Log.i("ViewModel", "Renaming group ${group.name} to $name")

        group.name = name

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                serializeAndSave()
            }
        }
    }

    fun setGroupImage(
        uri: Uri,
        group: RecordingGroup
    ) {
//        groups.add(groups.indexOf(group), groups.removeAt(groups.indexOf(group)).copy(imgUri = uri.toString()))
        groups.replace(
            group, group.copy(imgUri = uri.toString())
        )

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                serializeAndSave()
            }
        }
    }

    fun removeRecordingsFromGroup(
        recordings: List<Recording>,
        group: RecordingGroup
    ) {
        Log.i("ViewModel", "Removing recordings from group ${group.name}${recordings.fold("") { str, it -> str + "\n\t${it.name}" }}")

//        groups.add(
//            groups.indexOf(group), groups.removeAt(groups.indexOf(group)).copy(
//                recordings = group.recordings.filter { it !in recordings.map { it.uri.toString() } }
//            )
//        )
        groups.replace(
            group,
            group.copy(
                recordings = group.recordings.filter { it !in recordings.map { it.uri.toString() } }
            )
        )

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                serializeAndSave()
            }
        }
    }

    fun reorderGroup(
        recordings: List<String>,
        group: RecordingGroup
    ) {
        group.recordings = recordings.toList()

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                serializeAndSave()
            }
        }
    }

    fun addRecordingsToGroup(
        recordings: List<Recording>,
        group: RecordingGroup
    ) {
        val filteredList = recordings.map {
            it.uri.toString()
        }.filter {
            it !in group.recordings
        }

//        groups.add(groups.indexOf(group), groups.removeAt(groups.indexOf(group)).copy(
//            recordings = group.recordings + filteredList
//        ))
        groups.replace(
            group,
            group.copy(
                recordings = group.recordings + filteredList
            )
        )


        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                serializeAndSave()
            }
        }
    }

    fun deleteGroup (group: RecordingGroup) {
        groups.remove(group)

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                serializeAndSave()
            }
        }
    }

    fun addAttachmentToRecording (recording: Recording, attachment: Attachment) {

        val recordingData = getRecordingData(recording)
//        recordingsData.add(recordingsData.indexOf(recordingData), recordingsData.removeAt(recordingsData.indexOf(recordingData)).copy(
//            attachments = recordingData.attachments + attachment
//        ))
//
        recordingsData.replace(
            recordingData,
            recordingData.copy(
                attachments = recordingData.attachments + attachment
            )
        )

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                serializeAndSave()
            }
        }
    }

    fun deleteAttachmentToRecording (recording: Recording, attachment: Attachment) {
        val recordingData = getRecordingData(recording)

        recordingsData.replace(
            recordingData,
            recordingData.copy(
                attachments = recordingData.attachments - attachment
            )
        )

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                serializeAndSave()
            }
        }
    }

    fun editAttachmentToRecording(recording: Recording, attachment: Attachment, name: String) {
        val recordingData = getRecordingData(recording)

        recordingsData.replace(
            recordingData,
            recordingData.apply {
                attachments += attachment.copy(name = name)
                attachments -= attachment
            }
        )

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                serializeAndSave()
            }
        }
    }

    fun finishRecordingEdit (recording: Recording, description: String) {
        getRecordingData(recording).description = description

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                serializeAndSave()
            }
        }
    }

    fun clearSelectedRecordings () {
        selectedRecordings.removeAll(selectedRecordings)
    }

    fun starRecording(recording: Recording) {
        Log.e("VM - STAR", groups.find { it.uuid == starredGroupUUID }.toString())
        groups.find { it.uuid == starredGroupUUID }?.also {
            addRecordingToGroup(it, recording)
        }
    }

    fun unstarRecording(recording: Recording) {
        groups.find { it.uuid == starredGroupUUID }?.also {
            removeRecordingsFromGroup(listOf(recording), it)
        }
    }

    fun isRecordingStarred(recording: Recording) : Boolean = groups.find { it.uuid == starredGroupUUID }
        ?.recordings?.contains(recording.uri.toString())
        ?: false


    fun setTheme(option: ThemeOptions) {
        settings = settings.copy(
            theme = option
        )

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                serializeAndSave()
            }
        }
    }

    fun dismissExtra (id: DismissableExtraId) {
        if (settings.dismissedExtras.contains(id))
            return
        settings = settings.copy(
            dismissedExtras = settings.dismissedExtras + id
        )

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                serializeAndSave()
            }
        }
    }

    fun clearFirstLaunch () {
        settings = settings.copy(
            firstLaunch = !settings.firstLaunch,
            dismissedExtras = listOf()
        )
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                serializeAndSave()
            }
        }
    }

    fun setBitrate(bitrate: Int) {
        settings = settings.copy(
            encodingBitRate = bitrate
        )

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                serializeAndSave()
            }
        }
    }
}