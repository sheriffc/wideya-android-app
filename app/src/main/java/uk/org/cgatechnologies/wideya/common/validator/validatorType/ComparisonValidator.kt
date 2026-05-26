package uk.org.cgatechnologies.wideya.common.validator.validatorType

import uk.org.cgatechnologies.wideya.common.validator.base.BaseValidator
import uk.org.cgatechnologies.wideya.common.validator.base.ValidationResult
import uk.org.cgatechnologies.wideya.common.validator.data.ComparisonOperator
import java.text.SimpleDateFormat
import java.util.*

class ComparisonValidator : BaseValidator() {
    private var errorMessage = "Already exist!"
    override fun validate(
        input: String,
        isRequired: Boolean,
        minLength: Int,
        maxLength: Int,
        compareInput: String?,
        errorMessage: String,
        comparisonOperator: String
    ): ValidationResult {
        if (compareInput != null && compareInput.isNotEmpty()) {
            return if (input.isNotEmpty() || isRequired) {
                if (errorMessage.isNotEmpty()) {
                    this.errorMessage = errorMessage
                }
                when (comparisonOperator) {
                    ComparisonOperator.EQUALS -> equals(input, compareInput)
                    ComparisonOperator.NOT_EQUALS -> notEquals(input, compareInput)
                    ComparisonOperator.DATE_AFTER -> dateAfter(input, compareInput)
                    ComparisonOperator.PERIOD_LENGTH -> periodLength(input, compareInput, maxLength)
                    else -> equals(input, compareInput)
                }

            } else {
                ValidationResult(
                    true,
                    String()
                )
            }
        }
        return ValidationResult(
            true,
            String()
        )
    }

    private fun equals(first: String, second: String): ValidationResult {
        return if (first == second) {
            ValidationResult(false, errorMessage)
        } else {
            ValidationResult(
                true,
                String()
            )
        }
    }

    private fun notEquals(first: String, second: String): ValidationResult {
        return if (first != second) {
            ValidationResult(false, errorMessage)
        } else {
            ValidationResult(
                true,
                String()
            )
        }
    }

    private fun dateAfter(endDate: String, startDate: String): ValidationResult {
        val sdf = SimpleDateFormat("HH:mm", Locale.ENGLISH)
        sdf.timeZone = TimeZone.getTimeZone("UTC")

        val endTime = sdf.parse(endDate)
        val startTime = sdf.parse(startDate)

        return if ((endTime?.compareTo(startTime) ?: -1) <= 0) {
            ValidationResult(false, errorMessage)
        } else {
            ValidationResult(
                true,
                String()
            )
        }
    }

    private fun periodLength(endDate: String, startDate: String, length: Int): ValidationResult {
        val sdf = SimpleDateFormat("HH:mm", Locale.ENGLISH)
        sdf.timeZone = TimeZone.getTimeZone("UTC")

        val endTime = sdf.parse(endDate)
        val startTime = sdf.parse(startDate)

        val diffMillis = endTime?.time?.minus(startTime?.time ?: 0L) ?: 0L
        val diffHour = (diffMillis / (1000 * 60 * 60)) % 24

        return if (diffHour > length.toLong()) {
            ValidationResult(false, errorMessage)
        } else {
            ValidationResult(
                true,
                String()
            )
        }
    }
}