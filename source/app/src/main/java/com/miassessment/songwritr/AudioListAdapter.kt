package com.miassessment.songwritr

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

// I learned how to use a Recycler View from the following tutorial:
// https://www.tutorialkart.com/kotlin-android/kotlin-android-recyclerview/
// any example code has been heavily adapted

internal class AudioListAdapter (private var audioList: MutableList<Audio>, private var audioInterface: AudioRecyclerInterface) : RecyclerView.Adapter<AudioListAdapter.AudioViewHolder>() {
    internal inner class AudioViewHolder(val view: View): RecyclerView.ViewHolder(view) {
        val cardView = view.findViewById<MaterialCardView>(R.id.cardView)
        val audioNameV = view.findViewById<TextView>(R.id.audioName)
        val audioDetails = view.findViewById<TextView>(R.id.audioDetails)
        val durationText = view.findViewById<TextView>(R.id.durationText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AudioListAdapter.AudioViewHolder {
        val itemV = LayoutInflater.from(parent.context).inflate(R.layout.audio_list_item, parent, false)
        return AudioViewHolder(itemV)
    }

    override fun onBindViewHolder(holder: AudioViewHolder, position: Int) {
        val audio = audioList[position]
        holder.audioNameV.text = audio.name
        holder.durationText.text = getAudioDuration(audio.path)
        holder.audioDetails.text = "${getAudioDuration(audio.path, true)}  -  ${audio.created.format(patternWithOrdinals("ddd MMM yy", audio.created))}"
        holder.cardView.setOnClickListener { audioInterface.onItemClick(holder.cardView, audio) }
    }

    override fun getItemCount(): Int {
        return audioList.size
    }
}