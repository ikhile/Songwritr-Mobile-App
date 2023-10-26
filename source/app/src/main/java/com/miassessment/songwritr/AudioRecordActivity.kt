package com.miassessment.songwritr

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import java.io.File
import java.time.LocalDateTime

// I learned how to use the Media Recorder using the Android Developers Reference
// https://developer.android.com/guide/topics/media/mediarecorder
// https://developer.android.com/reference/kotlin/android/media/MediaRecorder

class AudioRecordActivity : AppCompatActivity() {
    private lateinit var defaultDisplayName: String
    private lateinit var fileName: String
    private lateinit var filePath: String
    private lateinit var audioNameV: EditText
    private lateinit var pauseBtn: ImageButton
    private lateinit var recordIndicator: TextView
    private lateinit var created: LocalDateTime
    private lateinit var recorder: MediaRecorder
    private var permissions = arrayOf(Manifest.permission.RECORD_AUDIO)
    private var permissionGranted = false
    private var isRecording = false
    private var isPaused = false
    private var attachToNoteID = -1
    private var cancel = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_audio_record)

        created = LocalDateTime.now()
        val directory = File("${getExternalFilesDir(null)}/AudioRecordings/")
        if (!directory.isDirectory) directory.mkdirs() // src https://www.techiedelight.com/check-if-directory-exists-kotlin/ and https://stackoverflow.com/a/44425281
        val fileExtension = "aac"
        fileName = "AudioRecording-$created"
        filePath = "${directory.absolutePath}/$fileName"//.$fileExtension" //.replace(".", "_")
        defaultDisplayName = LocalDateTime.now().format(patternWithOrdinals("'Audio recorded' ddd MMMM yyyy 'at' HH:mm", LocalDateTime.now()))

        audioNameV = findViewById(R.id.audioName)
        audioNameV.hint = defaultDisplayName

        // get ID of note to attach audio to
        attachToNoteID = intent.getIntExtra(getString(R.string.intent_note_id_key), -1)

        pauseBtn = findViewById(R.id.playPauseBtn)
        // pause/resume functions require minimum API level 24
        if (Build.VERSION.SDK_INT < 24) pauseBtn.visibility = View.GONE

        recorder = MediaRecorder()
        recordIndicator = findViewById(R.id.recordIndicator)

        // PERMISSIONS
        // To check and request permissions I followed the following tutorial: https://www.youtube.com/watch?v=4Q89J0FsjgI
        permissionGranted = ActivityCompat.checkSelfPermission(this, permissions[0]) == PackageManager.PERMISSION_GRANTED
        if (permissionGranted) startRecording() // start recording if has permission, otherwise start when permission received
        else ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE)

        // TOOLBAR
        findViewById<ImageButton>(R.id.toolbarHome).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }


    override fun onStop(){
        super.onStop()
        // pause recorder if pausable
        if (Build.VERSION.SDK_INT >= 24) {
            if (isRecording && !isPaused) pause()

        // save and exit recorder if not pausable
        } else onSave(pauseBtn)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isRecording && !cancel) onSave(pauseBtn)
    }

    // To check and request permissions I followed the following tutorial: https://www.youtube.com/watch?v=4Q89J0FsjgI
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE) permissionGranted = grantResults[0] == PackageManager.PERMISSION_GRANTED

        if (permissionGranted) startRecording()
        else {
            finish()
        }
    }

    private fun startRecording() {
        if (!permissionGranted) {
            ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE)
            return
        }

        if (permissionGranted) { // redundant but just in case
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            recorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT)
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT)
            recorder.setOutputFile(filePath)
            recorder.prepare()
            recorder.start()

            isRecording = true
            recordIndicator.text = "Recording"

            if (Build.VERSION.SDK_INT >= 24) {
                pauseBtn.visibility = View.VISIBLE
                pauseBtn.setImageResource(R.drawable.ic_baseline_pause_24)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun onPauseResume(view: View) {
        if (isRecording) {
            if (isPaused) resume()
            else pause()
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun resume() {
        recorder.resume()
        pauseBtn.setImageResource(R.drawable.ic_baseline_pause_24)
        recordIndicator.text = "Recording"
        isPaused = false
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun pause() {
        recorder.pause()
        pauseBtn.setImageResource(R.drawable.ic_baseline_fiber_manual_record_24)
        recordIndicator.text = "Paused"
        isPaused = true
    }

    fun onSave(view: View) {
        if (!isRecording) return

        recorder.stop()
        isRecording = false

        // add to database
        val db = DBHelper(this, null)
        val newID = db.addAudio(if (audioNameV.text.isNotBlank()) audioNameV.text.toString() else defaultDisplayName, filePath)

        // attach to note if necessary
        if (attachToNoteID >= 0 && db.getNote(attachToNoteID)!!.count == 1) { // and statement checks if note exists
            db.attachAudioToNote(attachToNoteID, newID)
        }

        // return to previous activity
        finish() // finish prevents returning to the recording page when back button is pressed
        val intent = Intent(this, if (attachToNoteID >= 0) EditNoteActivity::class.java else AudioListActivity::class.java)

        if (attachToNoteID >= 0) intent.putExtra(getString(R.string.intent_note_id_key), attachToNoteID)
        intent.putExtra(getString(R.string.intent_show_audio_pane), attachToNoteID >= 0)
        startActivity(intent)
    }

    fun onCancel(view: View) {
        cancel = true
        if (File(filePath).isFile) File(filePath).delete()
        finish()
    }
}