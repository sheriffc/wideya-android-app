package uk.org.cgatechnologies.wideya.common.validator.inputValidation

import android.widget.EditText
import com.google.android.material.textfield.TextInputLayout
import uk.org.cgatechnologies.wideya.R
import uk.org.cgatechnologies.wideya.common.data.database.AppDatabase
import uk.org.cgatechnologies.wideya.common.validator.ValidationUtils
import uk.org.cgatechnologies.wideya.common.validator.base.BaseInputValidation
import uk.org.cgatechnologies.wideya.common.validator.base.ValidationResult
import uk.org.cgatechnologies.wideya.school_group_management.SchoolGroupManagementDao

class SchoolGroupNameValidation(editText: EditText,
                                textInputLayout: TextInputLayout,
                                schoolUuid: String,
                                schoolGroupUuid: String,
                                schoolGroupLevelOid: EditText,
) : BaseInputValidation(editText, textInputLayout, extraData1 = schoolUuid, extraData2 = schoolGroupUuid, associatedEditText1 = schoolGroupLevelOid) {
    override fun validate(): Boolean {

        val schoolGroupManagementDao: SchoolGroupManagementDao? = AppDatabase.getInstance()?.schoolGroupManagementDao()
        if(schoolGroupManagementDao == null) {
            return true
        }

        val schoolGroupLevelOid: String = associatedField1?.getTag(R.string.idTag)?.toString().orEmpty()
        // skip validation if school group level is not set yet
        if(schoolGroupLevelOid.isNullOrEmpty()) {
            return true
        }

        var inputText = editText.text.toString().trim()
        val preexistingSchoolGroupName = schoolGroupManagementDao.checkDuplicateSchoolGroupNameAndLevel(
            inputText,
            extraData1,
            schoolGroupLevelOid,
            extraData2,
        )

        val validationResult: ValidationResult
        if (preexistingSchoolGroupName != null && inputText.isNullOrEmpty()) {
            validationResult = ValidationResult(
                false,
                "Cannot have two empty classroom names at the same school level"
            )
        } else if (preexistingSchoolGroupName != null) {
            validationResult = ValidationResult(
                false,
                "There is already a classroom with this name in the same school level"
            )
        } else {
            validationResult = ValidationResult(
                true,
                String()
            )
        }
        ValidationUtils.setTextLayoutValidationStatus(textInputLayout, validationResult)
        return validationResult.isSuccess
    }
}