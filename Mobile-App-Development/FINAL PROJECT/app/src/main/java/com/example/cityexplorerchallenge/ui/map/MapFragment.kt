package com.example.cityexplorerchallenge.ui.map

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.IntentSenderRequest
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.preference.PreferenceManager
import com.example.cityexplorerchallenge.databinding.FragmentMapBinding
import com.example.cityexplorerchallenge.ui.MainViewModel
import com.example.cityexplorerchallenge.ui.MainViewModel.Companion.COMPLETION_RADIUS_METERS
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

class MapFragment : Fragment() {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels()
    private var userMarker: Marker? = null
    private var targetMarker: Marker? = null
    private var routeLine: Polyline? = null
    private var completionCircle: Polygon? = null
    private var pendingMoveToUser = false
    private var pendingPermissionAction: PendingPermissionAction? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (fineGranted || coarseGranted) {
            viewModel.startTracking()
            ensureLocationSettingsThenRunPendingAction()
        } else {
            showPermissionDeniedMessage()
            pendingPermissionAction = null
        }
    }

    private val locationSettingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            runPendingLocationAction()
        } else {
            Snackbar.make(
                binding.root,
                "Location services must be enabled for dynamic missions.",
                Snackbar.LENGTH_LONG
            ).show()
            pendingPermissionAction = null
        }
    }

    companion object {
        private val KRAKOW_CENTER = GeoPoint(50.0614, 19.9366)
        private const val DEFAULT_ZOOM = 15.0
        private const val USER_ZOOM = 17.0
    }

    private enum class PendingPermissionAction {
        MOVE_TO_USER,
        GENERATE_MISSION
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val config = Configuration.getInstance()
        config.load(requireContext(), PreferenceManager.getDefaultSharedPreferences(requireContext()))
        config.userAgentValue = "CityExplorerChallenge/1.0"

        val osmdroidBasePath = java.io.File(requireContext().cacheDir, "osmdroid")
        osmdroidBasePath.mkdirs()
        config.osmdroidBasePath = osmdroidBasePath
        config.osmdroidTileCache = java.io.File(osmdroidBasePath, "tiles")

        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.mapView.setMultiTouchControls(true)
        centerOnKrakow()

        binding.fabMyLocation.setOnClickListener {
            moveToUserLocation()
        }

        binding.btnCenterKrakow.setOnClickListener {
            centerOnKrakow()
            Toast.makeText(requireContext(), "Map centered on Krakow.", Toast.LENGTH_SHORT).show()
        }

        binding.btnCheckCompletion.setOnClickListener {
            if (viewModel.activeChallenge.value == null) {
                checkLocationPermissionAndGenerate()
            } else {
                viewModel.checkCompletion()
            }
        }

        if (hasLocationPermission()) {
            viewModel.startTracking()
            viewModel.refreshLocation()
        }

        observeActiveChallenge()
        observeCurrentLocation()
        observeLocationStatus()
        observeMessages()
        observeLoading()
    }

    private fun observeActiveChallenge() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.activeChallenge.collect { challenge ->
                    if (challenge != null) {
                        val placeName = challenge.targetPlaceName.ifEmpty { challenge.title }
                        val targetPoint = GeoPoint(challenge.targetLat, challenge.targetLng)

                        binding.tvTargetInfo.text = "Target: $placeName"
                        binding.btnCheckCompletion.text = "Check Completion"
                        binding.tvCompletionHint.text = "Complete when you are within $COMPLETION_RADIUS_METERS m of the target."

                        if (targetMarker == null) {
                            targetMarker = Marker(binding.mapView).apply {
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                binding.mapView.overlays.add(this)
                            }
                        }
                        targetMarker?.position = targetPoint
                        targetMarker?.title = placeName
                        updateCompletionCircle(targetPoint)

                        val userLoc = viewModel.currentLocation.value
                        if (userLoc != null) {
                            val userPoint = GeoPoint(userLoc.latitude, userLoc.longitude)
                            updateDistanceAndRoute(userPoint, targetPoint)
                        } else {
                            binding.tvDistanceInfo.text = "Distance: ~${challenge.distanceMeters} m"
                        }

                        binding.mapView.invalidate()
                    } else {
                        binding.tvTargetInfo.text = "Target: -"
                        binding.tvDistanceInfo.text = "Distance: -"
                        binding.tvCompletionHint.text = "Generate a mission to see the completion radius."
                        binding.btnCheckCompletion.text = "Generate Mission"
                        clearTargetOverlays()
                    }
                }
            }
        }
    }

    private fun observeCurrentLocation() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.currentLocation.collect { location ->
                    if (location != null) {
                        val userPoint = GeoPoint(location.latitude, location.longitude)

                        if (userMarker == null) {
                            userMarker = Marker(binding.mapView).apply {
                                title = "Your location"
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                                binding.mapView.overlays.add(this)
                            }
                        }
                        userMarker?.position = userPoint

                        if (pendingMoveToUser) {
                            pendingMoveToUser = false
                            animateTo(userPoint, USER_ZOOM)
                        }

                        val challenge = viewModel.activeChallenge.value
                        if (challenge != null) {
                            updateDistanceAndRoute(
                                userPoint,
                                GeoPoint(challenge.targetLat, challenge.targetLng)
                            )
                        }

                        binding.mapView.invalidate()
                    }
                }
            }
        }
    }

    private fun observeLocationStatus() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.locationStatus.collect { status ->
                    binding.tvMapStatus.text = status
                }
            }
        }
    }

    private fun observeMessages() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.message.collect { msg ->
                    if (msg != null) {
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                        viewModel.clearMessage()
                    }
                }
            }
        }
    }

    private fun observeLoading() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isLoading.collect { loading ->
                    binding.btnCheckCompletion.isEnabled = !loading
                    binding.fabMyLocation.isEnabled = !loading
                    binding.btnCenterKrakow.isEnabled = !loading
                }
            }
        }
    }

    private fun updateDistanceAndRoute(from: GeoPoint, to: GeoPoint) {
        val liveDistance = viewModel.locationTracker.calculateDistance(
            from.latitude,
            from.longitude,
            to.latitude,
            to.longitude
        )
        binding.tvDistanceInfo.text = "Distance: ~${liveDistance.toInt()} m"
        binding.tvCompletionHint.text = if (liveDistance <= COMPLETION_RADIUS_METERS) {
            "You are inside the $COMPLETION_RADIUS_METERS m completion radius. Tap Check Completion."
        } else {
            val remaining = (liveDistance - COMPLETION_RADIUS_METERS).toInt()
            "Move about $remaining m closer to enter the $COMPLETION_RADIUS_METERS m completion radius."
        }
        updateRouteLine(from, to)
    }

    private fun updateRouteLine(from: GeoPoint, to: GeoPoint) {
        routeLine?.let { binding.mapView.overlays.remove(it) }

        routeLine = Polyline(binding.mapView).apply {
            addPoint(from)
            addPoint(to)
            outlinePaint.color = Color.parseColor("#00897B")
            outlinePaint.strokeWidth = 8f
            outlinePaint.isAntiAlias = true
        }
        binding.mapView.overlays.add(0, routeLine)
        binding.mapView.invalidate()
    }

    private fun clearTargetOverlays() {
        targetMarker?.let { binding.mapView.overlays.remove(it) }
        targetMarker = null
        routeLine?.let { binding.mapView.overlays.remove(it) }
        routeLine = null
        completionCircle?.let { binding.mapView.overlays.remove(it) }
        completionCircle = null
        binding.mapView.invalidate()
    }

    private fun updateCompletionCircle(center: GeoPoint) {
        completionCircle?.let { binding.mapView.overlays.remove(it) }

        completionCircle = Polygon(binding.mapView).apply {
            points = Polygon.pointsAsCircle(center, COMPLETION_RADIUS_METERS.toDouble())
            fillPaint.color = Color.argb(42, 0, 137, 123)
            outlinePaint.color = Color.parseColor("#00897B")
            outlinePaint.strokeWidth = 3f
        }
        binding.mapView.overlays.add(0, completionCircle)
        binding.mapView.invalidate()
    }

    private fun centerOnKrakow() {
        pendingMoveToUser = false
        binding.mapView.controller.setZoom(DEFAULT_ZOOM)
        binding.mapView.controller.setCenter(KRAKOW_CENTER)
        binding.tvMapStatus.text = "Map centered on Krakow."
        binding.mapView.invalidate()
    }

    private fun moveToUserLocation() {
        pendingPermissionAction = PendingPermissionAction.MOVE_TO_USER
        if (!hasLocationPermission()) {
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
            return
        }

        viewModel.startTracking()
        val current = viewModel.currentLocation.value
        pendingMoveToUser = true
        if (current != null) {
            animateTo(GeoPoint(current.latitude, current.longitude), USER_ZOOM)
            binding.tvMapStatus.text = "Map centered on your current location."
        }
        ensureLocationSettingsThenRunPendingAction()
    }

    private fun checkLocationPermissionAndGenerate() {
        pendingPermissionAction = PendingPermissionAction.GENERATE_MISSION
        if (hasLocationPermission()) {
            viewModel.startTracking()
            ensureLocationSettingsThenRunPendingAction()
        } else {
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun ensureLocationSettingsThenRunPendingAction() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            10_000L
        ).build()
        val settingsRequest = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
            .setAlwaysShow(true)
            .build()

        LocationServices.getSettingsClient(requireActivity())
            .checkLocationSettings(settingsRequest)
            .addOnSuccessListener {
                runPendingLocationAction()
            }
            .addOnFailureListener { exception ->
                if (exception is ResolvableApiException) {
                    try {
                        val request = IntentSenderRequest.Builder(exception.resolution).build()
                        locationSettingsLauncher.launch(request)
                    } catch (sendException: IntentSender.SendIntentException) {
                        Snackbar.make(
                            binding.root,
                            "Could not open location settings. Enable GPS manually.",
                            Snackbar.LENGTH_LONG
                        ).show()
                        pendingPermissionAction = null
                    }
                } else {
                    Snackbar.make(
                        binding.root,
                        "Enable device location services before using location.",
                        Snackbar.LENGTH_LONG
                    ).show()
                    pendingPermissionAction = null
                }
            }
    }

    private fun runPendingLocationAction() {
        when (pendingPermissionAction) {
            PendingPermissionAction.MOVE_TO_USER -> {
                pendingMoveToUser = true
                viewModel.refreshLocation(showMessage = true)
            }
            PendingPermissionAction.GENERATE_MISSION -> viewModel.generateNewChallenge()
            null -> viewModel.refreshLocation()
        }
        pendingPermissionAction = null
    }

    private fun animateTo(point: GeoPoint, zoom: Double) {
        binding.mapView.controller.setZoom(zoom)
        binding.mapView.controller.animateTo(point)
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
    }

    private fun showPermissionDeniedMessage() {
        Snackbar.make(binding.root, "Location permission is required for dynamic missions.", Snackbar.LENGTH_LONG)
            .setAction("Settings") {
                openAppSettings()
            }
            .show()
    }

    private fun openAppSettings() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", requireContext().packageName, null)
        )
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        userMarker = null
        targetMarker = null
        routeLine = null
        completionCircle = null
        _binding = null
    }
}
