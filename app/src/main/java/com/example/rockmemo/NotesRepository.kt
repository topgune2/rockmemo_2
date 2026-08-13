package com.example.rockmemo

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class NotesRepository(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_notes_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun loadNotes(): List<Note> {
        val json = prefs.getString(KEY_NOTES, "[]") ?: "[]"
        val array = JSONArray(json)
        val notes = mutableListOf<Note>()

        for (index in 0 until array.length()) {
            val item = array.getJSONObject(index)
            notes.add(
                Note(
                    id = item.getString("id"),
                    text = item.getString("text"),
                    createdAt = item.optLong("createdAt", 0L)
                )
            )
        }

        return notes.sortedByDescending { it.createdAt }
    }

    fun addNote(text: String) {
        val notes = loadNotes().toMutableList()
        notes.add(
            Note(
                id = UUID.randomUUID().toString(),
                text = text,
                createdAt = System.currentTimeMillis()
            )
        )
        saveNotes(notes)
    }

    fun deleteNote(id: String) {
        val notes = loadNotes().filterNot { it.id == id }
        saveNotes(notes)
    }

    fun saveNotes(notes: List<Note>) {
        val array = JSONArray()
        notes.forEach { note ->
            JSONObject().apply {
                put("id", note.id)
                put("text", note.text)
                put("createdAt", note.createdAt)
            }.let(array::put)
        }
        prefs.edit().putString(KEY_NOTES, array.toString()).apply()
    }

    fun isUnlocked(): Boolean = prefs.getBoolean(KEY_UNLOCKED, false)

    fun setUnlocked(unlocked: Boolean) {
        prefs.edit().putBoolean(KEY_UNLOCKED, unlocked).apply()
    }

    companion object {
        private const val KEY_NOTES = "notes"
        private const val KEY_UNLOCKED = "unlocked"
    }
}
