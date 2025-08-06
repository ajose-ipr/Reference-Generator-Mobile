package com.ipr.reference_generator.fragments // Admin only
// fragments/SettingsFragment.kt

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textview.MaterialTextView
import com.ipr.reference_generator.R
import com.ipr.reference_generator.network.FirebaseRepository
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {

    private lateinit var repository: FirebaseRepository
    private lateinit var recyclerDropdownOptions: RecyclerView
    private lateinit var etOptionType: MaterialAutoCompleteTextView
    private lateinit var etOptionValue: TextInputEditText
    private lateinit var etOptionDisplayName: TextInputEditText
    private lateinit var btnAddOption: MaterialButton

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        repository = FirebaseRepository.getInstance(requireContext())

        // Check admin access
        if (!repository.isAdmin()) {
            // Show access denied message
            view.findViewById<MaterialTextView>(R.id.tvAccessDenied).visibility = View.VISIBLE
            return
        }

        initViews(view)
        setupUI()
        loadDropdownOptions()
    }

    private fun initViews(view: View) {
        recyclerDropdownOptions = view.findViewById(R.id.recyclerDropdownOptions)
        etOptionType = view.findViewById(R.id.etOptionType)
        etOptionValue = view.findViewById(R.id.etOptionValue)
        etOptionDisplayName = view.findViewById(R.id.etOptionDisplayName)
        btnAddOption = view.findViewById(R.id.btnAddOption)
    }

    private fun setupUI() {
        recyclerDropdownOptions.layoutManager = LinearLayoutManager(requireContext())

        // Setup option type dropdown
        val types = arrayOf("PARTICULARS", "CLIENT_CODE", "SITE_NAME", "STATE_NAME")
        val adapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, types)
        etOptionType.setAdapter(adapter)

        btnAddOption.setOnClickListener {
            addCustomOption()
        }
    }


    private fun addCustomOption() {
        val type = etOptionType.text.toString().trim()
        val value = etOptionValue.text.toString().trim().uppercase()
        val displayName = etOptionDisplayName.text.toString().trim()

        if (type.isEmpty() || value.isEmpty() || displayName.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            repository.addCustomDropdownOption(type, value, displayName)
                .onSuccess {
                    Toast.makeText(requireContext(), "Option added successfully", Toast.LENGTH_SHORT).show()
                    etOptionValue.text?.clear()
                    etOptionDisplayName.text?.clear()
                    loadDropdownOptions()
                }
                .onFailure { error ->
                    Toast.makeText(requireContext(), "Failed to add option: ${error.message}", Toast.LENGTH_LONG).show()
                }
        }
    }



    private fun loadDropdownOptions() {
        // Load and display existing dropdown options
        // Implementation would show a list of all dropdown options with edit/delete capabilities
    }
}