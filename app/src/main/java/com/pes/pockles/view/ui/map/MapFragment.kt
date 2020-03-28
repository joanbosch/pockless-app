package com.pes.pockles.view.ui.map


import android.os.Bundle
import android.os.Looper
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.afollestad.assent.Permission
import com.afollestad.assent.runWithPermissions
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.pes.pockles.R
import com.pes.pockles.data.Resource
import com.pes.pockles.databinding.FragmentMapBinding
import com.pes.pockles.model.Pock
import com.pes.pockles.util.LastLocationListener
import com.pes.pockles.util.LocationUtils
import com.pes.pockles.view.viewmodel.ViewModelFactory
import kotlin.math.cos
import kotlin.math.ln


/**
 * A [Fragment] subclass for map view.
 */
open class MapFragment : Fragment(), OnMapReadyCallback {

    companion object {
        const val INTERVAL: Long = 60 * 1000 //interval for updates the loc
        const val FASTEST_INTERVAL: Long = 10 * 1000 //this is when it need higher precision
    }

    private val viewModel: MapViewModel by lazy {
        ViewModelProviders.of(this, ViewModelFactory()).get(MapViewModel::class.java)
    }

    private var googleMap: GoogleMap? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding: FragmentMapBinding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_map, container, false
        )

        binding.lifecycleOwner = this
        binding.mapViewModel = viewModel

        val mapFragment = childFragmentManager.findFragmentById(R.id.mapView) as SupportMapFragment?
        mapFragment!!.getMapAsync(this)
        binding.outlinedButton.setOnClickListener() {
            showFilterDialog()
        }

        return binding.root
    }

    private fun showFilterDialog() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        val categories = arrayOf("Anuncios", "Compra&Venta", "Deportes", "Entretenimiento", "Mascotas", "Salud", "Tecnología", "Tursimo", "+18", "Varios")
        val checkedItems = booleanArrayOf(true, true, true, true, true, true, true, true, true, true)
        lateinit var dialog: AlertDialog
        val builder = context?.let { AlertDialog.Builder(it) }
        builder?.setTitle("Marca las categorías de Pock que desees ver en tu mapa...")


        builder?.setMultiChoiceItems(categories, checkedItems, {dialog,which,isChecked->
            // Update the clicked item checked status
            categories[which] = isChecked.toString()
            !checkedItems[which] //Keep new checked status

            //envio de información [INCOMPLETO]
        })

        builder?.setPositiveButton("FILTRAR") { dialog, which ->
            // Do something when user press the positive button

        }
        builder?.setNeutralButton("ATRÁS") { _, _ ->
        }
        dialog = builder?.create()
        dialog.show()
    }


    override fun onMapReady(googleMap: GoogleMap) {
        this.googleMap = googleMap
        runWithPermissions(Permission.ACCESS_COARSE_LOCATION, Permission.ACCESS_FINE_LOCATION) {

            setupMap();

            startLocationUpdates()

            viewModel.pocks.observe(
                this,
                Observer { value: Resource<List<Pock>>? ->
                    value?.let {
                        when (value) {
                            is Resource.Success<*> -> handleSuccess(value as Resource.Success<List<Pock>>)
                            //   is Resource.Error -> handleError(value.exception)
                        }
                    }
                })
        }
    }

    private fun setupMap() {
        googleMap!!.isMyLocationEnabled = true
        googleMap!!.cameraPosition

        LocationUtils.getLatLocation(activity!!, object : LastLocationListener {
            override fun onLocationReady(location: android.location.Location) {
                val latLng = LatLng(location.latitude, location.longitude)
                val center: CameraPosition = CameraPosition.Builder()
                    .target(latLng)
                    .zoom(getZoomForMetersWide(latLng, 500))
                    .build();

                googleMap!!.animateCamera(CameraUpdateFactory.newCameraPosition(center))
            }

            override fun onLocationError(error: Throwable?) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }
        })

    }

    fun getZoomForMetersWide(
        latLngPoint: LatLng,
        desiredMeters: Int
    ): Float {
        // calculate width screen
        val displayMetrics = DisplayMetrics()
        activity!!.windowManager.defaultDisplay.getMetrics(displayMetrics)
        val mapViewWidth: Int = displayMetrics.widthPixels

        // get zoom for meters
        val metrics: DisplayMetrics = context!!.resources.displayMetrics
        val mapWidth: Float = mapViewWidth / metrics.density
        val latitudinalAdjustment: Double = cos(Math.PI * latLngPoint.latitude / 180.0)
        val arg: Double = 40075004 * mapWidth * latitudinalAdjustment / (desiredMeters * 256.0)
        return (ln(arg) / ln(2.0)).toFloat()
    }

    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            onLocationChanged(locationResult.lastLocation)
        }
    }

    fun onLocationChanged(location: android.location.Location?) {
        location?.let {
            viewModel.updateLocation(it)
        }
    }

    private fun startLocationUpdates() {
        val locationRequest: LocationRequest = LocationRequest.create()
        locationRequest.priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
        locationRequest.interval = INTERVAL
        locationRequest.fastestInterval = FASTEST_INTERVAL
        locationRequest.smallestDisplacement = 10f // move 10 meters as minimum to get a callback

        LocationServices.getFusedLocationProviderClient(activity!!).requestLocationUpdates(
            locationRequest, mLocationCallback, Looper.myLooper()
        )
    }


    private fun handleSuccess(list: Resource.Success<List<Pock>>) {
        list.data.let {
            it.map { pock ->
                LatLng(
                    pock.location.latitude,
                    pock.location.longitude
                )
            }.forEach { latLng ->
                val marker: Marker = googleMap!!.addMarker(MarkerOptions().position(latLng))
            }
        }
    }
}
