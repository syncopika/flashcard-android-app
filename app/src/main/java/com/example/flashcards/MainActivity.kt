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
import androidx.compose.material3.Button
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
import androidx.compose.ui.graphics.Path
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
import com.google.mlkit.common.MlKitException
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.vision.digitalink.DigitalInkRecognition
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModel
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModelIdentifier
import com.google.mlkit.vision.digitalink.DigitalInkRecognizer
import com.google.mlkit.vision.digitalink.DigitalInkRecognizerOptions
import com.google.mlkit.vision.digitalink.Ink
import com.google.mlkit.vision.digitalink.RecognitionResult
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
            it.value.contains(searchText)
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
                    DrawingCanvasDialog (
                        { showDrawingCanvas = false },
                        { searchTextVal: String ->
                            searchText = searchTextVal
                            filteredCards =
                                filterCards(json, selectedOption, searchText)
                            currIndex = 0
                        }
                    )
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
fun DrawingCanvasDialog(onDismissRequest: () -> Unit, onSubmitRequest: (newSearchTextVal: String) -> Unit) {

    fun doInkRecognition(model: DigitalInkRecognitionModel, inkData: Ink){
        //showSnackbar(R.string.processing_msg)

        val recognizer: DigitalInkRecognizer = DigitalInkRecognition.getClient(
            DigitalInkRecognizerOptions.builder(model).build()
        )

        recognizer.recognize(inkData)
            .addOnSuccessListener { result: RecognitionResult ->
                val res = result.candidates[0].text

                Log.i("INFO", res)

                onSubmitRequest(res)
                onDismissRequest()
            }
            .addOnFailureListener { e: Exception ->
                //showSnackbar(R.string.failure_msg_recognition)
                Log.e("ERROR", "Error during recognition: $e")
                onDismissRequest()
            }
    }

    Dialog(onDismissRequest = { onDismissRequest() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(600.dp)
                .padding(14.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            var inkBuilder by remember { mutableStateOf(Ink.Builder()) }
            val path = remember { Path() }

            DrawingCanvas(inkBuilder, path)

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Button(onClick = {
                    val inkData = inkBuilder.build()

                    var modelIdentifier: DigitalInkRecognitionModelIdentifier? = null
                    try {
                        // traditional chinese
                        modelIdentifier = DigitalInkRecognitionModelIdentifier.fromLanguageTag("zh-TW")

                        if (modelIdentifier != null) {
                            val model = DigitalInkRecognitionModel.builder(modelIdentifier).build()
                            val remoteModelManager = RemoteModelManager.getInstance()

                            //showSnackbar(R.string.check_download_language_msg)

                            remoteModelManager.isModelDownloaded(model).addOnSuccessListener { bool ->
                                when (bool) {
                                    true -> {
                                        //showSnackbar(R.string.downloaded_language_msg)
                                        doInkRecognition(model, inkData)
                                    }
                                    false -> {
                                        // download it
                                        //showSnackbar(R.string.download_language_msg)
                                        Log.i("INFO", "NEED TO DOWNLOAD MODEL")

                                        remoteModelManager.download(model, DownloadConditions.Builder().build())
                                            .addOnSuccessListener {
                                                Log.i("INFO", "Model downloaded")
                                                //showSnackbar(R.string.downloaded_language_msg)
                                                doInkRecognition(model, inkData)
                                            }
                                            .addOnFailureListener { e: Exception ->
                                                //showSnackbar(R.string.failure_msg_download)
                                                Log.e("ERROR", "Error while downloading a model: $e")
                                            }
                                    }
                                }
                            }
                        }else{
                            // no model was found, handle error.
                        }
                    } catch (e: MlKitException) {
                        // language tag failed to parse, handle error.
                    }


                }) {
                    Text(text = "submit")
                }
                Button(onClick = {
                    path.reset()
                    inkBuilder = Ink.Builder()
                }) {
                    Text(text = "clear")
                }
                Button(onClick = onDismissRequest) {
                    Text(text = "cancel")
                }
            }
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