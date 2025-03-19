package com.example.recipe

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.recipe.data.RecipeViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.recipe.data.Ingredient
import com.example.recipe.data.Method
import com.example.recipe.data.Recipe
import com.example.recipe.helpers.RecipeRequest
import com.example.recipe.helpers.getResizedBitmap

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val recipesViewModel = remember { RecipeViewModel() }
            Main(
                recipesViewModel
            )
        }
    }
}

@Composable
fun Main(recipeViewModel: RecipeViewModel) {
    val recipesUIState by recipeViewModel.uiState.collectAsState()
    var search by remember { mutableStateOf("") }
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        if (recipesUIState.isFullScreen) {
            Button(
                onClick = { recipeViewModel.backToListView() },
            ) {
                Text("Back")
            }
            if (recipesUIState.selectedRecipe != null) {
                ViewRecipe(
                    recipesUIState.selectedRecipe!!
                )
            } else {
                CreateRecipe(
                    onSave = { recipeViewModel.saveRecipe(it) }
                )
            }
        } else {
            Row (
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SearchInput(
                    search = search,
                    onValueChange = { search = it }
                )
                Filter (
                    onToggle = { },
                    isOpen = true
                )
            }

            Button(
                onClick = { recipeViewModel.reloadRecipes() },
                modifier = Modifier
                    .align(Alignment.End)

            ) {
                Text("Reload")
            }
            ListOfRecipes(
                recipesUIState.recipes,
                onView = { recipeViewModel.viewRecipe(it) }
            )
            Add(
                onAdd = { recipeViewModel.createRecipe() }
            )
        }
    }
}

@Composable
fun SearchInput(
    search: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(value = search,
        onValueChange = onValueChange,
        leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "") },
        placeholder = { Text(text = "Search") },
        shape = RoundedCornerShape(50),
        modifier = Modifier
            .fillMaxWidth(0.85F)
    )
}

@Composable
fun Filter(
    onToggle: (Boolean) -> Unit,
    isOpen: Boolean
) {
    Icon(
        imageVector = Icons.Filled.Settings,
        contentDescription = "Settings Icon",
        modifier = Modifier.size(45.dp)
            .clickable { onToggle(!isOpen) }
    )
}

@Composable
fun Add(
    onAdd: () -> Unit
) {
    Box (
        modifier = Modifier
            .clip(CircleShape)
            .border(
                width = 1.dp,
                color = Color.Black,
                shape = RoundedCornerShape(50.dp)
            )
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = "Settings Icon",
            modifier = Modifier.size(45.dp)
                .clickable { onAdd() }
                .align(Alignment.BottomEnd)
        )
    }
}

@Composable
fun ListOfRecipes(
    recipes: List<Recipe>,
    onView: (recipe: Recipe) -> Unit
) {
    for (recipe in recipes) {
        Row {
            Column(
                modifier = Modifier
                    .border(BorderStroke(1.dp, Color.Black))
                    .padding(0.dp, 10.dp)
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(0.5f)
                        .clickable { onView(recipe) }
                ) {
                    if (recipe.imageUrl != "") {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(recipe.imageUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = recipe.name,
                            modifier = Modifier.height(150.dp)
                        )
                    }

                    Text(
                        text = recipe.name,
                    )

                    if (recipe.portion?.value != null) {
                        Text(
                            text = recipe.portion.value.toString() + " " + recipe.portion.measurement,
                        )
                    }

                    if (recipe.ingredients.isNotEmpty()) {
                        val ingredientsLabel = if (recipe.ingredients.size > 1) "ingredients" else "ingredient"
                        Text(
                            text = recipe.ingredients.size.toString() + " " + ingredientsLabel,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ViewRecipe(
    recipe: Recipe
) {
    Column {
        Text(
            text = recipe.name,
        )
    }
}

@Composable
fun CreateRecipe(
    onSave: (recipeRequest: RecipeRequest) -> Unit
) {
    var name by remember { mutableStateOf("") }
    val recipeRequest by remember {
        mutableStateOf(RecipeRequest(
            name = "",
            imageUrl = ""
        ))
    }

    // portion handlers
    var isExpandedPortionSelector by remember { mutableStateOf(false) }
    var portionSelection by remember { mutableStateOf("") }
    var portionValue by remember { mutableIntStateOf(1) }
    var placeholderPortionValue by remember { mutableStateOf("1") }

    // ingredients handlers
    var ingredients = remember { mutableStateListOf<Ingredient>() }
    var showIngredientModal by remember { mutableStateOf(false) }
    var selectedIngredient = remember { mutableStateOf<Ingredient?>(null) }

    // ingredients handlers
    var methods = remember { mutableStateListOf<Method>() }
    var showMethodModal by remember { mutableStateOf(false) }
    var selectedMethod = remember { mutableStateOf<Method?>(null) }

    Column {
        ImageUploader(
            onImageUpload = { recipeRequest.imageUrl = it.toString() }
        )
        TextField(
            value = name,
            onValueChange = {
                name = it
                recipeRequest.name = it
            },
            label = { Text("Name") },
        )
        Text("Portion")
        Row {
            TextField(
                value = placeholderPortionValue,
                onValueChange = {
                    if (it != "") {
                        portionValue = it.toInt()
                    }
                    placeholderPortionValue = it
                },
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            )

            DropdownMenuItem(
                text = { Text("Choose") },
                onClick = {
                    isExpandedPortionSelector = !isExpandedPortionSelector
                }
            )
            DropdownMenu(
                expanded = isExpandedPortionSelector,
                onDismissRequest = { isExpandedPortionSelector = !isExpandedPortionSelector }
            ) {
                for (portion in arrayOf("day", "portion")) {
                    DropdownMenuItem(
                        text = { Text(portion) },
                        onClick = {
                            portionSelection = portion
                            isExpandedPortionSelector = false
                        }
                    )
                }
            }
        }
        Row {
            Text("Ingredients")
            TextButton(
                onClick = {
                    showIngredientModal = true
                    selectedIngredient.value = null
                }
            ) {
                Text("+")
            }
        }
        for (ingredient in ingredients) {
            Row {
                Text(ingredient.name)
                Text(" - ")
                Text(ingredient.value.toString())
                Text(ingredient.measurement)
                TextButton(
                    onClick = {
                        showIngredientModal = true
                        selectedIngredient.value = ingredient
                    }
                ) { }
            }
        }

        if (showIngredientModal) {
            AddOrEditIngredient(
                ingredient = selectedIngredient.value,
                onConfirmation = {
                    ingredients.add(it)
                    showIngredientModal = false
                },
                onDismissRequest = { showIngredientModal = false },
                dialogTitle = if (selectedIngredient.value != null) "Edit ingredient" else "New ingredient"
            )
        }

        Row {
            Text("Methods")
            TextButton(
                onClick = {
                    showMethodModal = true
                    selectedMethod.value = null
                }
            ) {
                Text("+")
            }
        }

        for (method in methods) {
            val methodDivider = if (method.sortOrder != null) method.sortOrder.inc().toString() + ". " else "- "
            Row {
                Text(methodDivider + method.value)
                TextButton(
                    onClick = {
                        showMethodModal = true
                        selectedMethod.value = method
                    }
                ) { }
            }
        }

        if (showMethodModal) {
            AddOrEditMethodStep(
                method = selectedMethod.value,
                onConfirmation = {
                    methods.add(it)
                    showMethodModal = false
                },
                onDismissRequest = { showMethodModal = false },
                dialogTitle = if (selectedMethod.value != null) "Edit method step" else "New method step"
            )
        }

        Button(
            onClick = { onSave(recipeRequest) },
            modifier = Modifier
                .align(Alignment.End),
            enabled = recipeRequest.name !== "" || recipeRequest.imageUrl !== ""
        ) {
            Text("Save")
        }
    }
}

@Composable
fun ImageUploader(
    onImageUpload: (imageUri: Uri) -> Unit
) {
    val context = LocalContext.current as ComponentActivity
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var bitmapState by remember { mutableStateOf<Bitmap?>(null) }

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            uri?.let {
                imageUri = it
                val resizedBitmap = getResizedBitmap(context, it, 800, 800)
                bitmapState = resizedBitmap
                println(resizedBitmap)
                onImageUpload(it)
            }
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        bitmapState?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = "Selected image",
                modifier = Modifier.size(200.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                pickImageLauncher.launch("image/*")
            }
        ) {
            Text("Upload Image")
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
    var isExpandedIngredientPortionSelector by remember { mutableStateOf(false) }
    val newIngredient by remember { mutableStateOf(Ingredient(
        name = ingredient?.name ?: "",
        measurement = ingredient?.measurement ?: "",
        value = ingredient?.value ?: 1,
    )) }
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
                )
                Row {
                    Box (
                        Modifier.width(100.dp)
                    ) {
                        TextField(
                            value = newIngredient.value.toString(),
                            onValueChange = {
                                newIngredient.value = it.toFloat()
                            },
                            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                        )
                    }

                    DropdownMenuItem(
                        text = { Text(if (newIngredient.measurement !== "") newIngredient.measurement else "Choose") },
                        onClick = {
                            isExpandedIngredientPortionSelector = !isExpandedIngredientPortionSelector
                        }
                    )
                    DropdownMenu(
                        expanded = isExpandedIngredientPortionSelector,
                        onDismissRequest = { isExpandedIngredientPortionSelector = !isExpandedIngredientPortionSelector }
                    ) {
                        for (portion in arrayOf("bottle", "can", "item", "g", "kg", "mL", "L", "tbsp", "tsp", "cup")) {
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
    val newMethod by remember { mutableStateOf(Method(
        value = method?.value ?: "",
        id = 0,
        sortOrder = 0,
    )) }
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