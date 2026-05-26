package uk.org.cgatechnologies.wideya.common.validator

import androidx.core.widget.doOnTextChanged
import com.google.android.material.textfield.TextInputLayout
import uk.org.cgatechnologies.wideya.common.validator.base.BaseInputValidation
import uk.org.cgatechnologies.wideya.common.validator.base.ValidationResult

class ValidationUtils {
    companion object{
        fun setTextLayoutValidationStatus(textInputLayout: TextInputLayout, validationResult: ValidationResult) {
            textInputLayout.apply {
                if (!validationResult.isSuccess) {
                    error = validationResult.message
                    isErrorEnabled = true
                } else {
                    error = null
                    isErrorEnabled = false
                }
            }
        }

        fun checkFieldsAreValid(validationsArray: MutableSet<BaseInputValidation>): Boolean {
            val validationErrors = mutableListOf<Boolean>()

            for(validation in validationsArray) {
                validation.editText.apply {
                    if (!validation.validate()) validationErrors.add(false) else null
                }
                // turn on live validation
                validation.isValidationInitiated = true
            }
            return validationErrors.isEmpty()
        }

        fun addLiveValidation(validationsArray: MutableSet<BaseInputValidation>) {

            for (validation in validationsArray){
                validation.editText.apply {
                    doOnTextChanged { text, _, _, _ ->
                        if (!validation.isValidationInitiated && text != null && text.length >= validation.minLength) {
                            validation.isValidationInitiated = true
                        }

                        if (validation.isValidationInitiated) {
                            validation.validate()
                        }
                    }
                }
            }
        }
    }
}