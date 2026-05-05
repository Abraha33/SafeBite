package unab.edu.co.abrahamcaceres.safebite.ui.allergens

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import unab.edu.co.abrahamcaceres.safebite.databinding.ItemAllergenBinding
import unab.edu.co.abrahamcaceres.safebite.model.Allergen

/**
 * Lista de alérgenos con tarjetas Material y acción de eliminar.
 */
class AllergenAdapter(
    private val onDelete: (Allergen) -> Unit
) : ListAdapter<Allergen, AllergenAdapter.AllergenViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AllergenViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemAllergenBinding.inflate(inflater, parent, false)
        return AllergenViewHolder(binding, onDelete)
    }

    override fun onBindViewHolder(holder: AllergenViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class AllergenViewHolder(
        private val binding: ItemAllergenBinding,
        private val onDelete: (Allergen) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Allergen) {
            binding.textAllergenName.text = item.getDisplayName()
            binding.buttonDeleteAllergen.setOnClickListener { onDelete(item) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<Allergen>() {
        override fun areItemsTheSame(oldItem: Allergen, newItem: Allergen): Boolean =
            oldItem.getId() == newItem.getId()

        override fun areContentsTheSame(oldItem: Allergen, newItem: Allergen): Boolean =
            oldItem.getDisplayName() == newItem.getDisplayName() &&
                oldItem.getNormalizedName() == newItem.getNormalizedName()
    }
}
