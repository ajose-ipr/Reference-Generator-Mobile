// fragments/HomeFragment.kt
package com.ipr.reference_generator.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView
import com.ipr.reference_generator.MainActivity
import com.ipr.reference_generator.R
import com.ipr.reference_generator.adapters.EntryAdapter
import com.ipr.reference_generator.models.Entry
import com.ipr.reference_generator.network.FirebaseRepository
import com.ipr.reference_generator.utils.AppUtils
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private lateinit var repository: FirebaseRepository
    private lateinit var tvWelcome: MaterialTextView
    private lateinit var tvRole: MaterialTextView
    private lateinit var tvTotalEntries: MaterialTextView
    private lateinit var tvMyEntries: MaterialTextView
    private lateinit var tvThisMonth: MaterialTextView
    private lateinit var recyclerRecentEntries: RecyclerView
    private lateinit var btnCreateEntry: MaterialButton
    private lateinit var btnAdminSettings: MaterialButton

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        repository = FirebaseRepository.getInstance(requireContext())

        initViews(view)
        setupUI()
        loadData()
    }

    private fun initViews(view: View) {
        tvWelcome = view.findViewById(R.id.tvWelcome)
        tvRole = view.findViewById(R.id.tvRole)
        tvTotalEntries = view.findViewById(R.id.tvTotalEntries)
        tvMyEntries = view.findViewById(R.id.tvMyEntries)
        tvThisMonth = view.findViewById(R.id.tvThisMonth)
        recyclerRecentEntries = view.findViewById(R.id.recyclerRecentEntries)
        btnCreateEntry = view.findViewById(R.id.btnCreateEntry)
        btnAdminSettings = view.findViewById(R.id.btnAdminSettings)
    }

    private fun setupUI() {
        repository.currentUser.observe(viewLifecycleOwner) { user ->
            user?.let {
                tvWelcome.text = "Welcome, ${it.username}!"
                tvRole.text = "Role: ${it.role}"
                btnAdminSettings.visibility = if (it.role == "admin") View.VISIBLE else View.GONE

                // Load user-specific data when user info is available
                loadUserSpecificData()
            }
        }

        btnCreateEntry.setOnClickListener {
            (activity as? MainActivity)?.findViewById<BottomNavigationView>(R.id.bottomNavigation)
                ?.selectedItemId = R.id.nav_entries
        }

        btnAdminSettings.setOnClickListener {
            (activity as? MainActivity)?.findViewById<BottomNavigationView>(R.id.bottomNavigation)
                ?.selectedItemId = R.id.nav_settings
        }

        recyclerRecentEntries.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun loadData() {
        lifecycleScope.launch {
            // Load total entries and this month entries
            repository.getEntries()
                .onSuccess { entries ->
                    updateGeneralStatistics(entries)
                    setupRecentEntries(entries.take(5))
                }
                .onFailure {
                    // Handle error - set default values
                    tvTotalEntries.text = "0"
                    tvThisMonth.text = "0"
                }
        }
    }

    private fun loadUserSpecificData() {
        val currentUser = repository.getCurrentUser()
        if (currentUser != null) {
            lifecycleScope.launch {
                // Use getUserEntries function to get user-specific entries
                repository.getUserEntries(currentUser.username)
                    .onSuccess { userEntries ->
                        tvMyEntries.text = userEntries.size.toString()
                    }
                    .onFailure {
                        tvMyEntries.text = "0"
                    }
            }
        }
    }

    private fun updateGeneralStatistics(entries: List<Entry>) {
        val thisMonthEntries = entries.filter { entry ->
            entry.createdAt?.let { AppUtils.isCurrentMonth(it.time) } ?: false
        }

        tvTotalEntries.text = entries.size.toString()
        tvThisMonth.text = thisMonthEntries.size.toString()
    }

    private fun setupRecentEntries(recentEntries: List<Entry>) {
        val currentUser = repository.getCurrentUser()
        val adapter = EntryAdapter(
            entries = recentEntries,
            currentUser = currentUser,
            onEntryClick = { entry ->
                // Navigate to entries fragment
                (activity as? MainActivity)?.findViewById<BottomNavigationView>(R.id.bottomNavigation)
                    ?.selectedItemId = R.id.nav_entries
            }
        )
        recyclerRecentEntries.adapter = adapter
    }
}