package uk.org.cgatechnologies.wideya.common.validator.inputValidation

import android.widget.EditText
import com.google.android.material.textfield.TextInputLayout
import uk.org.cgatechnologies.wideya.common.validator.ValidationUtils
import uk.org.cgatechnologies.wideya.common.validator.base.BaseInputValidation
import uk.org.cgatechnologies.wideya.common.validator.base.BaseValidator
import uk.org.cgatechnologies.wideya.common.validator.validatorType.LengthValidator
import uk.org.cgatechnologies.wideya.common.validator.validatorType.NameIllegalCharacterValidator

class NameInputValidation(
    editText: EditText,
    textInputLayout: TextInputLayout,
    isRequired: Boolean = false
) : BaseInputValidation(editText, textInputLayout, isRequired) {
    override var minLength = 2
    override var maxLength = 30
    override fun validate(): Boolean {
        val validationResult = BaseValidator.validate(
            LengthValidator(),
            NameIllegalCharacterValidator(),
            input = editText.text.toString(),
            isRequired = isRequired,
            minLength = minLength,
            maxLength = maxLength
        )
        ValidationUtils.setTextLayoutValidationStatus(textInputLayout, validationResult)
        return validationResult.isSuccess
    }
}