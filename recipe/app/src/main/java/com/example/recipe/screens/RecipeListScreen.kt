package com.example.recipe.screens

import android.util.Base64
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.HapticFeedbackConstantsCompat
import androidx.core.view.ViewCompat
import com.example.recipe.byteArrayToBitmap
import com.example.recipe.data.Recipe
import com.example.recipe.data.RecipeViewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyStaggeredGridState

@Composable
fun RecipeListScreen(
    recipeViewModel: RecipeViewModel,
) {
    val recipesUIState by recipeViewModel.uiState.collectAsState()

    var search by remember { mutableStateOf("") }
    var showFilterDialog = remember { mutableStateOf(false)}
    var sortKeyState by remember { mutableStateOf(recipesUIState.selectedSortKey) }
    var sortDirectionKeyState by remember { mutableStateOf(recipesUIState.selectedSortDirection) }
    var recipes = remember { mutableStateOf(recipesUIState.recipes) }

    Column(
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
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
        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            var list by remember { mutableStateOf(recipes) }

            RecipeList(
                recipes = recipesUIState.recipes,
                onView = { recipeViewModel.viewRecipe(it) },
                onReorder = {
                    recipeViewModel.onReorder()
                    list.value = it
                },
                shouldReset = recipesUIState.shouldReset,
                onSaveReset = { recipeViewModel.onSaveReset() },
                isSortedByOrder = recipesUIState.selectedSortKey == "sortOrder"
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

        if (showFilterDialog.value) {
            FilterAndSortDialog(
                selectedIngredientNames=recipesUIState.selectedIngredientNames,
                selectedSortKey = sortKeyState,
                selectedSortDirection = sortDirectionKeyState,
                availableIngredientNames = recipesUIState.availableIngredients,
                onApply = { selectedNames, sortKey, sortDirection ->
                    if (sortKeyState == "sortOrder" && sortKey != "sortOrder" && recipesUIState.isReordered) {
                        recipeViewModel.onReset()
                    }
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
fun RecipeList(
    recipes: List<Recipe>,
    onView: (recipe: Recipe) -> Unit,
    onReorder: (List<Recipe>) -> Unit,
    shouldReset: Boolean,
    onSaveReset: () -> Unit,
    isSortedByOrder: Boolean,
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
        if (isSortedByOrder) {
            list = list.toMutableList().apply {
                add(to.index, removeAt(from.index))
            }
            onReorder(list)
        }
    }

    LazyVerticalStaggeredGrid(
        state = lazyStaggeredGridState,
        columns = StaggeredGridCells.Adaptive(minSize = 300.dp),
        modifier = Modifier.fillMaxSize().padding(5.dp),
        verticalItemSpacing = 4.dp,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        content = {
            items(list, key = { it.id.toString() }) { recipe ->
                ReorderableItem(reorderableLazyStaggeredGridState, key = recipe.id.toString()) { isDragging ->
                    val elevation by animateDpAsState(if (isDragging && isSortedByOrder) 4.dp else 0.dp)
                    Surface (shadowElevation = elevation) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp)
                                .clickable { onView(recipe) }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                var modifier = Modifier.fillMaxWidth(0.5f)

                                if (isSortedByOrder) {
                                    modifier = modifier.draggableHandle(
                                        onDragStarted = {
                                            if (isSortedByOrder) {
                                                ViewCompat.performHapticFeedback(
                                                    view,
                                                    HapticFeedbackConstantsCompat.GESTURE_START
                                                )
                                            }
                                        },
                                        onDragStopped = {
                                            if (isSortedByOrder) {
                                                ViewCompat.performHapticFeedback(
                                                    view,
                                                    HapticFeedbackConstantsCompat.GESTURE_END
                                                )
                                            }
                                        },
                                    )
                                }

                                if (recipe.image?.url != null) {
                                    val decodedBytes = Base64.decode(recipe.image.url, Base64.DEFAULT)
                                    if (decodedBytes != null) {
                                        val bitmap = byteArrayToBitmap(decodedBytes)
                                        val imageBitmap = bitmap.asImageBitmap()
                                        Image(
                                            bitmap = imageBitmap,
                                            contentDescription = recipe.name + " image",
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(max = 100.dp)
                                                .clip(RoundedCornerShape(8.dp))

                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    val currentHorizontalArrangement = if (recipe.type == "") {
                                        Arrangement.SpaceBetween
                                    } else {
                                        Arrangement.Start
                                    }

                                    val modifier = if (recipe.type == "") {
                                        Modifier.fillMaxWidth()
                                    } else {
                                        Modifier
                                    }

                                    Row (
                                        modifier = modifier,
                                        horizontalArrangement = currentHorizontalArrangement,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val maxWidth = if (recipe.type != "") 170.dp else 230.dp
                                        Text(
                                            text = recipe.name,
                                            textAlign = TextAlign.Center,
                                            fontWeight = FontWeight.Bold,
                                            overflow = TextOverflow.Visible,
                                            modifier = Modifier.widthIn(max = maxWidth)
                                        )

                                        if (recipe.portion != null) {
                                            Text(
                                                text = "(" + recipe.portion.value.toString() + " " + recipe.portion.measurement + ")",
                                                modifier = Modifier.padding(start = 5.dp),
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    if (recipe.type != "") {
                                        Text(
                                            text = recipe.type,
                                            fontStyle = FontStyle.Italic,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Ingredients:",
                                        overflow = TextOverflow.Ellipsis,
                                    )

                                    var itemLabel = "item"

                                    if (recipe.ingredients == null || recipe.ingredients.size > 1) {
                                        itemLabel = "items"
                                    }

                                    Text(
                                        text = (if (recipe.ingredients == null) "0" else recipe.ingredients.size.toString()) + " " + itemLabel,
                                        fontStyle = FontStyle.Italic,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                if (recipe.ingredients != null) {
                                    var allIngredients = recipe.ingredients.joinToString("\n") { "${it.name} ${it.value} ${it.measurement}" }
                                    Text(
                                        text = allIngredients,
                                        overflow = TextOverflow.Ellipsis,maxLines = 5

                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Methods:",
                                        overflow = TextOverflow.Ellipsis,
                                    )

                                    var itemLabel = "step"

                                    if (recipe.methods == null || recipe.methods.size > 1) {
                                        itemLabel = "steps"
                                    }

                                    Text(
                                        text = (if (recipe.methods == null) "0" else recipe.methods.size.toString()) + " " + itemLabel,
                                        fontStyle = FontStyle.Italic,
                                        overflow = TextOverflow.Ellipsis,
                                        maxLines = 3
                                    )
                                }

                                if (recipe.methods != null) {
                                    var allMethods = recipe.methods.joinToString("\n") { method ->
                                        val indicator =
                                            method.sortOrder ?: (recipe.methods.indexOf(method) + 1)
                                        "$indicator. ${method.value}"
                                    }

                                    Text(
                                        text = allMethods,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
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