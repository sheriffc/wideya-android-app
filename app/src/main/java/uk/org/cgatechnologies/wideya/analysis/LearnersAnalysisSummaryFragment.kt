package uk.org.cgatechnologies.wideya.analysis

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.snackbar.Snackbar
import com.highsoft.highcharts.common.HIColor
import com.highsoft.highcharts.common.hichartsclasses.*
import com.highsoft.highcharts.core.HIChartView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import uk.org.cgatechnologies.wideya.R
import uk.org.cgatechnologies.wideya.analysis.models.LearnerAttendanceReportModel
import uk.org.cgatechnologies.wideya.common.utils.Utils
import uk.org.cgatechnologies.wideya.databinding.FragmentLearnersAnalysisSummaryBinding
import uk.org.cgatechnologies.wideya.school_management.LatestSchoolDetailUiState
import uk.org.cgatechnologies.wideya.school_management.SchoolManagementViewModel
import java.time.Duration
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private const val TAG = "learners_analysis"

class LearnersAnalysisSummaryFragment : Fragment() {

    private var _binding: FragmentLearnersAnalysisSummaryBinding? = null
    private val binding get() = _binding!!

    private val analysisViewModel by activityViewModels<AnalysisViewModel>()
    private val schoolManagementViewModel by activityViewModels<SchoolManagementViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLearnersAnalysisSummaryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                schoolManagementViewModel.schoolDetail.collect { item ->
                    when (item) {
                        is LatestSchoolDetailUiState.Success -> {
                            item.school?.let {
                                bindData(view)
                            }
                        }
                        is LatestSchoolDetailUiState.Error -> {
                            Snackbar.make(binding.root, item.exception.message.toString(), 5000).show()
                        }
                    }
                }
            }
        }
    }

    private fun bindData(view: View) {
        initDisplay()

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                analysisViewModel.learnerAttendanceReportList.collect { item ->
                    when (item) {
                        is LatestLearnerAttendanceReportListListUiState.Success -> {
                            if(item.learnerAttendanceReportList.isNotEmpty()) {
                                Log.d(TAG, "onViewCreated: highchart initialised")
                                attendanceReportingChart(item.learnerAttendanceReportList)
                            }
                        }
                        is LatestLearnerAttendanceReportListListUiState.Error -> {
                            val mySnackbar = Snackbar.make(binding.root, item.exception.message.toString(), 5000)
                            mySnackbar.show()
                        }
                    }
                }
            }
        }

//        viewLifecycleOwner.lifecycleScope.launch {
//            repeatOnLifecycle(Lifecycle.State.STARTED) {
//                analysisViewModel.learnerAttendanceReport.collect { item ->
//                    when (item) {
//                        is LatestLearnerAttendanceReportUiState.Success -> {
//                            binding.apply {
//
//                                learnersCardView.apply {
//                                    item.learnerAttendanceReport?.apply {
//                                        cardTitle.text = "Learners"
//                                        cardSubTitle.text = "$count_learners Registered"
//                                        topAttendanceLabel.text = "$count_attendance reported"
//                                        bottomAttendanceLabel.text =
//                                            "${(count_learners - count_attendance)} not reported"
//
//                                        var percentage = "0"
//                                        if (count_attendance != 0) {
//                                            val x = count_attendance.toFloat() / count_learners.toFloat() * 100
//                                            percentage = "${x.roundToInt()}"
//                                        }
//                                        attendancePercentage.text = "${percentage}%"
//                                    }
//                                }
//
//                                learnersAttendanceCardView.apply {
//                                    item.learnerAttendanceReport?.apply {
//                                        val presentTotal = count_present + count_late
//                                        cardTitle.text = "Learners Attendance"
//                                        cardSubTitle.text = "$count_attendance Reported"
//                                        topAttendanceLabel.text = "$presentTotal present"
//                                        bottomAttendanceLabel.text = "$count_absent absent"
//
//                                        var percentage = "0"
//                                        if (count_attendance != 0) {
//                                            val x = presentTotal.toFloat() / count_attendance.toFloat() * 100
//                                            percentage = "${x.roundToInt()}"
//                                        }
//                                        attendancePercentage.text = "${percentage}%"
//                                    }
//                                }
//                            }
//                        }
//                        is LatestLearnerAttendanceReportUiState.Error -> {
//                            val mySnackbar = Snackbar.make(binding.root, item.exception.message.toString(), 5000)
//                            mySnackbar.show()
//                        }
//                    }
//                }
//            }
//        }

        binding.apply {
            prevBtn.setOnClickListener {
                getPreviousAttendanceReportDate(1)
                clearButtonState()
            }
            prevWeekBtn.setOnClickListener {
                getPreviousAttendanceReportDate(7)
                clearButtonState()
            }
            nextWeekBtn.setOnClickListener {
                getNextAttendanceReportDate(7)
                clearButtonState()
            }
            nextBtn.setOnClickListener {
                getNextAttendanceReportDate(1)
                clearButtonState()
            }
            todayBtn.setOnClickListener {
                setAttendanceReport(LocalDate.now())
                clearButtonState()
            }
        }
    }

    private fun clearButtonState(){
        binding.apply {
            btnGroupDays.clearChecked()
            btnGroupWeeks.clearChecked()
        }
    }

    private fun getNextAttendanceReportDate(days: Long) {
        if (analysisViewModel.isSelectedDateInitialized()) {
            val date = analysisViewModel.selectedDate.plusDays(days)
            setAttendanceReport(date)
        }
    }

    private fun getPreviousAttendanceReportDate(days: Long) {
        val date: LocalDate = if (analysisViewModel.isSelectedDateInitialized()) {
            analysisViewModel.selectedDate.minusDays(days)
        } else {
            LocalDate.now().minusDays(days)
        }
        setAttendanceReport(date)
    }

    private fun updateAttendanceReportingChart(options: HIOptions){
        //https://github.com/highcharts/highcharts-android/issues/257
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // chromium, enable hardware acceleration
            binding.hc.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        } else {
            // older android version, disable hardware acceleration
            binding.hc.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }

        val hcChartView: HIChartView = binding.hc

        hcChartView.options = options
        hcChartView.update(options)
    }

    private fun attendanceReportingChart(reportList: List<LearnerAttendanceReportModel>) {
        Log.d(TAG, "attendanceReportingChart")
//        val hcChartView: HIChartView = binding.hc
        val options = HIOptions()
        val hcChart = HIChart()
        val hcTitle = HITitle()
        val hcSubTitle = HISubtitle()
        val hcAxisTitle = HITitle()
        val hixAxis = HIXAxis()

        hcChart.type = "column"
        hcTitle.text = "Learner Attendance Reporting"
        hcSubTitle.text = "Period: Last Week"

        val categoryList = arrayListOf<String>()
        val notReportedList = arrayListOf<Int>()
        val absentList = arrayListOf<Int>()
        val presentList = arrayListOf<Int>()
        val lateList = arrayListOf<Int>()
        reportList.forEach {
            categoryList.add(
                Utils.getReadableSimpleLocalDate(
                    LocalDate.parse(it.attendance_date, DateTimeFormatter.ISO_LOCAL_DATE)
                )
            )
            if (it.count_submitted > 0) {
                absentList.add(it.count_absent)
                presentList.add(it.count_present)
                lateList.add(it.count_late)
                notReportedList.add(it.count_learners - it.count_attendance)
            } else {
                absentList.add(0)
                presentList.add(0)
                lateList.add(0)
                notReportedList.add(it.count_learners)
            }

        }
        hixAxis.categories = categoryList

        hcAxisTitle.text = "Learners"
        val hiyAxis = HIYAxis().apply {
            title = hcAxisTitle
            stackLabels = HIStackLabels()
            stackLabels.enabled = true
        }
        val hcDataLabel = HIDataLabels()
        hcDataLabel.enabled = true
        val hiPlotOptions = HIPlotOptions().apply {
            column = HIColumn()
            column.stacking = "normal"
        }

        val notReportedCl = HIColumn().apply {
            name = getString(R.string.not_submitted)
            data = notReportedList
            color = HIColor.initWithHexValue("8D99AE")
            dataLabels = arrayListOf(hcDataLabel)
        }

        val presentCl = HIColumn().apply {
            name = getString(R.string.on_time)
            data = presentList
            color = HIColor.initWithHexValue("9FD356")
            dataLabels = arrayListOf(hcDataLabel)
        }

        val lateCl = HIColumn().apply {
            name = getString(R.string.half_day)
            data = lateList
            color = HIColor.initWithHexValue("F8E859")
            dataLabels = arrayListOf(hcDataLabel)
        }

        val absentCl = HIColumn().apply {
            name = getString(R.string.absent)
            data = absentList
            color = HIColor.initWithHexValue("EE6352")
            dataLabels = arrayListOf(hcDataLabel)
        }

        options.apply {
            chart = hcChart
            title = hcTitle
            subtitle = hcSubTitle
            series = arrayListOf(notReportedCl, absentCl, lateCl,presentCl )
            plotOptions = hiPlotOptions
            yAxis = arrayListOf(hiyAxis)
            xAxis = arrayListOf(hixAxis)
            credits = HICredits().apply {
                enabled = false
            }
            navigation = HINavigation().apply {
                buttonOptions = HIButtonOptions().apply { enabled = false }
            }
        }

        updateAttendanceReportingChart(options)
    }

    private fun disableNextDateButton() {
        binding.apply {
            if (analysisViewModel.isSelectedDateInitialized()) {
                val todayDate = LocalDate.now()
                val selectedDate = analysisViewModel.selectedDate
                val isToday = selectedDate != todayDate
                nextBtn.isEnabled = isToday
                val duration = Duration.between(selectedDate.atStartOfDay(), todayDate.atStartOfDay())
                nextWeekBtn.isEnabled = duration.toDays() >= 7
                todayBtn.isEnabled = isToday
            } else {
                nextBtn.isEnabled = false
                nextWeekBtn.isEnabled = false
                todayBtn.isEnabled = false
            }
        }
    }

    private fun setSelectedDateTitle(date: LocalDate) {
        binding.tvCurrentDate.text = Utils.getReadableLocalDate(date)
    }

    private fun initDisplay() {
        val date: LocalDate = if (analysisViewModel.isSelectedDateInitialized()) {
            analysisViewModel.selectedDate
        } else {
            LocalDate.now()
        }
        setAttendanceReport(date)
    }

    private fun setAttendanceReport(date: LocalDate) {
        analysisViewModel.selectedDate = date
        disableNextDateButton()
        setSelectedDateTitle(date)
//        if (date != LocalDate.now()) {
//            analysisViewModel.getSchoolLearnerAttendanceReportByDate(
//                schoolManagementViewModel.currentSchool.uuid,
//                date.toString()
//            )
//        } else {
//            analysisViewModel.setLearnerAttendanceReport(schoolManagementViewModel.currentSchool.uuid)
//        }
        val startDate = date.minusDays(6)
        analysisViewModel.getSchoolLearnerAttendanceByInterval(
            schoolManagementViewModel.currentSchool.uuid,
            startDate.toString(),
            date.toString()
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}