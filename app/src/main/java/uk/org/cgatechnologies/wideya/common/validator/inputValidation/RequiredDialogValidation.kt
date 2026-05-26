package uk.org.cgatechnologies.wideya.common.validator.inputValidation

import android.widget.EditText
import com.google.android.material.textfield.TextInputLayout
import uk.org.cgatechnologies.wideya.common.validator.ValidationUtils
import uk.org.cgatechnologies.wideya.common.validator.base.BaseInputValidation
import uk.org.cgatechnologies.wideya.common.validator.validatorType.EmptyValidator

class RequiredDialogValidation(editText: EditText,
                               textInputLayout: TextInputLayout
) : BaseInputValidation(editText, textInputLayout) {
    override fun validate(): Boolean {
        val validationResult = EmptyValidator().validate(editText.text.toString())
        ValidationUtils.setTextLayoutValidationStatus(textInputLayout, validationResult)
        return validationResult.isSuccess
    }
}