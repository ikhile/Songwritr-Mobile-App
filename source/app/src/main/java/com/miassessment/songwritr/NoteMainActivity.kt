package com.miassessment.songwritr

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar

class NoteMainActivity : AppCompatActivity(), RecyclerViewInterface {
    private lateinit var db: DBHelper
    private lateinit var lastDeleted: Note

    // RECYCLER VIEW
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: NoteListAdapter
    private lateinit var layoutManager: LinearLayoutManager
    private var noteList = mutableListOf<Note>()

    // SORTING
    private var sortNewestFirst = true
    private lateinit var sortBtn: ImageButton
    private lateinit var sortText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_note_main)

        sortBtn = findViewById(R.id.sortButton)
        sortText = findViewById(R.id.sortText)
        sortText.text = if (sortNewestFirst) getString(R.string.sort_newest_first) else getString(R.string.sort_oldest_first)

        // DATABASE
        db = DBHelper(this, null)
        db.deleteEmptyNotes()
        noteList = db.cursorToNoteObjects(db.getAllNotes(newestFirst = sortNewestFirst)!!)

        // RECYCLER VIEW
        recyclerView = findViewById(R.id.recyclerView)
        layoutManager = LinearLayoutManager(applicationContext)
        adapter = NoteListAdapter(noteList, this)
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = adapter

        // TOOLBAR
        findViewById<ImageButton>(R.id.toolbarHome).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        noteList = db.cursorToNoteObjects(db.getAllNotes(newestFirst = sortNewestFirst)!!)
        adapter = NoteListAdapter(noteList, this)
        recyclerView.adapter = adapter
        adapter.notifyDataSetChanged()
    }

    fun newNotePressed(view: View) {
        val intent = Intent(this, EditNoteActivity::class.java)
        startActivity(intent)
    }

    fun toggleSort(view: View) {
        // toggle boolean value
        sortNewestFirst = !sortNewestFirst

        // get notes from database and update recycler view
        noteList.clear()
        noteList.addAll(db.cursorToNoteObjects(db.getAllNotes(newestFirst = sortNewestFirst)!!))
        adapter = NoteListAdapter(noteList, this)
        recyclerView.adapter = adapter

        // change text and button image
        sortBtn.rotationX = if (sortNewestFirst) 0F else 180F
        sortText.text = if (sortNewestFirst) getString(R.string.sort_newest_first) else getString(R.string.sort_oldest_first)
    }

    override fun onItemClick(view: View, id: Int) {
        if (view.id == R.id.cardView) {
            val intent = Intent(this, EditNoteActivity::class.java)
            intent.putExtra(getString(R.string.intent_note_id_key), id)
            startActivity(intent)
        }
    }

    // I learned how to use an Alert Dialog from https://www.tutorialkart.com/kotlin-android/android-alert-dialog-example/
    override fun onItemDelete(view: View, id: Int, position: Int) {
        val dialogBuilder = AlertDialog.Builder(view.context)
        dialogBuilder.setMessage("Are you sure you want to delete this note?")
            .setPositiveButton("OK") { dialog, btnId ->
                db.deleteNote(id)
                lastDeleted = noteList.get(position)
                noteList.removeAt(position)
                adapter.notifyItemRemoved(position)
                Snackbar.make(view, "Note deleted", Snackbar.LENGTH_LONG)
                        .setAction("UNDO", View.OnClickListener {
                            onDeleteUndo(position)
                        })
                        .show()

            }
            .setNegativeButton("Cancel") { dialog, btnId ->
                dialog.cancel()
            }

        val alert = dialogBuilder.create()
        alert.setTitle("Delete Note")
        alert.show()
    }

    private fun onDeleteUndo(position: Int) {
        // add to list
        noteList.add(position, lastDeleted)

        // add back to database
        db.addNoteWithID(
            lastDeleted.id,
            lastDeleted.created,
            lastDeleted.updated,
            lastDeleted.title,
            lastDeleted.content,
            lastDeleted.hasPrompt,
            lastDeleted.promptType,
            lastDeleted.promptData,
            lastDeleted.hasAudio,
            lastDeleted.attachedAudio,
            lastDeleted.favourited,
        )

        // update recycler view
        adapter.notifyItemInserted(position)
    }
}