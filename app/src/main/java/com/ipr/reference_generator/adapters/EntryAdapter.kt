//adapters/EntryAdapter.kt
package com.ipr.reference_generator.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textview.MaterialTextView
import com.google.android.material.chip.Chip
import com.ipr.reference_generator.R
import com.ipr.reference_generator.models.Entry
import com.ipr.reference_generator.models.User
import com.ipr.reference_generator.utils.AppUtils

class EntryAdapter(
    private var entries: List<Entry>,
    private val currentUser: User?,
    private val onEntryClick: (Entry) -> Unit,
    private val onEntryDelete: ((Entry) -> Unit)? = null
) : RecyclerView.Adapter<EntryAdapter.EntryViewHolder>() {

    fun updateEntries(newEntries: List<Entry>) {
        entries = newEntries
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_entry, parent, false)
        return EntryViewHolder(view)
    }

    override fun onBindViewHolder(holder: EntryViewHolder, position: Int) {
        holder.bind(entries[position])
    }

    override fun getItemCount() = entries.size

    inner class EntryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvReferenceCode: MaterialTextView = itemView.findViewById(R.id.tvReferenceCode)
        private val chipParticulars: Chip = itemView.findViewById(R.id.chipParticulars)
        private val chipClient: Chip = itemView.findViewById(R.id.chipClient)
        private val chipCapacity: Chip = itemView.findViewById(R.id.chipCapacity)
        private val tvCreatedAt: MaterialTextView = itemView.findViewById(R.id.tvCreatedAt)
        private val tvCreatedBy: MaterialTextView = itemView.findViewById(R.id.tvCreatedBy)

        fun bind(entry: Entry) {
            tvReferenceCode.text = entry.referenceCode
            chipParticulars.text = entry.particulars
            chipClient.text = entry.clientCode
            chipCapacity.text = "${entry.capacityMW.toInt()}MW"
            tvCreatedAt.text = AppUtils.formatDate(entry.createdAt)
            tvCreatedBy.text = "Created by: ${entry.createdBy}"

            // Only admins can edit entries
            val canEdit = currentUser?.role == "admin"

            if (canEdit) {
                // Single tap to edit
                itemView.setOnClickListener { onEntryClick(entry) }

                // Long press to delete (only for admins)
                itemView.setOnLongClickListener {
                    onEntryDelete?.invoke(entry)
                    true
                }

                itemView.alpha = 1.0f
                itemView.foreground = ContextCompat.getDrawable(itemView.context, R.drawable.clickable_background)
            } else {
                itemView.setOnClickListener(null)
                itemView.setOnLongClickListener(null)
                itemView.alpha = 0.9f
                itemView.foreground = null
            }
        }
    }
}