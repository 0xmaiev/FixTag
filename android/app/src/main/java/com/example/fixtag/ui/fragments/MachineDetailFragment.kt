package com.example.fixtag.ui.fragments

import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.fixtag.AppViewModelFactory
import com.example.fixtag.FixTag
import com.example.fixtag.R
import kotlin.uuid.Uuid

class MachineDetailFragment : Fragment() {

    companion object {
        fun newInstance() = MachineDetailFragment()
    }
    private lateinit var incidentId: Uuid

    private val viewModel: MachineDetailViewModel by viewModels {
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
        return inflater.inflate(R.layout.fragment_machine_detail, container, false)
    }
}