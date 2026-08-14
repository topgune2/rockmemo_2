package com.example.rockmemo

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    private lateinit var notesRepository: NotesRepository
    private lateinit var versionText: TextView
    private lateinit var notesListView: ListView
    private lateinit var addNoteButton: FloatingActionButton
    private val noteEntries = mutableListOf<Note>()
    private lateinit var adapter: ArrayAdapter<String>

    companion object {
        const val EXTRA_NOTE_ID = "note_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        notesRepository = NotesRepository(this)
        versionText = findViewById(R.id.versionText)
        notesListView = findViewById(R.id.notesList)
        addNoteButton = findViewById(R.id.fabAddNote)

        val versionName = "1.1"
        versionText.text = "Version $versionName"

        adapter = ArrayAdapter(this, R.layout.note_list_item, mutableListOf())
        notesListView.adapter = adapter

        addNoteButton.setOnClickListener {
            showAddTitleDialog()
        }

        notesListView.setOnItemClickListener { _, _, position, _ ->
            val selected = noteEntries[position]
            val intent = Intent(this, NoteDetailActivity::class.java)
            intent.putExtra(EXTRA_NOTE_ID, selected.id)
            startActivity(intent)
        }

        notesListView.setOnItemLongClickListener { _, _, position, _ ->
            val selected = noteEntries[position]
            AlertDialog.Builder(this)
                .setTitle(selected.title.ifEmpty { "Note" })
                .setItems(arrayOf("Edit", "Delete", "Cancel")) { dialog, which ->
                    when (which) {
                        0 -> {
                            // Edit
                            val intent = Intent(this, NoteDetailActivity::class.java)
                            intent.putExtra(EXTRA_NOTE_ID, selected.id)
                            startActivity(intent)
                        }
                        1 -> {
                            // Confirm delete
                            AlertDialog.Builder(this)
                                .setTitle("Delete note")
                                .setMessage("Do you really want to delete this note?")
                                .setPositiveButton("Delete") { _, _ ->
                                    notesRepository.deleteNote(selected.id)
                                    renderNotes()
                                }
                                .setNegativeButton("Cancel", null)
                                .show()
                        }
                        else -> dialog.dismiss()
                    }
                }
                .show()
            true
        }

        showBiometricPromptIfNeeded()
    }

    override fun onResume() {
        super.onResume()
        renderNotes()
    }

    private fun showBiometricPromptIfNeeded() {
        val biometricManager = BiometricManager.from(this)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL

        when (biometricManager.canAuthenticate(authenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                val executor = ContextCompat.getMainExecutor(this)
                val promptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle(getString(R.string.unlock_title))
                    .setSubtitle(getString(R.string.unlock_subtitle))
                    .setAllowedAuthenticators(authenticators)
                    .build()

                val biometricPrompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        notesRepository.setUnlocked(true)
                        renderNotes()
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        Toast.makeText(this@MainActivity, "Authentication is unavailable or was rejected. $errString", Toast.LENGTH_LONG).show()
                        notesRepository.setUnlocked(true)
                        renderNotes()
                    }

                    override fun onAuthenticationFailed() {
                        Toast.makeText(this@MainActivity, "Authentication failed. Please try again.", Toast.LENGTH_SHORT).show()
                    }
                })

                biometricPrompt.authenticate(promptInfo)
            }
            else -> {
                Toast.makeText(
                    this,
                    "Biometric or device lock is not available. Continuing without a biometric gate.",
                    Toast.LENGTH_LONG
                ).show()
                notesRepository.setUnlocked(true)
                renderNotes()
            }
        }
    }

    private fun showAddTitleDialog() {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT
            setSingleLine(true)
        }

        AlertDialog.Builder(this)
            .setTitle("New Note")
            .setMessage("Enter a title for the note:")
            .setView(input)
            .setPositiveButton("Create") { _, _ ->
                val title = input.text.toString().trim()
                // create note with empty body and open editor
                notesRepository.addNote(title, "")
                // open the newly created note (it's the most recent)
                val created = notesRepository.loadNotes().firstOrNull()
                created?.let {
                    val intent = Intent(this, NoteDetailActivity::class.java)
                    intent.putExtra(EXTRA_NOTE_ID, it.id)
                    startActivity(intent)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun renderNotes() {
        noteEntries.clear()
        noteEntries.addAll(notesRepository.loadNotes())

        adapter.clear()
        val values = noteEntries.map { note ->
            if (note.title.isNotEmpty()) note.title else note.text.lineSequence().firstOrNull() ?: "(empty)"
        }
        adapter.addAll(values)
        adapter.notifyDataSetChanged()
    }
}
