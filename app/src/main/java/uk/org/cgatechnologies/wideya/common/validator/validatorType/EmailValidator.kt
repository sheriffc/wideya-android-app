package uk.org.cgatechnologies.wideya.common.validator.validatorType

import android.text.TextUtils
import uk.org.cgatechnologies.wideya.common.validator.base.BaseValidator
import uk.org.cgatechnologies.wideya.common.validator.base.ValidationResult

class EmailValidator : BaseValidator() {
    override fun validate(input:String,
                          isRequired: Boolean,
                          minLength: Int,
                          maxLength: Int,
                          compareInput: String?,
                          errorMessage: String,
                          comparisonOperator: String): ValidationResult {
        if (input.isNotEmpty() || isRequired) {
            val isValid =
                !TextUtils.isEmpty(input) && android.util.Patterns.EMAIL_ADDRESS.matcher(input)
                    .matches()
            return ValidationResult(
                isValid,
                if (isValid) String() else "Invalid email format"
            )
        }
        return ValidationResult(
            true,
            String()
        )
    }

}