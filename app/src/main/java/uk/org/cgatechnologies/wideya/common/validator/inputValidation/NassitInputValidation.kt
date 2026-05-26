package uk.org.cgatechnologies.wideya.common.validator.inputValidation

import android.widget.EditText
import com.google.android.material.textfield.TextInputLayout
import uk.org.cgatechnologies.wideya.common.validator.ValidationUtils
import uk.org.cgatechnologies.wideya.common.validator.base.BaseInputValidation
import uk.org.cgatechnologies.wideya.common.validator.base.BaseValidator
import uk.org.cgatechnologies.wideya.common.validator.validatorType.AlphaNumericValidator
import uk.org.cgatechnologies.wideya.common.validator.validatorType.LengthValidator

class NassitInputValidation(editText: EditText,
                            textInputLayout: TextInputLayout
) : BaseInputValidation(editText, textInputLayout) {
    override var minLength = 17
    override var maxLength = 17
    override fun validate(): Boolean {
        val validationResult = BaseValidator.validate(
            LengthValidator(),
            AlphaNumericValidator(),
            input = editText.text.toString(),
            isRequired = isRequired,
            minLength = minLength,
            maxLength = maxLength
        )
        ValidationUtils.setTextLayoutValidationStatus(textInputLayout, validationResult)
        return validationResult.isSuccess
    }
}