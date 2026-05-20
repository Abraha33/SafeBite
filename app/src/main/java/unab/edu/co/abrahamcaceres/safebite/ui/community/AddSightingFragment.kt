package unab.edu.co.abrahamcaceres.safebite.ui.community

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import unab.edu.co.abrahamcaceres.safebite.R
import unab.edu.co.abrahamcaceres.safebite.databinding.FragmentAddSightingBinding
import unab.edu.co.abrahamcaceres.safebite.model.Sighting
import unab.edu.co.abrahamcaceres.safebite.utils.InputValidators
import unab.edu.co.abrahamcaceres.safebite.viewmodel.CommunityViewModel
import unab.edu.co.abrahamcaceres.safebite.viewmodel.CommunityViewModelFactory

class AddSightingFragment : Fragment() {

    private var _binding: FragmentAddSightingBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CommunityViewModel by viewModels {
        CommunityViewModelFactory(requireActivity().application)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddSightingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupTextWatchers()
        setupPublishButton()
    }

    private fun setupToolbar() {
        binding.toolbarAddSighting.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun setupTextWatchers() {
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validateForm()
            }
        }
        binding.etProductName.addTextChangedListener(watcher)
        binding.etStoreName.addTextChangedListener(watcher)
    }

    private fun validateForm() {
        val product = binding.etProductName.text?.toString().orEmpty()
        val store = binding.etStoreName.text?.toString().orEmpty()
        val valid = InputValidators.isNotBlank(product) && InputValidators.isNotBlank(store)
        binding.btnPublishSighting.isEnabled = valid
    }

    private fun setupPublishButton() {
        binding.btnPublishSighting.setOnClickListener {
            publishSighting()
        }
    }

    private fun publishSighting() {
        val productName = binding.etProductName.text?.toString().orEmpty().trim()
        val storeName = binding.etStoreName.text?.toString().orEmpty().trim()
        val price = binding.etPrice.text?.toString().orEmpty().trim()
        val communityTip = binding.etCommunityTip.text?.toString().orEmpty().trim()

        val allergenTag = when (binding.chipGroupAllergenTag.checkedChipId) {
            R.id.chipGlutenFree -> "Gluten-Free"
            R.id.chipLactoseFree -> "Lactose-Free"
            R.id.chipVegan -> "Vegano"
            R.id.chipPeanutFree -> "Peanut-Free"
            R.id.chipSugarFree -> "Sugar-Free"
            else -> ""
        }

        val sighting = Sighting.crear(
            creatorName = "Tú",
            timeAgo = "Justo ahora",
            productName = productName,
            storeName = storeName,
            price = if (price.isNotBlank()) "$$price" else "",
            communityTip = communityTip,
            targetCity = "Bucaramanga",
            allergenTag = allergenTag
        )

        viewModel.publishSighting(sighting)

        clearFields()
        hideKeyboard()
        findNavController().popBackStack()
    }

    private fun clearFields() {
        binding.etProductName.text?.clear()
        binding.etStoreName.text?.clear()
        binding.etPrice.text?.clear()
        binding.etCommunityTip.text?.clear()
        binding.chipGroupAllergenTag.clearCheck()
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.btnPublishSighting.windowToken, 0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
