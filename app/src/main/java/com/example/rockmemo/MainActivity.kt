package com.example.rockmemo

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        notesRepository = NotesRepository(this)
        versionText = findViewById(R.id.versionText)
        notesListView = findViewById(R.id.notesList)
        addNoteButton = findViewById(R.id.fabAddNote)

        val versionName = "1.0"
        val parts = versionName.split(".", limit = 2)
        val major = parts.getOrElse(0) { "0" }
        val minor = parts.getOrElse(1) { "0" }
        versionText.text = "Version $versionName\nMain: $major / Minor: $minor"

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf())
        notesListView.adapter = adapter

        addNoteButton.setOnClickListener {
            showAddNoteDialog()
        }

        notesListView.setOnItemLongClickListener { _, _, position, _ ->
            val selected = noteEntries[position]
            noteEntries.removeAt(position)
            notesRepository.deleteNote(selected.id)
            renderNotes()
            true
        }

        showBiometricPromptIfNeeded()
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
                        Toast.makeText(this@MainActivity, "Authentication error: $errString", Toast.LENGTH_SHORT).show()
                        finish()
                    }

                    override fun onAuthenticationFailed() {
                        Toast.makeText(this@MainActivity, "Authentication failed. Please try again.", Toast.LENGTH_SHORT).show()
                    }
                })

                biometricPrompt.authenticate(promptInfo)
            }
            else -> {
                notesRepository.setUnlocked(true)
                renderNotes()
            }
        }
    }

    private fun showAddNoteDialog() {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            setSingleLine(false)
        }

        AlertDialog.Builder(this)
            .setTitle("Add Note")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val text = input.text.toString().trim()
                if (text.isEmpty()) {
                    Toast.makeText(this, "Note cannot be empty.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                notesRepository.addNote(text)
                renderNotes()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun renderNotes() {
        noteEntries.clear()
        noteEntries.addAll(notesRepository.loadNotes())

        adapter.clear()
        val values = noteEntries.map { note -> note.text }
        adapter.addAll(values)
        adapter.notifyDataSetChanged()
    }
}
