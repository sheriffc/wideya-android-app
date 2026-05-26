package uk.org.cgatechnologies.wideya.common.validator.base

interface IValidator {
    fun validate(input:String,
                 isRequired: Boolean = false,
                 minLength: Int = 0,
                 maxLength: Int = 0,
                 compareInput: String? = String(),
                 errorMessage: String = String(),
                 comparisonOperator: String = String()
    ): ValidationResult
}