package com.soerjo.myndicam.data.camera.internal

import android.util.Size
import androidx.camera.core.AspectRatio
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy

object CameraHelper {
    fun createPreview(): Preview {
        return Preview.Builder().build()
    }

    fun createImageAnalysis(
        width: Int,
        height: Int,
        targetFps: Int = 30
    ): ImageAnalysis {
        return ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
            .setResolutionSelector(createResolutionSelector(width, height))
            .build()
    }

    fun createResolutionSelector(width: Int, height: Int): ResolutionSelector {
        return ResolutionSelector.Builder()
            .setResolutionStrategy(
                ResolutionStrategy(
                    Size(width, height),
                    ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                )
            )
            .setAspectRatioStrategy(createAspectRatioStrategy())
            .build()
    }

    fun createAspectRatioStrategy(): AspectRatioStrategy {
        return AspectRatioStrategy(
            AspectRatio.RATIO_16_9,
            AspectRatioStrategy.FALLBACK_RULE_AUTO
        )
    }
}
