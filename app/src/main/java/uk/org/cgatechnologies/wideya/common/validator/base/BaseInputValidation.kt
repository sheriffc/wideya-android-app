package uk.org.cgatechnologies.wideya.common.validator.base

import android.widget.EditText
import com.google.android.material.textfield.TextInputLayout

abstract class BaseInputValidation(
    editText: EditText,
    textInputLayout: TextInputLayout,
    isRequired: Boolean = false,
    extraData1: String = String(),
    extraData2: String = String(),
    associatedEditText1: EditText? = null,
) {
    open var minLength: Int = 0
    open var maxLength: Int = 500
    var isValidationInitiated: Boolean = false
    var textInputLayout = textInputLayout
    var editText = editText
    val isRequired = isRequired
    val extraData1 = extraData1
    val extraData2 = extraData2
    val associatedField1 = associatedEditText1

    abstract fun validate(): Boolean
}