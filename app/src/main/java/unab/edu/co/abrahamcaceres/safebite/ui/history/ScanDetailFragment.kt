package unab.edu.co.abrahamcaceres.safebite.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.textview.MaterialTextView
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import unab.edu.co.abrahamcaceres.safebite.R
import unab.edu.co.abrahamcaceres.safebite.data.repository.FirebaseFirestoreRepository
import unab.edu.co.abrahamcaceres.safebite.databinding.FragmentScanDetailBinding
import unab.edu.co.abrahamcaceres.safebite.model.ScanRisk
import unab.edu.co.abrahamcaceres.safebite.viewmodel.ScanHistoryViewModel
import unab.edu.co.abrahamcaceres.safebite.viewmodel.ScanHistoryViewModelFactory
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class ScanDetailFragment : Fragment() {

    private var _binding: FragmentScanDetailBinding? = null
    private val binding get() = _binding!!

    private val args: ScanDetailFragmentArgs by navArgs()

    private val viewModel: ScanHistoryViewModel by viewModels {
        ScanHistoryViewModelFactory(requireActivity().application)
    }

    private val firestoreRepo = FirebaseFirestoreRepository()

    private val dateFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy, HH:mm", Locale.forLanguageTag("es-ES"))
            .withZone(ZoneId.systemDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScanDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(insets.left, insets.top, insets.right, insets.bottom)
            windowInsets
        }

        binding.toolbarDetail.setupWithNavController(findNavController())

        loadUserAllergens()

        val scanId = args.scanId

        viewModel.scans.observe(viewLifecycleOwner) { list ->
            val scan = list.find { it.getId() == scanId }
            scan?.let { item ->
                val rawText = item.getDetectedText()
                bindVerdict(item.getRiskLevel())
                binding.textScanDate.text = dateFormatter.format(
                    Instant.ofEpochMilli(item.getCreatedAtMs())
                )
                parseAndDisplayIngredients(rawText)
            }
        }
    }

    private fun loadUserAllergens() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        lifecycleScope.launch {
            firestoreRepo.fetchUserProfile(uid).onSuccess { user ->
                val allergens = user.allergens
                if (allergens.isEmpty()) {
                    binding.cardAllergensPanel.visibility = View.GONE
                    return@launch
                }
                binding.layoutAllergensChecklist.removeAllViews()
                for (allergen in allergens) {
                    val row = layoutInflater.inflate(R.layout.item_allergen_check, null) as ViewGroup
                    row.findViewById<MaterialTextView>(R.id.textAllergenName).text = allergen
                    binding.layoutAllergensChecklist.addView(row)
                }
            }.onFailure {
                binding.cardAllergensPanel.visibility = View.GONE
            }
        }
    }

    private fun parseAndDisplayIngredients(rawText: String) {
        val lower = rawText.lowercase(Locale.getDefault())
        val ingredientsStart = findIngredientsStart(lower)

        val ingredientList = if (ingredientsStart >= 0) {
            val afterLabel = rawText.substring(ingredientsStart).trim()
            afterLabel
                .split(",")
                .map { it.trim().replace(Regex("^[\\s\\.\\;\\:\\!\\?]+|[\\s\\.\\;\\:\\!\\?]+$"), "") }
                .filter { it.isNotBlank() }
        } else {
            rawText
                .split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() && it.length > 2 }
        }

        binding.chipIngredientCount.text = "${ingredientList.size} ${getString(R.string.ingredients_processed)}"
        binding.chipIngredientCount.visibility = View.VISIBLE

        binding.tvIngredientsList.text = "(${ingredientList.joinToString(", ")})"

        binding.layoutIngredientGrid.removeAllViews()
        val chunked = ingredientList.chunked(2)
        for (pair in chunked) {
            val row = layoutInflater.inflate(R.layout.item_ingredient_row, null) as ViewGroup
            val leftText = row.findViewById<MaterialTextView>(R.id.textIngredientLeft)
            val rightText = row.findViewById<MaterialTextView>(R.id.textIngredientRight)
            leftText.text = pair.getOrElse(0) { "" }
            leftText.visibility = if (pair.size > 0) View.VISIBLE else View.GONE
            rightText.text = pair.getOrElse(1) { "" }
            rightText.visibility = if (pair.size > 1) View.VISIBLE else View.GONE
            binding.layoutIngredientGrid.addView(row)
        }
    }

    private fun findIngredientsStart(text: String): Int {
        val keywords = listOf("ingredientes:", "ingredientes ")
        for (kw in keywords) {
            val idx = text.indexOf(kw)
            if (idx >= 0) return idx + kw.length
        }
        return -1
    }

    private fun bindVerdict(riskLevel: String) {
        val ctx = requireContext()
        val (bgRes, textColorRes, title, description) = when (riskLevel) {
            ScanRisk.DANGER -> arrayOf(
                R.color.risk_danger_container,
                R.color.risk_danger_on,
                getString(R.string.verdict_danger_title),
                getString(R.string.verdict_danger_body)
            )
            ScanRisk.WARNING -> arrayOf(
                R.color.risk_warning_container,
                R.color.risk_warning_on,
                getString(R.string.verdict_warning_title),
                getString(R.string.verdict_warning_body)
            )
            else -> arrayOf(
                R.color.risk_safe_container,
                R.color.risk_safe_on,
                getString(R.string.verdict_safe_title),
                getString(R.string.verdict_safe_body)
            )
        }

        binding.cardVerdictContainer.setCardBackgroundColor(
            ContextCompat.getColor(ctx, bgRes as Int)
        )
        binding.textVerdictTitle.text = title as String
        binding.textVerdictTitle.setTextColor(ContextCompat.getColor(ctx, textColorRes as Int))
        binding.textVerdictDescription.text = description as String
        binding.textVerdictDescription.setTextColor(ContextCompat.getColor(ctx, textColorRes))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
