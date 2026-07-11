package com.ilyne.helloszigetkmp.core.image

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.disk.DiskCache
import okio.FileSystem

expect fun initAppImageLoader()

fun createAppImageLoader(context: PlatformContext): ImageLoader =
    ImageLoader.Builder(context)
        .diskCache {
            DiskCache.Builder()
                .directory(FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "image_cache")
                .maxSizeBytes(100L * 1024 * 1024)
                .build()
        }
        .build()
