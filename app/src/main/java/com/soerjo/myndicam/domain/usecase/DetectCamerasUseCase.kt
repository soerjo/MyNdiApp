package com.soerjo.myndicam.domain.usecase

import com.soerjo.myndicam.domain.model.CameraInfo
import com.soerjo.myndicam.domain.repository.CameraRepository
import javax.inject.Inject

/**
 * Use case for detecting available cameras
 */
class DetectCamerasUseCase @Inject constructor(
    private val cameraRepository: CameraRepository
) {
    suspend operator fun invoke(): List<CameraInfo> {
        return cameraRepository.detectCameras()
    }
}
