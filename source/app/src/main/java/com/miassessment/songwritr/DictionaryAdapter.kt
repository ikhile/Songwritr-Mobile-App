package com.miassessment.songwritr

import android.view.*
import android.view.ActionMode.Callback
import android.widget.TextView
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.RecyclerView

// I learned how to use a Recycler View from the following tutorial:
// https://www.tutorialkart.com/kotlin-android/kotlin-android-recyclerview/
// any example code has been heavily adapted

internal class DictionaryAdapter(private var itemList: MutableList<String>, private var dictInterface: DictInterface) : RecyclerView.Adapter<DictionaryAdapter.DictViewHolder>() {
    internal inner class DictViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var dictItemTextV = view.findViewById<TextView>(R.id.itemTextV)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DictViewHolder {
        val itemV = LayoutInflater.from(parent.context).inflate(R.layout.dict_item, parent, false)
        return DictViewHolder(itemV)
    }

    override fun onBindViewHolder(holder: DictViewHolder, position: Int) {
        val item = itemList[position]
        holder.dictItemTextV.text = HtmlCompat.fromHtml(item, 0) // using HTML to style text -> src https://www.tutorialspoint.com/how-to-display-html-in-textview-in-android

        holder.dictItemTextV.customSelectionActionModeCallback = object : Callback {
            override fun onCreateActionMode(actionMode: ActionMode?, menu: Menu?): Boolean {
                val dictMenu = menu!!.add(Menu.NONE, Menu.NONE, 0, "Search in dictionary")
                dictMenu.setOnMenuItemClickListener {
                    val v = holder.dictItemTextV
                    dictInterface.onSearchPressed(v.text.substring(v.selectionStart, v.selectionEnd))
                    true
                }
                return true
            }

            override fun onPrepareActionMode(p0: ActionMode?, p1: Menu?): Boolean { return false }
            override fun onActionItemClicked(p0: ActionMode?, p1: MenuItem?): Boolean { return false }
            override fun onDestroyActionMode(p0: ActionMode?) {}

        }
    }

    override fun getItemCount(): Int {
        return itemList.size
    }


}