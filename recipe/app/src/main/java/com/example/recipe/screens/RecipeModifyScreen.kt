package com.example.recipe.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.recipe.data.byteArrayToBitmap
import com.example.recipe.data.Image
import com.example.recipe.data.Ingredient
import com.example.recipe.data.Method
import com.example.recipe.data.Portion
import com.example.recipe.data.RecipeViewModel
import com.example.recipe.helpers.RecipeRequest
import com.example.recipe.helpers.getResizedBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import sh.calvin.reorderable.ReorderableColumn
import java.io.ByteArrayOutputStream
import kotlin.collections.orEmpty

@Composable
fun RecipeModifyScreen(
    recipeViewModel: RecipeViewModel,
) {
    val recipesUIState by recipeViewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        var recipe = recipesUIState.selectedRecipe
        var name by remember { mutableStateOf(recipe?.name ?: "") }
        val decodedBytes = Base64.decode(recipe?.image?.url ?: "", Base64.DEFAULT)
        var imageBytes by remember { mutableStateOf(if (recipe?.image?.url != null) decodedBytes else null) }
        var isExpandedTypeSelector by remember { mutableStateOf(false) }
        var typeSelection by remember { mutableStateOf((if(recipe?.type != "") recipe?.type else "Choose") ?: "Choose") }
        var recipeExternalUrl by remember { mutableStateOf(recipe?.url ?: "") }

        // portion handlers
        var isExpandedPortionSelector by remember { mutableStateOf(false) }
        var portionSelection by remember { mutableStateOf(recipe?.portion?.measurement ?: "day") }
        var portionValue by remember { mutableStateOf(recipe?.portion?.value ?: 1) }
        var placeholderPortionValue by remember {
            mutableStateOf(
                recipe?.portion?.value?.toString() ?: "1"
            )
        }

        // ingredients handlers
        var ingredients = remember { mutableStateOf<List<Ingredient>>(recipe?.ingredients ?: listOf()) }
        var showIngredientModal by remember { mutableStateOf(false) }
        val selectedIngredient = remember { mutableStateOf<Ingredient?>(null) }
        val selectedIngredientIndex = remember { mutableIntStateOf(0) }

        // methods handlers
        var methods = remember { mutableStateOf<List<Method>>(recipe?.methods ?: listOf()) }
        var showMethodModal by remember { mutableStateOf(false) }
        val selectedMethod = remember { mutableStateOf<Method?>(null) }
        val selectedMethodIndex = remember { mutableIntStateOf(0) }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Go back",
                modifier = Modifier
                    .clickable {
                        if (recipe != null) {
                            recipeViewModel.saveRecipe(
                                RecipeRequest(
                                    id = recipe.id,
                                    name = name,
                                    type = if (typeSelection === "Choose") "" else typeSelection,
                                    url = recipeExternalUrl
                                ),
                                Portion(
                                    id = recipe.portion?.id ?: 0,
                                    value = portionValue.toFloat(),
                                    measurement = if (portionSelection === "Choose") "days" else portionSelection
                                ),
                                ingredients.value,
                                methods.value,
                                imageBytes,
                            )
                        } else {
                            recipeViewModel.backToListView()
                        }
                    }
                    .size(50.dp, 50.dp)
            )
            if (recipe == null) {
                Button(
                    onClick = {
                        recipeViewModel.saveRecipe(
                            RecipeRequest(
                                id = recipe?.id ?: 0,
                                name = name,
                                type = if (typeSelection === "Choose") "" else typeSelection,
                                url = recipeExternalUrl
                            ),
                            Portion(
                                id = recipe?.portion?.id ?: 0,
                                value = portionValue.toFloat(),
                                measurement = if (portionSelection === "Choose") "days" else portionSelection
                            ),
                            ingredients.value,
                            methods.value,
                            imageBytes,
                        )
                    }
                ) {
                    Text("Save")
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(10.dp, 0.dp),
        ) {
            ImageUploader(
                onImageUpload = { imageBytes = it },
                image = recipe?.image
            )
            Row {
                TextField(
                    value = name,
                    onValueChange = {
                        name = it
                    },
                    label = { Text("Name") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                    ),
                    modifier = Modifier.fillMaxWidth(0.6F)
                )
                Column(
                    modifier = Modifier.padding(start = 20.dp)
                ) {
                    TextField(
                        value = placeholderPortionValue,
                        onValueChange = {
                            if (it.toFloatOrNull() != null) {
                                portionValue = it.toFloat()
                            }
                            placeholderPortionValue = it
                        },
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                        ),
                    )

                    DropdownMenuItem(
                        text = { Text(portionSelection) },
                        onClick = {
                            isExpandedPortionSelector = !isExpandedPortionSelector
                        }
                    )
                    DropdownMenu(
                        expanded = isExpandedPortionSelector,
                        onDismissRequest = { isExpandedPortionSelector = !isExpandedPortionSelector }
                    ) {
                        for (portion in arrayOf("day", "portion", "gram", "kg", "bar", "item", "mL", "L")) {
                            DropdownMenuItem(
                                modifier = Modifier
                                    .background(if (portion == portionSelection) Color.LightGray else Color.Transparent),
                                text = { Text(portion) },
                                onClick = {
                                    portionSelection = portion
                                    isExpandedPortionSelector = false
                                },
                            )
                        }
                    }
                }
            }

            TextField(
                label = { Text("Original recipe URL:") },
                value = recipeExternalUrl,
                onValueChange = {
                    recipeExternalUrl = it
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                ),
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Type: ")
                DropdownMenuItem(
                    text = { Text(typeSelection) },
                    onClick = {
                        isExpandedTypeSelector = !isExpandedTypeSelector
                    }
                )
                DropdownMenu(
                    expanded = isExpandedTypeSelector,
                    onDismissRequest = { isExpandedTypeSelector = !isExpandedTypeSelector }
                ) {
                    for (type in arrayOf("breakfast", "lunch", "dinner", "dessert", "snack")) {
                        DropdownMenuItem(
                            modifier = Modifier
                                .background(if (type == typeSelection) Color.LightGray else Color.Transparent),
                            text = { Text(type) },
                            onClick = {
                                typeSelection = type
                                isExpandedTypeSelector = false
                            },
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Ingredients")
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Add Icon",
                    modifier = Modifier
                        .padding(start = 20.dp)
                        .size(20.dp)
                        .clickable {
                            showIngredientModal = true
                            selectedIngredient.value = null
                        }
                )
            }
            ReorderableColumn(
                list = ingredients.value,
                onSettle = { fromIndex, toIndex ->
                    ingredients.value = ingredients.value.toMutableList().apply {
                        add(toIndex, removeAt(fromIndex))
                    }
                }
            ) { index, ingredient, isDragging ->
                key(index) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .draggableHandle(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.clickable {
                                showIngredientModal = true
                                selectedIngredient.value = ingredient
                                selectedIngredientIndex.intValue = index
                            }
                        ) {
                            Text(ingredient.name)
                            Text(" - ")
                            Text(ingredient.value.toString())
                            Text(ingredient.measurement)
                        }
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete ingredient button",
                            modifier = Modifier
                                .size(24.dp)
                                .clickable {
                                    ingredients.value = ingredients.value.toMutableList().apply {
                                        remove(ingredient)
                                    }
                                }
                        )
                    }
                }
            }

            if (showIngredientModal) {
                AddOrEditIngredient(
                    ingredient = selectedIngredient.value,
                    onConfirmation = {
                        if (selectedIngredient.value != null) {
                            ingredients.value = ingredients.value.toMutableList().apply {
                                add(selectedMethodIndex.intValue, it)
                            }
                        } else {
                            ingredients.value = ingredients.value.toMutableList().apply {
                                add(it)
                            }
                        }

                        selectedIngredient.value = null
                        showIngredientModal = false
                    },
                    onDismissRequest = { showIngredientModal = false },
                    dialogTitle = if (selectedIngredient.value != null) "Edit ingredient" else "New ingredient"
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 20.dp)
            ) {
                Text("Methods")
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Add Icon",
                    modifier = Modifier
                        .padding(start = 20.dp)
                        .size(20.dp)
                        .clickable {
                            showMethodModal = true
                            selectedMethod.value = null
                        }
                )
            }

            ReorderableColumn(
                list = methods.value,
                onSettle = { fromIndex, toIndex ->
                    methods.value = methods.value.toMutableList().apply {
                        add(toIndex, removeAt(fromIndex))
                    }
                }
            ) { index, method, isDragging ->
                key(method.id) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .draggableHandle(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val methodDivider = index.inc().toString() + ". "
                        Row(
                            modifier = Modifier.clickable {
                                showMethodModal = true
                                selectedMethod.value = method
                                selectedMethodIndex.intValue = index
                            }
                        ) {
                            Text(methodDivider + method.value)
                        }
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete method button",
                            modifier = Modifier
                                .size(24.dp)
                                .clickable {
                                    methods.value = methods.value.toMutableList().apply {
                                        remove(method)
                                    }
                                }
                        )
                    }
                }
            }

            if (showMethodModal) {
                AddOrEditMethodStep(
                    method = selectedMethod.value,
                    onConfirmation = {
                        if (selectedIngredient.value != null) {
                            methods.value = methods.value.toMutableList().apply {
                                add(selectedMethodIndex.intValue, it)
                            }
                        } else {
                            methods.value = methods.value.toMutableList().apply {
                                add(it)
                            }
                        }
                        selectedMethod.value = null
                        showMethodModal = false
                    },
                    onDismissRequest = { showMethodModal = false },
                    dialogTitle = if (selectedMethod.value != null) "Edit method step" else "New method step"
                )
            }
        }
    }
}

@Composable
fun ImageUploader(
    onImageUpload: (byteArray: ByteArray) -> Unit,
    image: Image?
) {
    val context = LocalContext.current
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var bitmapState by remember { mutableStateOf<Bitmap?>(null) }
    val scope = rememberCoroutineScope()

    val clipboardManager = LocalClipboardManager.current

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            uri?.let {
                imageUri = it
                scope.launch(Dispatchers.IO) {
                    val resizedBitmap = getResizedBitmap(context, it, 800, 800)
                    resizedBitmap?.let { bmp ->
                        val outputStream = ByteArrayOutputStream()
                        bmp.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                        withContext(Dispatchers.Main) {
                            bitmapState = bmp
                            onImageUpload(outputStream.toByteArray())
                        }
                    }
                }
            }
        }
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Display area for the image
        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            when {
                bitmapState != null -> {
                    Image(
                        bitmap = bitmapState!!.asImageBitmap(),
                        contentDescription = "Selected image",
                        modifier = Modifier.fillMaxSize()
                    )
                }
                image?.url != null -> {
                    val decodedBytes = Base64.decode(image.url, Base64.DEFAULT)
                    val bitmap = byteArrayToBitmap(decodedBytes)
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Existing image",
                        modifier = Modifier.fillMaxSize()
                    )
                }
                else -> {
                    Text("No image selected")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    pickImageLauncher.launch("image/*")
                }
            ) {
                Text("Upload Image")
            }

            Button(
                onClick = {
                    val clipData = clipboardManager.getClip()
                    if (clipData != null && clipData.clipData.itemCount > 0) {
                        val clipDataItem = clipData.clipData.getItemAt(0)
                        val uri = clipDataItem.uri
                        if (uri != null) {
                            try {
                                val inputStream = context.contentResolver.openInputStream(uri)
                                bitmapState = BitmapFactory.decodeStream(inputStream)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
            ) {
                Text("Paste Image")
            }
        }
    }
}


@Composable
fun AddOrEditIngredient(
    ingredient: Ingredient?,
    onDismissRequest: () -> Unit,
    onConfirmation: (ingredient: Ingredient) -> Unit,
    dialogTitle: String,
) {
    var name by remember { mutableStateOf(ingredient?.name ?: "") }
    var amount by remember { mutableStateOf(ingredient?.value?.toString() ?: "1") }
    var isExpandedIngredientPortionSelector by remember { mutableStateOf(false) }
    val newIngredient by remember {
        mutableStateOf(
            Ingredient(
                id = ingredient?.id ?: 0,
                name = ingredient?.name ?: "",
                measurement = ingredient?.measurement ?: "",
                value = ingredient?.value ?: 1,
                sortOrder = ingredient?.sortOrder ?: 1
            )
        )
    }

    AlertDialog(
        title = {
            Text(text = dialogTitle)
        },
        text = {
            Column {
                TextField(
                    value = name,
                    onValueChange = {
                        newIngredient.name = it
                        name = it
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                    ),
                )
                Row (
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Box(
                        Modifier.width(100.dp)
                    ) {
                        TextField(
                            value = amount,
                            onValueChange = {
                                amount = it
                            },
                            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                            ),
                        )
                    }

                    DropdownMenuItem(
                        text = { Text(if (newIngredient.measurement !== "") newIngredient.measurement else "Choose") },
                        onClick = {
                            isExpandedIngredientPortionSelector =
                                !isExpandedIngredientPortionSelector
                        }
                    )
                    DropdownMenu(
                        expanded = isExpandedIngredientPortionSelector,
                        onDismissRequest = {
                            isExpandedIngredientPortionSelector =
                                !isExpandedIngredientPortionSelector
                        }
                    ) {
                        for (portion in arrayOf(
                            "bottle",
                            "can",
                            "item",
                            "g",
                            "kg",
                            "mL",
                            "L",
                            "tbsp",
                            "tsp",
                            "cup"
                        )) {
                            DropdownMenuItem(
                                text = { Text(portion) },
                                onClick = {
                                    newIngredient.measurement = portion
                                    isExpandedIngredientPortionSelector = false
                                }
                            )
                        }
                    }
                }
            }
        },
        onDismissRequest = {
            onDismissRequest()
        },
        confirmButton = {
            TextButton(
                onClick = {
                    newIngredient.value = amount.toFloat()
                    onConfirmation(newIngredient)
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDismissRequest()
                }
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AddOrEditMethodStep(
    method: Method?,
    onDismissRequest: () -> Unit,
    onConfirmation: (method: Method) -> Unit,
    dialogTitle: String,
) {
    var value by remember { mutableStateOf(method?.value ?: "") }
    val newMethod by remember {
        mutableStateOf(
            Method(
                value = method?.value ?: "",
                id = 0,
                sortOrder = 0,
            )
        )
    }
    AlertDialog(
        title = {
            Text(text = dialogTitle)
        },
        text = {
            Column {
                TextField(
                    value = value,
                    onValueChange = {
                        newMethod.value = it
                        value = it
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                    ),
                )
            }
        },
        onDismissRequest = {
            onDismissRequest()
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirmation(newMethod)
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDismissRequest()
                }
            ) {
                Text("Cancel")
            }
        }
    )
}