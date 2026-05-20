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

        val latitude = arguments?.getFloat("latitude", 7.1193f) ?: 7.1193f
        val longitude = arguments?.getFloat("longitude", -73.1226f) ?: -73.1226f

        setupWebView(latitude, longitude)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView(latitude: Float, longitude: Float) {
        val webView = binding.mapWebView
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.webViewClient = WebViewClient()

        val leafletHtml = """
<!DOCTYPE html>
<html>
<head>
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
    <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
    <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
    <style>
        body, html, #map { margin: 0; padding: 0; height: 100%; width: 100%; }
    </style>
</head>
<body>
    <div id="map"></div>
    <script>
        var map = L.map('map').setView([$latitude, $longitude], 16);

        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
            attribution: '© OpenStreetMap'
        }).addTo(map);

        L.marker([$latitude, $longitude]).addTo(map)
            .bindPopup('<b>¡Producto Seguro Encontrado Aquí!</b>')
            .openPopup();
    </script>
</body>
</html>
""".trimIndent()

        webView.loadDataWithBaseURL(null, leafletHtml, "text/html", "UTF-8", null)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
