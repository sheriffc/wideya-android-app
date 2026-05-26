package uk.org.cgatechnologies.wideya.common.validator.base

import uk.org.cgatechnologies.wideya.common.utils.Utils
import uk.org.cgatechnologies.wideya.common.data.Constants

abstract class BaseValidator: IValidator {
    companion object {
        fun validate(
            vararg validators: IValidator,
            input: String,
            isRequired: Boolean,
            minLength: Int = 0,
            maxLength: Int = 0,
            compareInput: String = String(),
            errorMessage: String = String(),
            comparisonOperator: String = String()
        ): ValidationResult {
            // trim input before validating, since inputs are trimmed before saving
            val inputTrimmed = Utils.trimWhiteSpaceString(input)
            if (isRequired && inputTrimmed.isEmpty()) return ValidationResult(false, Constants.EMPTY_FIELD_VALIDATION)
            validators.forEach {
                val result = it.validate(inputTrimmed, isRequired, minLength, maxLength, compareInput, errorMessage, comparisonOperator)
                if (!result.isSuccess)
                    return result
            }
            return ValidationResult(true, "is successful")
        }
    }
}