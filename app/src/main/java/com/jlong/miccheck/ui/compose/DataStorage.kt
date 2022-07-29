package com.jlong.miccheck.ui.compose

import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.toMutableStateList
import com.jlong.miccheck.*
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.*
import java.time.LocalDateTime

const val starredGroupUUID = "starredGroupReserved"

fun createStarredGroup() = RecordingGroup(
    "Starred Recordings",
    null,
    starredGroupUUID
)

fun MainActivity.serializeAndSaveData() {
    val packagedData = Json.encodeToString(
        PackagedData.serializer(), PackagedData(
            recordingsData = viewModel.recordingsData.toList() as List<VersionedRecordingData>,
            tags = viewModel.tags.toList() as List<VersionedTag>,
            groups = viewModel.groups.toList() as List<VersionedRecordingGroup>
        )
    )
    val settings = Json.encodeToString(
        UserAndSettings.serializer(), viewModel.settings
    )

    val stats = Json.encodeToString(
        AppStats.serializer(), viewModel.stats
    )

    val dataFile = File(applicationContext.filesDir, "MicCheckAppData.json")
    if (!dataFile.exists()) dataFile.createNewFile()
    dataFile.writeText(packagedData)

    val settingsFile = File(applicationContext.filesDir, "MicCheckSettings.json")
    if (!settingsFile.exists()) settingsFile.createNewFile()
    settingsFile.writeText(settings)

    val statsFile = File(applicationContext.filesDir, "MicCheckStats.json")
    if (!statsFile.exists()) statsFile.createNewFile()
    statsFile.writeText(stats)
}

fun MainActivity.loadData() {
    val statsFile = File(applicationContext.filesDir, "MicCheckStats.json")
    if (statsFile.exists()) {
        val stats = try {
            Json.decodeFromString(AppStats.serializer(), statsFile.readText())
        } catch (e: SerializationException) {
            Log.e("LoadData", e.stackTraceToString())
            AppStats()
        }
        viewModel.stats = stats.copy(
            appLaunches = stats.appLaunches + 1
        )
    }

    val settingsFile = File(applicationContext.filesDir, "MicCheckSettings.json")
    if (settingsFile.exists()) {
        val settings = try {
            Json.decodeFromString(UserAndSettings.serializer(), settingsFile.readText())
        } catch (e: SerializationException) {
            Log.e("LoadData", e.stackTraceToString())
            UserAndSettings()
        }
        viewModel.settings = settings
    }

    val dataFile = File(applicationContext.filesDir, "MicCheckAppData.json")
    if (!dataFile.exists()) {
        viewModel.groups.add(createStarredGroup())
        return
    }
    val packagedData = dataFile.readText()
    val unpackedData: PackagedData =
        try {
            Json.decodeFromString(PackagedData.serializer(), packagedData)
        } catch (e: SerializationException) {
            Log.e("LoadData", e.stackTraceToString())
            PackagedData(
                listOf(),
                listOf(),
                listOf()
            )
        }
    val currentVersionTags = unpackedData.tags.let { tags ->
        val list = mutableListOf<Tag>()
        tags.forEach {
            list.add(it.toLatestVersion())
        }
        list
    }
    val currentVersionGroups = unpackedData.groups.let { groups ->
        val list = mutableListOf<RecordingGroup>()
        groups.forEach {
            list.add(it.toLatestVersion())
        }
        list
    }
    val currentVersionRecordingData = unpackedData.recordingsData.let { recData ->
        val list = mutableListOf<RecordingData>()
        recData.forEach { rec ->
            when (rec) {
                is VersionedRecordingData.V4 -> {
                    rec.groupUUIDS.forEach { uuid ->
                        currentVersionGroups.find {
                            it.uuid == uuid
                        }?.let {
                            it.recordings += rec.recordingUri
                        }
                    }
                }
                is VersionedRecordingData.V5 -> {
                    rec.groupUUIDS.forEach { uuid ->
                        currentVersionGroups.find {
                            it.uuid == uuid
                        }?.let {
                            it.recordings += rec.recordingUri
                        }
                    }
                }
                is VersionedRecordingData.V6 -> {
                    rec.groupUUIDS.forEach { uuid ->
                        currentVersionGroups.find {
                            it.uuid == uuid
                        }?.let {
                            it.recordings += rec.recordingUri
                        }
                    }
                }
                else -> Unit
            }
            list.add(rec.toLatestVersion())
        }
        list
    }

    viewModel.tags = currentVersionTags.toMutableStateList()
    viewModel.recordingsData = currentVersionRecordingData.toMutableStateList()
    viewModel.groups = currentVersionGroups.toMutableStateList()

    verifyData()
}

private fun MainActivity.verifyData() {
    var modified = false

//    modified = modified or viewModel.recordingsData.removeIf { recData ->
//        viewModel.recordings.find {
//            it.uri.toString() == recData.recordingUri
//        } == null
//    }

    viewModel.recordingsData.forEach { recordingData ->
        recordingData.clipParentUri?.let { uri ->
            if (viewModel.recordingsData.find {it.recordingUri == uri} == null) {
                recordingData.clipParentUri = null
                modified = true
            }

        }
    }

    modified = modified or viewModel.tags.removeIf { tag ->
        if (viewModel.recordingsData.find { recData ->
            recData.tags.find {
                Log.i("VERIFY", "Reviewed tag $it")
                tag.name == it.name
            } != null
        } == null) {
            Log.i("VERIFY", "Removed tag $tag")
            true
        }
        else
            false
    }

    viewModel.groups.forEach {
        it.imgUri?.let { uri ->
            val file = File(applicationContext.filesDir, Uri.parse(uri).lastPathSegment ?: return@let)
            if (!file.exists())
            {
                val index = viewModel.groups.indexOf(it)
                viewModel.groups[index].imgUri = null
                modified = true
            }
        }

        it.recordings.forEach { recordingUri ->
            if (it.recordings.count {it == recordingUri} > 1)
                it.recordings = it.recordings.toMutableList().apply { remove(recordingUri) }
        }
    }

    if (viewModel.groups.find {  it.uuid == starredGroupUUID } == null)
        viewModel.groups.add(createStarredGroup())

    viewModel.settings = viewModel.settings.copy(lastLaunchVersion = currentMicCheckVersion)

    if (modified)
        serializeAndSaveData()
}

fun MainActivity.startExportData() {
    createFile("${LocalDateTime.now()}.json") {
        exportData(it)
    }
}

private fun MainActivity.exportData(uri: Uri) {
    try {
        val dataFile = File(applicationContext.filesDir, "MicCheckAppData.json")
        val inputStream = FileInputStream(dataFile)
        val outputStream = contentResolver.openFileDescriptor(uri, "w")?.let {
            val oS = FileOutputStream(it.fileDescriptor)
            oS
        }
        outputStream?.write(inputStream.readBytes())
        inputStream.close()
        outputStream?.close()
        Toast.makeText(this, "Data exported!", Toast.LENGTH_SHORT).show()
    } catch (e: FileNotFoundException) {
        e.printStackTrace()
    } catch (e: IOException) {
        e.printStackTrace()
    }
}


fun MainActivity.startImportData() {
    pickFile {
        importData(it)
    }
}

private fun MainActivity.importData (uri: Uri) {
    val stringBuilder = StringBuilder()
    try {
        contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                var line: String? = reader.readLine()
                while (line != null) {
                    stringBuilder.append(line)
                    line = reader.readLine()
                }
            }
        }
    } catch (e: FileNotFoundException) {
        e.printStackTrace()
    } catch (e: IOException) {
        e.printStackTrace()
    }

    val dataStr = stringBuilder.toString()
    val unpackedData: PackagedData =
        try {
            Json.decodeFromString(PackagedData.serializer(), dataStr)
        } catch (e: SerializationException) {
            Toast.makeText(this, "Failed to import data. Read error.", Toast.LENGTH_LONG).show()
            e.printStackTrace()
            return
        }

    unpackedData.tags.forEach { tag ->
        tag.toLatestVersion().also { updatedTag ->
            if (viewModel.tags.find { it.name == updatedTag.name } == null)
                viewModel.tags.add(updatedTag)
        }
    }

    unpackedData.groups.forEach { group ->
        group.toLatestVersion().also { updatedGroup ->
            if (viewModel.groups.find { it.uuid == updatedGroup.uuid } != null) {
                viewModel.groups.remove(viewModel.groups.find { it.uuid == updatedGroup.uuid })
            }
            viewModel.groups.add(updatedGroup)
        }
    }

    unpackedData.recordingsData.forEach { recData ->
        recData.toLatestVersion().also { updatedRecData ->
            if (viewModel.recordingsData.find { it.recordingUri == updatedRecData.recordingUri } != null)
                viewModel.recordingsData.remove(viewModel.recordingsData.find { it.recordingUri == updatedRecData.recordingUri })
            viewModel.recordingsData.add(updatedRecData)
        }

    }

    verifyData()
    serializeAndSaveData()

}