package com.example.cityexplorerchallenge.ui.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.cityexplorerchallenge.R
import com.example.cityexplorerchallenge.databinding.FragmentChallengeDetailsBinding
import com.example.cityexplorerchallenge.ui.MainViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch
import java.util.Locale

class ChallengeDetailsFragment : Fragment() {

    private var _binding: FragmentChallengeDetailsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChallengeDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnNavigateToTarget.setOnClickListener {
            requireActivity()
                .findViewById<BottomNavigationView>(R.id.bottom_navigation)
                ?.selectedItemId = R.id.nav_map
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.activeChallenge.collect { challenge ->
                    if (challenge != null) {
                        val placeName = challenge.targetPlaceName.ifEmpty { challenge.title }

                        binding.tvChallengeTitle.text = challenge.title
                        binding.tvDescription.text = challenge.description
                        binding.tvReason.text = challenge.reason.ifBlank {
                            "- Generated from nearby places, history, distance and time of day."
                        }
                        binding.tvPlaceName.text = "Name: $placeName"
                        binding.tvCategory.text = "Category: ${challenge.category}"
                        binding.tvDistance.text = "Distance: ${challenge.distanceMeters} m"
                        binding.tvCoordinates.text = String.format(
                            Locale.US,
                            "Coordinates: %.5f, %.5f",
                            challenge.targetLat,
                            challenge.targetLng
                        )
                        binding.btnNavigateToTarget.isEnabled = true
                    } else {
                        binding.tvChallengeTitle.text = "No active mission"
                        binding.tvDescription.text = "Generate a mission from the Home or Map screen first."
                        binding.tvReason.text = "-"
                        binding.tvPlaceName.text = "Name: -"
                        binding.tvCategory.text = "Category: -"
                        binding.tvDistance.text = "Distance: -"
                        binding.tvCoordinates.text = "Coordinates: -"
                        binding.btnNavigateToTarget.isEnabled = false
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
