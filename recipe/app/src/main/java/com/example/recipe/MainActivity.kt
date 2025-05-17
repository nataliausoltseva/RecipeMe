package com.example.recipe

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.recipe.data.RecipeViewModel
import com.example.recipe.data.Ingredient
import com.example.recipe.data.Method
import com.example.recipe.data.Recipe
import com.example.recipe.helpers.RecipeRequest
import com.example.recipe.helpers.getResizedBitmap
import sh.calvin.reorderable.ReorderableColumn
import sh.calvin.reorderable.ReorderableItem
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import com.example.recipe.data.Image
import com.example.recipe.data.Portion
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.collections.orEmpty
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.view.HapticFeedbackConstantsCompat
import androidx.core.view.ViewCompat
import sh.calvin.reorderable.rememberReorderableLazyStaggeredGridState

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
    var sortKeyState by remember { mutableStateOf(recipesUIState.selectedSortKey) }
    var sortDirectionKeyState by remember { mutableStateOf(recipesUIState.selectedSortDirection) }
    var showFilterDialog = remember { mutableStateOf(false)}
    var recipes = remember { mutableStateOf(recipesUIState.recipes) }

    Column(
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        if (recipesUIState.isFullScreen) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Go back",
                modifier = Modifier
                    .clickable { recipeViewModel.backToListView() }
                    .size(50.dp, 50.dp)
            )
            if (recipesUIState.selectedRecipe != null) {
                if (recipesUIState.isEditingRecipe) {
                    CreateOrEditRecipe(
                        onSave = { recipe, portion, ingredients, methods, imageBytes ->
                            recipeViewModel.saveRecipe(
                                recipe,
                                portion,
                                ingredients,
                                methods,
                                imageBytes
                            )
                        },
                        recipe = recipesUIState.selectedRecipe
                    )
                } else {
                    ViewRecipe(
                        recipesUIState.selectedRecipe!!
                    )
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Edit button",
                        modifier = Modifier
                            .clickable { recipeViewModel.onEditRecipe() }
                            .size(50.dp, 50.dp)
                    )
                }
            } else {
                CreateOrEditRecipe(
                    onSave = { recipe, portion, ingredients, methods, imageBytes ->
                        recipeViewModel.saveRecipe(
                            recipe,
                            portion,
                            ingredients,
                            methods,
                            imageBytes
                        )
                    }
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SearchInput(
                    search = search,
                    onValueChange = {
                        search = it
                        recipeViewModel.onSearch(it)
                    }
                )
                Filter(
                    onToggle = { showFilterDialog.value = !showFilterDialog.value },
                )
            }

            Button(
                onClick = { recipeViewModel.reloadRecipes() },
                modifier = Modifier
                    .align(Alignment.End)

            ) {
                Text("Reload")
            }
            Box(
                modifier = Modifier.fillMaxSize(),
            ) {
                var list by remember { mutableStateOf(recipes) }

                ListOfRecipes(
                    recipes = recipesUIState.recipes,
                    onView = { recipeViewModel.viewRecipe(it) },
                    onReorder = {
                        recipeViewModel.onReorder()
                        list.value = it
                    },
                    shouldReset = recipesUIState.shouldReset,
                    onSaveReset = { recipeViewModel.onSaveReset() }
                )
                FloatingActionButton(
                    onClick = { recipeViewModel.createRecipe() },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                ) {
                    Add()
                }
                if (recipesUIState.isReordered) {
                    FloatingActionButton(
                        onClick = { recipeViewModel.onReset() },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp, 16.dp, 80.dp, 16.dp)
                    ) {
                        Revert()
                    }
                    FloatingActionButton(
                        onClick = { recipeViewModel.onSaveReorder(list.value) },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp, 16.dp, 145.dp, 16.dp)
                    ) {
                        Save()
                    }
                }
            }
        }

        if (showFilterDialog.value) {
            FilterAndSortDialog(
                selectedIngredientNames=recipesUIState.selectedIngredientNames,
                selectedSortKey = sortKeyState,
                selectedSortDirection = sortDirectionKeyState,
                availableIngredientNames = recipesUIState.availableIngredients,
                onApply = { selectedNames, sortKey, sortDirection ->
                    recipeViewModel.onFilterOrSort(selectedNames, sortKey, sortDirection)
                    showFilterDialog.value = false
                    sortKeyState = sortKey
                    sortDirectionKeyState = sortDirection
                },
                onClose = { showFilterDialog.value = false}
            )
        }
    }
}

@Composable
fun SearchInput(
    search: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = search,
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
    onToggle: () -> Unit
) {
    Icon(
        imageVector = Icons.Filled.Settings,
        contentDescription = "Settings Icon",
        modifier = Modifier
            .size(45.dp)
            .clickable { onToggle() }
    )
}

@Composable
fun Add() {
    Box(
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
            contentDescription = "Add Icon",
            modifier = Modifier
                .size(45.dp)
        )
    }
}

@Composable
fun Revert() {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .border(
                width = 1.dp,
                color = Color.Black,
                shape = RoundedCornerShape(50.dp)
            )
    ) {
        Icon(
            imageVector = Icons.Filled.Refresh,
            contentDescription = "Revert Icon",
            modifier = Modifier
                .size(45.dp)
        )
    }
}


@Composable
fun Save() {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .border(
                width = 1.dp,
                color = Color.Black,
                shape = RoundedCornerShape(50.dp)
            )
    ) {
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = "Revert Icon",
            modifier = Modifier
                .size(45.dp)
        )
    }
}

@Composable
fun ListOfRecipes(
    recipes: List<Recipe>,
    onView: (recipe: Recipe) -> Unit,
    onReorder: (List<Recipe>) -> Unit,
    shouldReset: Boolean,
    onSaveReset: () -> Unit,
) {
    val view = LocalView.current

    var list by remember { mutableStateOf(recipes) }

    LaunchedEffect(recipes, shouldReset) {
        if (list !== recipes || shouldReset) {
            list = recipes

            if (shouldReset) {
                onSaveReset()
            }
        }
    }

    val lazyStaggeredGridState = rememberLazyStaggeredGridState()
    val reorderableLazyStaggeredGridState = rememberReorderableLazyStaggeredGridState(lazyStaggeredGridState) { from, to ->
        list = list.toMutableList().apply {
            add(to.index, removeAt(from.index))
        }
        onReorder(list)
    }

    LazyVerticalStaggeredGrid(
        state = lazyStaggeredGridState,
        columns = StaggeredGridCells.Adaptive(200.dp),
        verticalItemSpacing = 4.dp,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        content = {
            items(list, key = { it.id.toString() }) { recipe ->
                ReorderableItem(reorderableLazyStaggeredGridState, key = recipe.id.toString()) { isDragging ->
                    val elevation by animateDpAsState(if (isDragging) 4.dp else 0.dp)
                    Surface (shadowElevation = elevation) {
                        Card(
                            modifier = Modifier.padding(bottom = 10.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(0.dp, 10.dp)
                            ) {
                                Box(
                                    Modifier
                                        .fillMaxWidth(0.5f)
                                        .draggableHandle(
                                            onDragStarted = {
                                                ViewCompat.performHapticFeedback(
                                                    view,
                                                    HapticFeedbackConstantsCompat.GESTURE_START
                                                )
                                            },
                                            onDragStopped = {
                                                ViewCompat.performHapticFeedback(
                                                    view,
                                                    HapticFeedbackConstantsCompat.GESTURE_END
                                                )
                                            },
                                        )
                                        .clickable { onView(recipe) }
                                ) {
                                    Column {
                                        if (recipe.image?.url != null) {
                                            val decodedBytes = Base64.decode(recipe.image.url, Base64.DEFAULT)
                                            if (decodedBytes != null) {
                                                val bitmap = byteArrayToBitmap(decodedBytes)
                                                val imageBitmap = bitmap.asImageBitmap()
                                                Image(
                                                    bitmap = imageBitmap,
                                                    contentDescription = recipe.name + " image",
                                                    modifier = Modifier
                                                        .size(200.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                )
                                            }
                                        }

                                        Text(
                                            text = recipe.name,
                                        )
                                        Text(
                                            text = "SortOrder: " + recipe.sortOrder.toString(),
                                        )

                                        if (recipe.portion != null) {
                                            Text(
                                                text = recipe.portion.value.toString() + " " + recipe.portion.measurement,
                                            )
                                        }

                                        Text(
                                            text = "Ingredients:",
                                        )

                                        if (recipe.ingredients != null) {
                                            for (ingredient in recipe.ingredients) {
                                                Text(
                                                    text = ingredient.name + " " + ingredient.value + " " + ingredient.measurement,
                                                )
                                            }
                                        }

                                        Text(
                                            text = "Methods:",
                                        )

                                        if (recipe.methods != null) {
                                            for (method in recipe.methods) {
                                                val indicator =
                                                    if (method.sortOrder != null) (method.sortOrder + 1) else 1;
                                                Text(
                                                    text = indicator.toString() + ". " + method.value,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun ViewRecipe(
    recipe: Recipe
) {
    Column {
        if (recipe.image?.url != null) {
            val decodedBytes = Base64.decode(recipe.image.url, Base64.DEFAULT)
            if (decodedBytes != null) {
                val bitmap = byteArrayToBitmap(decodedBytes)
                val imageBitmap = bitmap.asImageBitmap()
                Image(
                    bitmap = imageBitmap,
                    contentDescription = recipe.name + " image",
                    modifier = Modifier
                        .size(200.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            }
        }

        Text(
            text = recipe.name,
            modifier = Modifier.padding(top = 20.dp)
        )

        if (recipe.portion?.value != null) {
            val portionValue = if (recipe.portion.value % 1 == 0f) recipe.portion.value.toUInt() else recipe.portion.value
            val portionLabel = if (recipe.portion.value >= 2.0) recipe.portion.measurement + "s" else recipe.portion.measurement
            Text(
                text = "$portionValue $portionLabel",
                modifier = Modifier.padding(top = 20.dp)
            )
        }

        if (recipe.ingredients != null) {
            Text(
                text = "Ingredients:",
                modifier = Modifier.padding(top = 20.dp)
            )

            for (ingredient in recipe.ingredients) {
                Text(
                    text = ingredient.name + " " + ingredient.value + " " + ingredient.measurement,
                )
            }
        }

        if (recipe.methods != null) {
            Text(
                text = "Methods:",
                modifier = Modifier.padding(top = 20.dp)
            )

            for (method in recipe.methods) {
                val indicator = if (method.sortOrder != null) (method.sortOrder + 1) else 1;
                Text(
                    text = indicator.toString() + ". " + method.value,
                )
            }
        }
    }
}

@Composable
fun CreateOrEditRecipe(
    onSave: (
        recipeRequest: RecipeRequest,
        portionRequest: Portion,
        ingredientRequests: List<Ingredient>,
        methodRequests: List<Method>,
        imageBytes: ByteArray?
    ) -> Unit,
    recipe: Recipe? = null
) {
    var name by remember { mutableStateOf(recipe?.name ?: "") }
    val decodedBytes = Base64.decode(recipe?.image?.url ?: "", Base64.DEFAULT)
    var imageBytes by remember { mutableStateOf(if (recipe?.image?.url != null) decodedBytes else null) }

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
    var ingredients =
        remember { mutableStateOf(listOf<Ingredient>(*recipe?.ingredients.orEmpty())) }
    var showIngredientModal by remember { mutableStateOf(false) }
    val selectedIngredient = remember { mutableStateOf<Ingredient?>(null) }
    val selectedIngredientIndex = remember { mutableIntStateOf(0) }

    // methods handlers
    var methods = remember { mutableStateOf(listOf<Method>(*recipe?.methods.orEmpty())) }
    var showMethodModal by remember { mutableStateOf(false) }
    val selectedMethod = remember { mutableStateOf<Method?>(null) }
    val selectedMethodIndex = remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier.fillMaxHeight().verticalScroll(rememberScrollState()),
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
                    for (portion in arrayOf("day", "portion")) {
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
                {
                    ingredients.value = ingredients.value.toMutableList().apply {
                        add(toIndex, removeAt(fromIndex))
                    }
                }
            }
        ) { index, ingredient, isDragging ->
            key(index) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
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
                        imageVector = Icons.Default.FavoriteBorder,
                        contentDescription = "Drag Handle",
                        modifier = Modifier
                            .size(24.dp)
                            .draggableHandle() // This is correct for the drag handle
                    )
                }
            }
        }

        if (showIngredientModal) {
            AddOrEditIngredient(
                ingredient = selectedIngredient.value,
                onConfirmation = {
                    if (selectedIngredient.value != null) {
                        val updatedList = ingredients.value.toMutableList()
                        updatedList[selectedIngredientIndex.intValue] = it
                        ingredients.value = updatedList
                    } else {
                        ingredients.value = ingredients.value.toMutableList().apply { add(it) }
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
                {
                    methods.value = methods.value.toMutableList().apply {
                        add(toIndex, removeAt(fromIndex))
                    }
                }
            }
        ) { index, method, isDragging ->
            key(index) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
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
                        imageVector = Icons.Default.FavoriteBorder,
                        contentDescription = "Drag Handle",
                        modifier = Modifier
                            .size(24.dp)
                            .draggableHandle() // This is correct for the drag handle
                    )
                }
            }
        }

        if (showMethodModal) {
            AddOrEditMethodStep(
                method = selectedMethod.value,
                onConfirmation = {
                    if (selectedIngredient.value != null) {
                        val updatedList = methods.value.toMutableList()
                        updatedList[selectedMethodIndex.intValue] = it
                        methods.value = updatedList
                    } else {
                        methods.value = methods.value.toMutableList().apply { add(it) }
                    }
                    selectedMethod.value = null
                    showMethodModal = false
                },
                onDismissRequest = { showMethodModal = false },
                dialogTitle = if (selectedMethod.value != null) "Edit method step" else "New method step"
            )
        }

        Button(
            onClick = {
                onSave(
                    RecipeRequest(
                        id = recipe?.id ?: 0,
                        name = name
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
            },
            modifier = Modifier
                .align(Alignment.End),
            enabled = name !== ""
        ) {
            Text("Save")
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
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (image?.url != null) {
            val decodedBytes = Base64.decode(image.url, Base64.DEFAULT)
            if (decodedBytes != null) {
                val bitmap = byteArrayToBitmap(decodedBytes)
                val imageBitmap = bitmap.asImageBitmap()
                Image(
                    bitmap = imageBitmap,
                    contentDescription = "Existing image",
                    modifier = Modifier
                        .size(200.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            }
        }

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
            Text("Upload new image")
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

@Composable
fun FilterAndSortDialog(
    selectedIngredientNames: Array<String>,
    onApply: (ingredientNames: Array<String>, sortKey: String, sortDirection: String) -> Unit,
    selectedSortKey: String = "",
    selectedSortDirection: String = "",
    availableIngredientNames: Array<String>,
    onClose: () -> Unit
) {
    val sortOptions = mapOf(
        "name" to mapOf(
            "label" to "Name",
            "directionKeys" to listOf(
                mapOf("asc" to "A to Z", "desc" to "Z to A")
            )
        ),
        "portion" to mapOf(
            "label" to "Portion",
            "directionKeys" to listOf(
                mapOf("asc" to "Small to Large", "desc" to "Large to Small")
            )
        ),
        "createdAt" to mapOf(
            "label" to "Created at",
            "directionKeys" to listOf(
                mapOf("asc" to "Oldest to Newest", "desc" to "Newest to Oldest")
            )
        ),
        "lastEditedAt" to mapOf(
            "label" to "Last edited",
            "directionKeys" to listOf(
                mapOf("asc" to "Oldest to Newest", "desc" to "Newest to Oldest")
            )
        ),
        "sortOrder" to mapOf(
            "label" to "Defined Order",
            "directionKeys" to listOf(
                mapOf("asc" to "First to Last", "desc" to "Last to First")
            )
        )
    )


    var newSelectedIngredientNames = remember { mutableStateOf(listOf<String>(*selectedIngredientNames)) }
    var newSelectedSortKey = remember { mutableStateOf(selectedSortKey) }
    var newSelectedSortDirection = remember { mutableStateOf(selectedSortDirection) }

    var isSelectedSortKeyExpanded by remember { mutableStateOf(false) }
    var isSelectedSortDirectionExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        text = {
            Column {
                Column(
                    modifier = Modifier.padding(bottom = 10.dp)
                ) {
                    Text("Filter by ingredients:")
                    Column {
                        val rows = (availableIngredientNames.size + 2 - 1) / 2
                        for (rowIndex in 0 until rows) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                for (colIndex in 0 until 2) {
                                    val itemIndex = rowIndex * 2 + colIndex
                                    if (itemIndex < availableIngredientNames.size) {
                                        val name = availableIngredientNames[itemIndex]
                                        val isChecked = name in newSelectedIngredientNames.value
                                        Checkbox(
                                            checked = isChecked,
                                            onCheckedChange = { isChecked ->
                                                if (isChecked) {
                                                    newSelectedIngredientNames.value = newSelectedIngredientNames.value.toMutableList().apply { add(name) }
                                                } else {
                                                    val selectedIngredientNameIndex = newSelectedIngredientNames.value.indexOfFirst { it == name }
                                                    newSelectedIngredientNames.value = newSelectedIngredientNames.value.toMutableList().apply { removeAt(selectedIngredientNameIndex) }
                                                }
                                            }
                                        )
                                        var marginRight = if (colIndex == 0) 8 else 0

                                        Text(
                                            text = name,
                                            modifier = Modifier.padding(end = marginRight.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Column {
                    Text("Sort by:")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        var key = sortOptions[newSelectedSortKey.value]!!
                        var directionKeys = key["directionKeys"] as List<Map<String, String>>

                        Column (
                            modifier = Modifier.weight(1f)
                        ) {
                            DropdownMenuItem(
                                text = { Text(key["label"].toString()) },
                                onClick = {
                                    isSelectedSortKeyExpanded = !isSelectedSortKeyExpanded
                                }
                            )
                            DropdownMenu(
                                expanded = isSelectedSortKeyExpanded,
                                onDismissRequest = { isSelectedSortKeyExpanded = !isSelectedSortKeyExpanded }
                            ) {
                                for ((key, value) in sortOptions) {
                                    DropdownMenuItem(
                                        modifier = Modifier
                                            .background(if (key == newSelectedSortKey.value) Color.LightGray else Color.Transparent),
                                        text = { Text(value["label"].toString()) },
                                        onClick = {
                                            newSelectedSortKey.value = key.toString()
                                            isSelectedSortKeyExpanded = false
                                            directionKeys = value["directionKeys"] as List<Map<String, String>>
                                        },
                                    )
                                }
                            }
                        }
                        Column (
                            modifier = Modifier.weight(1f)
                        ) {
                            var currentDirection = directionKeys.firstOrNull()?.get(newSelectedSortDirection.value)
                            DropdownMenuItem(
                                text = { Text(currentDirection.toString()) },
                                onClick = {
                                    isSelectedSortDirectionExpanded = !isSelectedSortDirectionExpanded
                                }
                            )
                            DropdownMenu(
                                expanded = isSelectedSortDirectionExpanded,
                                onDismissRequest = { isSelectedSortDirectionExpanded = !isSelectedSortDirectionExpanded }
                            ) {
                                for (map in directionKeys) {
                                    for ((key, value) in map) {
                                        DropdownMenuItem(
                                            modifier = Modifier
                                                .background(if (key == newSelectedSortDirection.value) Color.LightGray else Color.Transparent),
                                            text = { Text(value) },
                                            onClick = {
                                                newSelectedSortDirection.value = key
                                                isSelectedSortDirectionExpanded = false
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        onDismissRequest = {
            onClose()
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onApply(newSelectedIngredientNames.value.toTypedArray(), newSelectedSortKey.value, newSelectedSortDirection.value)
                }
            ) {
                Text("Apply")
            }
        },
    )
}

fun byteArrayToBitmap(bytes: ByteArray): Bitmap {
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}
