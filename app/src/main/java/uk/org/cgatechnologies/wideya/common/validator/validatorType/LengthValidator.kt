package uk.org.cgatechnologies.wideya.common.validator.validatorType

import uk.org.cgatechnologies.wideya.common.validator.base.BaseValidator
import uk.org.cgatechnologies.wideya.common.validator.base.ValidationResult

class LengthValidator : BaseValidator() {
    override fun validate(input:String,
                          isRequired: Boolean,
                          minLength: Int,
                          maxLength: Int,
                          compareInput: String?,
                          errorMessage: String,
                          comparisonOperator: String
    ): ValidationResult {
        if (input.isNotEmpty() || isRequired){
            // special case error message if minLength = maxLength
            if(minLength == maxLength && input.length != minLength) {
                return ValidationResult(false, "Must be $minLength characters")
            }
            if(input.length < minLength) {
                return ValidationResult(false, "Minimum characters: $minLength")
            }
            if(input.length > maxLength) {
                return ValidationResult(false, "Maximum characters: $maxLength")
            }
        }
        return ValidationResult(
            true,
            String()
        )
    }

}