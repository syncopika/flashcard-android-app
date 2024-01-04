package com.example.flashcards

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.example.flashcards.ui.theme.FlashcardsTheme
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.launch

// {"value": "搖頭晃腦", "pinyin": "yao2 tou2 huang4 nao3", "definition": "to look pleased with one's self", "tags": ["idiom"]},
data class ChineseJSONObject(
    @SerializedName("value") val value: String,
    @SerializedName("pinyin") val pinyin: String,
    @SerializedName("definition") val definition: String,
    @SerializedName("tags") val tags: List<String>
)

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val data: String = assets.open("chinese.json").bufferedReader().use { it.readText() }
        val gson = Gson()
        val json = gson.fromJson(data, Array<ChineseJSONObject>::class.java)

        val filteredCards = json.copyOf()
        //Log.i("INFO", json[0].value)
        //Log.i("INFO", data)

        setContent {
            FlashcardsTheme {
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()

                var searchText by remember{ mutableStateOf("") }

                val searchOptions = listOf("front", "back", "pinyin")
                val (selectedOption, onOptionSelected) = remember{ mutableStateOf(searchOptions[0]) }

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        ModalDrawerSheet {
                            OutlinedTextField(
                                value=searchText,
                                onValueChange={ searchText = it /* TODO: depending on selectedOption, filter the flashcards */},
                                label={Text("search")}
                            )

                            // Note that Modifier.selectableGroup() is essential to ensure correct accessibility behavior
                            Column(Modifier.selectableGroup()) {
                                searchOptions.forEach { text ->
                                    Row(
                                        Modifier
                                            .fillMaxWidth()
                                            .height(56.dp)
                                            .selectable(
                                                selected = (text == selectedOption),
                                                onClick = { onOptionSelected(text) },
                                                role = Role.RadioButton
                                            )
                                            .padding(horizontal = 16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = (text == selectedOption),
                                            onClick = null // null recommended for accessibility with screenreaders
                                        )
                                        Text(
                                            text = text,
                                            style = MaterialTheme.typography.bodyLarge,
                                            modifier = Modifier.padding(start = 16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    },
                ) {
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                colors = topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    titleContentColor = MaterialTheme.colorScheme.primary,
                                ),
                                title = {
                                    Text(
                                        text = "flashcards",
                                        fontSize = 6.em,
                                        color = Color.White,
                                        modifier = Modifier.padding(4.dp, 0.dp, 0.dp, 0.dp)
                                    )
                                },
                                navigationIcon = {
                                    Icon(
                                        Icons.Default.Menu,
                                        "",
                                        modifier = Modifier.clickable(onClick = {
                                            scope.launch {
                                                drawerState.apply {
                                                    // TODO: also close keyboard if open
                                                    if (isClosed) open() else close()
                                                }
                                            }
                                        })
                                    )
                                }
                            )
                        }
                    ) { innerPadding ->
                        MainContent(filteredCards, innerPadding)
                    }
                }

            }
        }
    }
}

@Composable
fun MainContent(json: Array<ChineseJSONObject>, innerPadding: PaddingValues){
    var offsetX by remember { mutableStateOf(0f) }
    var currIndex by remember { mutableStateOf(100) }
    // A surface container using the 'background' color from the theme
    Surface(
        modifier = Modifier
            .padding(paddingValues = innerPadding)
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val (x, y) = dragAmount
                        offsetX += dragAmount.x
                    },
                    onDragEnd = {
                        Log.i("INFO", "swipe done: " + offsetX)
                        if (offsetX > 0) {
                            currIndex--
                            if (currIndex < 0) {
                                currIndex = json.size - 1
                            }
                        } else {
                            currIndex++
                            if (currIndex > json.size - 1) {
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