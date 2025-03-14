package org.inquest.utils

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object DoubleExtensions {
    /**
     * @return A string with [this] formatted according to [pattern]
     */
    fun Double.format(pattern: String): String = DecimalFormat(pattern, DecimalFormatSymbols(Locale.GERMAN)).apply {
        this.minimumFractionDigits = 1
    }.format(this)

    fun Iterable<Double>.averageOrNull(): Double? = if (iterator().hasNext()) {
        average()
    } else {
        null
    }
}

object IntExtensions {
    /**
     * Pads this with [padding] spaces
     */
    fun Int.padded(padding: Int) = "%1$${padding}s".format(this)

    /**
     * Pads this with [padding] spaces
     */
    fun Long.padded(padding: Int) = "%1$${padding}s".format(this)

    fun Iterable<Int>.averageOrNull(): Double? = if (iterator().hasNext()) {
        average()
    } else {
        null
    }
}
