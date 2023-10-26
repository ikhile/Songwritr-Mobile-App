package com.miassessment.songwritr

import android.content.Intent
import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import java.util.*

class AudioListActivity : AppCompatActivity(), AudioRecyclerInterface {
    private lateinit var recyclerView: RecyclerView
    private lateinit var layoutManager: LinearLayoutManager
    private lateinit var adapter: AudioListAdapter
    private var audioList = mutableListOf<Audio>()

    private lateinit var currentAudio: Audio
    private lateinit var lastDeleted: Audio
    private lateinit var editName: EditText
    private lateinit var updateNameBtn: Button
    private lateinit var favBtn: ImageButton
    private lateinit var playPauseBtn: ImageButton
    private lateinit var durationText: TextView
    private lateinit var currentTimeText: TextView
    private lateinit var noRecordingsText: TextView
    private lateinit var mediaLayout: LinearLayout
    private lateinit var seekbar: SeekBar
    private lateinit var db: DBHelper
    private lateinit var timer: Timer
    private var timerRunning = false
    private var player = MediaPlayer()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_audio_list)

        db = DBHelper(this, null)

        // RECYCLER VIEW
        audioList = db.cursorToAudioObjects(db.getAllAudio()!!)
        recyclerView = findViewById(R.id.recyclerView)
        layoutManager = LinearLayoutManager(applicationContext)
        adapter = AudioListAdapter(audioList, this)
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = adapter

        // PLAYER
        currentTimeText = findViewById(R.id.currentTimeText)
        noRecordingsText = findViewById(R.id.noRecordingsText)
        mediaLayout = findViewById(R.id.mediaPlayerLayout)
        editName = findViewById(R.id.audioNameEdit)
        updateNameBtn = findViewById(R.id.saveNameBtn)
        updateNameBtn.visibility = View.GONE
        favBtn = findViewById(R.id.favBtn)
        durationText = findViewById(R.id.durationText)
        playPauseBtn = findViewById(R.id.playPauseBtn)
        playPauseBtn.setImageResource(R.drawable.ic_baseline_play_arrow_24)

        if (audioList.size > 0) {
            val audioID = intent.getIntExtra(getString(R.string.intent_audio_id_key), -1)
            currentAudio = if (audioID < 0) audioList [0] else db.cursorToAudioObjects(db.getAudio(audioID)!!)[0]
            noRecordingsText.visibility = View.GONE
            editName.setText(currentAudio.name)
            favBtn.setImageResource(if (currentAudio.favourited) R.drawable.ic_baseline_star_24 else R.drawable.ic_baseline_star_outline_24)

        } else {
            noRecordingsText.visibility = View.VISIBLE
            mediaLayout.visibility = View.GONE
        }

        editName.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                if (updateNameBtn.visibility != View.VISIBLE && editName.text.toString() != currentAudio.name) {
                    updateNameBtn.visibility = View.VISIBLE
                }
            }
        })

        // SEEKBAR
        seekbar = findViewById(R.id.seekbar)
        seekbar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                player.seekTo(progress)
                currentTimeText.text = formatAudioDuration(player.currentPosition)
            }
            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {}
        })

        // PLAYER COMPLETION LISTENER
        // set once everything else initialised just in case
        player.setOnCompletionListener {
            if (timerRunning) { timer.cancel(); timerRunning = false }
            currentTimeText.text = formatAudioDuration(player.duration)
            seekbar.progress = seekbar.max

            // slight delay to let seekbar update
            Handler(Looper.getMainLooper()).postDelayed({
                player.reset()
                setPlayer(autoplay = false)
                playPauseBtn.setImageResource(R.drawable.ic_baseline_play_arrow_24)
            }, 500)
        }

        if (audioList.size > 0) setPlayer(autoplay = false)

        // TOOLBAR
        findViewById<ImageButton>(R.id.toolbarHome).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        audioList = db.cursorToAudioObjects(db.getAllAudio()!!)
        adapter = AudioListAdapter(audioList, this)
        recyclerView.adapter = adapter
        adapter.notifyDataSetChanged()

        if (audioList.size > 0) {
            val audioID = intent.getIntExtra(getString(R.string.intent_audio_id_key), -1)
            currentAudio = if (audioID < 0) audioList [0] else db.cursorToAudioObjects(db.getAudio(audioID)!!)[0]
            noRecordingsText.visibility = View.GONE
            mediaLayout.visibility = View.VISIBLE
            editName.setText(currentAudio.name)
            favBtn.setImageResource(if (currentAudio.favourited) R.drawable.ic_baseline_star_24 else R.drawable.ic_baseline_star_outline_24)

        } else {
            noRecordingsText.visibility = View.VISIBLE
            mediaLayout.visibility = View.GONE
        }
    }

    override fun onPause() {
        super.onPause()
        if (player.isPlaying) pause()
    }

    override fun onStop(){
        super.onStop()
        if (player.isPlaying) pause()
    }

    fun onRecordClick(view: View) {
//        finish()
        val intent = Intent(this, AudioRecordActivity::class.java)
        startActivity(intent)
    }

    override fun onItemClick(view: View, audio: Audio) {
        player.reset()
        currentAudio = audio
        setPlayer()
        updateNameBtn.visibility = View.GONE
        view.hideKeyboard()
        editName.clearFocus()
        favBtn.setImageResource(if (currentAudio.favourited) R.drawable.ic_baseline_star_24 else R.drawable.ic_baseline_star_outline_24)
    }

    override fun attachAudio(view: View, audioID: Int) {}

    fun onPlayPause(view: View) {
        if (player.isPlaying) pause() else play()
    }

    private fun play() {
        player.start()
        playPauseBtn.setImageResource(R.drawable.ic_baseline_pause_24)
        timer = Timer()
        setIntervalTimeLabel()
    }

    private fun pause() {
        player.pause()
        playPauseBtn.setImageResource(R.drawable.ic_baseline_play_arrow_24)
        if (timerRunning) { timer.cancel(); timerRunning = false }
    }

    private fun setIntervalTimeLabel() {
        if (timerRunning) { timer.cancel(); timerRunning = false }
        timer.scheduleAtFixedRate(object: TimerTask() {
            override fun run() {
                runOnUiThread {
                    if (player.isPlaying) {
                        currentTimeText.text = formatAudioDuration(player.currentPosition)
                        seekbar.progress = player.currentPosition
                    }
                }
            }
        }, 0, 1000)
        timerRunning = true
    }

    private fun setPlayer(autoplay: Boolean = true) {
        player.reset()
        player.setDataSource("file://${currentAudio.path}")
        player.prepare()
        durationText.text = formatAudioDuration(player.duration)
        seekbar.max = player.duration
        player.seekTo(0)
        seekbar.progress = 0
        currentTimeText.text = formatAudioDuration(0)

        editName.setText(currentAudio.name)
        favBtn.setImageResource(if (currentAudio.favourited) R.drawable.ic_baseline_star_24 else R.drawable.ic_baseline_star_outline_24)

        if (timerRunning) { timer.cancel(); timerRunning = false }
        if (autoplay) play()
    }

    fun updateAudioName(view: View) {
        db.updateAudioName(currentAudio.id, editName.text.toString())
        audioList = db.cursorToAudioObjects(db.getAllAudio()!!)
        adapter = AudioListAdapter(audioList, this)
        recyclerView.adapter = adapter
        adapter.notifyDataSetChanged()
        updateNameBtn.visibility = View.GONE
        view.hideKeyboard()
        editName.clearFocus()
    }

    fun favPressed(view: View) {
        currentAudio.favourited = !currentAudio.favourited
        db.updateAudioFav(currentAudio.id, currentAudio.favourited)

        favBtn.setImageResource(if (currentAudio.favourited) R.drawable.ic_baseline_star_24 else R.drawable.ic_baseline_star_outline_24)
    }

    fun deletePressed(view: View) {
        val dialogBuilder = AlertDialog.Builder(view.context)
        dialogBuilder.setMessage("Are you sure you want to delete this audio recording?")
            .setPositiveButton("OK") { dialog, btnId ->
                db.deleteAudio(currentAudio.id)
                lastDeleted = currentAudio
                audioList.remove(currentAudio)
                recyclerView.adapter = adapter
                adapter.notifyDataSetChanged()
                setPlayer(autoplay = false)

                if (audioList.size > 0) {
                    currentAudio = audioList[0]
                    noRecordingsText.visibility = View.GONE
                    editName.setText(currentAudio.name)
                    favBtn.setImageResource(if (currentAudio.favourited) R.drawable.ic_baseline_star_24 else R.drawable.ic_baseline_star_outline_24)

                } else {
                    noRecordingsText.visibility = View.VISIBLE
                    mediaLayout.visibility = View.GONE
                }

                Snackbar.make(view, "Recording deleted", Snackbar.LENGTH_LONG)
                        .setAction("UNDO") { onDeleteUndo() }
                        .show()
            }
            .setNegativeButton("Cancel") { dialog, btnId -> dialog.cancel() }

        val alert = dialogBuilder.create()
        alert.setTitle("Delete Audio Recording")
        alert.show()
    }

    private fun onDeleteUndo() {
        db.addAudioWithID(lastDeleted.id, lastDeleted.created, lastDeleted.name, lastDeleted.path, lastDeleted.favourited)
        audioList = db.cursorToAudioObjects(db.getAllAudio()!!)
        // works but doesn't show audio player if deleted only audio so may as well restart activity
//        adapter = AudioListAdapter(audioList, this)
//        recyclerView.adapter = adapter
//        adapter.notifyDataSetChanged()
        finish()
        startActivity(intent)
    }
}