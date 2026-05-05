package unab.edu.co.abrahamcaceres.safebite.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import unab.edu.co.abrahamcaceres.safebite.R
import unab.edu.co.abrahamcaceres.safebite.databinding.FragmentScannerBinding

/**
 * Destino post-autenticación (placeholder).
 * Aquí integrarás CameraX + ML Kit según el alcance del proyecto.
 */
class ScannerFragment : Fragment() {

    private var _binding: FragmentScannerBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScannerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.buttonManageAllergens.setOnClickListener {
            findNavController().navigate(R.id.action_scanner_to_allergens)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
