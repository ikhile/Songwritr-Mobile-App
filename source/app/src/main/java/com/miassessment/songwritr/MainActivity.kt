package com.miassessment.songwritr

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun goToPrompts(view: View) {
        val intent = Intent(this, PromptsActivity::class.java)
        startActivity(intent)
    }

    fun goToDict(view: View) {
        val intent = Intent(this, DictionaryActivity::class.java)
        startActivity(intent)
    }

    fun goToNotes(view: View) {
        val intent = Intent(this, NoteMainActivity::class.java)
        startActivity(intent)
    }

    fun goToRecorder(view: View) {
        val intent = Intent(this, AudioListActivity::class.java)
        startActivity(intent)
    }
}