package com.miassessment.songwritr

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import java.util.*

class EditNoteActivity : AppCompatActivity(), AudioRecyclerInterface {
    private lateinit var titleEditText: EditText
    private lateinit var bodyEditText: EditText
    private lateinit var favBtn: ImageButton
    private lateinit var promptLayout: ConstraintLayout
    private lateinit var promptImageV: ImageView
    private lateinit var promptTextV: TextView
    private lateinit var showHidePrompt: LinearLayout
    private lateinit var showHidePromptBtn: ImageButton
    private lateinit var audioLayout: LinearLayout
    private lateinit var showHideAudio: LinearLayout
    private lateinit var showHideAudioBtn: ImageButton
    private lateinit var currentNote: Note
    private var selectedText = ""

    private lateinit var recyclerView: RecyclerView
    private lateinit var layoutManager: LinearLayoutManager
    private lateinit var adapter: AudioListAdapter
    private var audioList = mutableListOf<Audio>()

    private var db = DBHelper(this, null)
    private var imagePromptLoaded = false // set to true when image loads; if failed to load will try again when prompt next opened

    // attach audio adapter
    private lateinit var closeAttachLayoutBtn: ImageButton
    private lateinit var recordNewBtn: ImageButton
    private lateinit var attachRecyclerView: RecyclerView
    private lateinit var attachLayoutManager: LinearLayoutManager
    private lateinit var attachAdapter: AttachAudioAdapter
    private var attachAudioList = mutableListOf<Audio>()

    // player
    private lateinit var currentTimeText: TextView
    private lateinit var mediaLayout: ConstraintLayout
    private lateinit var durationText: TextView
    private lateinit var playPauseBtn: ImageButton
    private lateinit var audioName: TextView
    private lateinit var seekbar: SeekBar
    private var audioNameMaxLength = 22
    private var lastRemovedAudio = -1

    private lateinit var currentAudio: Audio
    private lateinit var timer: Timer
    private var timerRunning = false
    private var player = MediaPlayer()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_note)

        favBtn = findViewById(R.id.favBtn)
        titleEditText = findViewById(R.id.titleEdit)
        bodyEditText = findViewById(R.id.bodyEdit)
        promptLayout = findViewById(R.id.promptLayout)
        promptImageV = findViewById(R.id.promptImageV)
        promptTextV = findViewById(R.id.promptTextV)
        showHidePrompt = findViewById(R.id.showHidePromptLayout)
        showHidePromptBtn = findViewById(R.id.showPromptBtn)
        audioLayout = findViewById(R.id.audioLayout)
        showHideAudio = findViewById(R.id.showHideAudioLayout)
        showHideAudioBtn = findViewById(R.id.showAudioBtn)

        var initNoteID = intent.getIntExtra(getString(R.string.intent_note_id_key), -1) // get note id from intent
        if (initNoteID == -1) { // if no ID on intent...
            // create new note
            initNoteID = db.addNote().toInt()

            // get prompt data from intent
            if (intent.getBooleanExtra(getString(R.string.intent_has_prompt_key), false)) {
                db.updateNote(initNoteID, DBHelper.NoteColumns.HAS_PROMPT, true)
                db.updateNote(initNoteID, DBHelper.NoteColumns.PROMPT_TYPE, intent.getStringExtra(getString(R.string.intent_prompt_type_key))!!.uppercase())
                db.updateNote(initNoteID, DBHelper.NoteColumns.PROMPT_DATA, intent.getStringExtra(getString(R.string.intent_prompt_data_key))!!)
                promptLayout.visibility = View.VISIBLE
            }
        }

        // create Note object from ID
        currentNote = Note(this, initNoteID)

        // pre-fill fields
        setFavBtnImg()
        titleEditText.setText(currentNote.title)
        bodyEditText.setText(currentNote.content)

        if (currentNote.hasPrompt) { loadPrompt(); showHidePrompt.visibility = View.VISIBLE }
        else { showHidePrompt.visibility = View.GONE }

        if (currentNote.hasAudio) {
            showHideAudio.visibility = View.VISIBLE
            audioList = db.cursorToAudioObjects(db.getAudiosFromIdList(currentNote.attachedAudio)!!)
            recyclerView = findViewById(R.id.audioRecyclerView)
            layoutManager = LinearLayoutManager(applicationContext)
            adapter = AudioListAdapter(audioList, this)
            recyclerView.layoutManager = layoutManager
            recyclerView.adapter = adapter

            currentTimeText = findViewById(R.id.currentTimeText)
            mediaLayout = findViewById(R.id.mediaPlayerLayout)
            audioName = findViewById(R.id.audioName)
            durationText = findViewById(R.id.durationText)
            playPauseBtn = findViewById(R.id.playPauseBtn)
            playPauseBtn.setImageResource(R.drawable.ic_baseline_play_arrow_24)

            currentAudio = audioList[0]
            durationText.text = getAudioDuration(currentAudio.path)
            audioName.text =
                if (currentAudio.name.length <= audioNameMaxLength) currentAudio.name
                else currentAudio.name.substring(0, audioNameMaxLength) + "..."

            // SEEKBAR
            seekbar = findViewById(R.id.seekbar)
            seekbar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    player.seekTo(progress)
                    currentTimeText.text = formatAudioDuration(player.currentPosition)
                }
                override fun onStartTrackingTouch(p0: SeekBar?) {}
                override fun onStopTrackingTouch(p0: SeekBar?) {}
            })

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

            setPlayer(autoplay = false)

            if (intent.getBooleanExtra(getString(R.string.intent_show_audio_pane), false)) audioLayout.visibility = View.VISIBLE

        } else { showHideAudio.visibility = View.GONE; audioLayout.visibility = View.GONE }

        showHidePromptBtn.setImageResource(if (promptLayout.visibility == View.VISIBLE) R.drawable.ic_baseline_remove_circle_outline_24 else R.drawable.ic_baseline_add_circle_outline_24)
        showHideAudioBtn.setImageResource(if (audioLayout.visibility == View.VISIBLE) R.drawable.ic_baseline_remove_circle_outline_24 else R.drawable.ic_baseline_add_circle_outline_24)


        // AUTO SAVE ON INPUT
        titleEditText.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                db.updateNote(currentNote.id, DBHelper.NoteColumns.TITLE, titleEditText.text.toString(), true)
            }
        })

        bodyEditText.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                db.updateNote(currentNote.id, DBHelper.NoteColumns.CONTENT, bodyEditText.text.toString(), true)
            }
        })


        // ATTACH AUDIO TO NOTE
        db = DBHelper(this, null)
        closeAttachLayoutBtn = findViewById(R.id.closeAttachLayoutBtn)
        recordNewBtn = findViewById(R.id.recordBtn)
        attachAudioList = db.cursorToAudioObjects(db.getAllAudio()!!)
        attachRecyclerView = findViewById(R.id.attachAudioRecycler)
        attachLayoutManager = LinearLayoutManager(applicationContext)
        attachAdapter = AttachAudioAdapter(attachAudioList, this)
        attachRecyclerView.layoutManager = attachLayoutManager
        attachRecyclerView.adapter = attachAdapter
        showHideAttachAudioPopUp(false)


        // TOOLBAR
        findViewById<ImageButton>(R.id.toolbarHome).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }


        // DICTIONARY MENU ITEM
        // Implemented using the Android Developers Reference: https://developer.android.com/reference/kotlin/android/widget/TextView?hl=en#setcustomselectionactionmodecallback
        val goToDict = findViewById<LinearLayout>(R.id.goToDictGroup)
        val callback = object : ActionMode.Callback {
            override fun onCreateActionMode(actionMode: ActionMode?, menu: Menu?): Boolean {
                // add item to nav bar
                goToDict.visibility = View.VISIBLE
                selectedText =
                    if (titleEditText.hasSelection()) titleEditText.text.substring(titleEditText.selectionStart, titleEditText.selectionEnd)
                    else if (bodyEditText.hasSelection()) bodyEditText.text.substring(bodyEditText.selectionStart, bodyEditText.selectionEnd)
                    else "" // just in case

                // add item to proper menu
                val dictMenu = menu!!.add(Menu.NONE, Menu.NONE, 1, "Dictionary")
                dictMenu.setOnMenuItemClickListener {
                    goToDictPressed(bodyEditText)
                    true
                }
                return true
            }

            override fun onPrepareActionMode(actionMode: ActionMode?, menu: Menu?): Boolean { return false }
            override fun onActionItemClicked(actionMode: ActionMode?, item: MenuItem?): Boolean { return false }

            override fun onDestroyActionMode(actionMode: ActionMode?) {
                goToDict.visibility = View.GONE
            }
        }
        titleEditText.customSelectionActionModeCallback = callback
        bodyEditText.customSelectionActionModeCallback = callback
    }

    override fun onPause() {
        super.onPause()
        if (player.isPlaying) pause()
    }

    override fun onStop(){
        super.onStop()
        if (player.isPlaying) pause()
    }

    // use to play audio
    override fun onItemClick(view: View, audio: Audio) {
        currentAudio = audio
        setPlayer()
    }

    fun onPlayPause(view: View) { if (player.isPlaying) pause() else play() }

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

    // set a 1-second interval to update timestamp and seekbar position
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
        seekbar.progress = player.currentPosition
        currentTimeText.text = formatAudioDuration(player.currentPosition)

        if (timerRunning) { timer.cancel(); timerRunning = false }
        if (autoplay) play()
    }

    fun backBtnPressed(view: View) {
        finish()
        val intent = Intent(this, NoteMainActivity::class.java)
        startActivity(intent)
    }

    fun favBtnPressed(view: View) {
        currentNote.favourited = !currentNote.favourited
        db.updateNote(currentNote.id, DBHelper.NoteColumns.FAVOURITED, currentNote.favourited)
        setFavBtnImg()
    }

    private fun setFavBtnImg() {
        favBtn.setImageResource(if (currentNote.favourited) R.drawable.ic_baseline_star_24 else R.drawable.ic_baseline_star_outline_24)
    }

    fun showHidePrompt(view: View) {
        promptLayout.visibility = if (promptLayout.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        showHidePromptBtn.setImageResource(if (promptLayout.visibility == View.GONE) R.drawable.ic_baseline_add_circle_outline_24 else R.drawable.ic_baseline_remove_circle_outline_24)

        // if showing the prompt and the image hasn't loaded, attempt to load it again
        if (currentNote.promptType == "IMAGE" && promptLayout.visibility == View.VISIBLE && !imagePromptLoaded) loadImagePrompt()
    }

    // accepts a boolean to explicitly set (rather than toggle) visibility
    private fun showHidePrompt(setVisible: Boolean) {
        promptLayout.visibility = if (setVisible) View.VISIBLE else View.GONE
        showHidePromptBtn.setImageResource(if (setVisible) R.drawable.ic_baseline_remove_circle_outline_24 else R.drawable.ic_baseline_add_circle_outline_24)
        if (currentNote.promptType == "IMAGE" && setVisible && !imagePromptLoaded) loadImagePrompt()
    }

    private fun loadPrompt() {
        when (currentNote.promptType) {
            "IMAGE" -> loadImagePrompt()
            "WORDS" -> promptTextV.text = currentNote.promptData.split(",").joinToString(", ")
            "QUOTE" -> {
                if (Regex("q:.*,a:.*").containsMatchIn(currentNote.promptData)) {
                    val q = Regex("q:(.*),a:.*").find(currentNote.promptData)!!.groupValues[1]
                    val a = Regex("q:.*,a:(.*)").find(currentNote.promptData)!!.groupValues[1]
                    promptTextV.text = "\"$q\"- $a"
                }

                else promptTextV.text = "Quote and author have been incorrectly stored"
            }
        }
    }

    private fun loadImagePrompt() {
        // check format of prompt data
        if (!Regex("url:.*,attr:.*").containsMatchIn(currentNote.promptData)) {
            promptTextV.text = "Image URL and attribution have been incorrectly stored"

        } else {
            promptImageV.visibility = View.VISIBLE

            val imgUrl = Regex("url:(.*),attr:.*").find(currentNote.promptData)!!.groupValues[1]
            val imgAttr = Regex("url:.*,attr:(.*)").find(currentNote.promptData)!!.groupValues[1]

            Picasso.get()
                .load(imgUrl)
                .into(promptImageV, object : Callback {
                    // source for performing an operation on Picasso image load: https://stackoverflow.com/a/26548894
                    // NB: image takes time to load, so imagePromptLoaded does not update immediately
                    override fun onSuccess() { imagePromptLoaded = true }
                    override fun onError(e: Exception?) { imagePromptLoaded = false }
                })

            promptTextV.text = "\u00a9 $imgAttr"
        }
    }

    fun showHideAudio(view: View) {
        audioLayout.visibility = if (audioLayout.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        showHideAudioBtn.setImageResource(if (audioLayout.visibility == View.GONE) R.drawable.ic_baseline_add_circle_outline_24 else R.drawable.ic_baseline_remove_circle_outline_24)
    }

    fun deleteBtnPressed(view: View) {
        val dialogBuilder = AlertDialog.Builder(view.context)
        dialogBuilder.setMessage("Are you sure you want to delete this note?\nThis action can't be undone.")
            .setPositiveButton("OK") { dialog, btnId -> db.deleteNote(currentNote.id); finish() }
            .setNegativeButton("Cancel") { dialog, btnId -> dialog.cancel() }

        val alert = dialogBuilder.create()
        alert.setTitle("Delete Note")
        alert.show()
    }

    private fun showHideAttachAudioPopUp(show: Boolean, hideAfterDelay:Boolean = true, delay: Long = 15000) {
        attachRecyclerView.visibility = if (show) View.VISIBLE else View.GONE
        attachRecyclerView.layoutManager?.scrollToPosition(0)
        closeAttachLayoutBtn.visibility = if (show) View.VISIBLE else View.GONE

        if (show && hideAfterDelay) {
            // Sources for Handler delay: https://stackoverflow.com/a/28195667 & https://stackoverflow.com/a/63851895
            Handler(Looper.getMainLooper()).postDelayed({ showHideAttachAudioPopUp(false) }, delay)
        }
    }

    fun attachAudioPressed(view: View) { showHideAttachAudioPopUp(true) }

    fun hideAttachAudio(view: View) { showHideAttachAudioPopUp(false) }

    fun recordNewPressed(view: View) {
        val intent = Intent(this, AudioRecordActivity::class.java)
        intent.putExtra(getString(R.string.intent_note_id_key), currentNote.id)
        startActivity(intent)
    }

    // use to attach audio to the current note
    override fun attachAudio(view: View, audioID: Int) {
        db.attachAudioToNote(currentNote.id, audioID.toLong())

        // reload current activity
        finish()
        intent.putExtra(getString(R.string.intent_show_audio_pane), true)
        startActivity(intent)
    }

    fun audioEditPressed(view: View) {
        val intent = Intent(this, AudioListActivity::class.java)
        intent.putExtra(getString(R.string.intent_audio_id_key), currentAudio.id) // to show audio on list page
        startActivity(intent)
    }

    fun audioRemovePressed(view: View) {
        val dialogBuilder = AlertDialog.Builder(view.context)
        dialogBuilder.setMessage("Are you sure you want to remove the audio recording \"${currentAudio.name}\" from this note?\nThe recording will still be available in the AudioRecorder")
            .setPositiveButton("OK") { dialog, btnId ->
                pause()
                db.removeAudioFromNote(currentNote.id, currentAudio.id)
                lastRemovedAudio = currentAudio.id
                audioList.remove(currentAudio)
                adapter = AudioListAdapter(audioList, this)
                recyclerView.adapter = adapter
                adapter.notifyDataSetChanged()


                if (audioList.size > 0) {
                    currentAudio = audioList[0]
                    setPlayer(autoplay = false)
                    player.seekTo(0)
                    seekbar.progress = 0
                    currentTimeText.text = formatAudioDuration(0)
                } else {
                    audioLayout.visibility = View.GONE
                    showHideAudioBtn.visibility = View.GONE
                }
                Snackbar.make(view, "Audio \"${currentAudio.name}\" removed from note", Snackbar.LENGTH_LONG)
                        .setAction("UNDO", View.OnClickListener { onAudioRemoveUndo() })
                        .show()

            }
            .setNegativeButton("Cancel") { dialog, btnId -> dialog.cancel() }

        val alert = dialogBuilder.create()
        alert.setTitle("Remove recording from note")
        alert.show()
    }

    private fun onAudioRemoveUndo() {
        // reattach audio by ID and refresh
        db.attachAudioToNote(currentNote.id, lastRemovedAudio.toLong())
        finish()
        intent.putExtra(getString(R.string.intent_show_audio_pane), true)
        startActivity(intent)
    }

    fun goToDictPressed(view: View) {
        val intent =  Intent(this, DictionaryActivity::class.java)
        intent.putExtra(getString(R.string.intent_word_key), selectedText)
        startActivity(intent)
    }
}