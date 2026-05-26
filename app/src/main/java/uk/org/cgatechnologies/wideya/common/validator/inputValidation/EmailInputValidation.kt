package uk.org.cgatechnologies.wideya.common.validator.inputValidation

import android.widget.EditText
import com.google.android.material.textfield.TextInputLayout
import uk.org.cgatechnologies.wideya.common.validator.ValidationUtils
import uk.org.cgatechnologies.wideya.common.validator.base.BaseInputValidation
import uk.org.cgatechnologies.wideya.common.validator.base.BaseValidator
import uk.org.cgatechnologies.wideya.common.validator.validatorType.EmailValidator

class EmailInputValidation(editText: EditText,
                           textInputLayout: TextInputLayout
) : BaseInputValidation(editText, textInputLayout) {
    override fun validate(): Boolean {
        val validationResult = BaseValidator.validate(
            EmailValidator(),
            input = editText.text.toString(),
            isRequired = isRequired
        )
        ValidationUtils.setTextLayoutValidationStatus(textInputLayout, validationResult)
        return validationResult.isSuccess
    }
}