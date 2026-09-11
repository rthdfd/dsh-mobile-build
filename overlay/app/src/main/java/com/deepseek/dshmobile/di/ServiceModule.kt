package com.deepseek.dshmobile.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.deepseek.dshmobile.service.DshEngineManager
import com.deepseek.dshmobile.util.NetworkHelper

@Module
@InstallIn(SingletonComponent::class)
object ServiceModule {

    @Provides
    @Singleton
    fun provideNetworkHelper(@ApplicationContext context: Context): NetworkHelper =
        NetworkHelper(context)

    @Provides
    @Singleton
    fun provideDshEngineManager(@ApplicationContext context: Context): DshEngineManager =
        DshEngineManager.get(context)
}
