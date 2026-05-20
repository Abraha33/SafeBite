package unab.edu.co.abrahamcaceres.safebite.ui.community

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import unab.edu.co.abrahamcaceres.safebite.R
import unab.edu.co.abrahamcaceres.safebite.databinding.ItemCommunityPostBinding
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
            binding.textProductName.text = sighting.getProductName()
            binding.textCommunityTip.text = sighting.getCommunityTip()
            binding.textStoreLocation.text = sighting.getStoreName()
            binding.textPrice.text = sighting.getPrice()

            val tag = sighting.getAllergenTag()
            val isSafe = tag.contains("Free", ignoreCase = true) ||
                tag.contains("Seguro", ignoreCase = true)

            binding.textAllergenTag.text = if (isSafe) {
                ctx.getString(R.string.community_allergen_safe)
            } else {
                ctx.getString(R.string.community_allergen_alert, tag)
            }

            val bgRes = if (isSafe) R.color.risk_safe_on else R.color.risk_danger_on
            binding.cardAllergenAlert.setCardBackgroundColor(
                ContextCompat.getColor(ctx, bgRes)
            )

            binding.root.setOnClickListener { onItemClick(sighting) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<Sighting>() {
        override fun areItemsTheSame(oldItem: Sighting, newItem: Sighting): Boolean =
            oldItem.getId() == newItem.getId()

        override fun areContentsTheSame(oldItem: Sighting, newItem: Sighting): Boolean =
            oldItem.getProductName() == newItem.getProductName() &&
                oldItem.getCommunityTip() == newItem.getCommunityTip() &&
                oldItem.getAllergenTag() == newItem.getAllergenTag() &&
                oldItem.getStoreName() == newItem.getStoreName()
    }
}
