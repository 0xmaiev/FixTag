package com.example.fixtag.ui.fragments

import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.savedstate.serialization.saved
import com.example.fixtag.AppViewModelFactory
import com.example.fixtag.FixTag
import com.example.fixtag.R
import kotlin.uuid.Uuid

class IncidentDetailFragment : Fragment() {

    companion object {
        fun newInstance() = IncidentDetailFragment()
    }
    private lateinit var incidentId: Uuid

    private val viewModel: IncidentDetailViewModel by viewModels {
        val context = requireContext().applicationContext
        val repository = (context as FixTag).repository

        AppViewModelFactory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState != null) {
            val idString = savedInstanceState.getString("id")
            if (idString != null) {
                incidentId = Uuid.parse(idString)
            }
        }

        // TODO: Use the ViewModel
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_incident_detail, container, false)
    }
}