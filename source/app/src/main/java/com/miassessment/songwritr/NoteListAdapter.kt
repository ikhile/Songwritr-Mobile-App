package com.miassessment.songwritr

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// I learned how to use a Recycler View from the following tutorial:
// https://www.tutorialkart.com/kotlin-android/kotlin-android-recyclerview/
// any example code has been heavily adapted

internal class NoteListAdapter (private var noteList: MutableList<Note>, private var rvInterface: RecyclerViewInterface) : RecyclerView.Adapter<NoteListAdapter.NoteViewHolder>() {
    internal inner class NoteViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        var titleTextV = view.findViewById<TextView>(R.id.noteTitle)
        var snippetTextV = view.findViewById<TextView>(R.id.noteSnippet)
        var timestampTextV = view.findViewById<TextView>(R.id.noteTimestamp)
        var cardView = view.findViewById<MaterialCardView>(R.id.cardView)
        var starIcon = view.findViewById<ImageView>(R.id.starIcon)
        var deleteBtn = view.findViewById<ImageButton>(R.id.deleteBtn)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val itemV = LayoutInflater.from(parent.context).inflate(R.layout.notes_list_item, parent, false)
        return NoteViewHolder(itemV)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = noteList[position]

        if (note.title.isBlank()) holder.titleTextV.visibility = View.GONE
        else holder.titleTextV.text = note.title

        if (note.content.isBlank()) holder.snippetTextV.visibility = View.GONE
        else holder.snippetTextV.text = note.content

        holder.starIcon.visibility = if (note.favourited) View.VISIBLE else View.GONE
        holder.timestampTextV.text = "Edited ${formatTimestamp(note.updated)}"
        holder.cardView.translationZ = -1f
        holder.cardView.setOnClickListener { rvInterface.onItemClick(holder.cardView, note.id) }
        holder.deleteBtn.setOnClickListener { rvInterface.onItemDelete(holder.deleteBtn, note.id, position) }
    }

    override fun getItemCount(): Int {
        return noteList.size
    }

    private fun formatTimestamp(dateTime: LocalDateTime): String {
        // current date
        val today = LocalDate.now()
        val thisWeek = LocalDate.now().format(DateTimeFormatter.ofPattern("w"))

        // target date
        val date = dateTime.toLocalDate()
        val week = dateTime.format(DateTimeFormatter.ofPattern("w"))

        // construct timestamp
        var timestamp = if (week == thisWeek) {
                            when (date) {
                                today -> "" // "today"
                                today.minusDays(1) -> "yesterday"
                                else -> dateTime.format(DateTimeFormatter.ofPattern("EEEE"))
                            }
                        } else "${addOrdinals(date.dayOfMonth)} ${capitalize(date.month.toString())}"

        timestamp += if (date.year != today.year) " ${date.year}" else ""
        timestamp += " at ${dateTime.format(DateTimeFormatter.ofPattern("HH:mm"))}"

        return timestamp.trim()
    }

}