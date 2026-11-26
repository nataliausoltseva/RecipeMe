package com.example.recipe.helpers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri

fun getResizedBitmap(context: Context, uri: Uri, maxWidth: Int, maxHeight: Int): Bitmap? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeStream(inputStream, null, options)
        inputStream.close()

        options.inSampleSize = calculateInSampleSize(options, maxWidth, maxHeight)
        options.inJustDecodeBounds = false

        val resizedInputStream = context.contentResolver.openInputStream(uri) ?: return null
        val bitmap = BitmapFactory.decodeStream(resizedInputStream, null, options)
        resizedInputStream.close()

        if (bitmap != null) {
            val rotated = rotateBitmapIfRequired(context, uri, bitmap)
            scaleDownBitmapIfNeeded(rotated, maxWidth.coerceAtMost(maxHeight))
        } else {
            null
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun rotateBitmapIfRequired(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return bitmap
        val exifInterface = ExifInterface(inputStream)
        val orientation = exifInterface.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_UNDEFINED
        )
        inputStream.close()

        val degrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }

        // Safeguard against double rotation:
        // If EXIF indicates a 90° or 270° rotation, but the decoded bitmap is ALREADY taller than it is wide
        // (portrait), the Android framework/decoder already applied EXIF orientation during decoding.
        if (degrees == 90f || degrees == 270f) {
            if (bitmap.height > bitmap.width) {
                return bitmap // Already upright portrait, do not double-rotate!
            }
        }

        if (degrees != 0f) {
            val matrix = Matrix()
            matrix.postRotate(degrees)
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } else {
            bitmap
        }
    } catch (e: Exception) {
        e.printStackTrace()
        bitmap
    }
}

fun scaleDownBitmapIfNeeded(bitmap: Bitmap, maxDimension: Int = 1024): Bitmap {
    val width = bitmap.width
    val height = bitmap.height
    if (width <= maxDimension && height <= maxDimension) return bitmap

    val aspectRatio = width.toFloat() / height.toFloat()
    val (targetWidth, targetHeight) = if (width > height) {
        maxDimension to (maxDimension / aspectRatio).toInt()
    } else {
        (maxDimension * aspectRatio).toInt() to maxDimension
    }

    return Bitmap.createScaledBitmap(bitmap, targetWidth.coerceAtLeast(1), targetHeight.coerceAtLeast(1), true)
}

fun decodeByteArraySafely(bytes: ByteArray, maxDimension: Int = 1024): Bitmap? {
    return try {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)

        options.inSampleSize = calculateInSampleSize(options, maxDimension, maxDimension)
        options.inJustDecodeBounds = false

        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        bitmap?.let { scaleDownBitmapIfNeeded(it, maxDimension) }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
    val (height: Int, width: Int) = options.run { outHeight to outWidth }
    var inSampleSize = 1

    if (height > reqHeight || width > reqWidth) {
        val halfHeight: Int = height / 2
        val halfWidth: Int = width / 2

        while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
            inSampleSize *= 2
        }
    }

    return inSampleSize
}
