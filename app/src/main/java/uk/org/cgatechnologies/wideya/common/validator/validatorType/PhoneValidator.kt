package uk.org.cgatechnologies.wideya.common.validator.validatorType

import uk.org.cgatechnologies.wideya.common.validator.base.BaseValidator
import uk.org.cgatechnologies.wideya.common.validator.base.ValidationResult

class PhoneValidator: BaseValidator() {
    private val phonePattern = Regex("^[3,7,8,9]")
    override fun validate(input:String,
                          isRequired: Boolean,
                          minLength: Int,
                          maxLength: Int,
                          compareInput: String?,
                          errorMessage: String,
                          comparisonOperator: String
    ): ValidationResult {
        if (input.isNotEmpty() || isRequired) {
            val isValid = !phonePattern.containsMatchIn(input).not()
            return ValidationResult(
                isValid,
                if (isValid) String() else "Must begin with one of: 3,7,8,9"
            )
        }
        return ValidationResult(
            true,
            String()
        )
    }


}