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
import com.ipr.reference_generator.network.Repository
import com.ipr.reference_generator.utils.AppUtils
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private lateinit var tvWelcome: MaterialTextView
    private lateinit var tvTotalEntries: MaterialTextView
    private lateinit var tvMyEntries: MaterialTextView
    private lateinit var tvThisMonth: MaterialTextView
    private lateinit var recyclerRecentEntries: RecyclerView
    private lateinit var repository: Repository

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        repository = Repository.getInstance(requireContext())

        initViews(view)
        setupUI()
        loadData()
    }

    private fun initViews(view: View) {
        tvWelcome = view.findViewById(R.id.tvWelcome)
        tvTotalEntries = view.findViewById(R.id.tvTotalEntries)
        tvMyEntries = view.findViewById(R.id.tvMyEntries)
        tvThisMonth = view.findViewById(R.id.tvThisMonth)
        recyclerRecentEntries = view.findViewById(R.id.recyclerRecentEntries)
    }

    private fun setupUI() {
        repository.currentUser.observe(viewLifecycleOwner) { user ->
            tvWelcome.text = "Welcome, ${user?.username}!"
        }

        view?.findViewById<MaterialButton>(R.id.btnCreateEntry)?.setOnClickListener {
            (activity as? MainActivity)?.let { mainActivity ->
                mainActivity.findViewById<BottomNavigationView>(R.id.bottomNavigation)
                    .selectedItemId = R.id.nav_entries
            }
        }

        recyclerRecentEntries.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun loadData() {
        lifecycleScope.launch {
            repository.getEntries()
                .onSuccess { entries ->
                    updateStatistics(entries)
                    setupRecentEntries(entries.take(5))
                }
                .onFailure {
                    // Handle error
                }
        }
    }

    private fun updateStatistics(entries: List<Entry>) {
        val currentUser = repository.getUser()
        val myEntries = entries.filter { it.CREATED_BY == currentUser?.username }
        val thisMonthEntries = entries.filter { AppUtils.isCurrentMonth(it.CREATED_AT) }

        tvTotalEntries.text = entries.size.toString()
        tvMyEntries.text = myEntries.size.toString()
        tvThisMonth.text = thisMonthEntries.size.toString()
    }

    private fun setupRecentEntries(recentEntries: List<Entry>) {
        val adapter = EntryAdapter(recentEntries) { entry ->
            // Handle entry click
        }
        recyclerRecentEntries.adapter = adapter
    }
}
