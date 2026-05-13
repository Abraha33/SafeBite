package unab.edu.co.abrahamcaceres.safebite.ui.allergens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels

import androidx.recyclerview.widget.LinearLayoutManager
import unab.edu.co.abrahamcaceres.safebite.R
import unab.edu.co.abrahamcaceres.safebite.databinding.FragmentAllergensBinding
import unab.edu.co.abrahamcaceres.safebite.viewmodel.AllergenViewModel
import unab.edu.co.abrahamcaceres.safebite.viewmodel.AllergenViewModelFactory

/**
 * GestiÃ³n de la lista negra de alÃ©rgenos: formulario validado + RecyclerView + Room.
 */
class AllergensFragment : Fragment() {

    private var _binding: FragmentAllergensBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AllergenViewModel by viewModels {
        AllergenViewModelFactory(requireActivity().application)
    }

    private val adapter = AllergenAdapter { allergen ->
        viewModel.deleteAllergen(allergen)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAllergensBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvAllergens.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAllergens.adapter = adapter

        binding.etAllergenName.doAfterTextChanged {
            binding.tilAllergen.error = null
            viewModel.clearFormError()
        }

        viewModel.formError.observe(viewLifecycleOwner) { message ->
            binding.tilAllergen.error = message
            binding.tilAllergen.isErrorEnabled = message != null
        }

        viewModel.allergens.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            val empty = list.isNullOrEmpty()
            binding.textEmptyAllergens.visibility = if (empty) View.VISIBLE else View.GONE
            binding.rvAllergens.visibility = if (empty) View.GONE else View.VISIBLE
        }

        binding.btnAddAllergen.setOnClickListener {
            val name = binding.etAllergenName.text?.toString().orEmpty().trim()

            when {
                name.isEmpty() -> {
                    binding.tilAllergen.error = getString(R.string.error_allergen_empty)
                    binding.tilAllergen.isErrorEnabled = true
                }

                name.length < 3 -> {
                    binding.tilAllergen.error = getString(R.string.error_allergen_short)
                    binding.tilAllergen.isErrorEnabled = true
                }

                else -> {
                    binding.tilAllergen.error = null
                    binding.tilAllergen.isErrorEnabled = false
                    viewModel.insertAllergen(name)
                    binding.etAllergenName.text?.clear()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
