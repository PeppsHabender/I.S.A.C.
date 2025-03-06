package org.inquest.utils

import org.jetbrains.kotlinx.kandy.letsplot.feature.Layout
import org.jetbrains.kotlinx.kandy.letsplot.style.CustomStyle
import org.jetbrains.kotlinx.kandy.util.color.Color
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Default plotting style for isac plots.
 */
fun Layout.defaultStyle(block: CustomStyle.() -> Unit = {}) {
    style {
        plotCanvas.caption {
            color = Color.WHITE
        }

        plotCanvas.background {
            fillColor = Color.rgb(21, 21, 21)
        }

        panel.grid.majorLine {
            color = Color.GREY
        }

        legend.background {
            fillColor = Color.rgb(21, 21, 21)
        }

        legend.title {
            color = Color.WHITE
        }

        legend.text {
            color = Color.WHITE
        }

        axis.title {
            color = Color.WHITE
        }

        axis.text {
            color = Color.WHITE
        }

        block()
    }
}

fun <K, V> mapWithPutDefault(defaultValue: (key: K) -> V): ReadWriteProperty<Any?, Map<K, V>> =
    object : ReadWriteProperty<Any?, Map<K, V>> {
        private var map: MutableMap<K, V> = with(mutableMapOf<K, V>()) {
            withDefault { key -> getOrPut(key) { defaultValue(key) } }
        }

        override fun getValue(thisRef: Any?, property: KProperty<*>): Map<K, V> = map

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: Map<K, V>) {
            this.map = value.toMutableMap()
        }
    }
