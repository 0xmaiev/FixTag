package com.example.fixtag.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.example.fixtag.AppViewModelFactory
import com.example.fixtag.FixTag
import com.example.fixtag.R
import com.example.fixtag.data.model.creatableRoles
import com.example.fixtag.ui.adapters.MemberAdapter
import com.example.fixtag.ui.main.MainViewModel
import com.example.fixtag.ui.main.MainViewModelFactory
import com.example.fixtag.ui.showFor
import com.example.fixtag.ui.showPendingCount
import com.example.fixtag.ui.viewmodels.MembersViewModel
import kotlinx.coroutines.launch

class MembersFragment : Fragment() {

    private val mainViewModel: MainViewModel by activityViewModels {
        val app = requireContext().applicationContext as FixTag
        MainViewModelFactory(app.repository, app.syncScheduler)
    }

    private val viewModel: MembersViewModel by viewModels {
        val repository = (requireContext().applicationContext as FixTag).repository
        AppViewModelFactory(repository)
    }

    private val memberAdapter = MemberAdapter()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_members, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val newMember = view.findViewById<Button>(R.id.newMemberButton)
        view.findViewById<RecyclerView>(R.id.recyclerView).adapter = memberAdapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { members -> memberAdapter.submitList(members) }
            }
        }
        newMember.setOnClickListener { findNavController().navigate(R.id.new_member) }
        view.findViewById<Button>(R.id.switchProfileButton).setOnClickListener {
            mainViewModel.switchProfile()
        }
        showPendingCount(view, mainViewModel.pendingCount)
        showFor(newMember, mainViewModel.user) { it.creatableRoles.isNotEmpty() }
    }
}
