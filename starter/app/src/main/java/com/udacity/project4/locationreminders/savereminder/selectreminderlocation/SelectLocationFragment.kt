package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import java.util.*

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

    private val TAG = "SelectLocationFragment"
    private val REQUEST_LOCATION_PERMISSION = 1
    private val DEFAULT_LATITUDE = -33.87365
    private val DEFAULT_LONGTITUDE = 151.20689
    private val ZOOM_LEVEL = 16f

    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding

    private lateinit var map: GoogleMap
    private var currentMarker: Marker? = null

    private var selectedLatLng: LatLng? = null
    private var selectedPoI: PointOfInterest? = null
    private var selectedAddress: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)

        //Add the map setup implementation
        val mapFragment =
            childFragmentManager.findFragmentById(R.id.google_map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        setSaveButtonEnable(false)
        binding.saveBtn.setOnClickListener { onLocationSelected() }

        return binding.root
    }

    /**
     * save location selected.
     */
    private fun onLocationSelected() {
        _viewModel.latitude.value = selectedLatLng?.latitude
        _viewModel.longitude.value = selectedLatLng?.longitude
        _viewModel.selectedPOI.value = selectedPoI
        _viewModel.reminderSelectedLocationStr.value = selectedAddress
        _viewModel.navigationCommand.value = NavigationCommand.Back
    }

    /**
     * set save button enable
     */
    private fun setSaveButtonEnable(enable: Boolean) {
        Log.d(TAG, "setSaveButtonEnable: ")
        binding.saveBtn.isEnabled = enable
        binding.saveBtn.alpha = when (enable) {
            true -> 1f
            else -> 0.6f
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        // Change the map type based on the user's selection.
        R.id.normal_map -> {
            map.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }
        R.id.hybrid_map -> {
            map.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }
        R.id.satellite_map -> {
            map.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }
        R.id.terrain_map -> {
            map.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    /**
     * onMapReady
     */
    override fun onMapReady(googleMap: GoogleMap) {
        Log.d(TAG, "onMapReady: ")
        map = googleMap
        //Set default location
        val homeLatLng = LatLng(DEFAULT_LATITUDE, DEFAULT_LONGTITUDE)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(homeLatLng, ZOOM_LEVEL))
        setMapLongClick(map)
        setPositionClick(map)
        setGoogleMapStyle(map)
        setEnableMyLocation()
    }

    /**
     * setMapLongClick
     */
    private fun setMapLongClick(map: GoogleMap) {
        map.setOnMapLongClickListener { latLng ->
            resetCurrentMarker()
            // A Snippet is Additional text that's displayed below the title.
            val snippet = String.format(
                Locale.getDefault(),
                getString(R.string.lat_long_snippet),
                latLng.latitude,
                latLng.longitude
            )
            currentMarker = map.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title(getString(R.string.dropped_pin))
                    .snippet(snippet)
            )

            updateSelectedPosition(latLng, snippet)
            if (!binding.saveBtn.isEnabled) {
                setSaveButtonEnable(true)
            }
        }
    }

    /**
     * set position Click
     */
    private fun setPositionClick(map: GoogleMap) {
        map.setOnPoiClickListener { poi ->
            resetCurrentMarker()

            val poiMarker = map.addMarker(
                MarkerOptions().position(poi.latLng)
                    .title(poi.name)
            )
            poiMarker.showInfoWindow()
            currentMarker = poiMarker

            updateSelectedPosition(poi.latLng, poi.name, poi)
            if (!binding.saveBtn.isEnabled) {
                setSaveButtonEnable(true)
            }
        }
    }

    /**
     * updateSelectedPosition
     */
    private fun updateSelectedPosition(
        latLng: LatLng,
        address: String,
        poi: PointOfInterest? = null
    ) {
        selectedLatLng = latLng
        selectedAddress = address
        selectedPoI = poi
    }

    /**
     * resetCurrentMarker
     */
    private fun resetCurrentMarker() {
        if (currentMarker != null) {
            currentMarker?.remove()
            currentMarker = null
        }
    }

    /**
     * setMapStyle
     */
    private fun setGoogleMapStyle(map: GoogleMap) {
        try {
            val success = map.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    requireContext(),
                    R.raw.google_map_style
                )
            )
            if (!success) {
                Log.d(TAG, "setMapStyle: Failed")
            }
        } catch (e: Resources.NotFoundException) {
            Log.e(TAG, "Cannot find style. Error ", e)
        }
    }

    /**
     * enableMyLocation
     */
    private fun setEnableMyLocation() {
        if (isPermissionGranted()) {
            if (context?.let {
                    ActivityCompat.checkSelfPermission(
                        it,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
                } != PackageManager.PERMISSION_GRANTED && context?.let {
                    ActivityCompat.checkSelfPermission(
                        it,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                } != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return
            }
            map.setMyLocationEnabled(true)
        } else {
            requestPermissions(
                arrayOf<String>(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION_PERMISSION
            )
        }
    }

    /**
     * isPermissionGranted
     */
    private fun isPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
        ) === PackageManager.PERMISSION_GRANTED
    }

    /**
     * onRequestPermissionsResult
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.size > 0 && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                setEnableMyLocation()
            } else {
                Log.d(TAG, "onRequestPermissionsResult: Permission denied")
                Snackbar.make(
                    binding.root,
                    R.string.permission_denied_explanation, Snackbar.LENGTH_INDEFINITE
                )
                    .setAction(R.string.settings) {
                        // Displays App settings screen.
                        startActivity(Intent().apply {
                            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                            data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        })
                    }.show()
            }
        }
    }
}
