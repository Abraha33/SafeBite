package unab.edu.co.abrahamcaceres.safebite.ui.blacklist

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import unab.edu.co.abrahamcaceres.safebite.R
import unab.edu.co.abrahamcaceres.safebite.databinding.ItemBlacklistBinding
import unab.edu.co.abrahamcaceres.safebite.model.ScanRisk
import unab.edu.co.abrahamcaceres.safebite.model.cloud.ProductModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BlacklistAdapter(
    private val items: MutableList<ProductModel>,
    private val onRemove: (ProductModel) -> Unit
) : RecyclerView.Adapter<BlacklistAdapter.ViewHolder>() {

    private val dateFormat = SimpleDateFormat("d MMM yyyy, HH:mm", Locale.forLanguageTag("es-ES"))

    fun updateList(newList: List<ProductModel>) {
        items.clear()
        items.addAll(newList)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemBlacklistBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class ViewHolder(
        private val binding: ItemBlacklistBinding
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

            val riskChip = binding.chipBlacklistRisk
            when (product.riskLevel) {
                ScanRisk.DANGER -> {
                    riskChip.setText(R.string.blacklist_risk_danger)
                    riskChip.setChipBackgroundColorResource(R.color.risk_danger_container)
                    riskChip.setTextColor(ContextCompat.getColor(ctx, R.color.risk_danger_on))
                }
                ScanRisk.WARNING -> {
                    riskChip.setText(R.string.blacklist_risk_warning)
                    riskChip.setChipBackgroundColorResource(R.color.risk_warning_container)
                    riskChip.setTextColor(ContextCompat.getColor(ctx, R.color.risk_warning_on))
                }
                else -> {
                    riskChip.setText(R.string.risk_safe)
                    riskChip.setChipBackgroundColorResource(R.color.risk_safe_container)
                    riskChip.setTextColor(ContextCompat.getColor(ctx, R.color.risk_safe_on))
                }
            }

            binding.btnRemoveFromBlacklist.setOnClickListener {
                onRemove(product)
            }
        }
    }
}
