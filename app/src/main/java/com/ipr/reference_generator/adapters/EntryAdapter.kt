//adapters/EntryAdapter.kt
package com.ipr.reference_generator.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textview.MaterialTextView
import com.ipr.reference_generator.R
import com.ipr.reference_generator.models.Entry
import com.ipr.reference_generator.utils.AppUtils

class EntryAdapter(
    private var entries: List<Entry>,
    private val onEntryClick: (Entry) -> Unit
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
        private val tvParticulars: MaterialTextView = itemView.findViewById(R.id.tvParticulars)
        private val tvClient: MaterialTextView = itemView.findViewById(R.id.tvClient)
        private val tvCapacity: MaterialTextView = itemView.findViewById(R.id.tvCapacity)
        private val tvCreatedAt: MaterialTextView = itemView.findViewById(R.id.tvCreatedAt)

        fun bind(entry: Entry) {
            tvReferenceCode.text = entry.REFERENCE_CODE
            tvParticulars.text = entry.PARTICULARS
            tvClient.text = entry.CLIENT_CODE
            tvCapacity.text = "${entry.CAPACITY_MW} MW"
            tvCreatedAt.text = AppUtils.formatDateTime(entry.CREATED_AT)

            itemView.setOnClickListener { onEntryClick(entry) }
        }
    }
}
