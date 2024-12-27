package com.littletrickster.scanner

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import org.opencv.core.Mat
import org.opencv.core.Point
import kotlin.math.max

@Composable
fun PointPreview(
    modifier: Modifier = Modifier,
    imageCaptureConfig: ImageCapture
) {
    var parentSize by remember { mutableStateOf(IntSize(1, 1)) }

    var imageWidth by remember { mutableStateOf(1) }
    var imageHeight by remember { mutableStateOf(1) }

    var points by remember { mutableStateOf(emptyList<Point>()) }

    val mScale = remember(parentSize, imageWidth, imageHeight) {
        max(
            parentSize.height.toFloat() / imageHeight.toFloat(),
            parentSize.width.toFloat() / imageWidth.toFloat()
        )
    }

    val scaledWidth = remember(mScale, imageWidth) { imageWidth * mScale }

    val scaledHeight = remember(mScale, imageHeight) { imageHeight * mScale }


    val verticalOffset =
        remember(scaledHeight, parentSize) { (parentSize.height - scaledHeight) / 2 }
    val horizontalOffset =
        remember(scaledWidth, parentSize) { (parentSize.width - scaledWidth) / 2 }



    Box(modifier = modifier
        .onSizeChanged { parentSize = it }) {

        val surfaceProvider = previewView(modifier = Modifier.fillMaxSize(),
            builder = {
                scaleType = PreviewView.ScaleType.FILL_CENTER
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            })

        ImageAnalyser(
            imageAnalysis = remember {
                ImageAnalysis.Builder().setResolutionSelector(defaultResolutionSelector())
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                    .build()
            },
            imageCapture = imageCaptureConfig,
            preview = remember {
                val preview: Preview = Preview.Builder()
                    .setResolutionSelector(defaultResolutionSelector())
                    .build()

                preview.setSurfaceProvider(surfaceProvider)
                preview
            },
            analyze = {
                val mat = it.yuvToMat()

                val resized = Mat()
                val scale = mat.resizeMax(resized, 300.0)
                mat.release()
                val foundPoints = getPoints(resized)
                foundPoints.rotate(
                    it.imageInfo.rotationDegrees,
                    Point(resized.width() / 2.0, resized.height() / 2.0)
                )

                resized.release()

                foundPoints *= scale


                imageWidth = it.rotatedWidth()
                imageHeight = it.rotatedHeight()
                points = foundPoints
            })

        DrawConnectingPath(
            points = points.map { it.toOffset() },
            horizontalOffset = horizontalOffset,
            verticalOffset = verticalOffset,
            scale = mScale
        )
    }
}

@Composable
fun DrawConnectingPath(
    points: List<Offset>,
    horizontalOffset: Float,
    verticalOffset: Float,
    scale: Float
) {
    val animatedHorizontalOffset by animateFloatAsState(targetValue = horizontalOffset)
    val animatedVerticalOffset by animateFloatAsState(targetValue = verticalOffset)

    val animatedPoints = points.map { point ->
        val animatedPoint by animateOffsetAsState(
            targetValue = point,
            animationSpec = offsetAnim
        )
        animatedPoint
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        if (animatedPoints.size >= 2) {
            for (i in 0 until animatedPoints.size - 1) {
                val start = Offset(
                    x = animatedHorizontalOffset + animatedPoints[i].x * scale,
                    y = animatedVerticalOffset + animatedPoints[i].y * scale
                )
                val end = Offset(
                    x = animatedHorizontalOffset + animatedPoints[i + 1].x * scale,
                    y = animatedVerticalOffset + animatedPoints[i + 1].y * scale
                )
                drawLine(
                    color = Color.Red,
                    start = start,
                    end = end,
                    strokeWidth = 5f
                )
            }

            val firstPoint = Offset(
                x = animatedHorizontalOffset + animatedPoints.first().x * scale,
                y = animatedVerticalOffset + animatedPoints.first().y * scale
            )
            val lastPoint = Offset(
                x = animatedHorizontalOffset + animatedPoints.last().x * scale,
                y = animatedVerticalOffset + animatedPoints.last().y * scale
            )
            drawLine(
                color = Color.Red,
                start = firstPoint,
                end = lastPoint,
                strokeWidth = 5f
            )
        }
    }
}
