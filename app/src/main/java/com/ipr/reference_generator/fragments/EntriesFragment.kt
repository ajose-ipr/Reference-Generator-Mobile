// fragments/EntriesFragment.kt
package com.ipr.reference_generator.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.ipr.reference_generator.R
import com.ipr.reference_generator.adapters.EntryAdapter
import com.ipr.reference_generator.dialogs.EntryDialog
import com.ipr.reference_generator.network.Repository
import kotlinx.coroutines.launch

class EntriesFragment : Fragment() {

    private lateinit var repository: Repository
    private lateinit var recyclerEntries: RecyclerView
    private lateinit var fabAddEntry: FloatingActionButton
    private lateinit var entryAdapter: EntryAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_entries, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        repository = Repository.getInstance(requireContext())

        initViews(view)
        setupRecyclerView()
        setupUI()
        loadData()
    }

    private fun initViews(view: View) {
        recyclerEntries = view.findViewById(R.id.recyclerEntries)
        fabAddEntry = view.findViewById(R.id.fabAddEntry)
    }

    private fun setupRecyclerView() {
        entryAdapter = EntryAdapter(emptyList()) { entry ->
            // Handle entry click/edit
        }
        recyclerEntries.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = entryAdapter
        }
    }

    private fun setupUI() {
        fabAddEntry.setOnClickListener {
            showCreateEntryDialog()
        }
    }

    private fun loadData() {
        lifecycleScope.launch {
            repository.getEntries()
                .onSuccess { entries ->
                    entryAdapter.updateEntries(entries)
                }
                .onFailure {
                    // Handle error
                }
        }
    }

    private fun showCreateEntryDialog() {
        EntryDialog.show(childFragmentManager) { entry ->
            lifecycleScope.launch {
                repository.createEntry(entry)
                    .onSuccess {
                        loadData() // Refresh list
                    }
                    .onFailure {
                        // Handle error
                    }
            }
        }
    }
}