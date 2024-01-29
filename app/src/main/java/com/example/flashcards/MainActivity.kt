package com.example.flashcards

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
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
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.window.Dialog
import com.example.flashcards.ui.theme.DrawingCanvas
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

// https://stackoverflow.com/questions/41790357/close-hide-the-android-soft-keyboard-with-kotlin
fun Activity.hideKeyboard() {
    hideKeyboard(currentFocus ?: View(this))
}

fun Context.hideKeyboard(view: View) {
    val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
}

fun filterCards(cards: Array<ChineseJSONObject>, searchType: String, searchText: String): Array<ChineseJSONObject> {
    return cards.filter {
        //Log.i("INFO", searchText)
        if (searchText.trim() == "") {
            true
        } else if (searchType == "front") {
            it.value == searchText
        } else if (searchType == "back") {
            it.definition.contains(searchText) || it.pinyin.contains(searchText)
        } else if (searchType == "pinyin") {
            it.pinyin.contains(searchText)
        } else {
            true
        }
    }.toTypedArray()
}

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val data: String = assets.open("chinese.json").bufferedReader().use { it.readText() }
        val gson = Gson()
        val json = gson.fromJson(data, Array<ChineseJSONObject>::class.java)

        //Log.i("INFO", json[0].value)
        //Log.i("INFO", data)

        setContent {
            FlashcardsTheme {
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed) {
                    hideKeyboard()
                    true
                }
                val scope = rememberCoroutineScope()

                var searchText by remember{ mutableStateOf("") }
                val searchOptions = listOf("front", "back", "pinyin")
                val (selectedOption, onOptionSelected) = remember{ mutableStateOf(searchOptions[0]) }

                // when the list of filtered cards changes, the view should be updated accordingly
                var filteredCards by remember{ mutableStateOf(json.copyOf()) }
                var currIndex by remember { mutableStateOf(0) }

                var showDrawingCanvas by remember{ mutableStateOf(false) }

                // pencil icon composable to add to the search bar to provide
                // an option of writing a character to search for
                val trailingIconView = @Composable {
                    IconButton(
                        onClick = {
                            //Log.i("INFO", "opening canvas")
                            showDrawingCanvas = true
                        },
                    ) {
                        Icon(
                            Icons.Default.Create,
                            contentDescription = ""
                        )
                    }
                }

                if (showDrawingCanvas) {
                    DrawingCanvasDialog { showDrawingCanvas = false }
                } else {
                    ModalNavigationDrawer(
                        drawerState = drawerState,
                        drawerContent = {
                            ModalDrawerSheet {
                                OutlinedTextField(
                                    value = searchText,
                                    onValueChange = {
                                        searchText = it

                                        filteredCards =
                                            filterCards(json, selectedOption, searchText)

                                        //Log.i("INFO", "filtered cards size: " + filteredCards.size)
                                        // always reset curr index to 0 when we get a new filtered list
                                        currIndex = 0
                                    },
                                    label = { Text("search") },
                                    trailingIcon = trailingIconView,
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
                                                    onClick = {
                                                        onOptionSelected(text)
                                                        filteredCards =
                                                            filterCards(json, text, searchText)
                                                        currIndex = 0
                                                    },
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
                                                        if (isClosed) {
                                                            open()
                                                        } else {
                                                            // TODO: can we get the keyboard to close if open?
                                                            close()
                                                        }
                                                    }
                                                }
                                            })
                                        )
                                    }
                                )
                            },
                            content = { innerPadding ->
                                MainContent(
                                    { currIndex },
                                    { idx -> currIndex = idx },
                                    filteredCards,
                                    innerPadding
                                )
                            }
                        )
                    }
                }
            } // end FlashcardsTheme
        }
    }
}

@Composable
fun DrawingCanvasDialog(onDismissRequest: () -> Unit) {
    Dialog(onDismissRequest = { onDismissRequest() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(500.dp)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            // TODO: add MLKit
            DrawingCanvas()
        }
    }
}


@Composable
fun MainContent(
    getCurrIndex: () -> Int,
    setCurrIndex: (index: Int) -> Unit,
    json: Array<ChineseJSONObject>,
    innerPadding: PaddingValues)
{
    var offsetX by remember { mutableStateOf(0f) }
    var currIndex = getCurrIndex()

    // A surface container using the 'background' color from the theme
    Surface(
        modifier = Modifier
            .padding(paddingValues = innerPadding)
            .fillMaxSize()
            // note we have to pass the card data json since a closure is created
            .pointerInput(json) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val (x, y) = dragAmount
                        offsetX += dragAmount.x
                    },
                    onDragEnd = {
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
                        setCurrIndex(currIndex)
                        offsetX = 0f
                    }
                )
            }
        ,
        color = MaterialTheme.colorScheme.background
    ) {
        if (json.isNotEmpty()) {
            ChineseFlashcard(currIndex, json)
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