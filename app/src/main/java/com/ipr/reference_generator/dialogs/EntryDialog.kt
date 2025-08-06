package com.ipr.reference_generator.dialogs
// dialogs/EntryDialog.kt

import android.app.Dialog
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.ipr.reference_generator.R
import kotlinx.coroutines.launch
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.Toast
import com.google.android.material.textfield.TextInputLayout
import com.ipr.reference_generator.models.Entry
import com.ipr.reference_generator.models.EntryRequest
import com.ipr.reference_generator.network.FirebaseRepository
// Add this import at the top
import com.ipr.reference_generator.activities.LoginActivity

class EntryDialog : DialogFragment() {

    companion object {
        fun show(
            fragmentManager: FragmentManager,
            entry: Entry?,
            onEntrySubmitted: (EntryRequest) -> Unit
        ) {
            val dialog = EntryDialog()
            dialog.entry = entry
            dialog.onEntrySubmitted = onEntrySubmitted
            dialog.show(fragmentManager, "EntryDialog")
        }
    }

    private var entry: Entry? = null
    private var onEntrySubmitted: ((EntryRequest) -> Unit)? = null
    private lateinit var repository: FirebaseRepository

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        repository = FirebaseRepository.getInstance(requireContext())

        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_entry_form, null)

        val tilParticulars = view.findViewById<TextInputLayout>(R.id.tilParticulars)
        val etParticulars = view.findViewById<MaterialAutoCompleteTextView>(R.id.etParticulars)
        val tilClientCode = view.findViewById<TextInputLayout>(R.id.tilClientCode)
        val etClientCode = view.findViewById<MaterialAutoCompleteTextView>(R.id.etClientCode)
        val tilCapacity = view.findViewById<TextInputLayout>(R.id.tilCapacity)
        val etCapacity = view.findViewById<TextInputEditText>(R.id.etCapacity)
        val tilStateName = view.findViewById<TextInputLayout>(R.id.tilStateName)
        val etStateName = view.findViewById<MaterialAutoCompleteTextView>(R.id.etStateName)
        val tilSiteName = view.findViewById<TextInputLayout>(R.id.tilSiteName)
        val etSiteName = view.findViewById<MaterialAutoCompleteTextView>(R.id.etSiteName)

        // Load dropdown options
        loadDropdownOptions(etParticulars, "PARTICULARS")
        loadDropdownOptions(etClientCode, "CLIENT_CODE")
        loadDropdownOptions(etStateName, "STATE_NAME")
        loadDropdownOptions(etSiteName, "SITE_NAME")

        // Pre-fill form if editing
        entry?.let { existingEntry ->
            etParticulars.setText(existingEntry.particulars)
            etClientCode.setText(existingEntry.clientCode)
            etCapacity.setText(existingEntry.capacityMW.toString())
            etStateName.setText(existingEntry.stateName)
            etSiteName.setText(existingEntry.siteName)
        }

        val title = if (entry != null) "Edit Entry" else "Create New Entry"
        val buttonText = if (entry != null) "Update" else "Create"

        return AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setView(view)
            .setPositiveButton(buttonText) { _, _ ->
                // Validate inputs
                val particulars = etParticulars.text.toString().trim()
                val clientCode = etClientCode.text.toString().trim()
                val capacityText = etCapacity.text.toString().trim()
                val stateName = etStateName.text.toString().trim()
                val siteName = etSiteName.text.toString().trim()

                // Clear previous errors
                tilParticulars.error = null
                tilClientCode.error = null
                tilCapacity.error = null
                tilStateName.error = null
                tilSiteName.error = null

                // Validate
                var hasError = false

                if (particulars.isEmpty()) {
                    tilParticulars.error = "Particulars is required"
                    hasError = true
                }

                if (clientCode.isEmpty()) {
                    tilClientCode.error = "Client code is required"
                    hasError = true
                }

                val capacity = capacityText.toDoubleOrNull()
                if (capacity == null || capacity <= 0) {
                    tilCapacity.error = "Enter valid capacity"
                    hasError = true
                }

                if (stateName.isEmpty()) {
                    tilStateName.error = "State name is required"
                    hasError = true
                }

                if (siteName.isEmpty()) {
                    tilSiteName.error = "Site name is required"
                    hasError = true
                }

                if (!hasError && capacity != null) {
                    val request = EntryRequest(
                        particulars = particulars,
                        clientCode = clientCode,
                        capacityMW = capacity,
                        stateName = stateName,
                        siteName = siteName
                    )
                    onEntrySubmitted?.invoke(request)
                }
            }
            .setNegativeButton("Cancel", null)
            .create()
    }

    private fun loadDropdownOptions(
        autoCompleteTextView: MaterialAutoCompleteTextView,
        type: String
    ) {
        lifecycleScope.launch {
            repository.getDropdownOptions(type)
                .onSuccess { options ->
                    val values = options.filter { it.isActive }.map { it.value }.toMutableList()

                    // FIX: Use finalValues consistently
                    val finalValues = if (values.isEmpty()) {
                        when (type) {
                            "PARTICULARS" -> mutableListOf("TC", "GC", "PQM", "EVF", "OPT")
                            "CLIENT_CODE" -> mutableListOf("HFEX", "ADN", "HEXA", "GE")
                            "STATE_NAME" -> mutableListOf("KA", "TN", "AP", "TS")
                            "SITE_NAME" -> mutableListOf("SJPR", "BNSK", "GRID")
                            else -> mutableListOf()
                        }
                    } else values

                    // Add custom option
                    finalValues.add("+ Add Custom...")

                    val adapter = ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_dropdown_item_1line,
                        finalValues  // Use finalValues here, not values
                    )
                    autoCompleteTextView.setAdapter(adapter)

                    // Handle custom option selection
                    autoCompleteTextView.setOnItemClickListener { _, _, position, _ ->
                        if (finalValues[position] == "+ Add Custom...") {  // Use finalValues here too
                            showAddCustomOptionDialog(type, autoCompleteTextView)
                        }
                    }
                }
                .onFailure { error ->
                    Toast.makeText(requireContext(), "Failed to load $type options", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun showAddCustomOptionDialog(type: String, targetField: MaterialAutoCompleteTextView) {
        val input = EditText(requireContext())
        input.hint = "Enter custom $type"

        AlertDialog.Builder(requireContext())
            .setTitle("Add Custom $type")
            .setView(input)
            .setPositiveButton("Add") { _, _ ->
                val customValue = input.text.toString().trim().uppercase()
                if (customValue.isNotEmpty()) {
                    addCustomOption(type, customValue, customValue, targetField)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun addCustomOption(
        type: String,
        value: String,
        displayName: String,
        targetField: MaterialAutoCompleteTextView
    ) {
        lifecycleScope.launch {
            repository.addCustomDropdownOption(type, value, displayName)
                .onSuccess {
                    targetField.setText(value)
                    Toast.makeText(requireContext(), "Custom option added!", Toast.LENGTH_SHORT)
                        .show()
                    // Reload options to include the new one
                    loadDropdownOptions(targetField, type)
                }
                .onFailure { error ->
                    Toast.makeText(
                        requireContext(),
                        "Failed to add option: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
        }
    }
}