package uk.org.cgatechnologies.wideya.common.validator.inputValidation

import android.widget.EditText
import com.google.android.material.textfield.TextInputLayout
import uk.org.cgatechnologies.wideya.common.validator.ValidationUtils
import uk.org.cgatechnologies.wideya.common.validator.base.BaseInputValidation
import uk.org.cgatechnologies.wideya.common.validator.data.ComparisonOperator
import uk.org.cgatechnologies.wideya.common.validator.validatorType.ComparisonValidator
import uk.org.cgatechnologies.wideya.common.validator.validatorType.EmptyValidator

/**
 * Created by Mohamad Abuzaid on 1/19/2023.
 */

class TimetableLessonTimingValidation(
    etStartDate: EditText,
    etEndDate: EditText,
    tiEndDate: TextInputLayout,
    maxDurationInHours: String
) : BaseInputValidation(
    etEndDate,
    tiEndDate,
    extraData1 = maxDurationInHours,
    associatedEditText1 = etStartDate
) {
    override fun validate(): Boolean {
        val startDate = associatedField1?.text.toString().trim()
        val endDate = editText.text.toString().trim()
        val maxDurationInHours = extraData1.toInt()

        // checky empty
        var validationResult = EmptyValidator().validate(editText.text.toString())

        if (validationResult.isSuccess === true) {
            // check the end time is valid relative to start time
            validationResult = ComparisonValidator().validate(
                endDate,
                compareInput = startDate,
                comparisonOperator = ComparisonOperator.DATE_AFTER,
                errorMessage = "End time must be after start time"
            )
        }

        if (validationResult.isSuccess === true) {
            // check the duration of the lesson is valid
            validationResult = ComparisonValidator().validate(
                endDate,
                compareInput = startDate,
                comparisonOperator = ComparisonOperator.PERIOD_LENGTH,
                maxLength = maxDurationInHours,
                errorMessage = "Class can't be longer than $maxDurationInHours hours"
            )
        }

        ValidationUtils.setTextLayoutValidationStatus(textInputLayout, validationResult)
        return validationResult.isSuccess
    }
}