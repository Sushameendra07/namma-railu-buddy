package com.greatingcard.nammarailubuddy

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.greatingcard.nammarailubuddy.databinding.ActivityMapBinding

class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityMapBinding
    private lateinit var mMap: GoogleMap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        binding.fabBack.setOnClickListener { finish() }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        val lat = intent.getDoubleExtra(EXTRA_LAT, Double.NaN)
        val lng = intent.getDoubleExtra(EXTRA_LNG, Double.NaN)
        val title = intent.getStringExtra(EXTRA_TITLE) ?: getString(R.string.train_location_marker)
        val subtitle = intent.getStringExtra(EXTRA_SUBTITLE).orEmpty()
        val destLat = intent.getDoubleExtra(EXTRA_DEST_LAT, Double.NaN)
        val destLng = intent.getDoubleExtra(EXTRA_DEST_LNG, Double.NaN)
        val destTitle = intent.getStringExtra(EXTRA_DEST_TITLE)

        if (!lat.isNaN() && !lng.isNaN()) {
            val trainPos = LatLng(lat, lng)
            val trainMarker = MarkerOptions()
                .position(trainPos)
                .title(title)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
            if (subtitle.isNotBlank()) trainMarker.snippet(subtitle)
            mMap.addMarker(trainMarker)
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(trainPos, 11f))
        }

        if (!destLat.isNaN() && !destLng.isNaN()) {
            val destPos = LatLng(destLat, destLng)
            mMap.addMarker(
                MarkerOptions()
                    .position(destPos)
                    .title(destTitle ?: getString(R.string.destination_station_hint))
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
            )
            if (lat.isNaN()) {
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(destPos, 10f))
            }
        }
    }

    companion object {
        const val EXTRA_LAT = "extra_lat"
        const val EXTRA_LNG = "extra_lng"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_SUBTITLE = "extra_subtitle"
        const val EXTRA_DEST_LAT = "extra_dest_lat"
        const val EXTRA_DEST_LNG = "extra_dest_lng"
        const val EXTRA_DEST_TITLE = "extra_dest_title"
    }
}
