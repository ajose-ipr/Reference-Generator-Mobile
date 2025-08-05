// fragments/SettingsFragment.kt
package com.ipr.reference_generator.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textview.MaterialTextView
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.ipr.reference_generator.R
import com.ipr.reference_generator.models.DropdownOption
import com.ipr.reference_generator.network.Repository
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {

    private lateinit var repository: Repository
    private lateinit var progressBar: CircularProgressIndicator
    private lateinit var recyclerDropdownOptions: RecyclerView
    private lateinit var tvTotalOptions: MaterialTextView
    private lateinit var tvActiveOptions: MaterialTextView
    private lateinit var tvCustomOptions: MaterialTextView
    private lateinit var btnRefreshData: MaterialButton
    private lateinit var btnExportData: MaterialButton
    private lateinit var btnAddOption: MaterialButton

    private var dropdownOptions: List<DropdownOption> = emptyList()
    private lateinit var dropdownAdapter: DropdownOptionsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        repository = Repository.getInstance(requireContext())

        initViews(view)
        setupRecyclerView()
        setupClickListeners()
        checkAdminAccess()
        loadData()
    }

    private fun initViews(view: View) {
        progressBar = view.findViewById(R.id.progressBar)
        recyclerDropdownOptions = view.findViewById(R.id.recyclerDropdownOptions)
        tvTotalOptions = view.findViewById(R.id.tvTotalOptions)
        tvActiveOptions = view.findViewById(R.id.tvActiveOptions)
        tvCustomOptions = view.findViewById(R.id.tvCustomOptions)
        btnRefreshData = view.findViewById(R.id.btnRefreshData)
        btnExportData = view.findViewById(R.id.btnExportData)
        btnAddOption = view.findViewById(R.id.btnAddOption)
    }

    private fun setupRecyclerView() {
        dropdownAdapter = DropdownOptionsAdapter(
            options = dropdownOptions,
            onToggleClick = { option -> toggleOptionStatus(option) },
            onDeleteClick = { option -> confirmDeleteOption(option) }
        )

        recyclerDropdownOptions.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = dropdownAdapter
        }
    }

    private fun setupClickListeners() {
        btnRefreshData.setOnClickListener {
            loadData()
        }

        btnExportData.setOnClickListener {
            exportAllData()
        }

        btnAddOption.setOnClickListener {
            showAddOptionDialog()
        }
    }

    private fun checkAdminAccess() {
        val currentUser = repository.getUser()
        if (currentUser?.role != "admin") {
            // Show access denied message
            view?.findViewById<MaterialCardView>(R.id.cardAccessDenied)?.visibility = View.VISIBLE
            view?.findViewById<View>(R.id.adminContent)?.visibility = View.GONE
            return
        }
    }

    private fun loadData() {
        setLoading(true)

        lifecycleScope.launch {
            try {
                // Load all dropdown option types
                val types = listOf("PARTICULARS", "CLIENT_CODE", "SITE_NAME", "STATE_NAME")
                val allOptions = mutableListOf<DropdownOption>()

                for (type in types) {
                    repository.getDropdownOptions(type)
                        .onSuccess { options ->
                            allOptions.addAll(options)
                        }
                        .onFailure { error ->
                            Toast.makeText(requireContext(), "Failed to load $type: ${error.message}", Toast.LENGTH_SHORT).show()
                        }
                }

                dropdownOptions = allOptions
                updateUI()

            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error loading data: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                setLoading(false)
            }
        }
    }

    private fun updateUI() {
        // Update statistics
        val activeOptions = dropdownOptions.filter { it.isActive }
        val customOptions = dropdownOptions.filter { it.isCustom }

        tvTotalOptions.text = dropdownOptions.size.toString()
        tvActiveOptions.text = activeOptions.size.toString()
        tvCustomOptions.text = customOptions.size.toString()

        // Update RecyclerView
        dropdownAdapter.updateOptions(dropdownOptions)
    }

    private fun showAddOptionDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_option, null)

        val etType = dialogView.findViewById<MaterialAutoCompleteTextView>(R.id.etType)
        val etValue = dialogView.findViewById<TextInputEditText>(R.id.etValue)

        // Setup type dropdown
        val types = arrayOf("PARTICULARS", "CLIENT_CODE", "SITE_NAME", "STATE_NAME")
        val typeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, types)
        etType.setAdapter(typeAdapter)

        AlertDialog.Builder(requireContext())
            .setTitle("Add New Dropdown Option")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val type = etType.text.toString()
                val value = etValue.text.toString().trim().uppercase()

                if (type.isNotEmpty() && value.isNotEmpty()) {
                    addNewOption(type, value)
                } else {
                    Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun addNewOption(type: String, value: String) {
        lifecycleScope.launch {
            repository.addCustomDropdownOption(type, value) { success, error ->
                if (success) {
                    Toast.makeText(requireContext(), "Option added successfully", Toast.LENGTH_SHORT).show()
                    loadData() // Refresh data
                } else {
                    Toast.makeText(requireContext(), error ?: "Failed to add option", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun toggleOptionStatus(option: DropdownOption) {
        lifecycleScope.launch {
            // This would require implementing updateDropdownOption in Repository
            Toast.makeText(requireContext(), "Toggle functionality - implement in backend", Toast.LENGTH_SHORT).show()
        }
    }

    private fun confirmDeleteOption(option: DropdownOption) {
        if (!option.isCustom) {
            Toast.makeText(requireContext(), "Cannot delete system options", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Delete Option")
            .setMessage("Are you sure you want to delete '${option.value}'?")
            .setPositiveButton("Delete") { _, _ ->
                deleteOption(option)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteOption(option: DropdownOption) {
        lifecycleScope.launch {
            // This would require implementing deleteDropdownOption in Repository
            Toast.makeText(requireContext(), "Delete functionality - implement in backend", Toast.LENGTH_SHORT).show()
        }
    }

    private fun exportAllData() {
        lifecycleScope.launch {
            // This would trigger data export
            Toast.makeText(requireContext(), "Export functionality - implement in backend", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setLoading(loading: Boolean) {
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        btnRefreshData.isEnabled = !loading
        btnExportData.isEnabled = !loading
        btnAddOption.isEnabled = !loading
    }
}

// Adapter for dropdown options
class DropdownOptionsAdapter(
    private var options: List<DropdownOption>,
    private val onToggleClick: (DropdownOption) -> Unit,
    private val onDeleteClick: (DropdownOption) -> Unit
) : RecyclerView.Adapter<DropdownOptionsAdapter.OptionViewHolder>() {

    fun updateOptions(newOptions: List<DropdownOption>) {
        options = newOptions
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OptionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_dropdown_option, parent, false)
        return OptionViewHolder(view)
    }

    override fun onBindViewHolder(holder: OptionViewHolder, position: Int) {
        holder.bind(options[position])
    }

    override fun getItemCount() = options.size

    inner class OptionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvType: MaterialTextView = itemView.findViewById(R.id.tvType)
        private val tvValue: MaterialTextView = itemView.findViewById(R.id.tvValue)
        private val tvStatus: MaterialTextView = itemView.findViewById(R.id.tvStatus)
        private val tvCustom: MaterialTextView = itemView.findViewById(R.id.tvCustom)
        private val btnToggle: MaterialButton = itemView.findViewById(R.id.btnToggle)
        private val btnDelete: MaterialButton = itemView.findViewById(R.id.btnDelete)

        fun bind(option: DropdownOption) {
            tvType.text = option.type
            tvValue.text = option.value
            tvStatus.text = if (option.isActive) "Active" else "Inactive"
            tvStatus.setTextColor(
                if (option.isActive)
                    itemView.context.getColor(android.R.color.holo_green_dark)
                else
                    itemView.context.getColor(android.R.color.holo_red_dark)
            )

            tvCustom.visibility = if (option.isCustom) View.VISIBLE else View.GONE

            btnToggle.text = if (option.isActive) "Deactivate" else "Activate"
            btnToggle.setOnClickListener { onToggleClick(option) }

            btnDelete.visibility = if (option.isCustom) View.VISIBLE else View.GONE
            btnDelete.setOnClickListener { onDeleteClick(option) }
        }
    }
}