package com.example.cityexplorerchallenge.ui.statistics

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.cityexplorerchallenge.databinding.FragmentStatisticsBinding
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

class StatisticsFragment : Fragment() {

    private var _binding: FragmentStatisticsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: com.example.cityexplorerchallenge.ui.MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatisticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.historyFlow.collect { history ->
                    val completed = history.filter { it.status == "COMPLETED" }
                    val totalCompleted = completed.size
                    val totalDistance = completed.sumOf { it.distanceMeters } / 1000.0 // km
                    
                    binding.tvTotalCompleted.text = "Total completed missions: $totalCompleted"
                    binding.tvTotalDistance.text = "Total explored distance: ${String.format(Locale.US, "%.2f", totalDistance)} km"
                    
                    val categoriesCount = completed.groupingBy { it.category }.eachCount()
                    val categoriesText = categoriesCount.entries.joinToString("\n") { "- ${it.key}: ${it.value}" }
                    binding.tvCategoriesStats.text = if (categoriesText.isEmpty()) "No data" else categoriesText
                    
                    val mostExplored = categoriesCount.maxByOrNull { it.value }?.key ?: "-"
                    binding.tvMostExplored.text = "Most explored category: $mostExplored"

                    // Daily/weekly progress
                    val todayStart = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    val weekStart = Calendar.getInstance().apply {
                        set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis

                    val completedToday = completed.filter { (it.completedAt ?: 0) >= todayStart }
                    val completedThisWeek = completed.filter { (it.completedAt ?: 0) >= weekStart }

                    binding.tvCompletedToday.text = "Completed today: ${completedToday.size}"
                    binding.tvCompletedThisWeek.text = "Completed this week: ${completedThisWeek.size}"
                    
                    val distanceToday = completedToday.sumOf { it.distanceMeters } / 1000.0
                    val distanceThisWeek = completedThisWeek.sumOf { it.distanceMeters } / 1000.0
                    binding.tvDistanceToday.text = "Distance today: ${String.format(Locale.US, "%.2f", distanceToday)} km"
                    binding.tvDistanceThisWeek.text = "Distance this week: ${String.format(Locale.US, "%.2f", distanceThisWeek)} km"
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
