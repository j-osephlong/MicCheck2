package com.jlong.miccheck

import android.content.Intent
import android.net.Uri
import android.os.Parcelable
import java.io.File

fun importExternalFileFromIntent (intent: Intent) {
    (intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as? Uri)?.let { uri ->

    }
}