package unab.edu.co.abrahamcaceres.safebite.ui.history

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import unab.edu.co.abrahamcaceres.safebite.R
import unab.edu.co.abrahamcaceres.safebite.databinding.ItemScanHistoryBinding
import unab.edu.co.abrahamcaceres.safebite.model.ProductScan
import unab.edu.co.abrahamcaceres.safebite.model.ScanRisk
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Historial de escaneos con tarjetas Material y callback de selecciÃ³n.
 * La navegaciÃ³n queda en el Fragment para respetar la separaciÃ³n de responsabilidades.
 */
class ScanHistoryAdapter(
    private val onItemClick: (ProductScan) -> Unit
) : ListAdapter<ProductScan, ScanHistoryAdapter.ScanViewHolder>(DiffCallback) {

    private val dateFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.forLanguageTag("es-ES"))
            .withZone(ZoneId.systemDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScanViewHolder {
        val binding = ItemScanHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ScanViewHolder(binding, dateFormatter, onItemClick)
    }

    override fun onBindViewHolder(holder: ScanViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ScanViewHolder(
        private val binding: ItemScanHistoryBinding,
        private val dateFormatter: DateTimeFormatter,
        private val onItemClick: (ProductScan) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(scan: ProductScan) {
            val ctx = binding.root.context
            val instant = Instant.ofEpochMilli(scan.getCreatedAtMs())
            binding.textProductName.text = scan.getProductName()
            binding.textScanDate.text = dateFormatter.format(instant)
            binding.textScanSnippet.text = scan.getDetectedText()

            bindRiskChip(binding.chipRisk, scan.getRiskLevel(), ctx)
            binding.chipRisk.isClickable = false
            binding.chipRisk.isFocusable = false

            binding.root.setOnClickListener { onItemClick(scan) }
        }

        private fun bindRiskChip(chip: Chip, level: String, ctx: android.content.Context) {
            when (level) {
                ScanRisk.DANGER -> {
                    chip.setText(R.string.risk_danger)
                    chip.setChipBackgroundColorResource(R.color.risk_danger_container)
                    chip.setTextColor(ContextCompat.getColor(ctx, R.color.risk_danger_on))
                }

                ScanRisk.WARNING -> {
                    chip.setText(R.string.risk_warning)
                    chip.setChipBackgroundColorResource(R.color.risk_warning_container)
                    chip.setTextColor(ContextCompat.getColor(ctx, R.color.risk_warning_on))
                }

                else -> {
                    chip.setText(R.string.risk_safe)
                    chip.setChipBackgroundColorResource(R.color.risk_safe_container)
                    chip.setTextColor(ContextCompat.getColor(ctx, R.color.risk_safe_on))
                }
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<ProductScan>() {
        override fun areItemsTheSame(oldItem: ProductScan, newItem: ProductScan): Boolean =
            oldItem.getId() == newItem.getId()

        override fun areContentsTheSame(oldItem: ProductScan, newItem: ProductScan): Boolean =
            oldItem.getCreatedAtMs() == newItem.getCreatedAtMs() &&
                oldItem.getDetectedText() == newItem.getDetectedText() &&
                oldItem.getRiskLevel() == newItem.getRiskLevel()
    }
}
