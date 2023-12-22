package com.example.flashcards

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.example.flashcards.ui.theme.FlashcardsTheme
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

// {"value": "搖頭晃腦", "pinyin": "yao2 tou2 huang4 nao3", "definition": "to look pleased with one's self", "tags": ["idiom"]},
data class ChineseJSONObject(
    @SerializedName("value") val value: String,
    @SerializedName("pinyin") val pinyin: String,
    @SerializedName("definition") val definition: String,
    @SerializedName("tags") val tags: List<String>
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val data: String = assets.open("chinese.json").bufferedReader().use { it.readText() }
        val gson = Gson()
        val json = gson.fromJson(data, Array<ChineseJSONObject>::class.java)
        //Log.i("INFO", json[0].value)
        //Log.i("INFO", data)

        setContent {
            FlashcardsTheme {
                var offsetX by remember { mutableStateOf(0f) }
                var currIndex by remember { mutableStateOf(100) }

                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectDragGestures (
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val (x, y) = dragAmount
                                    offsetX += dragAmount.x
                                },
                                onDragEnd = {
                                    Log.i("INFO", "swipe done: " + offsetX)
                                    if(offsetX > 0){
                                        currIndex--
                                        if(currIndex < 0){
                                            currIndex = json.size - 1
                                        }
                                    }else{
                                        currIndex++
                                        if(currIndex > json.size - 1){
                                            currIndex = 0
                                        }
                                    }
                                    offsetX = 0f
                                }
                            )
                        }
                    ,
                    color = MaterialTheme.colorScheme.background
                ) {
                    ChineseFlashcard(currIndex, json)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChineseFlashcard(currIndex: Int, jsonData: Array<ChineseJSONObject>) {
    var rotated by remember { mutableStateOf(false) }

    val rotation by animateFloatAsState(
        targetValue = if (rotated) 180f else 0f,
        animationSpec = tween(500),
        label = "cardRotation"
    )

    //Log.i("INFO", "hey there")

    Box {
        Text(
            text = "flashcards",
            modifier = Modifier
                .align(Alignment.TopCenter),
            fontSize = 6.em,
            color = Color.White
        )
        Card(
            onClick = { rotated = !rotated },
            modifier = Modifier
                .fillMaxSize(0.7f)
                .align(Alignment.Center)
                .graphicsLayer {
                    rotationY = rotation
                    cameraDistance = 8 * density
                },
            colors = CardDefaults.cardColors(Color.White)
        ) {
            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (rotated) {
                    // back
                    val pinyin = jsonData[currIndex].pinyin
                    val definition = jsonData[currIndex].definition
                    Text(
                        text = "pinyin: $pinyin\n\ndefinition: $definition",
                        modifier = Modifier
                            .graphicsLayer {
                                rotationY = rotation
                            }
                            .align(Alignment.CenterHorizontally)
                            .padding(18.dp),
                        color = Color.Black
                    )
                } else {
                    // front
                    val word = jsonData[currIndex].value
                    Text(
                        text = "$word",
                        modifier = Modifier
                            .graphicsLayer {
                                rotationY = rotation
                            }
                            .align(Alignment.CenterHorizontally),
                        fontSize = 10.em,
                        color = Color.Black
                    )
                }
            }
        }
        // show card number at bottom-right of screen
        Text(
            text = "$currIndex/${jsonData.size}",
            modifier = Modifier
                .align(Alignment.BottomEnd),
            color = Color.White
        )
    }
}