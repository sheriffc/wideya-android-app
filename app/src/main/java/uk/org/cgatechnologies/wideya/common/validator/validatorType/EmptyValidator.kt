package uk.org.cgatechnologies.wideya.common.validator.validatorType

import uk.org.cgatechnologies.wideya.common.data.Constants
import uk.org.cgatechnologies.wideya.common.validator.base.BaseValidator
import uk.org.cgatechnologies.wideya.common.validator.base.ValidationResult

class EmptyValidator : BaseValidator() {
    override fun validate(input:String,
                          isRequired: Boolean,
                          minLength: Int,
                          maxLength: Int,
                          compareInput: String?,
                          errorMessage: String,
                          comparisonOperator: String
    ): ValidationResult {
        val isValid = input.isNotEmpty()
        return ValidationResult(
            isValid,
            if (isValid) String() else Constants.EMPTY_FIELD_VALIDATION
        )
    }

}