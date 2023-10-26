package com.miassessment.songwritr

import android.content.Context
import java.time.LocalDateTime

class Note(context: Context, val id: Int) { // only take context and ID, then use ID to get note from database
    var created: LocalDateTime
    var updated: LocalDateTime
    var title: String
    var content: String
    var hasPrompt: Boolean = false
    var promptType: String
    var promptData: String
    var hasAudio: Boolean = false
    var attachedAudio: MutableSet<Int>
    var favourited: Boolean = false

    init {
        val db = DBHelper(context, null)
        val cursor = db.getNote(id)

        created = getColumnDateTime(cursor, DBHelper.NoteColumns.CREATED)
        updated = getColumnDateTime(cursor, DBHelper.NoteColumns.UPDATED)
        title = getColumnString(cursor, DBHelper.NoteColumns.TITLE)
        content = getColumnString(cursor, DBHelper.NoteColumns.CONTENT)
        hasPrompt = getColumnBool(cursor, DBHelper.NoteColumns.HAS_PROMPT)
        promptType = getColumnString(cursor, DBHelper.NoteColumns.PROMPT_TYPE)
        promptData = getColumnString(cursor, DBHelper.NoteColumns.PROMPT_DATA)
        hasAudio = getColumnBool(cursor, DBHelper.NoteColumns.HAS_AUDIO)
        attachedAudio = splitAudioStringToSet(getColumnString(cursor, DBHelper.NoteColumns.AUDIO))
        favourited = getColumnBool(cursor, DBHelper.NoteColumns.FAVOURITED)

        db.close()
    }

    override fun toString(): String {
        var string = "ID: $id, Created: $created, Updated: $updated"

        if (title.isNotBlank()) string += ", Title: $title"
        if (content.isNotBlank()) string += ", Content: $content"

        string += ", Has Prompt? $hasPrompt"
        if (hasPrompt) string += ", Prompt Type: $promptType, Prompt Data: $promptData"

        string += ", Has Audio? $hasAudio"
        if (hasAudio) string += ", Attached Audio IDs: [${attachedAudio.joinToString(", ")}]"

        string += ", Favourited? $favourited"

        return string
    }

}