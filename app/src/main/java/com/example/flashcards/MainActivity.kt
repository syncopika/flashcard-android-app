package com.example.flashcards

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import androidx.lifecycle.lifecycleScope
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
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
    return cards.filter {it ->
        //Log.i("INFO", searchText)
        if (searchText.trim() == "") {
            true
        } else if (searchType == "front") {
            (it as ChineseJSONObject).value.contains(searchText)
        } else if (searchType == "back") {
            (it as ChineseJSONObject).definition.contains(searchText) || it.pinyin.contains(searchText)
        } else if (searchType == "pinyin") {
            (it as ChineseJSONObject).pinyin.contains(searchText)
        } else if(searchType == "tag") {
            if ((it as ChineseJSONObject).tags != null) {
                it.tags.contains(searchText)
            } else {
                false
            }
        } else {
            true
        }
    }.toTypedArray()
}

fun filterCardsJapanese(cards: Array<Any>, searchType: String, searchText: String): Array<Any> {
    return cards.filter {
        if (searchText.trim() == "") {
            true
        } else if (searchType == "front") {
            (it as JapaneseJSONObject).value.contains(searchText)
        } else if (searchType == "back") {
            (it as JapaneseJSONObject).definition.contains(searchText) || it.romaji.contains(searchText)
        } else if (searchType == "romaji") {
            (it as JapaneseJSONObject).romaji.contains(searchText)
        } else {
            true
        }
    }.toTypedArray()
}

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // TODO: use a viewmodel for the data? - see https://developer.android.com/topic/libraries/architecture/viewmodel
        // https://stackoverflow.com/questions/44318859/fetching-a-url-in-android-kotlin-asynchronously

        val gson = Gson()

        var chineseJson = emptyArray<Any>() // will be fetched from latest version of JSON data via url

        val japaneseData: String = assets.open("japanese.json").bufferedReader().use { it.readText() }
        val japaneseJson = gson.fromJson(japaneseData, Array<JapaneseJSONObject>::class.java) as Array<Any>

        // the name of the local file to cache
        val localChineseDataFilename = "flashcards_chinese_data.json"

        // try fetching chinese data json via url and add to cache
        val hasInternet = this.isConnectedToInternet()
        if (hasInternet) {
            lifecycleScope.launch {
                Log.i("INFO", "attempting to get chinese data from url...")
                withContext(Dispatchers.IO) {
                    val json =
                        URL("https://raw.githubusercontent.com/syncopika/flashcards/refs/heads/main/public/datasets/chinese.json").readText()
                    chineseJson = gson.fromJson(json, Array<ChineseJSONObject>::class.java) as Array<Any>
                    Log.i("INFO", "got chinese data from url!")

                    // write data to cache
                    applicationContext.openFileOutput(localChineseDataFilename, Context.MODE_PRIVATE).use {
                        Log.i("INFO", "writing chinese data to cache...")
                        it.write(json.toByteArray())
                    }
                }
            }
        } else {
            // if that doesn't work, use the cache
            val file = File(applicationContext.filesDir, localChineseDataFilename)
            if (file.exists()) {
                Log.i("INFO", "no internet, pulling data from cache...")
                val chineseData: String = applicationContext.openFileInput(localChineseDataFilename).bufferedReader().use { it.readText() }
                chineseJson = gson.fromJson(chineseData, Array<ChineseJSONObject>::class.java) as Array<Any>
            } else {
                // no cached file, just use asset file (might be outdated though)
                Log.i("INFO", "no internet + no cached file, using data from /assets")
                val chineseData: String = assets.open("chinese.json").bufferedReader().use { it.readText() }
                chineseJson = gson.fromJson(chineseData, Array<ChineseJSONObject>::class.java) as Array<Any>
            }
        }

        setContent {
            FlashcardsTheme() {
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed) {
                    hideKeyboard()
                    true
                }

                val scope = rememberCoroutineScope()

                var searchText by remember { mutableStateOf("") }
                var searchOptions = remember { mutableStateOf(listOf("front", "back", "pinyin", "tag")) }
                val (selectedOption, onOptionSelected) = remember { mutableStateOf(searchOptions.value[0]) }

                // when the list of filtered cards changes, the view should be updated accordingly
                var filteredCards by remember { mutableStateOf(chineseJson.copyOf()) }
                var currIndex by remember { mutableStateOf(0) }

                var showDrawingCanvas by remember { mutableStateOf(false) }

                val snackbarHostState = remember { SnackbarHostState() }

                var languageSelectionDropdownExpanded by remember { mutableStateOf(false) }
                var currFlashcardLanguage by remember { mutableStateOf("chinese") }

                // pencil icon composable to add to the search bar to provide
                // an option of writing a character to search for
                var trailingIconView = @Composable{}

                // currently only providing drawing support for lookup for chinese
                // TODO: have it work for japanese as well?
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

                                // add this refresh button to specifically help actually show the card
                                // data for Chinese after it gets asynchronously loaded via url
                                //
                                // we should figure out how to do this properly at some point with a viewModel
                                // but my situation is made more complicated I think with having the network fetching + fallback
                                // capability using a local file/assets and I'm too lazy to try to mess with viewModels atm
                                // (they look kinda complicated/annoying to implement lol)
                                //
                                // in the meantime though, this works well enough
                                Button(onClick = {
                                    filteredCards = chineseJson.copyOf()
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            "refreshed data!",
                                            null,
                                            false,
                                            SnackbarDuration.Short
                                        )
                                    }
                                }) {
                                    Text("refresh")
                                }
                            }
                        },
                    ) {
                        Scaffold(
                            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
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

    // https://stackoverflow.com/questions/51141970/check-internet-connectivity-android-in-kotlin
     private fun isConnectedToInternet(): Boolean {
        val connManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (connManager != null) {
            val netCapabilities = connManager.getNetworkCapabilities(connManager.activeNetwork)
            if(netCapabilities != null){
                if(netCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)){
                    return true
                }else if(netCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)){
                    return true
                }else if(netCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)){
                    return true
                }
            }
        }
        return false
    }
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
            Flashcard(currIndex, currLang, cards)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Flashcard(currIndex: Int, language: String, jsonData: Array<Any>) {
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
                    var backText = ""

                    if (language == "chinese") {
                        var card = jsonData[currIndex] as ChineseJSONObject
                        val pinyin = card.pinyin
                        val definition = card.definition
                        backText = "pinyin: $pinyin\n\ndefinition: $definition"
                    } else {
                        var card = jsonData[currIndex] as JapaneseJSONObject
                        val romaji = card.romaji
                        val definition = card.definition
                        backText = "romaji: $romaji\n\ndefinition: $definition"
                    }

                    Text(
                        text = backText,
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
                    var word = ""

                    if (language == "chinese") {
                        var card = jsonData[currIndex] as ChineseJSONObject
                        word = card.value
                    } else {
                        var card = jsonData[currIndex] as JapaneseJSONObject
                        word = card.value
                    }

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