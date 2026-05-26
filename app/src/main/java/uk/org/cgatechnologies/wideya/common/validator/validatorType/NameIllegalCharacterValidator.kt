package uk.org.cgatechnologies.wideya.common.validator.validatorType

import uk.org.cgatechnologies.wideya.common.validator.base.BaseValidator
import uk.org.cgatechnologies.wideya.common.validator.base.ValidationResult

class NameIllegalCharacterValidator : BaseValidator() {
    private val alphaNumeric = Regex("[^a-zA-Z \\u00C0-\\u00FF\\s.'()-]", setOf(RegexOption.IGNORE_CASE,RegexOption.DOT_MATCHES_ALL))
    override fun validate(input:String,
                          isRequired: Boolean,
                          minLength: Int,
                          maxLength: Int,
                          compareInput: String?,
                          errorMessage: String,
                          comparisonOperator: String
    ): ValidationResult {
        if (input.isNotEmpty() || isRequired) {
            var isValid = false
            isValid = !alphaNumeric.containsMatchIn(input)
            return ValidationResult(
                isValid,
                if (isValid) String() else "Only allowed letters, hyphen, apostrophe, brackets, full-stop"
            )

        }
        return ValidationResult(
            true,
            String()
        )
    }


}