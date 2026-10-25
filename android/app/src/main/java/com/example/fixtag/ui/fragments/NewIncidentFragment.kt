package com.example.fixtag.ui.fragments

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.net.Uri
import android.opengl.Visibility
import android.os.Build
import androidx.fragment.app.viewModels
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.example.fixtag.FixTag
import com.example.fixtag.R
import com.example.fixtag.data.model.AttachmentKind
import com.example.fixtag.data.model.IncidentSeverity
import com.example.fixtag.ui.ItemViewModelFactory
import com.example.fixtag.ui.MACHINE_ID_KEY
import com.example.fixtag.ui.SELECT_MACHINE_REQUEST_KEY
import com.example.fixtag.ui.VoicePlayer
import com.example.fixtag.ui.adapters.AttachmentAdapter
import com.example.fixtag.ui.dialogs.MachineSelectDialogFragment
import com.example.fixtag.ui.showPhoto
import com.example.fixtag.ui.viewmodels.InputBaseViewModel
import com.example.fixtag.ui.viewmodels.NewIncidentViewModel
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.chip.ChipGroup
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import org.w3c.dom.Text

class NewIncidentFragment : Fragment() {

    companion object {
        fun newInstance() = NewIncidentFragment()
    }

    private val viewModel: NewIncidentViewModel by viewModels {
        val context = requireContext().applicationContext
        val repository = (context as FixTag).repository

        ItemViewModelFactory(repository)
    }

    private val requestPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            result.filterValues { granted -> !granted }.keys.forEach {
                Log.d(this.tag, "Permission $it not granted")
            }
        }

    private val cameraPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) capturePhoto() else permissionDenied(R.string.camera_permission_needed)
    }
    private val micPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startRecording() else permissionDenied(R.string.mic_permission_needed)
    }

    private fun isGranted(permission: String) =
        ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED

    private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        viewModel.onCaptured(AttachmentKind.Photo, saved)
    }

    private val recordVideo = registerForActivityResult(ActivityResultContracts.CaptureVideo()) { saved ->
        viewModel.onCaptured(AttachmentKind.Video, saved)
    }

    private lateinit var model: TextView
    private lateinit var serialNumber: TextView
    private lateinit var createIncident: Button
    private lateinit var addPhoto: ImageButton
    private lateinit var addVideo: Button
    private lateinit var addAudio: ImageButton
    private lateinit var selectMachine: Button
    private lateinit var description: EditText
    private lateinit var severityChips: ChipGroup
    private lateinit var attachmentAdapter: AttachmentAdapter
    private lateinit var recyclerView: RecyclerView
    private var recorder: MediaRecorder? = null
    private val voicePlayer = VoicePlayer()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_new_incident, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<MaterialToolbar>(R.id.toolbar)
            .setNavigationOnClickListener { findNavController().navigateUp() }
        selectMachine = view.findViewById(R.id.selectMachineButton)
        addPhoto = view.findViewById(R.id.addPhotoButton)
        addAudio = view.findViewById(R.id.recordAudioButton)
        createIncident = view.findViewById(R.id.createIncidentButton)
        model = view.findViewById(R.id.model)
        serialNumber = view.findViewById(R.id.serialNumber)
        description = view.findViewById(R.id.description)
        severityChips = view.findViewById(R.id.severity)
        recyclerView = view.findViewById(R.id.attachments)

        attachmentAdapter = AttachmentAdapter(
            onOpen = { item ->
                when (item.kind) {
                    AttachmentKind.Photo -> showPhoto(item.path!!)
                    AttachmentKind.Audio -> voicePlayer.toggle(item.path!!)
                    AttachmentKind.Video -> Unit
                }
            },
            onRemove = { item -> voicePlayer.stop(); viewModel.removeAttachment(item) },
        )
        recyclerView.adapter = attachmentAdapter

        // select associated machine
        selectMachine.setOnClickListener {
            // shown as this fragment's child, so the result comes back here
            MachineSelectDialogFragment(viewModel.machine.value).show(childFragmentManager,
                SELECT_MACHINE_REQUEST_KEY
            )
        }
        // adding photos as attachments
        addPhoto.setOnClickListener {
            if (isGranted(Manifest.permission.CAMERA)) capturePhoto()
            else cameraPermission.launch(Manifest.permission.CAMERA)
        }
        // recording audio for an attachment
        addAudio.setOnClickListener {
            when {
                recorder != null -> stopRecording()
                isGranted(Manifest.permission.RECORD_AUDIO) -> startRecording()
                else -> micPermission.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
        // create the incident
        createIncident.setOnClickListener {
            val severity = when(severityChips.checkedChipId) {
                R.id.severityLowChip -> IncidentSeverity.Low
                R.id.severityMediumChip -> IncidentSeverity.Medium
                else -> IncidentSeverity.Critical
            }
            viewModel.submit(description.text.toString(), severity)
        }
        // set listener for result from ´MachineSelectDialogFragment`
        childFragmentManager.setFragmentResultListener(
            SELECT_MACHINE_REQUEST_KEY,
            viewLifecycleOwner) { _, result ->
            result.getString(MACHINE_ID_KEY)?.let {
                viewModel.selectMachine(it)
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.form.collect { form ->
                        createIncident.isEnabled = form.isDataValid
                        description.error = form.descriptionError?.let { getString(it) }
                        model.error = form.machineError?.let { getString(it) }
                    }
                }
                launch {
                    viewModel.machine.collect { value ->
                        if (value != null) {
                            model.text = value.model
                            serialNumber.text = value.serialNumber
                            serialNumber.isVisible = true
                        } else {
                            model.text = getString(R.string.no_machine_selected)
                            serialNumber.isVisible = false
                        }
                    }
                }
                launch {
                    viewModel.uiState.collect { state ->
                        if (state == InputBaseViewModel.State.Saved) {
                            Toast.makeText(
                                requireContext(),
                                R.string.incident_saved,
                                Toast.LENGTH_SHORT
                            ).show()
                            findNavController().popBackStack()
                        }
                    }
                }
                launch { viewModel.attachments.collect { attachmentAdapter.submitList(it) } }
                launch { voicePlayer.playing.collect { attachmentAdapter.playingPath = it } }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }

    private fun permissionDenied(@StringRes message: Int) {
        Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG)
            .setAction(R.string.settings) {
                startActivity(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", requireContext().packageName, null)
                    )
                )
            }
            .show()
    }

    // location

    // camera

    private fun capturePhoto() {
        val file = viewModel.newAttachmentFile(AttachmentKind.Photo, requireContext().filesDir)
        takePicture.launch(FileProvider.getUriForFile(
            requireContext(), "${requireContext().packageName}.files", file))
    }

    // audio

    private fun startRecording() {
        val file = viewModel.newAttachmentFile(AttachmentKind.Audio, requireContext().filesDir)
        @Suppress("DEPRECATION") // before API 31 the constructor takes no context
        val newRecorder = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(requireContext()) else MediaRecorder()
        recorder = try {
            newRecorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioChannels(1)
                setAudioEncodingBitRate(64_000)
                setAudioSamplingRate(44_100)
                setOutputFile(file)
                setMaxDuration(300_000) // 5 minutes
                setOnErrorListener { mr, what, extra ->
                    Log.e("Recorder", "MediaRecorder error: what=$what, extra=$extra")
                    stopRecording()
                }
                setOnInfoListener { mr, what, extra ->
                    Log.i("Recorder", "MediaRecorder info: what=$what, extra=$extra")
                    if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                        // Auto-stop or notify user
                        stopRecording()
                    }
                }
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e("Recorder", "MediaRecorder exception: $e")
            newRecorder.release()
            viewModel.onCaptured(AttachmentKind.Audio, false)  // deletes the empty file
            null
        }
    }

    private fun stopRecording() {
        val recorder = recorder ?: return
        this.recorder = null
        val saved = runCatching { recorder.stop() }.isSuccess  // stop() throws if no audio arrived yet
        recorder.release()
        viewModel.onCaptured(AttachmentKind.Audio, saved)
    }

    override fun onStop() {
        super.onStop()
        stopRecording()  // in the background the mic only delivers silence
        voicePlayer.stop()
    }
}