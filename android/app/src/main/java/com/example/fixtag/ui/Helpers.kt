package com.example.fixtag.ui

import android.app.Dialog
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.media.MediaPlayer
import android.os.Build
import android.os.PersistableBundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import coil3.load
import com.example.fixtag.R
import com.example.fixtag.data.Repository
import com.example.fixtag.data.model.IncidentSeverity
import com.example.fixtag.data.model.IncidentStatus
import com.example.fixtag.data.model.User
import com.example.fixtag.data.model.UserRole
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

// shows a view only to the users that may use it, e.g. showFor(button, user) { it.isSupport }.
// A role alone can't say it: a gestor works for a client or for a manufacturer
fun Fragment.showFor(view: View, user: Flow<User?>, allowed: (User) -> Boolean) {
    viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            user.collect { view.isVisible = it != null && allowed(it) }
        }
    }
}

// one place for how a status and a role read, shared by the lists, the detail screen and the forms
@StringRes
fun IncidentStatus.label(): Int = when (this) {
    IncidentStatus.Received -> R.string.incident_received
    IncidentStatus.Analysis -> R.string.incident_analysis
    IncidentStatus.Resolved -> R.string.incident_resolved
}

@StringRes
fun UserRole.label(): Int = when (this) {
    UserRole.Admin -> R.string.role_admin
    UserRole.Operador -> R.string.role_operator
    UserRole.Tecnico -> R.string.role_technician
    UserRole.Assistencia -> R.string.role_assistance
    UserRole.Gestor -> R.string.role_manager
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

// A new account's email and temporary password, which the server shows only once, in the answer
// that created the account. Only Done closes it, then onDone runs; Copy keeps it open
fun Fragment.showTemporaryPassword(account: User, password: String, onDone: () -> Unit): AlertDialog {
    val content = layoutInflater.inflate(R.layout.dialog_temporary_password, null)
    content.findViewById<TextView>(R.id.intro).text = getString(R.string.temporary_password_intro, account.name)
    content.findViewById<TextView>(R.id.email).text = account.email
    content.findViewById<TextView>(R.id.password).text = password

    val dialog = MaterialAlertDialogBuilder(requireContext())
        .setTitle(R.string.account_created_title)
        .setView(content)
        .setCancelable(false)
        .setPositiveButton(R.string.done) { _, _ -> onDone() }
        .setNeutralButton(R.string.copy_password, null)
        .show()
    // set after show(): a listener given to the builder would close the dialog too
    dialog.getButton(DialogInterface.BUTTON_NEUTRAL).setOnClickListener {
        val clip = ClipData.newPlainText(getString(R.string.label_temporary_password), password)
        // keeps the password out of the clipboard's on-screen preview
        clip.description.extras = PersistableBundle().apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
            } else {
                putBoolean("android.content.extra.IS_SENSITIVE", true)
            }
        }
        requireContext().getSystemService(ClipboardManager::class.java).setPrimaryClip(clip)
        // Android 13 and later confirm a copy themselves
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Toast.makeText(requireContext(), R.string.password_copied, Toast.LENGTH_SHORT).show()
        }
    }
    // a rotation recreates the fragment, which shows the dialog again: this one goes with the old view
    viewLifecycleOwner.lifecycle.addObserver(LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_DESTROY) dialog.dismiss()
    })
    return dialog
}