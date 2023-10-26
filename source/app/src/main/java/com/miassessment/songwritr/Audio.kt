package com.miassessment.songwritr

import android.content.Context
import java.io.File
import java.time.LocalDateTime

class Audio (context: Context, val id: Int) {
    var created: LocalDateTime
    var name: String
    var path: String
    var favourited = false
    var file: File

    init {
        val db = DBHelper(context, null)
        val cursor = db.getAudio(id)

        created = getColumnDateTime(cursor, DBHelper.NoteColumns.CREATED)
        name = getColumnString(cursor, DBHelper.AudioColumns.DISPLAY_NAME)
        path = getColumnString(cursor, DBHelper.AudioColumns.FILEPATH)
        favourited = getColumnBool(cursor, DBHelper.NoteColumns.FAVOURITED)
        file = File(path)

        db.close()
    }
}