package com.deepseek.dshmobile.di

import android.content.Context
import androidx.room.Room
import com.deepseek.dshmobile.database.AppDatabase
import com.deepseek.dshmobile.repository.SessionRepository
import com.deepseek.dshmobile.service.DshEngineManager
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [DatabaseModule::class, RepositoryModule::class, ServiceModule::class])
interface AppComponent {
    fun sessionRepository(): SessionRepository
    fun dshEngineManager(): DshEngineManager
    fun database(): AppDatabase

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun application(app: Context): Builder
        fun build(): AppComponent
    }

    companion object {
        @Volatile
        private var instanceInternal: AppComponent? = null

        val instance: AppComponent
            get() = instanceInternal
                ?: throw IllegalStateException("AppComponent has not been initialized")

        fun init(app: Context) {
            if (instanceInternal == null) {
                synchronized(this) {
                    if (instanceInternal == null) {
                        instanceInternal = DaggerAppComponent.builder()
                            .application(app.applicationContext)
                            .build()
                    }
                }
            }
        }

        /** 兼容旧入口写法 */
        fun provide(app: Context): AppComponent {
            init(app)
            return instance
        }
    }
}
