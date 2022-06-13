package com.gilardo.mygps.model

import android.content.Context
import java.io.File
//Membuat Kelas InternalFileRepository
class InternalFileRepository(var context: Context) :
    NoteRepository {
    //menambahkan function kedalam string
    private fun noteFile(fileName: String): File = File(noteDirectory(), fileName)
    private fun noteDirectory(): String = context.filesDir.absolutePath

    override fun addNote(note: Note) {
        context.openFileOutput(note.fileName, Context.MODE_PRIVATE).use { output ->
            output.write(note.noteText.toByteArray())
        }
    }

    override fun getNote(fileName: String): Note {
        val note = Note(fileName, "")
        context.openFileInput(fileName).use { stream ->
            val text = stream.bufferedReader().use {
                it.readText()
            }
            note.noteText = text
        }
        return note
    }

    override fun deleteNote(fileName: String): Boolean {
        return noteFile(fileName).delete()
    }
}