package uk.org.cgatechnologies.wideya.common.validator.inputValidation

import android.widget.EditText
import com.google.android.material.textfield.TextInputLayout
import uk.org.cgatechnologies.wideya.common.validator.ValidationUtils
import uk.org.cgatechnologies.wideya.common.validator.base.BaseInputValidation
import uk.org.cgatechnologies.wideya.common.validator.base.BaseValidator
import uk.org.cgatechnologies.wideya.common.validator.validatorType.LengthValidator

class FreetextInputValidation(editText: EditText,
                              textInputLayout: TextInputLayout,
                              isRequired: Boolean = false
) : BaseInputValidation(editText, textInputLayout, isRequired) {
    override var minLength = 5
    override var maxLength = 150
    override fun validate(): Boolean {
        val validationResult = BaseValidator.validate(
            LengthValidator(),
            input = editText.text.toString(),
            isRequired = isRequired,
            minLength = minLength,
            maxLength = maxLength
        )
        ValidationUtils.setTextLayoutValidationStatus(textInputLayout, validationResult)
        return validationResult.isSuccess
    }
}