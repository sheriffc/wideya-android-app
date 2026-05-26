package uk.org.cgatechnologies.wideya.common.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import uk.org.cgatechnologies.wideya.analysis.LearnerDisabilityItemViewHolder
import uk.org.cgatechnologies.wideya.analysis.models.LearnerDisabilityModel
import uk.org.cgatechnologies.wideya.common.viewholder.EmptyViewHolder
import uk.org.cgatechnologies.wideya.databinding.*
import uk.org.cgatechnologies.wideya.learner_management.LearnerManagementViewModel
import uk.org.cgatechnologies.wideya.school_group_management.SchoolGroupLearnerItemViewHolder
import uk.org.cgatechnologies.wideya.school_group_management.models.SchoolGroupLearnerModel
import uk.org.cgatechnologies.wideya.school_group_management.models.SchoolGroupModel
import uk.org.cgatechnologies.wideya.school_management.*
import uk.org.cgatechnologies.wideya.school_management.models.LearnerAdmissionModel
import uk.org.cgatechnologies.wideya.school_management.models.PersonAttendanceModel
import uk.org.cgatechnologies.wideya.school_management.models.SchoolModel
import uk.org.cgatechnologies.wideya.learner_management.LearnerSearchItemViewHolder
import uk.org.cgatechnologies.wideya.teacher_management.PayrollTeacherItemViewHolder
import uk.org.cgatechnologies.wideya.teacher_management.models.TeacherModel
import uk.org.cgatechnologies.wideya.teacher_management.models.TeacherPayrollModel
import kotlin.properties.Delegates

class CommonListAdapter(private val multiSelect: Boolean = false) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var uniformType by Delegates.notNull<Int>()

    private lateinit var schoolManagementViewModel: SchoolManagementViewModel

    private lateinit var learnerManagementViewModel: LearnerManagementViewModel

    private lateinit var fragmentManager: FragmentManager

    private lateinit var context: Context

    private val items = mutableListOf<Any>()

    @SuppressLint("NotifyDataSetChanged")
    fun setItems(items: List<Any>) {
        this.items.clear()
        this.items.addAll(items)
        notifyDataSetChanged()
    }

    fun setDiffItems(newItems: List<Any>) {
        val diffCallback = AdapterDiffCallback(items, newItems)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        this.items.clear()
        this.items.addAll(newItems)
        diffResult.dispatchUpdatesTo(this)
    }

    fun setType(type: Int) {
        this.uniformType = type
    }

    fun setSchoolManagementViewModel(viewModel: SchoolManagementViewModel) {
        this.schoolManagementViewModel = viewModel
    }

    fun setLearnerManagementViewModel(viewModel: LearnerManagementViewModel) {
        this.learnerManagementViewModel = viewModel
    }

    fun setFragmentManager(fragmentManager: FragmentManager) {
        this.fragmentManager = fragmentManager
    }

    fun setContext(context: Context) {
        this.context = context
    }

    var onLearnerSearchItemClick: ((LearnerAdmissionModel) -> Unit)? = null
    var onPayrollTeacherItemClick: ((TeacherPayrollModel) -> Unit)? = null


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        when (viewType) {
            SCHOOL -> {
                val itemBinding =
                    ViewholderSchoolListItemBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )

                itemBinding.cbSelect.setOnCheckedChangeListener { cb, selected ->
                    val school = items.find {
                        (it as SchoolModel).uuid == cb.tag
                    } as SchoolModel
                    if (cb.isPressed) learnerManagementViewModel.updateMultiSelectStateFlow(
                        school,
                        selected
                    )
                }

                return SchoolItemViewHolder(itemBinding, schoolManagementViewModel, context)
            }
            GROUP -> {
                val itemBinding =
                    ViewholderSchoolGroupListItemBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )

                itemBinding.cbSelect.setOnCheckedChangeListener { cb, selected ->
                    val group = items.find {
                        (it as SchoolGroupModel).uuid == cb.tag
                    } as SchoolGroupModel
                    if (cb.isPressed) learnerManagementViewModel.updateMultiSelectStateFlow(
                        group,
                        selected
                    )
                }

                return SchoolGroupItemViewHolder(itemBinding)
            }
            PAYROLL_TEACHER -> {
                val itemBinding =
                    ViewholderTeacherPayrollListItemBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )

                itemBinding.cbSelect.setOnCheckedChangeListener { cb, selected ->
                    val teacher = items.find {
                        (it as TeacherPayrollModel).uuid == cb.tag
                    } as TeacherPayrollModel
                    if (cb.isPressed) learnerManagementViewModel.updateMultiSelectStateFlow(
                        teacher,
                        selected
                    )
                }

                return PayrollTeacherItemViewHolder(itemBinding, onPayrollTeacherItemClick)
            }
            SCHOOL_TEACHER -> {
                val itemBinding =
                    ViewholderSchoolTeacherListItemBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )

                itemBinding.cbSelect.setOnCheckedChangeListener { cb, selected ->
                    val teacher = items.find {
                        (it as TeacherModel).uuid == cb.tag
                    } as TeacherModel
                    if (cb.isPressed) learnerManagementViewModel.updateMultiSelectStateFlow(
                        teacher,
                        selected
                    )
                }

                return SchoolTeacherItemViewHolder(itemBinding)
            }
            SCHOOL_LEARNER -> {
                val itemBinding =
                    ViewholderSchoolLearnerListItemBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )

                itemBinding.cbSelect.setOnCheckedChangeListener { cb, selected ->
                    val learner = items.find {
                        (it as LearnerAdmissionModel).uuid == cb.tag
                    } as LearnerAdmissionModel
                    learner.isSelected = selected
                    if (cb.isPressed) learnerManagementViewModel.updateMultiSelectStateFlow(
                        learner,
                        selected
                    )
                }
                return SchoolLearnerAdmissionItemViewHolder(itemBinding)
            }
            SCHOOL_GROUP_LEARNER -> {
                val itemBinding = ViewholderSchoolGroupLearnerListItemBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )

                itemBinding.cbSelect.setOnCheckedChangeListener { cb, selected ->
                    val learner = items.find {
                        (it as SchoolGroupLearnerModel).uuid == cb.tag
                    } as SchoolGroupLearnerModel
                    learner.isSelected = selected
                    if (cb.isPressed) learnerManagementViewModel.updateMultiSelectStateFlow(
                        learner,
                        selected
                    )
                }

                return SchoolGroupLearnerItemViewHolder(itemBinding)
            }
            SCHOOL_TEACHER_ATTENDANCE -> {
                val itemBinding = ViewholderSchoolTeacherAttendanceListItemBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )

                itemBinding.cbSelect.setOnCheckedChangeListener { cb, selected ->
                    val attendance = items.find {
                        (it as PersonAttendanceModel).uuid == cb.tag
                    } as PersonAttendanceModel
                    if (cb.isPressed) learnerManagementViewModel.updateMultiSelectStateFlow(
                        attendance,
                        selected
                    )
                }

                return SchoolTeacherAttendanceItemViewHolder(
                    itemBinding,
                    schoolManagementViewModel,
                    fragmentManager
                )
            }
            SCHOOL_LEARNER_ATTENDANCE -> {
                val itemBinding = ViewholderSchoolLearnerAttendanceListItemBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )

                itemBinding.cbSelect.setOnCheckedChangeListener { cb, selected ->
                    val attendance = items.find {
                        (it as PersonAttendanceModel).uuid == cb.tag
                    } as PersonAttendanceModel
                    if (cb.isPressed) learnerManagementViewModel.updateMultiSelectStateFlow(
                        attendance,
                        selected
                    )
                }

                return SchoolLearnerAttendanceItemViewHolder(itemBinding, schoolManagementViewModel)
            }
            ANALYSIS_LEARNER_DISABILITY -> {
                val itemBinding = ViewholderLearnerDisabilityItemBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                return LearnerDisabilityItemViewHolder(itemBinding)
            }
            LEARNER_SEARCH -> {
                val itemBinding = ViewholderLearnerSearchItemBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                return LearnerSearchItemViewHolder(itemBinding) { learner ->
                    onLearnerSearchItemClick?.invoke(learner)
                }
            }

            else -> {
                val itemBinding =
                    ViewholderEmptyListItemBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                return EmptyViewHolder(itemBinding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]

        when (getItemViewType(position)) {
            SCHOOL ->
                with(holder as SchoolItemViewHolder) {
                    val schoolItem: SchoolModel = item as SchoolModel
                    bind(schoolItem, multiSelect)
                }
            GROUP ->
                with(holder as SchoolGroupItemViewHolder) {
                    val schoolGroupItem: SchoolGroupModel = item as SchoolGroupModel
                    bind(schoolGroupItem, multiSelect)
                }
            PAYROLL_TEACHER ->
                with(holder as PayrollTeacherItemViewHolder) {
                    val teacher: TeacherPayrollModel = item as TeacherPayrollModel
                    bind(teacher, multiSelect)
                }
            SCHOOL_TEACHER ->
                with(holder as SchoolTeacherItemViewHolder) {
                    val teacher: TeacherModel = item as TeacherModel
                    bind(teacher, multiSelect)
                }
            SCHOOL_LEARNER ->
                with(holder as SchoolLearnerAdmissionItemViewHolder) {
                    val learnerAdmission: LearnerAdmissionModel = item as LearnerAdmissionModel
                    bind(learnerAdmission, multiSelect)
                }
            SCHOOL_GROUP_LEARNER ->
                with(holder as SchoolGroupLearnerItemViewHolder) {
                    val schoolGroupLearner: SchoolGroupLearnerModel =
                        item as SchoolGroupLearnerModel
                    bind(schoolGroupLearner, multiSelect)
                }
            SCHOOL_TEACHER_ATTENDANCE ->
                with(holder as SchoolTeacherAttendanceItemViewHolder) {
                    val teacher: PersonAttendanceModel = item as PersonAttendanceModel
                    bind(teacher, multiSelect)
                }
            SCHOOL_LEARNER_ATTENDANCE ->
                with(holder as SchoolLearnerAttendanceItemViewHolder) {
                    val learner: PersonAttendanceModel = item as PersonAttendanceModel
                    bind(learner, multiSelect)
                }
            ANALYSIS_LEARNER_DISABILITY ->
                with(holder as LearnerDisabilityItemViewHolder) {
                    val learner: LearnerDisabilityModel = item as LearnerDisabilityModel
                    bind(learner)
                }
            LEARNER_SEARCH ->
                with(holder as LearnerSearchItemViewHolder) {
                    val learner: LearnerAdmissionModel = item as LearnerAdmissionModel
                    bind(learner)
                }
            else ->
                with(holder as EmptyViewHolder) {
                    bind()
                }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateAllSelection(isSelected: Boolean) {
        items.forEach { item ->
            when (item) {
                is SchoolModel -> {}
                is SchoolGroupModel -> {}
                is TeacherPayrollModel -> {}
                is TeacherModel -> {}
                is LearnerAdmissionModel -> {
                    item.isSelected = isSelected
                    notifyDataSetChanged()
                }
                is SchoolGroupLearnerModel -> {
                    item.isSelected = isSelected
                    notifyDataSetChanged()
                }
                is PersonAttendanceModel -> {}
                else -> {}
            }
        }
        learnerManagementViewModel.updateMultiSelectStateFlowByMany(items, isSelected)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun checkSelectedItems(selectedList: MutableSet<Any>) {
        selectedList.forEach { selected ->
            when (selected) {
                is SchoolModel -> {}
                is SchoolGroupModel -> {}
                is TeacherPayrollModel -> {}
                is TeacherModel -> {}
                is LearnerAdmissionModel -> {
                    val learner = items.firstOrNull {
                        (it as LearnerAdmissionModel).uuid == selected.uuid
                    }
                    learner?.let {
                        (it as LearnerAdmissionModel).isSelected = true
                    } ?: run {
                        learnerManagementViewModel.clearMultiSelectStateFlow()
                    }

                    notifyDataSetChanged()
                }
                is SchoolGroupLearnerModel -> {
                    val learner = items.firstOrNull {
                        (it as SchoolGroupLearnerModel).uuid == selected.uuid
                    }
                    learner?.let {
                        (it as SchoolGroupLearnerModel).isSelected = true
                    } ?: run {
                        learnerManagementViewModel.clearMultiSelectStateFlow()
                    }

                    notifyDataSetChanged()
                }
                is PersonAttendanceModel -> {}
                else -> {}
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return uniformType
    }

    override fun getItemCount(): Int {
        return items.size
    }

    companion object {
        const val SCHOOL: Int = 0
        const val GROUP: Int = 1
        const val PAYROLL_TEACHER: Int = 2
        const val SCHOOL_TEACHER: Int = 3
        const val SCHOOL_LEARNER: Int = 4
        const val SCHOOL_GROUP_LEARNER: Int = 5
        const val SCHOOL_TEACHER_ATTENDANCE: Int = 6
        const val SCHOOL_LEARNER_ATTENDANCE: Int = 7
        const val ANALYSIS_LEARNER_DISABILITY: Int = 8
        const val LEARNER_SEARCH: Int = 9
    }
}

class AdapterDiffCallback(
    private val oldList: List<Any>,
    private val newList: List<Any>
) : DiffUtil.Callback() {

    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = oldList[oldItemPosition]
        val newItem = newList[newItemPosition]
        return when {
            oldItem is SchoolModel && newItem is SchoolModel -> {
                oldItem.uuid == (newItem).uuid
            }
            oldItem is SchoolGroupModel && newItem is SchoolGroupModel -> {
                oldItem.uuid == (newItem).uuid
            }
            oldItem is TeacherPayrollModel && newItem is TeacherPayrollModel -> {
                oldItem.uuid == (newItem).uuid
            }
            oldItem is TeacherModel && newItem is TeacherModel -> {
                oldItem.uuid == (newItem).uuid
            }
            oldItem is LearnerAdmissionModel && newItem is LearnerAdmissionModel -> {
                oldItem.uuid == (newItem).uuid
            }
            oldItem is SchoolGroupLearnerModel && newItem is SchoolGroupLearnerModel -> {
                oldItem.uuid == (newItem).uuid
            }
            oldItem is PersonAttendanceModel && newItem is PersonAttendanceModel -> {
                oldItem.uuid == (newItem).uuid
            }
            else -> {
                true
            }
        }
    }

    override fun areContentsTheSame(oldPosition: Int, newPosition: Int): Boolean {
        val oldItem = oldList[oldPosition]
        val newItem = newList[newPosition]
        return when {
            oldItem is SchoolModel && newItem is SchoolModel -> {
                val (_, oldValue, oldName) = oldItem
                val (_, newValue, newName) = newItem
                oldName == newName && oldValue == newValue
            }
            oldItem is SchoolGroupModel && newItem is SchoolGroupModel -> {
                val (_, oldValue, oldName) = oldItem
                val (_, newValue, newName) = newItem
                oldName == newName && oldValue == newValue
            }
            oldItem is TeacherPayrollModel && newItem is TeacherPayrollModel -> {
                val (_, oldValue, oldName) = oldItem
                val (_, newValue, newName) = newItem
                oldName == newName && oldValue == newValue
            }
            oldItem is TeacherModel && newItem is TeacherModel -> {
                val (_, oldValue, oldName) = oldItem
                val (_, newValue, newName) = newItem
                oldName == newName && oldValue == newValue
            }
            oldItem is LearnerAdmissionModel && newItem is LearnerAdmissionModel -> {
                val (_, oldValue, oldName) = oldItem
                val (_, newValue, newName) = newItem
                oldName == newName && oldValue == newValue
            }
            oldItem is SchoolGroupLearnerModel && newItem is SchoolGroupLearnerModel -> {
                val (_, oldValue, oldName) = oldItem
                val (_, newValue, newName) = newItem
                oldName == newName && oldValue == newValue
            }
            oldItem is PersonAttendanceModel && newItem is PersonAttendanceModel -> {
                val (_, oldValue, oldName) = oldItem
                val (_, newValue, newName) = newItem
                oldName == newName && oldValue == newValue
            }
            else -> {
                true
            }
        }
    }
}