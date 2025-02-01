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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
            CustomSearchView(
                search = search,
                onValueChange = { search = it }
            )

            Button(
                onClick = { recipeViewModel.reloadRecipes() },
                modifier = Modifier
                    .align(Alignment.End)

            ) {
                Text("Reload")
            }
            Button(
                onClick = { recipeViewModel.createRecipe() },
                modifier = Modifier
                    .align(Alignment.End),
            ) {
                Text("New")
            }
            ListOfRecipes(
                recipesUIState.recipes,
                onView = { recipeViewModel.viewRecipe(it) }
            )
        }
    }
}

@Composable
fun CustomSearchView(
    search: String,
    onValueChange: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .padding(20.dp)
            .clip(CircleShape)
            .background(Color(0XFF101921))

    ) {
        TextField(value = search,
            onValueChange = onValueChange,
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "") },
            placeholder = { Text(text = "Search") },
        )
    }
}

@Composable
fun ListOfRecipes(
    recipes: List<Recipe>,
    onView: (recipe: Recipe) -> Unit
) {
    val context = LocalContext.current as ComponentActivity
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

    // ingredients handlers
    var ingredients = remember { mutableStateListOf<Ingredient>() }

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
                value = portionValue.toString(),
                onValueChange = {
                    portionValue = it.toInt()
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
        Text("Ingredients")
        for (ingredient in ingredients) {
            Ingredient(
                ingredient,
                onAdd = { }
            )
        }
        Ingredient(
            ingredient = null,
            onAdd = { ingredients.add(it) }
        )

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
fun Ingredient(
    ingredient: Ingredient?,
    onAdd: (ingredient: Ingredient) -> Unit
) {
    var isExpandedIngredientPortionSelector by remember { mutableStateOf(false) }
    val newIngredient by remember { mutableStateOf(Ingredient(
        name = ingredient?.name ?: "",
        measurement = ingredient?.measurement ?: "",
        value = ingredient?.value ?: 1,
    )) }
    TextField(
        value = newIngredient.name,
        onValueChange = {
            newIngredient.name = it
        },
    )
    Row {
        TextField(
            value = newIngredient.value.toString(),
            onValueChange = {
                newIngredient.value = it.toFloat()
            },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
        )

        DropdownMenuItem(
            text = { Text("Choose") },
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
    Button(
        onClick = {
            onAdd(newIngredient)
        },
        enabled = newIngredient.name !== ""
    ) {
        Text("Add ingredient")
    }}