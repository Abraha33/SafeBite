package unab.edu.co.abrahamcaceres.safebite.ui.community

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import unab.edu.co.abrahamcaceres.safebite.R
import unab.edu.co.abrahamcaceres.safebite.databinding.ItemCommunityPostBinding
import unab.edu.co.abrahamcaceres.safebite.model.ScanRisk
import unab.edu.co.abrahamcaceres.safebite.model.Sighting

class CommunityAdapter(
    private val onItemClick: (Sighting) -> Unit
) : ListAdapter<Sighting, CommunityAdapter.SightingViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SightingViewHolder {
        val binding = ItemCommunityPostBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return SightingViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: SightingViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class SightingViewHolder(
        private val binding: ItemCommunityPostBinding,
        private val onItemClick: (Sighting) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(sighting: Sighting) {
            val ctx = binding.root.context

            binding.textCreatorName.text = sighting.getCreatorName()
            binding.textTimeAgo.text = sighting.getTimeAgo()
            binding.textPostTitle.text = sighting.getTitle()
            binding.textPostDescription.text = sighting.getDescription()
            binding.textLocationName.text = sighting.getLocationName()
            binding.textAllergenStatus.text = sighting.getAllergenStatusText()

            bindAllergenColors(sighting, ctx)

            binding.root.setOnClickListener { onItemClick(sighting) }
        }

        private fun bindAllergenColors(sighting: Sighting, ctx: android.content.Context) {
            val bgRes = when (sighting.getAllergenRiskLevel()) {
                ScanRisk.DANGER -> R.color.risk_danger_on
                ScanRisk.WARNING -> R.color.risk_warning_on
                else -> R.color.risk_safe_on
            }
            binding.cardAllergenAlert.setCardBackgroundColor(
                ContextCompat.getColor(ctx, bgRes)
            )
            binding.textAllergenStatus.setTextColor(
                ContextCompat.getColor(ctx, android.R.color.white)
            )
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<Sighting>() {
        override fun areItemsTheSame(oldItem: Sighting, newItem: Sighting): Boolean =
            oldItem.getId() == newItem.getId()

        override fun areContentsTheSame(oldItem: Sighting, newItem: Sighting): Boolean =
            oldItem.getTitle() == newItem.getTitle() &&
                oldItem.getDescription() == newItem.getDescription() &&
                oldItem.getAllergenRiskLevel() == newItem.getAllergenRiskLevel() &&
                oldItem.getTimeAgo() == newItem.getTimeAgo()
    }
}
