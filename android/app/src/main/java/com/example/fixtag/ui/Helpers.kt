package com.example.fixtag.ui

import android.R
import android.app.Dialog
import android.media.MediaPlayer
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.ImageView
import androidx.fragment.app.Fragment
import coil3.load
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

/**
 * Extension function to simplify setting an afterTextChanged action to EditText components.
 */
fun EditText.afterTextChanged(afterTextChanged: (String) -> Unit) {
    this.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(editable: Editable?) {
            afterTextChanged.invoke(editable.toString())
        }

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
    })
}

// plays one voice note at a time; tapping the playing note again stops it
class VoicePlayer {
    private var player: MediaPlayer? = null
    private val _playing = MutableStateFlow<String?>(null)
    val playing: StateFlow<String?> = _playing

    fun toggle(path: String) {
        val wasPlaying = _playing.value
        stop()
        if (wasPlaying == path) return
        val newPlayer = MediaPlayer()
        try {
            newPlayer.setDataSource(path)
            newPlayer.setOnCompletionListener { stop() }
            newPlayer.prepare()
            newPlayer.start()
        } catch (e: Exception) {
            newPlayer.release()
            return
        }
        player = newPlayer
        _playing.value = path
    }

    fun stop() {
        player?.release()
        player = null
        _playing.value = null
    }
}

/**
 * Extension function to simplify displaying a photo in both fragments that need it
 */
fun Fragment.showPhoto(path: String) {
    val dialog = Dialog(requireContext(), R.style.Theme_Black_NoTitleBar_Fullscreen)
    dialog.setContentView(ImageView(requireContext()).apply {
        load(File(path))
        setOnClickListener { dialog.dismiss() }
    })
    dialog.show()
}