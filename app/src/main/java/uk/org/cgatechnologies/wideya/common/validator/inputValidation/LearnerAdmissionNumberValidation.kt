package uk.org.cgatechnologies.wideya.common.validator.inputValidation

import android.widget.EditText
import com.google.android.material.textfield.TextInputLayout
import uk.org.cgatechnologies.wideya.common.data.database.AppDatabase
import uk.org.cgatechnologies.wideya.common.validator.ValidationUtils
import uk.org.cgatechnologies.wideya.common.validator.base.BaseInputValidation
import uk.org.cgatechnologies.wideya.common.validator.validatorType.ComparisonValidator
import uk.org.cgatechnologies.wideya.learner_management.LearnerManagementDao

class LearnerAdmissionNumberValidation(editText: EditText,
                                       textInputLayout: TextInputLayout,
                                       schoolUuid: String,
                                       learnerUuid: String
) : BaseInputValidation(editText, textInputLayout, extraData1 = schoolUuid, extraData2 = learnerUuid) {
    override fun validate(): Boolean {
        val learnerManagementDao: LearnerManagementDao? = AppDatabase.getInstance()?.learnerManagementDao()

        if(learnerManagementDao != null){
            var inputText = editText.text.toString()
            val existingAdmissionNumber = learnerManagementDao.checkAdmissionNumberExists(
                inputText,
                extraData1,
                extraData2
            ) ?: String()

            val validationResult = ComparisonValidator().validate(
                inputText,
                compareInput = existingAdmissionNumber,
                errorMessage = "This number is already assigned to another learner"
            )
            ValidationUtils.setTextLayoutValidationStatus(textInputLayout, validationResult)
            return validationResult.isSuccess
        }
        return true
    }
}