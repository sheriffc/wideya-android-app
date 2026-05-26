package uk.org.cgatechnologies.wideya.school_management

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import uk.org.cgatechnologies.wideya.common.utils.Utils
import uk.org.cgatechnologies.wideya.databinding.FragmentSchoolFeedingBinding
import uk.org.cgatechnologies.wideya.school_management.entities.SchoolFeeding
import uk.org.cgatechnologies.wideya.school_management.entities.SchoolFeedingStock
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID

private const val TAG = "SchoolFeedingFragment"

class SchoolFeedingFragment : Fragment() {

    private var _binding: FragmentSchoolFeedingBinding? = null
    private val binding get() = _binding!!

    private val vm by activityViewModels<SchoolManagementViewModel>()

    // ── Feeding support form ──────────────────────────────────────────────────
    private val supplyPeriodItems = listOf("First Term", "Second Term", "Third Term")
    private val supplyPeriodOids   = listOf("first_term", "second_term", "third_term")

    private val supplierItems = listOf("GoSL", "PLAN", "WFP", "CRS", "Other (specify)")
    private val supplierOids  = listOf("gosl", "plan", "wfp", "crs", "other")

    // ── Stock update form ─────────────────────────────────────────────────────
    private val stockMonthDisplayList = mutableListOf<String>()
    private val stockMonthValueList   = mutableListOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSchoolFeedingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupDropdowns()
        setupItemCheckboxes()
        setupDatePicker()
        setupSaveButton()
        setupNewSubmissionButton()
        setupStockCard()
    }

    override fun onResume() {
        super.onResume()
        val feedingEnabled = vm.currentSchool.receives_feeding == 1
        binding.cvFeedingDisabled.visibility = if (feedingEnabled) View.GONE else View.VISIBLE
        binding.cvFeedingDetails.visibility  = if (feedingEnabled) View.VISIBLE else View.GONE
        binding.cvStockUpdate.visibility     = if (feedingEnabled) View.VISIBLE else View.GONE
        binding.llActions.visibility         = if (feedingEnabled) View.VISIBLE else View.GONE
        if (feedingEnabled) {
            populateFromExistingRecord()
            populateStockFromRecord(selectedStockMonth())
        }
    }

    // ── Feeding support form ──────────────────────────────────────────────────

    private fun setupDropdowns() {
        binding.actvSupplyPeriod.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, supplyPeriodItems)
        )
        binding.actvSuppliedBy.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, supplierItems)
        )
        binding.actvSuppliedBy.setOnItemClickListener { _, _, position, _ ->
            val isOther = supplierOids[position] == "other"
            binding.tlSuppliedByOther.visibility = if (isOther) View.VISIBLE else View.GONE
            if (!isOther) binding.etSuppliedByOther.setText("")
        }
    }

    private fun setupItemCheckboxes() {
        binding.cbRice.setOnCheckedChangeListener { _, checked ->
            binding.tlQtyRice.visibility = if (checked) View.VISIBLE else View.GONE
            if (!checked) binding.etQtyRice.setText("")
        }
        binding.cbBeans.setOnCheckedChangeListener { _, checked ->
            binding.tlQtyBeans.visibility = if (checked) View.VISIBLE else View.GONE
            if (!checked) binding.etQtyBeans.setText("")
        }
        binding.cbGari.setOnCheckedChangeListener { _, checked ->
            binding.tlQtyGari.visibility = if (checked) View.VISIBLE else View.GONE
            if (!checked) binding.etQtyGari.setText("")
        }
        binding.cbVegOil.setOnCheckedChangeListener { _, checked ->
            binding.tlQtyVegOil.visibility = if (checked) View.VISIBLE else View.GONE
            if (!checked) binding.etQtyVegOil.setText("")
        }
        binding.cbSalt.setOnCheckedChangeListener { _, checked ->
            binding.tlQtySalt.visibility = if (checked) View.VISIBLE else View.GONE
            if (!checked) binding.etQtySalt.setText("")
        }
    }

    private fun setupDatePicker() {
        val showPicker = {
            val picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select Received At Date")
                .build()
            picker.addOnPositiveButtonClickListener { millis ->
                val date = Instant.ofEpochMilli(millis)
                    .atZone(ZoneOffset.UTC)
                    .toLocalDate()
                    .toString()
                binding.etReceivedAt.setText(date)
            }
            picker.show(parentFragmentManager, "feeding_date_picker")
        }
        binding.etReceivedAt.setOnClickListener { if (binding.etReceivedAt.isEnabled) showPicker() }
        binding.tlReceivedAt.setEndIconOnClickListener { if (binding.etReceivedAt.isEnabled) showPicker() }
    }

    private fun setupSaveButton() {
        binding.btnSaveFeeding.setOnClickListener {
            if (saveFeeding()) {
                setFormEditable(false)
                Snackbar.make(binding.root, "School feeding record saved", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupNewSubmissionButton() {
        binding.btnNewSubmission.setOnClickListener {
            clearFeedingForm()
            setFormEditable(true)
        }
    }

    private fun populateFromExistingRecord() {
        val record = vm.getSchoolFeeding() ?: return

        val periodIdx = supplyPeriodOids.indexOf(record.supply_period_oid)
        if (periodIdx >= 0) binding.actvSupplyPeriod.setText(supplyPeriodItems[periodIdx], false)

        binding.etReceivedAt.setText(record.received_at ?: "")

        val supplierIdx = supplierOids.indexOf(record.supplied_by_oid)
        if (supplierIdx >= 0) {
            binding.actvSuppliedBy.setText(supplierItems[supplierIdx], false)
            val isOther = record.supplied_by_oid == "other"
            binding.tlSuppliedByOther.visibility = if (isOther) View.VISIBLE else View.GONE
            binding.etSuppliedByOther.setText(record.supplied_by_other ?: "")
        }

        record.qty_rice?.let    { binding.cbRice.isChecked = true;   binding.etQtyRice.setText(it.toString()) }
        record.qty_beans?.let   { binding.cbBeans.isChecked = true;  binding.etQtyBeans.setText(it.toString()) }
        record.qty_gari?.let    { binding.cbGari.isChecked = true;   binding.etQtyGari.setText(it.toString()) }
        record.qty_veg_oil?.let { binding.cbVegOil.isChecked = true; binding.etQtyVegOil.setText(it.toString()) }
        record.qty_salt?.let    { binding.cbSalt.isChecked = true;   binding.etQtySalt.setText(it.toString()) }

        setFormEditable(false)
    }

    private fun setFormEditable(editable: Boolean) {
        binding.cbRice.isEnabled = editable
        binding.cbBeans.isEnabled = editable
        binding.cbGari.isEnabled = editable
        binding.cbVegOil.isEnabled = editable
        binding.cbSalt.isEnabled = editable
        binding.etQtyRice.isEnabled = editable
        binding.etQtyBeans.isEnabled = editable
        binding.etQtyGari.isEnabled = editable
        binding.etQtyVegOil.isEnabled = editable
        binding.etQtySalt.isEnabled = editable
        binding.tlSupplyPeriod.isEnabled = editable
        binding.actvSupplyPeriod.isEnabled = editable
        binding.tlReceivedAt.isEnabled = editable
        binding.etReceivedAt.isEnabled = editable
        binding.tlSuppliedBy.isEnabled = editable
        binding.actvSuppliedBy.isEnabled = editable
        binding.etSuppliedByOther.isEnabled = editable
        binding.btnSaveFeeding.visibility = if (editable) View.VISIBLE else View.GONE
        binding.btnNewSubmission.visibility = if (editable) View.GONE else View.VISIBLE
    }

    private fun clearFeedingForm() {
        binding.cbRice.isChecked = false
        binding.cbBeans.isChecked = false
        binding.cbGari.isChecked = false
        binding.cbVegOil.isChecked = false
        binding.cbSalt.isChecked = false
        binding.etQtyRice.setText("")
        binding.etQtyBeans.setText("")
        binding.etQtyGari.setText("")
        binding.etQtyVegOil.setText("")
        binding.etQtySalt.setText("")
        binding.actvSupplyPeriod.setText("", false)
        binding.etReceivedAt.setText("")
        binding.actvSuppliedBy.setText("", false)
        binding.etSuppliedByOther.setText("")
        binding.tlSuppliedByOther.visibility = View.GONE
    }

    private fun saveFeeding(): Boolean {
        val periodText = binding.actvSupplyPeriod.text.toString().trim()
        val periodIdx  = supplyPeriodItems.indexOf(periodText)
        val periodOid  = if (periodIdx >= 0) supplyPeriodOids[periodIdx] else null

        val receivedAt = binding.etReceivedAt.text.toString().trim().ifBlank { null }

        val supplierText = binding.actvSuppliedBy.text.toString().trim()
        val supplierIdx  = supplierItems.indexOf(supplierText)
        val supplierOid  = if (supplierIdx >= 0) supplierOids[supplierIdx] else null

        val supplierOther = binding.etSuppliedByOther.text.toString().trim().ifBlank { null }

        val qtyRice   = if (binding.cbRice.isChecked)   binding.etQtyRice.text.toString().toIntOrNull()   else null
        val qtyBeans  = if (binding.cbBeans.isChecked)  binding.etQtyBeans.text.toString().toIntOrNull()  else null
        val qtyGari   = if (binding.cbGari.isChecked)   binding.etQtyGari.text.toString().toIntOrNull()   else null
        val qtyVegOil = if (binding.cbVegOil.isChecked) binding.etQtyVegOil.text.toString().toIntOrNull() else null
        val qtySalt   = if (binding.cbSalt.isChecked)   binding.etQtySalt.text.toString().toIntOrNull()   else null

        val now = Utils.getISODateTimeUTC()
        val userId = Utils.getUserId(requireContext())

        vm.saveSchoolFeeding(
            SchoolFeeding(
                uuid               = UUID.randomUUID().toString(),
                school_uuid        = vm.currentSchool.uuid,
                receives_feeding   = 1,
                supply_period_oid  = periodOid,
                received_at        = receivedAt,
                supplied_by_oid    = supplierOid,
                supplied_by_other  = supplierOther,
                qty_rice    = qtyRice,
                qty_beans   = qtyBeans,
                qty_gari    = qtyGari,
                qty_veg_oil = qtyVegOil,
                qty_salt    = qtySalt,
                created_at  = now,
                created_by  = userId,
                updated_at  = now,
                updated_by  = userId,
            )
        )
        return true
    }

    // ── Stock update form ─────────────────────────────────────────────────────

    private fun setupStockCard() {
        // build last 12 months
        val displayFmt = DateTimeFormatter.ofPattern("MMMM yyyy")
        val valueFmt   = DateTimeFormatter.ofPattern("yyyy-MM")
        val now = YearMonth.now()
        for (i in 0..11) {
            val m = now.minusMonths(i.toLong())
            stockMonthDisplayList.add(m.format(displayFmt))
            stockMonthValueList.add(m.format(valueFmt))
        }

        binding.actvStockMonth.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, stockMonthDisplayList)
        )
        // default to current month
        binding.actvStockMonth.setText(stockMonthDisplayList[0], false)

        binding.actvStockMonth.setOnItemClickListener { _, _, position, _ ->
            populateStockFromRecord(stockMonthValueList[position])
        }

        binding.cbStockRice.setOnCheckedChangeListener { _, checked ->
            binding.tlStockQtyRice.visibility = if (checked) View.VISIBLE else View.GONE
            if (!checked) binding.etStockQtyRice.setText("")
        }
        binding.cbStockBeans.setOnCheckedChangeListener { _, checked ->
            binding.tlStockQtyBeans.visibility = if (checked) View.VISIBLE else View.GONE
            if (!checked) binding.etStockQtyBeans.setText("")
        }
        binding.cbStockGari.setOnCheckedChangeListener { _, checked ->
            binding.tlStockQtyGari.visibility = if (checked) View.VISIBLE else View.GONE
            if (!checked) binding.etStockQtyGari.setText("")
        }
        binding.cbStockVegOil.setOnCheckedChangeListener { _, checked ->
            binding.tlStockQtyVegOil.visibility = if (checked) View.VISIBLE else View.GONE
            if (!checked) binding.etStockQtyVegOil.setText("")
        }
        binding.cbStockSalt.setOnCheckedChangeListener { _, checked ->
            binding.tlStockQtySalt.visibility = if (checked) View.VISIBLE else View.GONE
            if (!checked) binding.etStockQtySalt.setText("")
        }

        binding.btnSaveStock.setOnClickListener {
            if (saveStock()) {
                setStockEditable(false)
                Snackbar.make(binding.root, "Stock updated for ${binding.actvStockMonth.text}", Snackbar.LENGTH_SHORT).show()
            }
        }

        binding.btnEditStock.setOnClickListener {
            setStockEditable(true)
        }
    }

    private fun selectedStockMonth(): String {
        val display = binding.actvStockMonth.text.toString()
        val idx = stockMonthDisplayList.indexOf(display)
        return if (idx >= 0) stockMonthValueList[idx] else stockMonthValueList[0]
    }

    private fun populateStockFromRecord(month: String) {
        clearStockForm()
        val record = vm.getSchoolFeedingStock(month)
        if (record == null) {
            setStockEditable(true)
            return
        }
        record.qty_rice?.let    { binding.cbStockRice.isChecked = true;   binding.etStockQtyRice.setText(it.toString()) }
        record.qty_beans?.let   { binding.cbStockBeans.isChecked = true;  binding.etStockQtyBeans.setText(it.toString()) }
        record.qty_gari?.let    { binding.cbStockGari.isChecked = true;   binding.etStockQtyGari.setText(it.toString()) }
        record.qty_veg_oil?.let { binding.cbStockVegOil.isChecked = true; binding.etStockQtyVegOil.setText(it.toString()) }
        record.qty_salt?.let    { binding.cbStockSalt.isChecked = true;   binding.etStockQtySalt.setText(it.toString()) }
        setStockEditable(false)
    }

    private fun clearStockForm() {
        binding.cbStockRice.isChecked = false
        binding.cbStockBeans.isChecked = false
        binding.cbStockGari.isChecked = false
        binding.cbStockVegOil.isChecked = false
        binding.cbStockSalt.isChecked = false
        binding.etStockQtyRice.setText("")
        binding.etStockQtyBeans.setText("")
        binding.etStockQtyGari.setText("")
        binding.etStockQtyVegOil.setText("")
        binding.etStockQtySalt.setText("")
    }

    private fun setStockEditable(editable: Boolean) {
        binding.cbStockRice.isEnabled = editable
        binding.cbStockBeans.isEnabled = editable
        binding.cbStockGari.isEnabled = editable
        binding.cbStockVegOil.isEnabled = editable
        binding.cbStockSalt.isEnabled = editable
        binding.etStockQtyRice.isEnabled = editable
        binding.etStockQtyBeans.isEnabled = editable
        binding.etStockQtyGari.isEnabled = editable
        binding.etStockQtyVegOil.isEnabled = editable
        binding.etStockQtySalt.isEnabled = editable
        binding.btnSaveStock.visibility = if (editable) View.VISIBLE else View.GONE
        binding.btnEditStock.visibility = if (editable) View.GONE else View.VISIBLE
    }

    private fun saveStock(): Boolean {
        val month = selectedStockMonth()
        if (month.isBlank()) {
            Snackbar.make(binding.root, "Please select a month", Snackbar.LENGTH_SHORT).show()
            return false
        }

        val qtyRice   = if (binding.cbStockRice.isChecked)   binding.etStockQtyRice.text.toString().toIntOrNull()   else null
        val qtyBeans  = if (binding.cbStockBeans.isChecked)  binding.etStockQtyBeans.text.toString().toIntOrNull()  else null
        val qtyGari   = if (binding.cbStockGari.isChecked)   binding.etStockQtyGari.text.toString().toIntOrNull()   else null
        val qtyVegOil = if (binding.cbStockVegOil.isChecked) binding.etStockQtyVegOil.text.toString().toIntOrNull() else null
        val qtySalt   = if (binding.cbStockSalt.isChecked)   binding.etStockQtySalt.text.toString().toIntOrNull()   else null

        val now    = Utils.getISODateTimeUTC()
        val userId = Utils.getUserId(requireContext())

        vm.saveSchoolFeedingStock(
            SchoolFeedingStock(
                uuid        = UUID.randomUUID().toString(),
                school_uuid = vm.currentSchool.uuid,
                stock_month = month,
                qty_rice    = qtyRice,
                qty_beans   = qtyBeans,
                qty_gari    = qtyGari,
                qty_veg_oil = qtyVegOil,
                qty_salt    = qtySalt,
                created_at  = now,
                created_by  = userId,
                updated_at  = now,
                updated_by  = userId,
            )
        )
        return true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
