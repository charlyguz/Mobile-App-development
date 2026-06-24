package com.example.cityexplorerchallenge.ui.main

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.cityexplorerchallenge.R
import com.example.cityexplorerchallenge.databinding.FragmentMainBinding
import com.example.cityexplorerchallenge.ui.MainViewModel
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import androidx.activity.result.IntentSenderRequest
import kotlinx.coroutines.launch
import java.util.Calendar

class MainFragment : Fragment() {

    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels()
    private var pendingPermissionAction: PendingPermissionAction? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val root = _binding?.root ?: return@registerForActivityResult
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (fineGranted || coarseGranted) {
            viewModel.startTracking()
            ensureLocationSettingsThenRunPendingAction()
        } else {
            showPermissionDeniedMessage(root)
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

    private enum class PendingPermissionAction {
        REFRESH_LOCATION,
        GENERATE_MISSION
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnNewChallenge.setOnClickListener {
            checkLocationPermissionAndGenerate()
        }

        binding.btnRequestLocation.setOnClickListener {
            requestLocationPermissionAndRefresh()
        }

        binding.btnOpenMap.setOnClickListener {
            requireActivity()
                .findViewById<BottomNavigationView>(R.id.bottom_navigation)
                ?.selectedItemId = R.id.nav_map
        }

        binding.btnChallengeDetails.setOnClickListener {
            openDetailsOrExplain()
        }

        binding.cardChallenge.setOnClickListener {
            if (viewModel.activeChallenge.value != null) {
                findNavController().navigate(R.id.nav_details)
            } else {
                checkLocationPermissionAndGenerate()
            }
        }

        observeActiveChallenge()
        observeHistory()
        observeLocationStatus()
        observeMessages()
        observeLoading()
    }

    private fun observeActiveChallenge() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.activeChallenge.collect { challenge ->
                    if (challenge != null) {
                        binding.tvChallengeTitle.text = challenge.title
                        binding.tvCategory.text = "Category: ${challenge.category}"
                        binding.tvDistance.text = "Distance: ~${challenge.distanceMeters} m"
                        binding.tvStatus.text = "Status: ${challenge.status}"
                        binding.btnOpenMap.isEnabled = true
                        binding.btnChallengeDetails.isEnabled = true
                        binding.tvCardHint.text = "Tap for details"

                        val colorRes = when (challenge.category) {
                            "Nature" -> R.color.category_nature
                            "Culture" -> R.color.category_culture
                            "Food" -> R.color.category_food
                            "Sport" -> R.color.category_sport
                            else -> R.color.primary
                        }
                        binding.categoryIndicator.setBackgroundColor(
                            ContextCompat.getColor(requireContext(), colorRes)
                        )
                        binding.categoryIndicator.visibility = View.VISIBLE

                        val statusColorRes = when (challenge.status) {
                            "ACTIVE" -> R.color.status_active
                            "COMPLETED" -> R.color.status_completed
                            "EXPIRED" -> R.color.status_expired
                            else -> R.color.on_surface_variant
                        }
                        binding.tvStatus.setTextColor(
                            ContextCompat.getColor(requireContext(), statusColorRes)
                        )
                    } else {
                        binding.tvChallengeTitle.text = "No active mission"
                        binding.tvCategory.text = "Category: -"
                        binding.tvDistance.text = "Distance: -"
                        binding.tvStatus.text = "Status: -"
                        binding.tvStatus.setTextColor(
                            ContextCompat.getColor(requireContext(), R.color.on_surface_variant)
                        )
                        binding.btnOpenMap.isEnabled = true
                        binding.btnChallengeDetails.isEnabled = false
                        binding.categoryIndicator.visibility = View.INVISIBLE
                        binding.tvCardHint.text = "Tap to generate your first mission"
                    }
                }
            }
        }
    }

    private fun observeHistory() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.historyFlow.collect { history ->
                    val completed = history.filter { it.status == "COMPLETED" }
                    val todayStart = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    val completedToday = completed.count { (it.completedAt ?: 0) >= todayStart }

                    binding.tvCompletedToday.text = "Completed today: $completedToday"
                    binding.tvTotalCompleted.text = "Total completed: ${completed.size}"
                }
            }
        }
    }

    private fun observeLocationStatus() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.locationStatus.collect { status ->
                    binding.tvLocationStatus.text = status
                }
            }
        }
    }

    private fun observeMessages() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.message.collect { msg ->
                    if (msg != null) {
                        Snackbar.make(binding.root, msg, Snackbar.LENGTH_SHORT).show()
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
                    binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
                    binding.btnNewChallenge.isEnabled = !loading
                    binding.cardChallenge.isEnabled = !loading
                }
            }
        }
    }

    private fun openDetailsOrExplain() {
        if (viewModel.activeChallenge.value != null) {
            findNavController().navigate(R.id.nav_details)
        } else {
            Snackbar.make(binding.root, "Generate a mission first.", Snackbar.LENGTH_SHORT).show()
        }
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

    private fun requestLocationPermissionAndRefresh() {
        pendingPermissionAction = PendingPermissionAction.REFRESH_LOCATION
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
                        "Enable device location services before generating missions.",
                        Snackbar.LENGTH_LONG
                    ).show()
                    pendingPermissionAction = null
                }
            }
    }

    private fun runPendingLocationAction() {
        when (pendingPermissionAction) {
            PendingPermissionAction.GENERATE_MISSION -> viewModel.generateNewChallenge()
            PendingPermissionAction.REFRESH_LOCATION,
            null -> viewModel.refreshLocation(showMessage = true)
        }
        pendingPermissionAction = null
    }

    private fun showPermissionDeniedMessage(root: View) {
        Snackbar.make(root, "Location permission is required for dynamic missions.", Snackbar.LENGTH_LONG)
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
