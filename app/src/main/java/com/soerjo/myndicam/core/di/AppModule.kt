package com.soerjo.myndicam.core.di

import com.soerjo.myndicam.data.datasource.CameraDataSource
import com.soerjo.myndicam.data.repository.CameraRepositoryImpl
import com.soerjo.myndicam.data.repository.SettingsRepositoryImpl
import com.soerjo.myndicam.domain.repository.CameraRepository
import com.soerjo.myndicam.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt DI module for app bindings
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindCameraRepository(
        impl: CameraRepositoryImpl
    ): CameraRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        impl: SettingsRepositoryImpl
    ): SettingsRepository
}
