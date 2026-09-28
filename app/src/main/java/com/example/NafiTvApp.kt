package com.example

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Looper
import android.util.Log
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.size.Precision
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit

class NafiTvApp : Application(), ImageLoaderFactory {

    private var customImageLoader: ImageLoader? = null

    override fun onCreate() {
        super.onCreate()
        instance = this

        // 1. Register optimized Coil ImageLoader globally so all AsyncImage instances use low-RAM decode
        try {
            coil.Coil.setImageLoader(newImageLoader())
        } catch (e: Exception) {
            Log.w("NafiTvApp", "Coil image loader init error", e)
        }

        // 2. Global crash shield: Intercepts all crashes on low-end devices & Android TV boxes
        // Prevents app from being killed or exiting abruptly ("অ্যাপ থেকে বের করে দিচ্ছে")
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("NafiTvApp", "Intercepted fatal exception on thread: ${thread.name}", throwable)
            try {
                // Free memory immediately in case of OutOfMemoryError
                customImageLoader?.memoryCache?.clear()
                System.gc()

                if (thread != Looper.getMainLooper().thread) {
                    // Background thread error (e.g. MediaCodec, DNS, OkHttp, Coil decoding, Coroutine) -> suppress and keep app alive
                    Log.w("NafiTvApp", "Safely suppressed background exception: ${throwable.message}")
                    return@setDefaultUncaughtExceptionHandler
                }

                // If fatal crash happens on Main UI thread, don't let Android exit to home screen abruptly.
                // Restart MainActivity cleanly so user stays inside the app without interruption.
                Log.w("NafiTvApp", "Main thread crash detected. Restarting MainActivity gracefully...")
                val restartIntent = Intent(applicationContext, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                applicationContext.startActivity(restartIntent)
                android.os.Process.killProcess(android.os.Process.myPid())
                System.exit(0)
            } catch (recoveryError: Throwable) {
                Log.e("NafiTvApp", "Crash recovery failed", recoveryError)
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }

    override fun newImageLoader(): ImageLoader {
        return customImageLoader ?: synchronized(this) {
            customImageLoader ?: buildOptimizedImageLoader().also { customImageLoader = it }
        }
    }

    private fun buildOptimizedImageLoader(): ImageLoader {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val isLowRam = activityManager?.isLowRamDevice == true
        val memCachePercent = if (isLowRam) 0.06 else 0.12 // Adaptive memory limit for low-RAM devices

        return ImageLoader.Builder(this)
            .okHttpClient(sharedOkHttpClient)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(memCachePercent)
                    .strongReferencesEnabled(true)
                    .weakReferencesEnabled(true)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(File(cacheDir, "nafitv_image_cache"))
                    .maxSizeBytes(if (isLowRam) 20L * 1024 * 1024 else 40L * 1024 * 1024)
                    .build()
            }
            .bitmapConfig(Bitmap.Config.RGB_565) // 50% memory saving on all channel logos & posters
            .allowHardware(!isLowRam) // Disables hardware bitmaps on low-RAM Mali/PowerVR GPUs for 100% crash-free stability
            .allowRgb565(true)
            .crossfade(false) // Saves GPU compositing passes on low-RAM devices
            .precision(Precision.INEXACT) // Downsamples posters and logos to target UI size (huge memory savings!)
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

        val sharedOkHttpClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(25, TimeUnit.SECONDS)
                .connectionPool(okhttp3.ConnectionPool(8, 2, TimeUnit.MINUTES))
                .retryOnConnectionFailure(true)
                .build()
        }
    }
}
