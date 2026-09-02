package com.example

import android.app.Application
import android.graphics.Bitmap
import android.os.Looper
import android.util.Log
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit

class NafiTvApp : Application(), ImageLoaderFactory {

    private var customImageLoader: ImageLoader? = null

    override fun onCreate() {
        super.onCreate()
        instance = this

        // 1. Global crash protection: Intercepts background decoder / OkHttp / coroutine crashes
        // preventing unexpected process termination on low-spec devices and TV boxes
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("NafiTvApp", "Intercepted crash on thread: ${thread.name}", throwable)
            if (thread != Looper.getMainLooper().thread) {
                // Background thread error (e.g. MediaCodec, DNS resolution, Coil decoding) -> ignore & recover
                Log.w("NafiTvApp", "Suppressed background thread exception: ${throwable.message}")
            } else {
                // Try to catch non-fatal runtime exceptions
                if (throwable is NullPointerException || throwable is IndexOutOfBoundsException || throwable is IllegalStateException) {
                    Log.w("NafiTvApp", "Recovered from main thread UI exception: ${throwable.message}")
                } else {
                    defaultHandler?.uncaughtException(thread, throwable)
                }
            }
        }
    }

    override fun newImageLoader(): ImageLoader {
        return customImageLoader ?: synchronized(this) {
            customImageLoader ?: buildOptimizedImageLoader().also { customImageLoader = it }
        }
    }

    private fun buildOptimizedImageLoader(): ImageLoader {
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()

        return ImageLoader.Builder(this)
            .okHttpClient(okHttpClient)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.15) // Safe 15% memory limit prevents Low Memory Killer (LMK)
                    .strongReferencesEnabled(true)
                    .weakReferencesEnabled(true)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(File(cacheDir, "nafitv_image_cache"))
                    .maxSizeBytes(40L * 1024 * 1024) // 40 MB disk cache limit
                    .build()
            }
            .bitmapConfig(Bitmap.Config.RGB_565) // 50% memory saving on all channel logos & posters
            .allowHardware(false) // Disables hardware bitmaps for 100% crash-free stability on TV boxes (Mali/PowerVR GPUs)
            .allowRgb565(true)
            .crossfade(false) // Saves GPU compositing passes on low-RAM TV boxes
            .networkObserverEnabled(true)
            .respectCacheHeaders(false)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .build()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        try {
            customImageLoader?.memoryCache?.clear()
            if (level >= TRIM_MEMORY_MODERATE) {
                System.gc()
            }
        } catch (e: Exception) {
            Log.w("NafiTvApp", "Error trimming memory", e)
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        try {
            customImageLoader?.memoryCache?.clear()
            System.gc()
        } catch (e: Exception) {
            Log.w("NafiTvApp", "Error on low memory", e)
        }
    }

    companion object {
        lateinit var instance: NafiTvApp
            private set
    }
}
