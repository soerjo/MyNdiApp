package com.soerjo.myndicam.domain.model

enum class ScreenMode(val value: Int) {
    EXPERIMENT_INTERNAL(3),
    INTERNAL(2),
    USB(1);

    companion object {
        fun fromValue(value: Int) = values().find { it.value == value } ?: INTERNAL
    }
}
