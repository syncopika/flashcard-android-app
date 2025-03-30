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
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import kotlin.random.Random

// {"value": "搖頭晃腦", "pinyin": "yao2 tou2 huang4 nao3", "definition": "to look pleased with one's self", "tags": ["idiom"]},
data class ChineseJSONObject(
    @SerializedName("value") val value: String,
    @SerializedName("pinyin") val pinyin: String,
    @SerializedName("definition") val definition: String,
    @SerializedName("tags") val tags: List<String>
)

// {"value": "翻訳", "romaji": "honyaku (ほんやく)", "definition": "translate"},
data class JapaneseJSONObject(
    @SerializedName("value") val value: String,
    @SerializedName("romaji") val romaji: String,
    @SerializedName("definition") val definition: String
)

// https://stackoverflow.com/questions/41790357/close-hide-the-android-soft-keyboard-with-kotlin
fun Activity.hideKeyboard() {
    hideKeyboard(currentFocus ?: View(this))
}

fun Context.hideKeyboard(view: View) {
    val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
}

fun filterCardsChinese(cards: Array<Any>, searchType: String, searchText: String): Array<Any> {
    return cards.filter {
        //Log.i("INFO", searchText)
        if (searchText.trim() == "") {
            true
        } else if (searchType == "front") {
            (it as ChineseJSONObject).value.contains(searchText)
        } else if (searchType == "back") {
            (it as ChineseJSONObject).definition.contains(searchText) || (it as ChineseJSONObject).pinyin.contains(searchText)
        } else if (searchType == "pinyin") {
            (it as ChineseJSONObject).pinyin.contains(searchText)
        } else if(searchType == "tag") {
            if ((it as ChineseJSONObject).tags != null) {
                (it as ChineseJSONObject).tags.contains(searchText)
            } else {
                false
            }
        } else {
            true
        }
    } as Array<Any>//.toTypedArray()
}

fun filterCardsJapanese(cards: Array<Any>, searchType: String, searchText: String): Array<Any> {
    return cards.filter {
        if (searchText.trim() == "") {
            true
        } else if (searchType == "front") {
            (it as JapaneseJSONObject).value.contains(searchText)
        } else if (searchType == "back") {
            (it as JapaneseJSONObject).definition.contains(searchText) || (it as JapaneseJSONObject).romaji.contains(searchText)
        } else {
            true
        }
    } as Array<Any>//.toTypedArray()
}

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val chineseData: String = assets.open("chinese.json").bufferedReader().use { it.readText() }
        val japaneseData: String = assets.open("japanese.json").bufferedReader().use { it.readText() }

        val gson = Gson()

        val chineseJson = gson.fromJson(chineseData, Array<ChineseJSONObject>::class.java) as Array<Any>
        val japaneseJson = gson.fromJson(japaneseData, Array<JapaneseJSONObject>::class.java) as Array<Any>

        //Log.i("INFO", json[0].value)
        //Log.i("INFO", data)

        setContent {
            FlashcardsTheme {
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed) {
                    hideKeyboard()
                    true
                }

                val scope = rememberCoroutineScope()

                var searchText by remember { mutableStateOf("") }

                // TODO: this should change based on flashcard language
                var searchOptions = remember { mutableStateOf(listOf("front", "back", "pinyin", "tag")) }

                val (selectedOption, onOptionSelected) = remember { mutableStateOf(searchOptions.value[0]) }

                // when the list of filtered cards changes, the view should be updated accordingly
                var filteredCards by remember { mutableStateOf<Array<Any>>(chineseJson.copyOf() as Array<Any>) }
                var currIndex by remember { mutableStateOf(0) }

                var showDrawingCanvas by remember { mutableStateOf(false) }

                val snackbarHostState = remember { SnackbarHostState() }

                var languageSelectionDropdownExpanded by remember { mutableStateOf(false) }
                var currFlashcardLanguage by remember { mutableStateOf<String>("chinese") }

                // pencil icon composable to add to the search bar to provide
                // an option of writing a character to search for
                var trailingIconView = @Composable{}

                if (currFlashcardLanguage == "chinese") {
                    trailingIconView = @Composable {
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
                }

                if (showDrawingCanvas) {
                    DrawingCanvasDialog (
                        { showDrawingCanvas = false },
                        { searchTextVal: String ->
                            searchText = searchTextVal
                            filteredCards = filterCardsChinese(chineseJson, selectedOption, searchText)
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

                                        if (currFlashcardLanguage == "chinese") {
                                            filteredCards = filterCardsChinese(chineseJson, selectedOption, searchText)
                                        } else {
                                            filteredCards = filterCardsJapanese(japaneseJson, selectedOption, searchText)
                                        }

                                        //Log.i("INFO", "filtered cards size: " + filteredCards.size)
                                        // always reset curr index to 0 when we get a new filtered list
                                        currIndex = 0
                                    },
                                    label = { Text("search") },
                                    trailingIcon = trailingIconView,
                                )

                                // Note that Modifier.selectableGroup() is essential to ensure correct accessibility behavior
                                Column(Modifier.selectableGroup()) {
                                    searchOptions.value.forEach { text ->
                                        Row(
                                            Modifier
                                                .fillMaxWidth()
                                                .height(56.dp)
                                                .selectable(
                                                    selected = (text == selectedOption),
                                                    onClick = {
                                                        onOptionSelected(text)

                                                        if (currFlashcardLanguage == "chinese") {
                                                            filteredCards = filterCardsChinese(chineseJson, text, searchText)
                                                        } else {
                                                            filteredCards = filterCardsJapanese(japaneseJson, text, searchText)
                                                        }

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
                                
                                Button(onClick = {
                                    filteredCards.shuffle()
                                    currIndex = Random.nextInt(0, filteredCards.size)
                                    scope.launch {
                                        snackbarHostState.showSnackbar("shuffled!", null, false, SnackbarDuration.Short)
                                    }
                                }) {
                                    Text("shuffle")
                                }
                            }
                        },
                    ) {
                        Scaffold(
                            snackbarHost = { SnackbarHost(snackbarHostState) },
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
                                    },
                                    actions = {
                                        // show dropdown for flashcard language selection
                                        Box(
                                            modifier = Modifier
                                                .padding(16.dp)
                                        ) {
                                            IconButton(onClick = { languageSelectionDropdownExpanded = !languageSelectionDropdownExpanded }) {
                                                Icon(Icons.Default.MoreVert, contentDescription = "flashcard language options")
                                            }
                                            DropdownMenu(
                                                expanded = languageSelectionDropdownExpanded,
                                                onDismissRequest = { languageSelectionDropdownExpanded = false }
                                            ) {
                                                DropdownMenuItem(
                                                    text = { Text("chinese") },
                                                    onClick = {
                                                        currFlashcardLanguage = "chinese"
                                                        filteredCards = chineseJson.copyOf()
                                                        currIndex = 0
                                                        searchOptions.value = listOf("front", "back", "pinyin", "tag")
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("japanese") },
                                                    onClick = {
                                                        currFlashcardLanguage = "japanese"
                                                        filteredCards = japaneseJson.copyOf()
                                                        currIndex = 0
                                                        searchOptions.value = listOf("front", "back", "romaji")
                                                    }
                                                )
                                            }
                                        }
                                    }
                                )
                            },
                            content = { innerPadding ->
                                MainContent(
                                    { currFlashcardLanguage },
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
        } // end setContent
    } // end onCreate
}

@Composable
fun DrawingCanvasDialog(
    onDismissRequest: () -> Unit,
    onSubmitRequest: (newSearchTextVal: String) -> Unit
) {
    Dialog(onDismissRequest = { onDismissRequest() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(600.dp)
                .padding(14.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            var isProcessing by remember { mutableStateOf(false) }
            var inkBuilder by remember { mutableStateOf(Ink.Builder()) }
            val path = remember { Path() }

            fun doInkRecognition(model: DigitalInkRecognitionModel, inkData: Ink){
                val recognizer: DigitalInkRecognizer = DigitalInkRecognition.getClient(
                    DigitalInkRecognizerOptions.builder(model).build()
                )

                recognizer.recognize(inkData)
                    .addOnSuccessListener { result: RecognitionResult ->
                        val res = result.candidates[0].text
                        //Log.i("INFO", res)
                        onSubmitRequest(res)
                        isProcessing = false
                        onDismissRequest()
                    }
                    .addOnFailureListener { e: Exception ->
                        Log.e("ERROR", "Error during recognition: $e")
                        isProcessing = false
                        onDismissRequest()
                    }
            }

            if (isProcessing) {
                // https://stackoverflow.com/questions/75260214/how-to-show-semi-transparent-loading-overlay-above-full-composable
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Gray.copy(alpha = 0.6f))
                ){
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ){
                        CircularProgressIndicator(
                            modifier = Modifier.width(64.dp),
                            color = MaterialTheme.colorScheme.secondary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                    }
                }
            }

            DrawingCanvas(inkBuilder, path)

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Button(onClick = {
                    isProcessing = true

                    val inkData = inkBuilder.build()

                    var modelIdentifier: DigitalInkRecognitionModelIdentifier? = null
                    try {
                        // traditional chinese
                        modelIdentifier = DigitalInkRecognitionModelIdentifier.fromLanguageTag("zh-TW")

                        if (modelIdentifier != null) {
                            val model = DigitalInkRecognitionModel.builder(modelIdentifier).build()
                            val remoteModelManager = RemoteModelManager.getInstance()

                            remoteModelManager.isModelDownloaded(model).addOnSuccessListener { bool ->
                                when (bool) {
                                    true -> {
                                        doInkRecognition(model, inkData)
                                    }
                                    false -> {
                                        // download it
                                        Log.i("INFO", "NEED TO DOWNLOAD MODEL")

                                        remoteModelManager.download(model, DownloadConditions.Builder().build())
                                            .addOnSuccessListener {
                                                Log.i("INFO", "Model downloaded")
                                                doInkRecognition(model, inkData)
                                            }
                                            .addOnFailureListener { e: Exception ->
                                                Log.e("ERROR", "Error while downloading a model: $e")
                                                isProcessing = false
                                            }
                                    }
                                }
                            }
                        }else{
                            // no model was found, handle error.
                            isProcessing = false
                        }
                    } catch (e: MlKitException) {
                        // language tag failed to parse, handle error.
                        isProcessing = false
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
    getCurrFlashcardLanguage: () -> String,
    getCurrIndex: () -> Int,
    setCurrIndex: (index: Int) -> Unit,
    cards: Array<Any>,
    innerPadding: PaddingValues)
{
    var offsetX by remember { mutableStateOf(0f) }
    var currIndex = getCurrIndex()
    var currLang = getCurrFlashcardLanguage()

    // A surface container using the 'background' color from the theme
    Surface(
        modifier = Modifier
            .padding(paddingValues = innerPadding)
            .fillMaxSize()
            // note we have to pass the most current card data json + current index since a closure is created
            // otherwise detectDragGestures will just use the initial values
            .pointerInput(cards, currIndex) {
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
                                currIndex = cards.size - 1
                            }
                        } else {
                            currIndex++
                            if (currIndex > cards.size - 1) {
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
        if (cards.isNotEmpty()) {
            if (currLang == "chinese") {
                ChineseFlashcard(currIndex, cards as Array<ChineseJSONObject>)
            } else {
                JapaneseFlashcard(currIndex, cards as Array<JapaneseJSONObject>)
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
            text = "${currIndex+1}/${jsonData.size}",
            modifier = Modifier
                .align(Alignment.BottomEnd),
            color = Color.White
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JapaneseFlashcard(currIndex: Int, jsonData: Array<JapaneseJSONObject>) {
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
                    val pinyin = jsonData[currIndex].romaji
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
            text = "${currIndex+1}/${jsonData.size}",
            modifier = Modifier
                .align(Alignment.BottomEnd),
            color = Color.White
        )
    }
}