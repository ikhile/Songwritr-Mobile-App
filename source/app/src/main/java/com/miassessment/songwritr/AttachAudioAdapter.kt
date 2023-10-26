package com.miassessment.songwritr

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView

// I learned how to use a Recycler View from the following tutorial:
// https://www.tutorialkart.com/kotlin-android/kotlin-android-recyclerview/
// any example code has been heavily adapted

internal class AttachAudioAdapter (private var audioList: MutableList<Audio>, private var audioInterface: AudioRecyclerInterface) : RecyclerView.Adapter<AttachAudioAdapter.AudioViewHolder>() {
    internal inner class AudioViewHolder(val view: View): RecyclerView.ViewHolder(view) {
        val audioName = view.findViewById<TextView>(R.id.audioName)
        val audioDuration = view.findViewById<TextView>(R.id.audioDuration)
        val audioDate = view.findViewById<TextView>(R.id.audioDate)
        val layout = view.findViewById<ConstraintLayout>(R.id.mainConstraintLayout)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AttachAudioAdapter.AudioViewHolder {
        val itemV = LayoutInflater.from(parent.context).inflate(R.layout.attach_audio_list_item, parent, false)
        return AudioViewHolder(itemV)
    }

    override fun onBindViewHolder(holder: AttachAudioAdapter.AudioViewHolder, position: Int) {
        val audio = audioList[position]
        holder.audioName.text = audio.name
        holder.audioDuration.text = getAudioDuration(audio.path, letters = true)
        holder.audioDate.text = audio.created.format(patternWithOrdinals("ddd MMM yy", audio.created))
        holder.audioName.setOnClickListener { audioInterface.attachAudio(holder.audioName, audio.id) }
    }

    override fun getItemCount(): Int {
        return audioList.size
    }
}