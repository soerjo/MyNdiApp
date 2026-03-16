package com.soerjo.myndicam.core.common

/**
 * App constants
 */
object Constants {
    const val SCREEN_MODE = 1

    const val TARGET_WIDTH = 1920
    const val TARGET_HEIGHT = 1080

    const val DEFAULT_SOURCE_NAME = "Android Camera"

    const val TARGET_ASPECT_RATIO_NUMERATOR = 16
    const val TARGET_ASPECT_RATIO_DENOMINATOR = 9
    val TARGET_ASPECT_RATIO = TARGET_ASPECT_RATIO_NUMERATOR.toFloat() / TARGET_ASPECT_RATIO_DENOMINATOR

    const val TAG_NDI = "NDI"
    const val TAG_CAMERA = "Camera"
    const val TAG_APP = "MyNdiCam"

    object Ui {
        const val TALLY_BORDER_WIDTH_DP = 12
        const val TALLY_BLINK_DURATION_MS = 500
        const val COLOR_PROGRAM_TALLY = 0xFF00FF00
        const val COLOR_PREVIEW_TALLY = 0xFFFFFF00

        const val LIVE_BADGE_FONT_SIZE_SP = 11
        const val LIVE_BADGE_CORNER_RADIUS_DP = 4
        const val LIVE_BADGE_ALPHA = 0.9f

        const val STATUS_BG_ALPHA_NORMAL = 0.5f
        const val STATUS_BG_ALPHA_OPAQUE = 0.9f
        const val STATUS_CORNER_RADIUS_DP = 8

        const val MAIN_FAB_SIZE_DP = 72
        const val FAB_ICON_SIZE_DP = 36
        const val CONTROL_BUTTON_SIZE_DP = 48
    }
}
