package com.example.exoself.ui.stats

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.exoself.databinding.FragmentStatsBinding
import com.github.aachartmodel.aainfographics.aachartcreator.AAChartModel
import com.github.aachartmodel.aainfographics.aachartcreator.AAChartType
import com.github.aachartmodel.aainfographics.aachartcreator.AASeriesElement

class StatsFragment : Fragment() {

    private var _binding: FragmentStatsBinding? = null
    private val binding get() = _binding!!
    private val statsViewModel: StatsViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        statsViewModel.getAuthState().observe(viewLifecycleOwner) { isUserSignedOut ->
            if (!isUserSignedOut) {
                setUpChart()
            }
        }
    }

    private fun setUpChart() {
        statsViewModel.topTenTags.observe(viewLifecycleOwner) { tags ->
            val tagNames = tags.map { it.name }
            val numberCompleted = tags.map { it.numberCompleted }
            val minutesCompleted = tags.map { it.minutesCompleted }

            val minutesCompletedAASeries =
                AASeriesElement().name("Minutes").data(minutesCompleted.toTypedArray())
            val numberCompletedAASeries =
                AASeriesElement().name("Sessions").data(numberCompleted.toTypedArray())

            val aaChartModel: AAChartModel = AAChartModel()
                .chartType(AAChartType.Bar)
                .categories(tagNames.toTypedArray())
                .title("Top 10 Tags")
                .subtitle("Sessions & Minutes Spent")
                .dataLabelsEnabled(true)
                .series(
                    arrayOf(
                        numberCompletedAASeries,
                        minutesCompletedAASeries
                    )
                )

            val aaChartView = binding.aaChartView
            aaChartView.aa_drawChartWithChartModel(aaChartModel)
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}