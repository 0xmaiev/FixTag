package com.example.fixtag.ui.fragments

import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.navigation.fragment.findNavController
import com.example.fixtag.FixTag
import com.example.fixtag.R
import com.example.fixtag.ui.ItemViewModelFactory
import com.example.fixtag.ui.viewmodels.NewIncidentViewModel

class NewIncidentFragment : Fragment() {

    companion object {
        fun newInstance() = NewIncidentFragment()
    }

    private val viewModel: NewIncidentViewModel by viewModels {
        val context = requireContext().applicationContext
        val repository = (context as FixTag).repository

        ItemViewModelFactory(repository)
    }

    private lateinit var backButton: Button
    private lateinit var changeMachineButton: Button
    private lateinit var modelTextView: TextView
    private lateinit var serialNumberTextView: TextView
    private lateinit var descriptionEditText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // TODO: Use the ViewModel
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_new_incident, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        backButton = view.findViewById(R.id.backButton)
        changeMachineButton = view.findViewById(R.id.changeMachineButton)
        modelTextView = view.findViewById(R.id.model)
        serialNumberTextView = view.findViewById(R.id.serialNumber)
        descriptionEditText = view.findViewById(R.id.descriptionEditText)

        backButton.setOnClickListener {
            findNavController().navigateUp()
        }

        changeMachineButton.setOnClickListener {

        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
    }
}