package com.soerjo.myndicam.data.repository

import android.content.Context
import com.soerjo.myndicam.domain.model.FrameRate
import com.soerjo.myndicam.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of SettingsRepository
 */
@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SettingsRepository {

    companion object {
        private const val PREFS_NAME = "NDIPrefs"
        private const val KEY_SOURCE_NAME = "ndi_source_name"
        private const val KEY_FRAME_RATE = "frame_rate"

        const val DEFAULT_SOURCE_NAME = "Android Camera"
        val DEFAULT_FRAME_RATE = FrameRate.FPS_30
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _sourceName = MutableStateFlow(
        prefs.getString(KEY_SOURCE_NAME, DEFAULT_SOURCE_NAME) ?: DEFAULT_SOURCE_NAME
    )
    private val _frameRate = MutableStateFlow(
        FrameRate.fromFps(prefs.getInt(KEY_FRAME_RATE, DEFAULT_FRAME_RATE.fps))
    )

    override fun getSourceName(): Flow<String> = _sourceName.asStateFlow()

    override suspend fun saveSourceName(name: String) {
        prefs.edit().putString(KEY_SOURCE_NAME, name).apply()
        _sourceName.value = name
    }

    override fun getFrameRate(): Flow<FrameRate> = _frameRate.asStateFlow()

    override suspend fun saveFrameRate(frameRate: FrameRate) {
        prefs.edit().putInt(KEY_FRAME_RATE, frameRate.fps).apply()
        _frameRate.value = frameRate
    }
}
