package com.example.flashcards.ui.theme

import android.util.Log
import android.view.MotionEvent
import android.view.ViewConfiguration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.mlkit.vision.digitalink.Ink
import kotlin.math.abs

// https://github.com/syncopika/digital-ink-recognition-android/blob/main/app/src/main/java/com/example/digital_ink_recognition_and_ocr/CanvasView.kt

// https://stackoverflow.com/questions/71090111/how-to-draw-on-jetpack-compose-canvas-using-touch-events
enum class MotionEvents { Idle, Up, Down, Move }

// maybe helpful?
// https://stackoverflow.com/questions/64571945/how-can-i-get-ontouchevent-in-jetpack-compose

class CanvasCoord(x: Float, y: Float){
    val x = x
    val y = y
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DrawingCanvas(inkBuilder: Ink.Builder, path: Path) {
    var motionEvent by remember { mutableStateOf(MotionEvents.Idle) }
    var currPosition by remember { mutableStateOf<CanvasCoord>(CanvasCoord(0f, 0f)) }
    var prevPosition by remember { mutableStateOf<CanvasCoord>(CanvasCoord(0f, 0f)) }
    val touchTolerance = ViewConfiguration.get(LocalContext.current).scaledTouchSlop
    //Log.i("INFO", "touch tolerance: $touchTolerance")

    var strokeBuilder = Ink.Stroke.builder()
    var currTime = 0L

    fun touchStart() {
        path.moveTo(currPosition.x, currPosition.y)
        strokeBuilder = Ink.Stroke.builder()
        strokeBuilder.addPoint(Ink.Point.create(currPosition.x, currPosition.y, currTime))
        prevPosition = currPosition
    }

    fun touchMove() {
        val dx = abs(currPosition.x - prevPosition.x)
        val dy = abs(currPosition.y - prevPosition.y)

        if(dx >= touchTolerance || dy >= touchTolerance) {
            path.quadraticBezierTo(
                prevPosition.x,
                prevPosition.y,
                (prevPosition.x + currPosition.x) / 2,
                (prevPosition.y + currPosition.y) / 2
            )

            strokeBuilder.addPoint(
                Ink.Point.create(currPosition.x, currPosition.y, currTime)
            )

            prevPosition = currPosition
        }
    }

    fun touchUp() {
        strokeBuilder.addPoint(
            Ink.Point.create(currPosition.x, currPosition.y, currTime)
        )
        inkBuilder.addStroke(strokeBuilder.build())
    }

    val drawModifier = Modifier
        .fillMaxWidth()
        .height(500.dp)
        .clipToBounds()
        .background(Color.White)
        .pointerInteropFilter { event ->
            currTime = System.currentTimeMillis()
            currPosition = CanvasCoord(event.x, event.y)
            Log.i("INFO", "curr pos: " + event.x.toString() + ", " + event.y.toString())

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    motionEvent = MotionEvents.Down
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    motionEvent = MotionEvents.Move
                    true
                }

                MotionEvent.ACTION_UP -> {
                    motionEvent = MotionEvents.Up
                    true
                }

                else -> false
            }
        }

    Canvas(modifier = drawModifier) {
        when (motionEvent) {
            MotionEvents.Down -> touchStart()
            MotionEvents.Move -> touchMove()
            MotionEvents.Up -> touchUp()
            else -> Unit
        }

        drawPath(
            color = Color.Black,
            path = path,
            style = Stroke(
                width = 4.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}