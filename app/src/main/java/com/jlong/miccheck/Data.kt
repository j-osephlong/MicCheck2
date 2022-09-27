package com.jlong.miccheck

import android.net.Uri
import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import java.util.*
import kotlin.collections.HashMap

enum class MicCheckVersion {
    V1, V2, V2_1
}

val currentMicCheckVersion = MicCheckVersion.V2_1

enum class ThemeOptions {
    Light, Dark, System
}

interface Searchable {
    val name: String
}

data class Recording(
    val uri: Uri,
    override var name: String,
    val duration: Int,
    // in bytes
    val size: Int,
    val sizeStr: String,
    val date: LocalDateTime = LocalDateTime.now(),
    val path: String
) : Searchable

@Serializable
sealed class VersionedRecordingData {
    /**
     * V3 - Clip parent URI
     */
    @Serializable
    data class V3(
        var recordingUri: String,
        var tags: List<Tag> = listOf(),
        var description: String = "",
        var groupUUID: String? = null,
        var groupOrderNumber: Int = -1,
        var timeStamps: List<TimeStamp> = listOf(),
        var clipParentUri: String? = null
    ) : VersionedRecordingData()
    /**
     * V4 - Multiple groups allowed
     */
    @Serializable
    data class V4(
        var recordingUri: String,
        var tags: List<Tag> = listOf(),
        var description: String = "",
        var groupUUIDS: List<String> = listOf(),
        var groupOrderNumber: Int = -1,
        var timeStamps: List<TimeStamp> = listOf(),
        var clipParentUri: String? = null
    ) : VersionedRecordingData()

    /**
     * V5/V6 - Multiple groups allowed,
     *          supporting group order for multiple groups
     *          groupOrderNumbers: Pair<Int (Order), String (Group UUID)>
     */
    @Serializable
    data class V5(
        var recordingUri: String,
        var tags: List<Tag> = listOf(),
        var description: String = "",
        var groupUUIDS: List<String> = listOf(),
        var groupOrderNumbers: MutableList<Pair<Int, String>> = mutableListOf(),
        var timeStamps: List<TimeStamp> = listOf(),
        var clipParentUri: String? = null
    ) : VersionedRecordingData()

    @Serializable
    data class V6(
        var recordingUri: String,
        var tags: List<Tag> = listOf(),
        var description: String = "",
        var groupUUIDS: List<String> = listOf(),
        var groupOrderNumbers: HashMap<String, Int> = hashMapOf(),
        var timeStamps: List<TimeStamp> = listOf(),
        var clipParentUri: String? = null
    ) : VersionedRecordingData()

    @Serializable
    data class V7(
        var recordingUri: String,
        var tags: List<Tag> = listOf(),
        var description: String = "",
        var timeStamps: List<TimeStamp> = listOf(),
        var clipParentUri: String? = null
    ) : VersionedRecordingData()

    @Serializable
    data class V8(
        var recordingUri: String,
        var tags: List<Tag> = listOf(),
        var description: String = "",
        var timeStamps: List<TimeStamp> = listOf(),
        var attachments: List<Attachment> = listOf(),
        var clipParentUri: String? = null
    ) : VersionedRecordingData()
}

fun VersionedRecordingData.toLatestVersion(): RecordingData = when (this) {
    is VersionedRecordingData.V3 -> this.let {
        RecordingData(
            recordingUri = it.recordingUri,
            tags = it.tags.let { oldTags ->
                val newTags = mutableListOf<Tag>()
                oldTags.forEach { tag -> newTags += tag.toLatestVersion() }
                newTags
            },
            description = it.description,
            timeStamps = it.timeStamps.let { oldStamps ->
                val newStamps = mutableListOf<TimeStamp>()
                oldStamps.forEach { stamp -> newStamps += stamp.toLatestVersion() }
                newStamps
            },
            clipParentUri = null
        )
    }
    is VersionedRecordingData.V4 -> this.let {
        RecordingData(
            recordingUri = it.recordingUri,
            tags = it.tags.let { oldTags ->
                val newTags = mutableListOf<Tag>()
                oldTags.forEach { tag -> newTags += tag.toLatestVersion() }
                newTags
            },
            description = it.description,
            timeStamps = it.timeStamps.let { oldStamps ->
                val newStamps = mutableListOf<TimeStamp>()
                oldStamps.forEach { stamp -> newStamps += stamp.toLatestVersion() }
                newStamps
            },
            clipParentUri = null
        )
    }
    is VersionedRecordingData.V5 -> this.let {
        RecordingData(
            recordingUri = it.recordingUri,
            tags = it.tags.let { oldTags ->
                val newTags = mutableListOf<Tag>()
                oldTags.forEach { tag -> newTags += tag.toLatestVersion() }
                newTags
            },
            description = it.description,
            timeStamps = it.timeStamps.let { oldStamps ->
                val newStamps = mutableListOf<TimeStamp>()
                oldStamps.forEach { stamp -> newStamps += stamp.toLatestVersion() }
                newStamps
            },
            clipParentUri = null
        )
    }
    is VersionedRecordingData.V6 -> this.let {
        RecordingData(
            recordingUri = it.recordingUri,
            tags = it.tags.let { oldTags ->
                val newTags = mutableListOf<Tag>()
                oldTags.forEach { tag -> newTags += tag.toLatestVersion() }
                newTags
            },
            description = it.description,
            timeStamps = it.timeStamps.let { oldStamps ->
                val newStamps = mutableListOf<TimeStamp>()
                oldStamps.forEach { stamp -> newStamps += stamp.toLatestVersion() }
                newStamps
            },
            clipParentUri = null
        )
    }
    is VersionedRecordingData.V7 -> this.let {
        RecordingData(
            recordingUri = it.recordingUri,
            tags = it.tags.let { oldTags ->
                val newTags = mutableListOf<Tag>()
                oldTags.forEach { tag -> newTags += tag.toLatestVersion() }
                newTags
            },
            description = it.description,
            timeStamps = it.timeStamps.let { oldStamps ->
                val newStamps = mutableListOf<TimeStamp>()
                oldStamps.forEach { stamp -> newStamps += stamp.toLatestVersion() }
                newStamps
            },
            clipParentUri = null,
            attachments = listOf()
        )
    }
    is VersionedRecordingData.V8 -> this
}

typealias RecordingData = VersionedRecordingData.V8

@Serializable
sealed class VersionedRecordingGroup {
    /**
     * V3 - Name is now mutable
     *    - Searchable
     */
    @Serializable
    data class V3(
        override var name: String,
        var imgUri: String? = null,
        var fallbackColor: Int = Color.White.toArgb(),
        val uuid: String,
    ) : VersionedRecordingGroup(), Searchable

    /**
     * V4 - Removed color for new design
     */
    @Serializable
    data class V4(
        override var name: String,
        var imgUri: String? = null,
        val uuid: String,
    ) : VersionedRecordingGroup(), Searchable

    /**
     * V5 - Moved recordings to group
     *      Hashmap represents group order
     */
    @Serializable
    data class V5(
        override var name: String,
        var imgUri: String? = null,
        val uuid: String,
        var recordings: Map<String, Int> = mapOf()
    ) : VersionedRecordingGroup(), Searchable

    /**
     * V6 - Moved recordings to group
     *      List represents group order
     */
    @Serializable
    data class V6(
        override var name: String,
        var imgUri: String? = null,
        val uuid: String,
        var recordings: List<String> = listOf()
    ) : VersionedRecordingGroup(), Searchable
}

fun VersionedRecordingGroup.toLatestVersion(): RecordingGroup = when (this) {
    is VersionedRecordingGroup.V3 -> RecordingGroup(
        this.name,
        this.imgUri,
        this.uuid,
        listOf()
    )

    is VersionedRecordingGroup.V4 -> RecordingGroup(
        this.name,
        this.imgUri,
        this.uuid,
        listOf()
    )
    is VersionedRecordingGroup.V5 -> RecordingGroup(
        this.name,
        this.imgUri,
        this.uuid,
        this.recordings.toList().map { it.first }
    )
    is VersionedRecordingGroup.V6 -> this
}

typealias RecordingGroup = VersionedRecordingGroup.V6

@Serializable
sealed class VersionedTag {
    @Serializable
    data class V1(
        var name: String,
    ) : VersionedTag()

    /**
     * V2 - Use count
     */
    @Serializable
    data class V2(
        var name: String,
        var useCount: Int = 0
    ) : VersionedTag()
}

fun VersionedTag.toLatestVersion(): Tag = when (this) {
    is VersionedTag.V1 -> VersionedTag.V2(this.name)
    is VersionedTag.V2 -> this
}

typealias Tag = VersionedTag.V2

@Serializable
sealed class VersionedTimeStamp {
    @Serializable
    data class V1(
        val timeMilli: Long,
        override var name: String,
        var recordingName: String,
        var recordingUri: String,
        var description: String? = null
    ) : Searchable, VersionedTimeStamp()

    /**
     * V2 - Made description non nullable
     *      Replaced recordingName and recordingUri with uuid
     */
    @Serializable
    data class V2(
        val timeMilli: Long,
        override var name: String,
        var description: String,
        val uuid: String
    ) : Searchable, VersionedTimeStamp()
}

fun VersionedTimeStamp.toLatestVersion(): TimeStamp = when (this) {
    is VersionedTimeStamp.V1 -> VersionedTimeStamp.V2(this.timeMilli, this.name, this.description?:"", UUID.randomUUID().toString())
    is VersionedTimeStamp.V2 -> this
}

typealias TimeStamp = VersionedTimeStamp.V2

@Serializable
data class PackagedData(
    val groups: List<VersionedRecordingGroup>,
    val tags: List<VersionedTag>,
    val recordingsData: List<VersionedRecordingData>
)

enum class DismissableExtraId {
    LoopUpdateV2, ExportToVideoV2, BitrateOptionsV2, TimestampUpdateV2, WhatsNewV2, ProV2_2
}

enum class OutputFormat {
    M4A, WAV
}

@Serializable
data class UserAndSettings (
    val firstLaunch: Boolean = true,
    val sampleRate: Int = 44100,
    val encodingBitRate: Int = 384000,
    val theme: ThemeOptions = ThemeOptions.System,
    val lastLaunchVersion: MicCheckVersion = MicCheckVersion.V1,
    val dismissedExtras: List<DismissableExtraId> = listOf(DismissableExtraId.ProV2_2),
    val preferredOutputFormat: OutputFormat = OutputFormat.M4A
)

@Serializable
sealed class VersionedStats {
    @Serializable
    data class V1(
        var appLaunches: Int = 0,
        var recordingsRecorded: Int = 0,
        var groupsCreated: Int = 0
    ): VersionedStats()
}

typealias AppStats = VersionedStats.V1

fun VersionedStats.toLatestVersion(): AppStats = when (this) {
    is VersionedStats.V1 -> this
}

@Serializable
sealed class VersionedAttachment {
    @Serializable
    data class V1(
        val attachmentUri: String,
        val name: String,
        val fileName: String,
        val mimeType: String
    ) : VersionedAttachment()
}

typealias Attachment = VersionedAttachment.V1

fun VersionedAttachment.toLatestVersion(): Attachment = when (this) {
    is VersionedAttachment.V1 -> this
}

fun Pair<Recording, RecordingGroup?>.toMetaData(): Bundle =
    Bundle().apply {
        putString(MediaMetadataCompat.METADATA_KEY_TITLE, this@toMetaData.first.name)
        putString(
            MediaMetadataCompat.METADATA_KEY_ALBUM,
            this@toMetaData.second?.name ?: "No Group"
        )
        putString(
            MediaMetadataCompat.METADATA_KEY_ARTIST,
            "Me" /*TODO*/
        )
        putLong(
            MediaMetadataCompat.METADATA_KEY_DURATION,
            this@toMetaData.first.duration.toLong()
        )
        if (this@toMetaData.second?.imgUri != null)
            putString(
                MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI,
                this@toMetaData.second!!.imgUri.toString()
            )
//        if (this@toMetaData.second != null)
//            putInt(
//                "CUSTOM_KEY_COLOR",
//                this@toMetaData.second!!.fallbackColor
//            )
    }