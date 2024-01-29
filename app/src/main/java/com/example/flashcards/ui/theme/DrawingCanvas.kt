package com.example.flashcards.ui.theme

import android.util.Log
import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.unit.dp
import com.google.mlkit.vision.digitalink.Ink

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
fun DrawingCanvas(inkBuilder: Ink.Builder) {
    var motionEvent by remember { mutableStateOf(MotionEvents.Idle) }
    var currPosition by remember { mutableStateOf<CanvasCoord?>(null) }
    var prevPosition by remember { mutableStateOf<CanvasCoord?>(null) }

    val path = remember { Path() }

    //val inkBuilder = Ink.Builder()
    var strokeBuilder = Ink.Stroke.builder()
    var currTime = 0L

    val drawModifier = Modifier
        .fillMaxWidth()
        //.fillMaxHeight()
        .height(400.dp)
        .clipToBounds()
        .background(Color.White)
        .pointerInteropFilter { event ->

            currTime = System.currentTimeMillis()

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    motionEvent = MotionEvents.Down
                    //Log.i("INFO", "motion event down" + "x: " + event.x + ", y: " + event.y)
                    prevPosition = CanvasCoord(event.x, event.y)
                    currPosition = CanvasCoord(event.x, event.y)
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    // TODO: move actions seem to get registered too easily
                    // maybe check distance between this pos and prev pos
                    // and have some threshold to determine if it really is a move
                    motionEvent = MotionEvents.Move
                    //Log.i("INFO", "motion event move")
                    prevPosition = currPosition
                    currPosition = CanvasCoord(event.x, event.y)
                    true
                }

                MotionEvent.ACTION_UP -> {
                    motionEvent = MotionEvents.Up
                    //Log.i("INFO", "motion event up")
                    currPosition = CanvasCoord(event.x, event.y)
                    true
                }

                else -> false
            }
        }

    Canvas(modifier = drawModifier) {
        when (motionEvent) {
            MotionEvents.Down -> {
                if (currPosition != null) {
                    path.moveTo(currPosition!!.x, currPosition!!.y)

                    strokeBuilder = Ink.Stroke.builder()
                    strokeBuilder.addPoint(
                        Ink.Point.create(currPosition!!.x, currPosition!!.y, currTime)
                    )
                }
            }

            MotionEvents.Move -> {
                if (currPosition != null && prevPosition != null) {
                    path.quadraticBezierTo(
                        prevPosition!!.x,
                        prevPosition!!.y,
                        (prevPosition!!.x + currPosition!!.x) / 2,
                        (prevPosition!!.y + currPosition!!.y) / 2
                    )

                    strokeBuilder.addPoint(
                        Ink.Point.create(currPosition!!.x, currPosition!!.y, currTime)
                    )
                }
            }

            MotionEvents.Up -> {
                if (currPosition != null) {
                    path.lineTo(currPosition!!.x, currPosition!!.y)

                    strokeBuilder.addPoint(
                        Ink.Point.create(currPosition!!.x, currPosition!!.y, currTime)
                    )

                    prevPosition = null
                    currPosition = null
                }

                inkBuilder.addStroke(strokeBuilder.build())
            }

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