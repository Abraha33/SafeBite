package unab.edu.co.abrahamcaceres.safebite.ui.community

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import unab.edu.co.abrahamcaceres.safebite.R
import unab.edu.co.abrahamcaceres.safebite.databinding.FragmentAddSightingBinding
import unab.edu.co.abrahamcaceres.safebite.model.cloud.SightingCloudModel
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
        observePublishResult()
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
        if (viewModel.isPublishing.value != true) {
            binding.btnPublishSighting.isEnabled = valid
        }
    }

    private fun setupPublishButton() {
        binding.btnPublishSighting.setOnClickListener {
            publishSighting()
        }
    }

    private fun observePublishResult() {
        viewModel.publishResult.observe(viewLifecycleOwner) { result ->
            if (result == null) return@observe
            if (result) {
                Toast.makeText(requireContext(), R.string.add_sighting_publish_ok, Toast.LENGTH_SHORT).show()
                clearFields()
                hideKeyboard()
                findNavController().popBackStack()
            } else {
                Toast.makeText(requireContext(), R.string.error_unknown, Toast.LENGTH_SHORT).show()
            }
            viewModel.consumePublishResult()
        }
        viewModel.isPublishing.observe(viewLifecycleOwner) { publishing ->
            binding.btnPublishSighting.isEnabled = !publishing
            binding.btnPublishSighting.text = if (publishing) {
                getString(R.string.add_sighting_publishing)
            } else {
                getString(R.string.add_sighting_publish)
            }
        }
    }

    private fun publishSighting() {
        val productName = binding.etProductName.text?.toString().orEmpty().trim()
        val storeName = binding.etStoreName.text?.toString().orEmpty().trim()
        val priceStr = binding.etPrice.text?.toString().orEmpty().trim()
        val communityTip = binding.etCommunityTip.text?.toString().orEmpty().trim()

        val allergenTag = when (binding.chipGroupAllergenTag.checkedChipId) {
            R.id.chipGlutenFree -> "Gluten-Free"
            R.id.chipLactoseFree -> "Lactose-Free"
            R.id.chipVegan -> "Vegano"
            R.id.chipPeanutFree -> "Peanut-Free"
            R.id.chipSugarFree -> "Sugar-Free"
            else -> ""
        }

        val price = priceStr.toDoubleOrNull() ?: 0.0
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        val sighting = SightingCloudModel(
            creatorUid = uid,
            creatorName = "",
            productName = productName,
            storeName = storeName,
            price = price,
            communityTip = communityTip,
            allergenTag = allergenTag,
            targetCity = "Bucaramanga"
        )
        viewModel.publishSighting(sighting)
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
