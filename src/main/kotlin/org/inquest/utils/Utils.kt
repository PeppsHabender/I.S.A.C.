package org.inquest.utils

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

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
