package unab.edu.co.abrahamcaceres.safebite.ui.history

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import unab.edu.co.abrahamcaceres.safebite.R
import unab.edu.co.abrahamcaceres.safebite.databinding.ItemScanHistoryBinding
import unab.edu.co.abrahamcaceres.safebite.model.ScanRisk
import unab.edu.co.abrahamcaceres.safebite.model.cloud.ScanHistoryModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ScanHistoryFirestoreAdapter(
    private val onItemClick: (ScanHistoryModel) -> Unit
) : ListAdapter<ScanHistoryModel, ScanHistoryFirestoreAdapter.ViewHolder>(DiffCallback) {

    private val dateFormat = SimpleDateFormat("d MMM yyyy, HH:mm", Locale.forLanguageTag("es-ES"))

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemScanHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding, dateFormat, onItemClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemScanHistoryBinding,
        private val dateFormat: SimpleDateFormat,
        private val onItemClick: (ScanHistoryModel) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(scan: ScanHistoryModel) {
            val ctx = binding.root.context
            binding.textProductName.text = scan.productName.ifBlank {
                ctx.getString(R.string.history_detail_empty_ingredients)
            }
            binding.textScanDate.text = dateFormat.format(Date(scan.scanDate))
            binding.textScanSnippet.text = scan.rawTextDetected

            when (scan.status) {
                ScanRisk.DANGER -> {
                    binding.chipRisk.setText(R.string.risk_danger)
                    binding.chipRisk.setChipBackgroundColorResource(R.color.risk_danger_container)
                    binding.chipRisk.setTextColor(ContextCompat.getColor(ctx, R.color.risk_danger_on))
                }
                ScanRisk.WARNING -> {
                    binding.chipRisk.setText(R.string.risk_warning)
                    binding.chipRisk.setChipBackgroundColorResource(R.color.risk_warning_container)
                    binding.chipRisk.setTextColor(ContextCompat.getColor(ctx, R.color.risk_warning_on))
                }
                else -> {
                    binding.chipRisk.setText(R.string.risk_safe)
                    binding.chipRisk.setChipBackgroundColorResource(R.color.risk_safe_container)
                    binding.chipRisk.setTextColor(ContextCompat.getColor(ctx, R.color.risk_safe_on))
                }
            }

            binding.chipRisk.isClickable = false
            binding.chipRisk.isFocusable = false
            binding.root.setOnClickListener { onItemClick(scan) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<ScanHistoryModel>() {
        override fun areItemsTheSame(oldItem: ScanHistoryModel, newItem: ScanHistoryModel): Boolean =
            oldItem.scanId == newItem.scanId

        override fun areContentsTheSame(oldItem: ScanHistoryModel, newItem: ScanHistoryModel): Boolean =
            oldItem.scanDate == newItem.scanDate &&
                oldItem.rawTextDetected == newItem.rawTextDetected &&
                oldItem.status == newItem.status
    }
}
