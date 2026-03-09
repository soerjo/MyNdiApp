package com.soerjo.myndicam.domain.usecase

import com.soerjo.myndicam.domain.model.FrameRate
import com.soerjo.myndicam.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Use case for saving app settings
 */
class SaveSettingsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    suspend fun saveSourceName(name: String) {
        settingsRepository.saveSourceName(name)
    }

    suspend fun saveFrameRate(frameRate: FrameRate) {
        settingsRepository.saveFrameRate(frameRate)
    }
}
