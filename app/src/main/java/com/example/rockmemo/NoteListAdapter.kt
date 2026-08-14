package com.example.rockmemo

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

class NoteListAdapter(context: Context, notes: MutableList<String>) : ArrayAdapter<String>(context, 0, notes) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.note_list_item, parent, false)
        val titleText = view.findViewById<TextView>(R.id.noteTitle)
        titleText.text = getItem(position)
        return view
    }
}
