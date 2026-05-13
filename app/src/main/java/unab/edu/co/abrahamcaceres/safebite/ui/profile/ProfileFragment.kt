package unab.edu.co.abrahamcaceres.safebite.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import unab.edu.co.abrahamcaceres.safebite.R
import unab.edu.co.abrahamcaceres.safebite.databinding.FragmentProfileBinding
import unab.edu.co.abrahamcaceres.safebite.viewmodel.ProfileViewModel
import unab.edu.co.abrahamcaceres.safebite.viewmodel.ProfileViewModelFactory

/**
 * Perfil del usuario autenticado con resumen simple de actividad y cierre de sesiÃ³n.
 */
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels {
        ProfileViewModelFactory(requireActivity().application)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbarProfile.setupWithNavController(findNavController())
        setupObservers()
        binding.buttonLogout.setOnClickListener {
            viewModel.logout()
        }
        viewModel.loadCurrentUser()
    }

    private fun setupObservers() {
        viewModel.currentUser.observe(viewLifecycleOwner) { user ->
            binding.textProfileName.text = user?.getFullName().orEmpty()
            binding.textProfileEmail.text = user?.getEmail().orEmpty()
        }

        viewModel.scanCount.observe(viewLifecycleOwner) { total ->
            binding.textProfileScanSummary.text = getString(R.string.profile_scan_summary, total)
        }

        viewModel.shouldReturnToLogin.observe(viewLifecycleOwner) { shouldNavigate ->
            if (shouldNavigate) {
                viewModel.consumeReturnToLogin()
                findNavController().navigate(R.id.action_profile_to_login)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
