package unab.edu.co.abrahamcaceres.safebite.ui.blacklist

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import unab.edu.co.abrahamcaceres.safebite.R
import unab.edu.co.abrahamcaceres.safebite.databinding.ItemBlacklistBinding
import unab.edu.co.abrahamcaceres.safebite.model.ScanRisk
import unab.edu.co.abrahamcaceres.safebite.model.cloud.ProductModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BlacklistAdapter(
    private val onRemove: (ProductModel) -> Unit
) : ListAdapter<ProductModel, BlacklistAdapter.ViewHolder>(DiffCallback) {

    private val dateFormat = SimpleDateFormat("d MMM yyyy, HH:mm", Locale.forLanguageTag("es-ES"))

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemBlacklistBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding, dateFormat, onRemove)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemBlacklistBinding,
        private val dateFormat: SimpleDateFormat,
        private val onRemove: (ProductModel) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(product: ProductModel) {
            val ctx = binding.root.context
            binding.textBlacklistProductName.text = product.productName.ifBlank {
                ctx.getString(R.string.history_detail_empty_ingredients)
            }
            binding.textBlacklistDetectedText.text = product.detectedText
            binding.textBlacklistDate.text = ctx.getString(
                R.string.blacklist_added_date,
                dateFormat.format(Date(product.addedAt))
            )

            when (product.riskLevel) {
                ScanRisk.DANGER -> {
                    binding.chipBlacklistRisk.setText(R.string.blacklist_risk_danger)
                    binding.chipBlacklistRisk.setChipBackgroundColorResource(R.color.risk_danger_container)
                    binding.chipBlacklistRisk.setTextColor(ContextCompat.getColor(ctx, R.color.risk_danger_on))
                }
                ScanRisk.WARNING -> {
                    binding.chipBlacklistRisk.setText(R.string.blacklist_risk_warning)
                    binding.chipBlacklistRisk.setChipBackgroundColorResource(R.color.risk_warning_container)
                    binding.chipBlacklistRisk.setTextColor(ContextCompat.getColor(ctx, R.color.risk_warning_on))
                }
                else -> {
                    binding.chipBlacklistRisk.setText(R.string.risk_safe)
                    binding.chipBlacklistRisk.setChipBackgroundColorResource(R.color.risk_safe_container)
                    binding.chipBlacklistRisk.setTextColor(ContextCompat.getColor(ctx, R.color.risk_safe_on))
                }
            }

            binding.btnRemoveFromBlacklist.setOnClickListener { onRemove(product) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<ProductModel>() {
        override fun areItemsTheSame(oldItem: ProductModel, newItem: ProductModel): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: ProductModel, newItem: ProductModel): Boolean =
            oldItem.productName == newItem.productName &&
                oldItem.detectedText == newItem.detectedText &&
                oldItem.riskLevel == newItem.riskLevel &&
                oldItem.addedAt == newItem.addedAt
    }
}
