package com.kgmedia.fastfilm.core.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Tone")
class Tone(
    @PrimaryKey
    val toneId: String,
    val toneMark: String? = null,
    val toneEmoji: String? = null,
    val toneSort: Int? = null,
    var editStatus: Int? = null
) {

}