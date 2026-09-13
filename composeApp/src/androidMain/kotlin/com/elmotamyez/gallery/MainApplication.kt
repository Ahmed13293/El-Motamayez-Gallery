package com.elmotamyez.gallery

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import com.elmotamyez.gallery.di.appModule
import com.elmotamyez.gallery.util.ApplicationContextHolder
import kotlinx.coroutines.Dispatchers
import okio.Path.Companion.toOkioPath
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class MainApplication : Application(), SingletonImageLoader.Factory {

    override fun onCreate() {
        super.onCreate()
        ApplicationContextHolder.context = this
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@MainApplication)
            modules(appModule)
        }
    }

    // Coil defaults: 25% heap cache + up to 64 concurrent downloads → OOM on low-memory devices.
    // Fix: cap memory cache at 10%, add 50MB disk cache, limit concurrent fetches/decodes.
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizeBytes((Runtime.getRuntime().maxMemory() * 0.10).toLong())
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("coil_image_cache").toOkioPath())
                    .maxSizeBytes(50L * 1024 * 1024)
                    .build()
            }
            // Limit how many images are in-flight at once; each in-flight image holds
            // its full compressed bytes + decoded bitmap in memory simultaneously.
            .fetcherCoroutineContext(Dispatchers.IO.limitedParallelism(4))
            .decoderCoroutineContext(Dispatchers.IO.limitedParallelism(2))
            .build()
}
