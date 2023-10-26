package com.miassessment.songwritr

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.time.LocalDateTime

// The class DBHelper heavily adapts code from the following database:
// https://www.geeksforgeeks.org/android-sqlite-database-in-kotlin/

class DBHelper(val context: Context, factory: SQLiteDatabase.CursorFactory?) :
    SQLiteOpenHelper(context, DATABASE_NAME, factory, DATABASE_VERSION) {

        override fun onCreate(db: SQLiteDatabase) {
            // CREATE
            val qNote = "CREATE TABLE ${NoteColumns.TABLE_NAME} (" +
                            "${NoteColumns.ID} INTEGER PRIMARY KEY, " +
                            "${NoteColumns.CREATED} TEXT NOT NULL, " +
                            "${NoteColumns.UPDATED} TEXT, " +
                            "${NoteColumns.TITLE} TEXT, " +
                            "${NoteColumns.CONTENT} TEXT, " +
                            "${NoteColumns.HAS_PROMPT} BOOLEAN, " +
                            "${NoteColumns.PROMPT_TYPE} TEXT, " +
                            "${NoteColumns.PROMPT_DATA} TEXT, " +
                            "${NoteColumns.HAS_AUDIO} BOOLEAN, " +
                            "${NoteColumns.AUDIO} TEXT, " +
                            "${NoteColumns.FAVOURITED} BOOLEAN, " +
                            "${NoteColumns.DELETED} BOOLEAN" +
                        ")"
            db.execSQL(qNote)

            // CREATE AUDIO TABLE
            val qAudio = "CREATE TABLE ${AudioColumns.TABLE_NAME} (" +
                            "${AudioColumns.ID} INTEGER PRIMARY KEY, " +
                            "${AudioColumns.CREATED} TEXT NOT NULL, " +
                            "${AudioColumns.DISPLAY_NAME} TEXT NOT NULL, " +
                            "${AudioColumns.FILEPATH} TEXT NOT NULL, " +
                            "${AudioColumns.FAVOURITED} BOOLEAN" +
                         ")"
            db.execSQL(qAudio)
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {

            /*if (oldVersion < 2) {
                db.execSQL("ALTER TABLE ${NoteColumns.TABLE_NAME} ADD ${NoteColumns.DELETED} BOOLEAN")
            }

            if (oldVersion < 3) {
                val qAudio = "CREATE TABLE ${AudioColumns.TABLE_NAME} (" +
                        "${AudioColumns.ID} INTEGER PRIMARY KEY, " +
                        "${AudioColumns.CREATED} TEXT NOT NULL, " +
                        "${AudioColumns.DISPLAY_NAME} TEXT NOT NULL, " +
                        "${AudioColumns.FILEPATH} TEXT NOT NULL" +
                        ")"
                db.execSQL(qAudio)
            }

            if (oldVersion < 4) {
                db.execSQL("ALTER TABLE ${AudioColumns.TABLE_NAME} ADD ${AudioColumns.FAVOURITED} BOOLEAN")
            }*/
        }

        companion object {
            const val DATABASE_NAME = "Songwritr_Database"
            const val DATABASE_VERSION = 1 // must be Int (not float) for SQLiteOpenHelper
        }

        object NoteColumns {
            const val TABLE_NAME = "notes"
            const val ID = "note_id"
            const val CREATED = "created"
            const val UPDATED = "updated"
            const val TITLE = "title"
            const val CONTENT = "content"
            const val HAS_PROMPT = "has_prompt"
            const val PROMPT_TYPE = "prompt_type"
            const val PROMPT_DATA = "prompt_data"
            const val HAS_AUDIO = "has_audio"
            const val AUDIO = "attached_audio"
            const val FAVOURITED = "favourited"
            const val DELETED = "deleted"
        }

        object AudioColumns {
            const val TABLE_NAME = "audio"
            const val ID = "id"
            const val CREATED = "created"
            const val DISPLAY_NAME = "display_name"
            const val FILEPATH = "filepath"
            const val FAVOURITED = "favourited"
        }

        // NOTES TABLE FUNCTIONS
        fun addNote(hasPrompt: Boolean = false, hasAudio: Boolean = false): Long {
            val values = ContentValues()
            values.put(NoteColumns.CREATED, LocalDateTime.now().toString())
            values.put(NoteColumns.UPDATED, LocalDateTime.now().toString())
            values.put(NoteColumns.HAS_PROMPT, hasPrompt)
            values.put(NoteColumns.HAS_AUDIO, hasAudio)
            values.put(NoteColumns.FAVOURITED, false)

            val db = this.writableDatabase
            return db.insert(NoteColumns.TABLE_NAME,null, values)
        }

        // use to re-add a note to database using preset values
        fun addNoteWithID(
            id: Int,
            created: LocalDateTime,
            updated: LocalDateTime,
            title: String,
            content: String,
            hasPrompt: Boolean,
            promptType: String,
            promptData: String,
            hasAudio: Boolean,
            attachedAudio: MutableSet<Int>,
            favourited: Boolean
        ) {
            val values = ContentValues()
            values.put(NoteColumns.ID, id)
            values.put(NoteColumns.CREATED, created.toString())
            values.put(NoteColumns.UPDATED, updated.toString())
            values.put(NoteColumns.TITLE, title)
            values.put(NoteColumns.CONTENT, content)
            values.put(NoteColumns.HAS_PROMPT, hasPrompt)
            values.put(NoteColumns.PROMPT_TYPE, promptType)
            values.put(NoteColumns.PROMPT_DATA, promptData)
            values.put(NoteColumns.HAS_AUDIO, hasAudio)
            values.put(NoteColumns.AUDIO, joinAudioSetToString(attachedAudio))
            values.put(NoteColumns.FAVOURITED, favourited)

            val db = this.writableDatabase
            db.insert(NoteColumns.TABLE_NAME, null, values)
        }

        // queries notes table to find all notes, ordered by favourited and in descending date order by default
        // returns a cursor containing all the notes in the table
        fun getAllNotes(newestFirst: Boolean = true): Cursor? {
            val q = "SELECT * " +
                    "FROM ${NoteColumns.TABLE_NAME} " +
                    "ORDER BY " +
                        "${NoteColumns.FAVOURITED} DESC, " +
                        "datetime(${NoteColumns.UPDATED}) " + if (newestFirst) "DESC" else "ASC"
            val db = this.readableDatabase
            return db.rawQuery(q, null)

            // diff between rawQuery & execSql https://stackoverflow.com/questions/9667031/difference-between-rawquery-and-execsql-in-android-sqlite-database
        }

        fun cursorToNoteObjects(cursor: Cursor): MutableList<Note> {
            val ids = mutableListOf<Int>()
            val list = mutableListOf<Note>()

            if (cursor.moveToFirst()) {
                ids.add(cursor.getInt(cursor.getColumnIndexOrThrow(NoteColumns.ID)))
                while (cursor.moveToNext()) ids.add(cursor.getInt(cursor.getColumnIndexOrThrow(NoteColumns.ID)))
                cursor.close()
            }

            for (id in ids) list.add(Note(context, id))
            return list
        }

        fun getNote(noteID: Int): Cursor? {
            val db = this.readableDatabase
            return db.rawQuery("SELECT * FROM ${NoteColumns.TABLE_NAME} WHERE ${NoteColumns.ID} = $noteID", null)
        }

        // for String data
        fun updateNote(noteID: Int, column: String, data: String, updateDate: Boolean = false) {
            val db = this.writableDatabase
            val values = ContentValues()
            if (updateDate) values.put(NoteColumns.UPDATED, LocalDateTime.now().toString())
            values.put(column, data)

            db.update(
                NoteColumns.TABLE_NAME,
                values,
                "${NoteColumns.ID} = $noteID",
                null
            )
        }

        // for Boolean data
        fun updateNote(noteID: Int, column: String, data: Boolean, updateDate: Boolean = false) {
            val db = this.writableDatabase
            val values = ContentValues()
            if (updateDate) values.put(NoteColumns.UPDATED, LocalDateTime.now().toString())
            values.put(column, data)

            db.update(NoteColumns.TABLE_NAME, values, "${NoteColumns.ID} = $noteID", null)
        }

        // for Int data
        fun updateNote(noteID: Int, column: String, data: Int, updateDate: Boolean = false) {
            val db = this.writableDatabase
            val values = ContentValues()
            if (updateDate) values.put(NoteColumns.UPDATED, LocalDateTime.now().toString())
            values.put(column, data)

            db.update(NoteColumns.TABLE_NAME, values, "${NoteColumns.ID} = $noteID", null)
        }

        fun attachAudioToNote(noteID: Int, audioID: Long) {
            val note = getNote(noteID)
            val audioSet = splitAudioStringToSet( getColumnString(note, NoteColumns.AUDIO) )
            audioSet.add(audioID.toInt())
            updateNote(noteID, NoteColumns.AUDIO, joinAudioSetToString(audioSet))
            updateNote(noteID, NoteColumns.HAS_AUDIO, true)
        }

        fun removeAudioFromNote(noteID: Int, audioID: Int) {
            val note = getNote(noteID)
            val audioSet = splitAudioStringToSet( getColumnString(note, NoteColumns.AUDIO) )
            audioSet.remove(audioID)
            updateNote(noteID, NoteColumns.AUDIO, joinAudioSetToString(audioSet))
            updateNote(noteID, NoteColumns.HAS_AUDIO, audioSet.size > 0)
        }

        fun deleteNote(noteID: Int): Int {
            val db = this.writableDatabase
            return db.delete(NoteColumns.TABLE_NAME, "${NoteColumns.ID} = $noteID", null)
        }

        // as clicking new note will create and save a note before any content (title, body) is added, this function will delete any notes without content and should be run before creating the list of notes on NoteMainActivity
        // source for checking empty strings and trim: two answers on https://stackoverflow.com/questions/16587761/.
        fun deleteEmptyNotes(): Int {
            val db = this.writableDatabase
            val q = "(${NoteColumns.TITLE} IS NULL OR TRIM(${NoteColumns.TITLE}) = '') AND " +
                    "(${NoteColumns.CONTENT} IS NULL OR TRIM(${NoteColumns.CONTENT}) = '') AND " +
                    "(${NoteColumns.PROMPT_TYPE} IS NULL OR TRIM(${NoteColumns.PROMPT_TYPE}) = '') AND " +
                    "(${NoteColumns.PROMPT_DATA} IS NULL OR TRIM(${NoteColumns.PROMPT_DATA}) = '') AND " +
                    "(${NoteColumns.AUDIO} IS NULL OR TRIM(${NoteColumns.AUDIO}) = '')"

            return db.delete(NoteColumns.TABLE_NAME, q, null)
        }

        // AUDIO TABLE FUNCTIONS

        // returns the ID of the new audio row
        fun addAudio(displayName: String, filepath: String): Long {
            val values = ContentValues()
            values.put(AudioColumns.CREATED, LocalDateTime.now().toString())
            values.put(AudioColumns.DISPLAY_NAME, displayName)
            values.put(AudioColumns.FILEPATH, filepath)
            values.put(AudioColumns.FAVOURITED, false)

            val db = this.writableDatabase
            return db.insert(AudioColumns.TABLE_NAME, null, values)
        }

        fun addAudioWithID(id: Int, created: LocalDateTime, displayName: String, filepath: String, favourited: Boolean): Long {
            val values = ContentValues()
            values.put(AudioColumns.ID, id)
            values.put(AudioColumns.CREATED, created.toString())
            values.put(AudioColumns.DISPLAY_NAME, displayName)
            values.put(AudioColumns.FILEPATH, filepath)
            values.put(AudioColumns.FAVOURITED, favourited)

            val db = this.writableDatabase
            return db.insert(AudioColumns.TABLE_NAME, null, values)
        }

        fun deleteAudio(audioID: Int): Int {
            val db = this.writableDatabase
            return db.delete(AudioColumns.TABLE_NAME, "${AudioColumns.ID} = $audioID", null)
        }

        fun getAudio(id: Int): Cursor? {
            val db = this.readableDatabase
            return db.rawQuery("SELECT * FROM ${AudioColumns.TABLE_NAME} WHERE ${AudioColumns.ID} = $id", null)
        }

        fun getAudiosFromIdList(ids: MutableSet<Int>): Cursor? {
            val db = this.readableDatabase
            val inStatement = "(${ids.joinToString(",")})"
            return db.rawQuery("SELECT * FROM ${AudioColumns.TABLE_NAME} WHERE ${AudioColumns.ID} IN $inStatement ORDER BY ${AudioColumns.CREATED} DESC", null)
        }

        fun getAllAudio(): Cursor? {
            val q = "SELECT * " +
                    "FROM ${AudioColumns.TABLE_NAME} " +
                    "ORDER BY " +
                    "${AudioColumns.FAVOURITED} DESC, " +
                    "${AudioColumns.CREATED} DESC"

            val db = this.readableDatabase
            return db.rawQuery(q, null)
        }

        fun cursorToAudioObjects(cursor: Cursor): MutableList<Audio> {
            val ids = mutableListOf<Int>()
            val list = mutableListOf<Audio>()

            if (cursor.moveToFirst()) {
                ids.add(cursor.getInt(cursor.getColumnIndexOrThrow(AudioColumns.ID)))
                while (cursor.moveToNext()) ids.add(cursor.getInt(cursor.getColumnIndexOrThrow(AudioColumns.ID)))
            }

            cursor.close()

            for (id in ids) list.add(Audio(context, id))
            return list
        }

        fun updateAudioName(id: Int, newName: String) {
            val db = this.writableDatabase
            val values = ContentValues()
            values.put(AudioColumns.DISPLAY_NAME, newName)

            db.update(AudioColumns.TABLE_NAME, values, "${AudioColumns.ID} = $id", null)
        }

        fun updateAudioFav(id: Int, booleanValue: Boolean) {
            val db = this.writableDatabase
            val values = ContentValues()
            values.put(AudioColumns.FAVOURITED, booleanValue)

            db.update(AudioColumns.TABLE_NAME, values, "${AudioColumns.ID} = $id", null)
        }
}