package unab.edu.co.abrahamcaceres.safebite.ui.community

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import unab.edu.co.abrahamcaceres.safebite.databinding.FragmentMapSightingBinding

class MapSightingFragment : Fragment() {

    private var _binding: FragmentMapSightingBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapSightingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val latitude = arguments?.getFloat("latitude", 7.1254f) ?: 7.1254f
        val longitude = arguments?.getFloat("longitude", -73.1198f) ?: -73.1198f

        setupWebView(latitude, longitude)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView(latitude: Float, longitude: Float) {
        val webView = binding.mapWebView
        webView.settings.javaScriptEnabled = true
        webView.webViewClient = WebViewClient()

        val mapUrl = "https://www.google.com/maps/search/?api=1&query=$latitude,$longitude"
        webView.loadUrl(mapUrl)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
