package com.example.rockmemo

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity

class NoteDetailActivity : AppCompatActivity() {

    private lateinit var notesRepository: NotesRepository
    private lateinit var titleEdit: EditText
    private lateinit var contentEdit: EditText
    private var noteId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_note_detail)

        notesRepository = NotesRepository(this)
        titleEdit = findViewById(R.id.editTitle)
        contentEdit = findViewById(R.id.editContent)

        noteId = intent.getStringExtra(MainActivity.EXTRA_NOTE_ID)
        val note = noteId?.let { notesRepository.loadNotes().find { n -> n.id == it } }
        note?.let {
            titleEdit.setText(it.title)
            contentEdit.setText(it.text)
        }

        findViewById<Button>(R.id.btnSave).setOnClickListener {
            val t = titleEdit.text.toString().trim()
            val c = contentEdit.text.toString()
            if (noteId != null) {
                notesRepository.updateNote(noteId!!, t, c)
            } else {
                notesRepository.addNote(t, c)
            }
            finish()
        }
    }
}
