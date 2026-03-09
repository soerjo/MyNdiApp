package com.soerjo.ndi.model

/**
 * Represents the NDI tally state
 * @param isOnPreview true when source is in preview (e.g., OBS preview window)
 * @param isOnProgram true when source is live/on-air (e.g., OBS program output)
 */
data class TallyState(
    val isOnPreview: Boolean = false,
    val isOnProgram: Boolean = false
) {
    /**
     * @return true when on preview OR program (any tally is active)
     */
    val isTallyOn: Boolean
        get() = isOnPreview || isOnProgram
}
