package uk.org.cgatechnologies.wideya.common.validator.validatorType

import uk.org.cgatechnologies.wideya.common.validator.base.BaseValidator
import uk.org.cgatechnologies.wideya.common.validator.base.ValidationResult

class NinIllegalCharacterValidator : BaseValidator() {
    private val unacceptedChar = Regex("[I,L,O,U]", setOf(RegexOption.IGNORE_CASE,RegexOption.DOT_MATCHES_ALL))
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
            isValid = !unacceptedChar.containsMatchIn(input)
            return ValidationResult(
                isValid,
                if (isValid) String() else "NIN can never contain: O,L,I,U"
            )

        }
        return ValidationResult(
            true,
            String()
        )
    }


}