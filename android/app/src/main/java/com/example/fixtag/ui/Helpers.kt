package com.example.fixtag.ui

import android.app.Dialog
import android.content.Context
import android.media.MediaPlayer
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import coil3.load
import com.example.fixtag.R
import com.example.fixtag.data.Repository
import com.example.fixtag.data.model.IncidentSeverity
import com.example.fixtag.data.model.UserRole
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

// shows a view only to the roles that may use it
fun Fragment.showFor(view: View, role: Flow<UserRole?>, vararg allowed: UserRole) {
    viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            role.collect { view.isVisible = it in allowed }
        }
    }
}

fun Context.recentIncidentsText(count: Int): String =
    if (count == 0) getString(R.string.machine_no_recent_incidents)
    else resources.getQuantityString(R.plurals.machine_history_summary, count, count)

// one place for how a severity looks, shared by the list and the detail screen
fun TextView.showSeverity(severity: IncidentSeverity) {
    val (label, background, textColor) = when (severity) {
        IncidentSeverity.Low -> Triple(R.string.severity_low, R.color.grey_blue, R.color.black)
        IncidentSeverity.Medium -> Triple(R.string.severity_medium, R.color.yellow, R.color.black)
        IncidentSeverity.Critical -> Triple(R.string.severity_critical, R.color.red, R.color.white)
    }
    setText(label)
    setBackgroundColor(ContextCompat.getColor(context, background))  // the colour value, not the R.color id
    setTextColor(ContextCompat.getColor(context, textColor))
}

// fills the status strip under the title; hidden while nothing is waiting
fun Fragment.showPendingCount(view: View, pendingCount: Flow<Int>) {
    val syncStatus = view.findViewById<TextView>(R.id.sync_status)
    viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            pendingCount.collect { count ->
                syncStatus.isVisible = count > 0
                syncStatus.text = getString(R.string.sync_status, count)
            }
        }
    }
}

// something never sent only exists on this phone, so ask before it's gone for good
fun Fragment.confirmDeletePending(
    name: String,
    @StringRes inUseMessage: Int? = null,
    delete: suspend () -> Repository.DeleteResult,
) {
    MaterialAlertDialogBuilder(requireContext())
        .setTitle(R.string.delete_pending_title)
        .setMessage(getString(R.string.delete_pending_message, name))
        .setNegativeButton(R.string.cancel, null)
        .setPositiveButton(R.string.delete) { _, _ ->
            viewLifecycleOwner.lifecycleScope.launch {
                if (delete() == Repository.DeleteResult.InUse && inUseMessage != null) {
                    Snackbar.make(requireView(), inUseMessage, Snackbar.LENGTH_LONG).show()
                }
            }
        }
        .show()
}

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
    val dialog = Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen)
    dialog.setContentView(ImageView(requireContext()).apply {
        load(File(path))
        setOnClickListener { dialog.dismiss() }
    })
    dialog.show()
}