package com.jlong.miccheck

import androidx.compose.runtime.snapshots.SnapshotStateList

fun <T> SnapshotStateList<T>.replace(item: T, newValue: T): Boolean {
    if (!this.contains(item)) return false
    this.add(
        this.indexOf(item),
        let {
            this.removeAt(this.indexOf(item))
            newValue
        }
    )
    return true
}