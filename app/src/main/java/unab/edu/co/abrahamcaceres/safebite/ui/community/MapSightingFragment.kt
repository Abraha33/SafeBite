package unab.edu.co.abrahamcaceres.safebite.ui.community

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import unab.edu.co.abrahamcaceres.safebite.R
import unab.edu.co.abrahamcaceres.safebite.databinding.FragmentMapSightingBinding

class MapSightingFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentMapSightingBinding? = null
    private val binding get() = _binding!!

    private var latitude: Float = 7.1254f
    private var longitude: Float = -73.1198f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            latitude = it.getFloat("latitude", 7.1254f)
            longitude = it.getFloat("longitude", -73.1198f)
        }
    }

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
        setupToolbar()
        val mapFragment = childFragmentManager
            .findFragmentById(R.id.mapContainer) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun setupToolbar() {
        binding.toolbarMap.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
    }

    override fun onMapReady(googleMap: com.google.android.gms.maps.GoogleMap) {
        val position = LatLng(latitude.toDouble(), longitude.toDouble())
        googleMap.clear()
        googleMap.addMarker(
            MarkerOptions()
                .position(position)
                .title("Avistamiento")
        )
        googleMap.moveCamera(
            CameraUpdateFactory.newLatLngZoom(position, 15f)
        )
        googleMap.uiSettings.isZoomControlsEnabled = true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
