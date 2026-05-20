package unab.edu.co.abrahamcaceres.safebite.ui.community

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import unab.edu.co.abrahamcaceres.safebite.R
import unab.edu.co.abrahamcaceres.safebite.databinding.FragmentCommunityBinding
import unab.edu.co.abrahamcaceres.safebite.model.Sighting
import unab.edu.co.abrahamcaceres.safebite.viewmodel.CommunityViewModel
import unab.edu.co.abrahamcaceres.safebite.viewmodel.CommunityViewModelFactory

class CommunityFragment : Fragment() {

    private var _binding: FragmentCommunityBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CommunityViewModel by viewModels {
        CommunityViewModelFactory(requireActivity().application)
    }

    private val adapter = CommunityAdapter { sighting ->
        onItemClick(sighting)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCommunityBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvCommunityFeed.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCommunityFeed.adapter = adapter

        setupCitySelector()
        setupFab()

        viewModel.seedIfEmpty()

        viewModel.sightings.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            val empty = list.isNullOrEmpty()
            binding.layoutEmptyFeed.visibility = if (empty) View.VISIBLE else View.GONE
            binding.rvCommunityFeed.visibility = if (empty) View.GONE else View.VISIBLE
        }
    }

    private fun setupCitySelector() {
        binding.layoutCitySelector.setOnClickListener {
            val cities = resources.getStringArray(R.array.cities)
            val currentCity = viewModel.selectedCity.value ?: getString(R.string.community_default_city)
            val checkedIndex = cities.indexOfFirst {
                it.equals(currentCity, ignoreCase = true) ||
                    (currentCity == getString(R.string.community_default_city) && it == cities[1])
            }.coerceAtLeast(0)

            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.community_city_dialog_title)
                .setSingleChoiceItems(cities, checkedIndex) { dialog, which ->
                    dialog.dismiss()
                    val selected = cities[which]
                    val filterValue = if (which == 0) "" else selected
                    viewModel.filterByCity(filterValue)
                    binding.textSelectedCity.text = selected
                }
                .show()
        }
    }

    private fun setupFab() {
        binding.fabPublishTip.setOnClickListener {
            viewModel.publishSighting(
                Sighting.crear(
                    creatorName = "Tú",
                    timeAgo = "Ahora",
                    productName = "Nuevo producto",
                    storeName = "Tienda local",
                    communityTip = "Comparte tu experiencia con la comunidad...",
                    targetCity = binding.textSelectedCity.text.toString(),
                    allergenTag = "Gluten-Free"
                )
            )
        }
    }

    private fun onItemClick(sighting: Sighting) {
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
