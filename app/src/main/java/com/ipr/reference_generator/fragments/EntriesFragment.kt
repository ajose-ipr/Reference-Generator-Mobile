package com.ipr.reference_generator.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textview.MaterialTextView
import com.ipr.reference_generator.R
import com.ipr.reference_generator.adapters.EntryAdapter
import com.ipr.reference_generator.dialogs.EntryDialog
import com.ipr.reference_generator.models.Entry
import com.ipr.reference_generator.network.FirebaseRepository
import kotlinx.coroutines.launch

class EntriesFragment : Fragment() {

    private lateinit var repository: FirebaseRepository
    private lateinit var recyclerEntries: RecyclerView
    private lateinit var fabAddEntry: FloatingActionButton
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var tvNoEntries: MaterialTextView
    private lateinit var entryAdapter: EntryAdapter
    private var currentSearchQuery = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setupMenuProvider()
        return inflater.inflate(R.layout.fragment_entries, container, false)
    }

    private fun setupMenuProvider() {
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.entries_menu, menu)

                val searchItem = menu.findItem(R.id.action_search)
                val searchView = searchItem.actionView as SearchView

                searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                    override fun onQueryTextSubmit(query: String?): Boolean {
                        currentSearchQuery = query ?: ""
                        loadData()
                        return true
                    }

                    override fun onQueryTextChange(newText: String?): Boolean {
                        if (newText.isNullOrEmpty()) {
                            currentSearchQuery = ""
                            loadData()
                        }
                        return true
                    }
                })
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repository = FirebaseRepository.getInstance(requireContext())
        initViews(view)
        setupRecyclerView()
        setupUI()
        loadData()
    }

    private fun initViews(view: View) {
        recyclerEntries = view.findViewById(R.id.recyclerEntries)
        fabAddEntry = view.findViewById(R.id.fabAddEntry)
        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        tvNoEntries = view.findViewById(R.id.tvNoEntries)
    }

    private fun setupRecyclerView() {
        val currentUser = repository.getCurrentUser()
        entryAdapter = EntryAdapter(
            entries = emptyList(),
            currentUser = currentUser,
            onEntryClick = { entry ->
                if (currentUser?.role == "admin") {
                    showEditEntryDialog(entry)
                } else {
                    showToast("Only admins can edit entries")
                }
            },
            onEntryDelete = { entry ->
                if (currentUser?.role == "admin") {
                    showDeleteConfirmationDialog(entry)
                }
            }
        )
        recyclerEntries.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = entryAdapter
        }
    }

    private fun setupUI() {
        fabAddEntry.setOnClickListener {
            showCreateEntryDialog()
        }

        swipeRefresh.setOnRefreshListener {
            loadData()
        }
    }

    private fun loadData() {
        lifecycleScope.launch {
            repository.getEntries(currentSearchQuery)
                .onSuccess { entries ->
                    swipeRefresh.isRefreshing = false
                    entryAdapter.updateEntries(entries)
                    updateEmptyState(entries.isEmpty())
                }
                .onFailure { error ->
                    swipeRefresh.isRefreshing = false
                    showToast(error.message ?: "Failed to load entries")
                }
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        tvNoEntries.visibility = if (isEmpty) View.VISIBLE else View.GONE
        recyclerEntries.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun showCreateEntryDialog() {
        EntryDialog.show(childFragmentManager, null) { request ->
            lifecycleScope.launch {
                repository.createEntry(request)
                    .onSuccess {
                        showToast("Entry created successfully!")
                        loadData()
                    }
                    .onFailure { error ->
                        showToast("Failed to create entry: ${error.message}")
                    }
            }
        }
    }

    private fun showEditEntryDialog(entry: Entry) {
        if (repository.getCurrentUser()?.role != "admin") {
            showToast("Only admins can edit entries")
            return
        }

        EntryDialog.show(childFragmentManager, entry) { request ->
            lifecycleScope.launch {
                repository.updateEntry(entry.id, request)
                    .onSuccess {
                        showToast("Entry updated successfully!")
                        loadData()
                    }
                    .onFailure { error ->
                        showToast("Failed to update entry: ${error.message}")
                    }
            }
        }
    }

    private fun showDeleteConfirmationDialog(entry: Entry) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Delete Entry")
            .setMessage("Are you sure you want to delete this entry?\n\n${entry.referenceCode}")
            .setPositiveButton("Delete") { _, _ -> deleteEntry(entry) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteEntry(entry: Entry) {
        lifecycleScope.launch {
            repository.deleteEntry(entry.id)
                .onSuccess {
                    showToast("Entry deleted successfully!")
                    loadData()
                }
                .onFailure { error ->
                    showToast("Failed to delete entry: ${error.message}")
                }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}