package com.littletrickster.scanner

import android.app.Activity
import android.graphics.Bitmap
import android.widget.Toast
import androidx.camera.core.ImageCapture
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Preview
@Composable
fun TakeImage(
    fileReceived: (file: Pair<Bitmap, Int>) -> Unit = {},
    showImages: () -> Unit = {}
) {
    val context = LocalContext.current
    val prefs = rememberScannerSharedPrefs()

    var currentFlashMode by remember {
        mutableStateOf(prefs.getInt("flash_mode", ImageCapture.FLASH_MODE_AUTO))
    }

    var capturing by remember { mutableStateOf(false) }


    val imageCaptureConfig = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setResolutionSelector(defaultResolutionSelector())
            .build()
    }

    LaunchedEffect(null) {
        val flow = snapshotFlow { currentFlashMode }

        flow.drop(1).onEach {
            prefs
                .edit {
                    this.putInt("flash_mode", it)
                }
        }.launchIn(this)

        flow.onEach { imageCaptureConfig.flashMode = it }
            .launchIn(this)
    }

    val scope = rememberCoroutineScope()



    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
//        PointPreview(
//            modifier = Modifier
//                .fillMaxWidth()
//                .weight(1f),
//            imageCaptureConfig = imageCaptureConfig
//        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            // Camera preview
            PointPreview(
                modifier = Modifier
                    .fillMaxSize(),
                imageCaptureConfig = imageCaptureConfig
            )

            // Button to get image from gallery
            GetImageFromGallery(
                modifier = Modifier
                    .align(Alignment.BottomCenter) // Align at the bottom center
                    .padding(bottom = 13.dp), // Add some padding if needed
                fileReceived = fileReceived
            )

            IconButton(
                onClick = {
                    (context as Activity).finish()
                },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBackIosNew,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            Box(modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)) {
                TakenImagesSmallView(click = { showImages() })
            }

            // Flash toggle button
            IconButton(
                onClick = {
                    val nextMode = when (currentFlashMode) {
                        ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_AUTO
                        ImageCapture.FLASH_MODE_AUTO -> ImageCapture.FLASH_MODE_ON
                        else -> ImageCapture.FLASH_MODE_OFF
                    }
                    currentFlashMode = nextMode
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(24.dp)
            ) {
                Icon(
                    imageVector = when (currentFlashMode) {
                        ImageCapture.FLASH_MODE_OFF -> Icons.Default.FlashOff
                        ImageCapture.FLASH_MODE_AUTO -> Icons.Default.FlashAuto
                        ImageCapture.FLASH_MODE_ON -> Icons.Default.FlashOn
                        else -> Icons.Default.FlashOff
                    },
                    contentDescription = "Toggle Flash",
                    tint = Color.White
                )
            }
        }

//        GetImageFromGallery(
//            fileReceived = fileReceived
//        )
        BottomBar2(modifier = Modifier.fillMaxWidth(),
            capturing = capturing,
            captureClick = {
                if (capturing) return@BottomBar2
                capturing = true
                scope.launch(Dispatchers.IO) {
                    try {

//                                val file = imageCaptureConfig.takePicture(tempFolder, "temp-image.jpg")
                        val image = imageCaptureConfig.getImage()
                        val bitmap = image.captureBitmap()
                        val rotation = image.imageInfo.rotationDegrees
                        image.close()

                        fileReceived(bitmap to rotation)

                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    } finally {
                        capturing = false
                    }
                }

            }, currentFlashMode = currentFlashMode,
            modeClick = {
                val nextMode = when (currentFlashMode) {
                    ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_AUTO
                    ImageCapture.FLASH_MODE_AUTO -> ImageCapture.FLASH_MODE_ON
                    else -> ImageCapture.FLASH_MODE_OFF
                }
                currentFlashMode = nextMode
            },
            imageClick = { showImages() }
        )
    }

}

@Preview
@Composable
fun BottomBar2(
    modifier: Modifier = Modifier,
    capturing: Boolean = false,
    captureClick: () -> Unit = {},
    currentFlashMode: Int = ImageCapture.FLASH_MODE_AUTO,
    modeClick: () -> Unit = {},
    imageClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .height(100.dp)
            .then(modifier)
            .background(Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color(0xFF00BFFF), Color(0xFFFF0000))
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .clickable(
                        enabled = !capturing,
                        onClick = { captureClick() }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (capturing) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(24.dp),
                        color = Color.White,
                        strokeCap = StrokeCap.Round
                    )
                } else {
                    Text(
                        text = "Chụp bill",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.W600
                    )
                }
            }
        }
    }
}

@Composable
fun BottomBar(
    modifier: Modifier = Modifier,
    capturing: Boolean = false,
    captureClick: () -> Unit = {},
    currentFlashMode: Int = ImageCapture.FLASH_MODE_AUTO,
    modeClick: () -> Unit = {},
    imageClick: () -> Unit = {}
) {

    Box(
        modifier = Modifier
            .height(100.dp)
            .then(modifier)
    ) {

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(modifier = Modifier.size(50.dp)) {

            }
            Box(modifier = Modifier.size(50.dp)) {

            }

            CaptureButton(enabled = !capturing) {
                captureClick()
            }

            val flashAuto = rememberVectorPainter(Icons.Filled.FlashAuto)
            val flashOff = rememberVectorPainter(Icons.Filled.FlashOff)
            val flashOn = rememberVectorPainter(Icons.Filled.FlashOn)


            val currentPainter = when (currentFlashMode) {
                ImageCapture.FLASH_MODE_AUTO -> flashAuto
                ImageCapture.FLASH_MODE_OFF -> flashOff
                else -> flashOn
            }

            Image(painter = currentPainter, "flash",
                Modifier
                    .clickable {
                        modeClick()
                    }
                    .padding(15.dp),
                colorFilter = ColorFilter.tint(Color.White)
            )

            TakenImagesSmallView(click = { imageClick() })

        }

    }
}


val offsetAnim = tween<Offset>(durationMillis = 220, easing = LinearEasing)


fun defaultResolutionSelector() = ResolutionSelector.Builder().apply {
    setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
}.build()


@Preview
@Composable
fun CaptureButtonPreview(
) {
    var enabled by remember { mutableStateOf(true) }
    CaptureButton(enabled) { enabled = !enabled }
}


@Composable
fun CaptureButton(
    enabled: Boolean = true,
    click: () -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }

    val clicked by interactionSource.collectIsPressedAsState()

    val delta by animateDpAsState(
        targetValue = if (clicked) 20.dp else 0.dp,
        animationSpec = tween(durationMillis = 220, easing = LinearEasing)
    )
    val alpha by animateFloatAsState(
        targetValue = if (enabled) 1f else 0f,
        animationSpec = tween(durationMillis = 220, easing = LinearEasing)
    )



    Box(
        modifier = Modifier
            .clickable(
                enabled = enabled,
                onClick = click,
                interactionSource = interactionSource,
                indication = null
            )
            .padding(2.dp)
            .border(2.dp, Color.White, CircleShape) // inner border
            .padding(6.dp + delta / 2) // padding

    ) {

        if (!enabled) {
            CircularProgressIndicator(
                Modifier
                    .alpha(1f - alpha)
                    .size(50.dp - delta),
//                color = Color.Black,
                strokeCap = StrokeCap.Round
            )
        }

        Box(
            Modifier
                .alpha(alpha)
                .size(50.dp - delta)
                .background(Color.White, CircleShape)

        )


    }
}