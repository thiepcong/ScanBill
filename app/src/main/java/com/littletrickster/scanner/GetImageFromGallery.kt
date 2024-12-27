package com.littletrickster.scanner

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.InputStream

@Preview
@Composable
fun GetImageFromGallery(
    textButton: String? = null,
    modifier: Modifier = Modifier,
    fileReceived: (file: Pair<Bitmap, Int>) -> Unit = {}
) {
    val context = LocalContext.current
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    // Launch an intent to get an image from the gallery
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            imageUri = uri
            uri?.let {
                val bitmap = uri.getBitmap(context.contentResolver)
                val rotation = getRotationFromUri(context, it)

                fileReceived(bitmap to rotation)
            }
        }

    Box(
        modifier = modifier
    ) {
        Button(
            onClick = { launcher.launch("image/*") },
            modifier = Modifier
                .align(Alignment.Center),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color.Black.copy(alpha = 0.2f),
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(24.dp)
        ) {
            Text(
                text = textButton ?: "Tải ảnh từ thư viện",
                fontWeight = FontWeight.W400,
                fontSize = 14.sp,
                color = Color.White,
            )
        }
    }
}

fun Uri.getBitmap(resolver: ContentResolver): Bitmap {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
        @Suppress("DEPRECATION")
        return MediaStore.Images.Media.getBitmap(resolver, this)
    } else {
        // https://developer.android.com/reference/android/graphics/ImageDecoder
        // CvException [org.opencv.core.CvException: OpenCV(4.1.1) /build/master_pack-android/opencv/modules/java/generator/src/cpp/utils.cpp:38: error: (-215:Assertion failed) AndroidBitmap_lockPixels(env, bitmap, &pixels) >= 0
        /*
          By default, a Bitmap created by ImageDecoder (including one that is inside a Drawable)
          will be immutable (i.e. Bitmap#isMutable returns false), and it will typically
          have Config Bitmap.Config#HARDWARE. Although these properties can be changed
          with setMutableRequired(true)
         */
        val source = ImageDecoder.createSource(resolver, this)
        return ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
            decoder.isMutableRequired = true
        }
    }
}

fun getRotationFromUri(context: android.content.Context, uri: Uri): Int {
    return try {
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        if (inputStream != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val exif = ExifInterface(inputStream)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        } else 0
    } catch (e: Exception) {
        e.printStackTrace()
        0
    }
}
