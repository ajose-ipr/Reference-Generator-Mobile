// dialogs/EntryDialog.kt
package com.ipr.reference_generator.dialogs

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
import com.ipr.reference_generator.network.Repository
import kotlinx.coroutines.launch

class EntryDialog : DialogFragment() {

    companion object {
        fun show(fragmentManager: FragmentManager, onEntryCreated: (Map<String, Any>) -> Unit) {
            val dialog = EntryDialog()
            dialog.onEntryCreated = onEntryCreated
            dialog.show(fragmentManager, "EntryDialog")
        }
    }

    private var onEntryCreated: ((Map<String, Any>) -> Unit)? = null
    private lateinit var repository: Repository

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        repository = Repository.getInstance(requireContext())

        val view = layoutInflater.inflate(R.layout.dialog_entry_form, null)

        val etParticulars = view.findViewById<MaterialAutoCompleteTextView>(R.id.etParticulars)
        val etClientCode = view.findViewById<MaterialAutoCompleteTextView>(R.id.etClientCode)
        val etCapacity = view.findViewById<TextInputEditText>(R.id.etCapacity)
        val etStateName = view.findViewById<MaterialAutoCompleteTextView>(R.id.etStateName)
        val etSiteName = view.findViewById<MaterialAutoCompleteTextView>(R.id.etSiteName)

        // Load dropdown options
        loadDropdownOptions(etParticulars, "PARTICULARS")
        loadDropdownOptions(etClientCode, "CLIENT_CODE")
        loadDropdownOptions(etStateName, "STATE_NAME")
        loadDropdownOptions(etSiteName, "SITE_NAME")

        return AlertDialog.Builder(requireContext())
            .setTitle("Create New Entry")
            .setView(view)
            .setPositiveButton("Create") { _, _ ->
                val entry = mapOf(
                    "PARTICULARS" to etParticulars.text.toString(),
                    "CLIENT_CODE" to etClientCode.text.toString(),
                    "CAPACITY_MW" to (etCapacity.text.toString().toDoubleOrNull() ?: 0.0),
                    "STATE_NAME" to etStateName.text.toString(),
                    "SITE_NAME" to etSiteName.text.toString()
                )
                onEntryCreated?.invoke(entry)
            }
            .setNegativeButton("Cancel", null)
            .create()
    }

    private fun loadDropdownOptions(autoCompleteTextView: MaterialAutoCompleteTextView, type: String) {
        lifecycleScope.launch {
            repository.getDropdownOptions(type)
                .onSuccess { options ->
                    val values = options.filter { it.isActive }.map { it.value }
                    val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, values)
                    autoCompleteTextView.setAdapter(adapter)
                }
        }
    }
}
