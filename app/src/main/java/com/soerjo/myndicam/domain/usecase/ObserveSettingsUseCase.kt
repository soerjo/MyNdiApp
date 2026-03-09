package com.soerjo.myndicam.domain.usecase

import com.soerjo.myndicam.domain.model.FrameRate
import com.soerjo.myndicam.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for observing app settings
 */
class ObserveSettingsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    fun getSourceName(): Flow<String> = settingsRepository.getSourceName()
    fun getFrameRate(): Flow<FrameRate> = settingsRepository.getFrameRate()
}
