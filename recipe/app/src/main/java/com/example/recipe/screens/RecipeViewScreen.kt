package com.example.recipe.screens

import android.app.Activity
import android.util.Base64
import android.util.Patterns
import android.view.WindowManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.recipe.data.Recipe
import com.example.recipe.data.RecipeViewModel
import com.example.recipe.data.byteArrayToBitmap


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeViewScreen(
    itemId: String?,
    recipeViewModel: RecipeViewModel,
    onNavigateBack: () -> Unit,
    onNavigateRecipeEdit: (id: String) -> Unit,
) {
    val recipesUIState by recipeViewModel.uiState.collectAsState()
    val showDeleteModal = remember { mutableStateOf(false) }

    val showFullImage = remember { mutableStateOf(false) }

    val recipe = recipesUIState.recipes.find { it.id.toString() == itemId }

    val context = LocalContext.current
    val activity = context as? Activity

    LaunchedEffect(recipesUIState.isCookingModeOn) {
        activity?.window?.let { window ->
            if (recipesUIState.isCookingModeOn) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    if (recipe === null) {
        onNavigateBack()
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Go back",
                modifier = Modifier
                    .clickable {
                        if (showFullImage.value) {
                            showFullImage.value = false
                        } else {
                            onNavigateBack()
                        }
                    }
                    .size(50.dp, 50.dp)
            )

            if (!showFullImage.value) {
                Text(
                    text = recipe.name,
                    fontWeight = FontWeight.Bold,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 22.sp,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                )
                Row {
                    val tooltipState = rememberTooltipState()
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                        tooltip = {
                            PlainTooltip {
                                Text("Cooking mode is " + if (recipesUIState.isCookingModeOn) "on" else "off")
                            }
                        },
                        state = tooltipState
                    ) {
                        Icon(
                            imageVector = if (recipesUIState.isCookingModeOn) Icons.Filled.Lightbulb else Icons.Outlined.Lightbulb,
                            contentDescription = if (recipesUIState.isCookingModeOn) "Disable Cooking Mode" else "Enable Cooking Mode",
                            modifier = Modifier
                                .padding(end = 10.dp)
                                .size(30.dp, 30.dp)
                                .clickable { recipeViewModel.onCookingModeToggle() }
                        )
                    }
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Edit button",
                        modifier = Modifier
                            .clickable { onNavigateRecipeEdit(recipe.id.toString()) }
                            .padding(end = 10.dp)
                            .size(30.dp, 30.dp)
                    )
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Delete button",
                        modifier = Modifier
                            .clickable { showDeleteModal.value = true }
                            .padding(end = 10.dp)
                            .size(30.dp, 30.dp)
                    )
                }
            }
        }

        if (showFullImage.value && recipe.image != null) {
            FullImage(recipe.image.url)
        } else {
            Recipe(
                recipe = recipe,
                onImageClick = { showFullImage.value = true }
            )
        }

        if (showDeleteModal.value) {
            AlertDialog(
                text = {
                    Text(
                        text = "Are you sure you want to delete this recipe?",
                        textAlign = TextAlign.Center,
                        fontSize = 16.sp
                    )
               },
                onDismissRequest = {
                    showDeleteModal.value = false
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            recipeViewModel.onDeleteRecipe(recipe)
                            onNavigateBack()
                        }
                    ) {
                        Text("Delete", color = Color.Red)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showDeleteModal.value = false }
                    ) {
                        Text("Cancel")
                    }
                },
            )
        }

    }
}

@Composable
fun Recipe(
    recipe: Recipe,
    onImageClick: () -> Unit
) {
    val scrollState = rememberScrollState()
    val checkedIngredients = remember { mutableStateSetOf<String>() }
    val checkedMethods = remember { mutableStateSetOf<Int>() }

    Column(
        modifier = Modifier
            .padding(10.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (recipe.image?.url != null) {
            val decodedBytes = Base64.decode(recipe.image.url, Base64.DEFAULT)
            if (decodedBytes != null) {
                val bitmap = byteArrayToBitmap(decodedBytes)
                val imageBitmap = bitmap.asImageBitmap()
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onImageClick() },
                    horizontalArrangement = Arrangement.Center
                ) {
                    Image(
                        bitmap = imageBitmap,
                        contentDescription = recipe.name + " image",
                        modifier = Modifier
                            .size(200.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row {
                Text(
                    text = recipe.name,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    overflow = TextOverflow.Ellipsis
                )

                if (recipe.portion != null) {
                    Text(
                        text = "(${recipe.portion.value} ${recipe.portion.measurement})",
                        modifier = Modifier.padding(start = 5.dp),
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (recipe.type.isNotEmpty()) {
                Text(
                    text = recipe.type,
                    fontStyle = FontStyle.Italic,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (recipe.portion?.value != null) {
            val portionValue = if (recipe.portion.value % 1 == 0f) recipe.portion.value.toUInt() else recipe.portion.value
            val portionLabel = if (recipe.portion.value >= 2.0) recipe.portion.measurement + "s" else recipe.portion.measurement
            Text(
                text = "$portionValue $portionLabel",
                modifier = Modifier.padding(top = 20.dp)
            )
        }

        val allDividerIngredientNames = recipe.dividers?.flatMap {
            it.ingredients?.map { ing -> ing.name } ?: emptyList()
        }?.toSet() ?: emptySet()

        if (!recipe.dividers.isNullOrEmpty()) {
            recipe.dividers.forEach { divider ->
                Text(
                    text = divider.title,
                    modifier = Modifier.padding(top = 20.dp, bottom = 5.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                if (!divider.ingredients.isNullOrEmpty()) {
                    val chunkedDividerIngredients = divider.ingredients!!.toList().chunked((divider.ingredients!!.size + 1) / 2)
                    Row(modifier = Modifier.fillMaxWidth()) {
                        chunkedDividerIngredients.forEach { columnItems ->
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp)
                            ) {
                                columnItems.forEach { item ->
                                    val isChecked = item.name in checkedIngredients
                                    val textDecoration = if (isChecked) TextDecoration.LineThrough else null
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 5.dp)
                                            .clickable {
                                                if (isChecked) {
                                                    checkedIngredients.remove(item.name)
                                                } else {
                                                    checkedIngredients.add(item.name)
                                                }
                                            },
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Text(
                                            text = item.name,
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(end = 8.dp),
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            softWrap = true,
                                            textDecoration = textDecoration
                                        )
                                        Text(
                                            text = "${item.value} ${item.measurement}",
                                            modifier = Modifier.align(Alignment.CenterVertically),
                                            textAlign = TextAlign.End,
                                            textDecoration = textDecoration
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        val mainIngredientsToDisplay = recipe.ingredients?.filterNot { it.name in allDividerIngredientNames }
        if (!mainIngredientsToDisplay.isNullOrEmpty()) {
            Text(
                text = "Other Ingredients:",
                modifier = Modifier.padding(top = 20.dp),
                fontWeight = FontWeight.Bold
            )

            val chunkedItems = mainIngredientsToDisplay.toList().chunked((mainIngredientsToDisplay.size + 1) / 2)

            Row(modifier = Modifier.fillMaxWidth()) {
                chunkedItems.forEach { columnItems ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    ) {
                        columnItems.forEach { item ->
                            val isChecked = item.name in checkedIngredients
                            val textDecoration = if (isChecked) TextDecoration.LineThrough else null
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 5.dp)
                                    .clickable {
                                        if (isChecked) {
                                            checkedIngredients.remove(item.name)
                                        } else {
                                            checkedIngredients.add(item.name)
                                        }
                                    },
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = item.name,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 8.dp),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    softWrap = true,
                                    textDecoration = textDecoration
                                )
                                Text(
                                    text = "${item.value} ${item.measurement}",
                                    modifier = Modifier.align(Alignment.CenterVertically),
                                    textAlign = TextAlign.End,
                                    textDecoration = textDecoration
                                )
                            }
                        }
                    }
                }
            }
        }

        if (!recipe.methods.isNullOrEmpty()) {
            Text(
                text = "Methods:",
                modifier = Modifier.padding(top = 20.dp),
                fontWeight = FontWeight.Bold // Added for consistency
            )

            recipe.methods.forEachIndexed { index, method ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    val isMethodChecked = index in checkedMethods
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isMethodChecked) {
                                    checkedMethods.remove(index)
                                } else {
                                    checkedMethods.add(index)
                                    // Keep existing logic for checking parent method checks related ingredients
                                    method.ingredients?.mapNotNull { recipe.ingredients?.find{ mainIng -> mainIng.name == it.name }?.name }?.let { names ->
                                        checkedIngredients.addAll(names.filterNot { it in checkedIngredients })
                                    }
                                }
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        if (isMethodChecked) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = "Checked",
                                tint = Color.Green,
                                modifier = Modifier.size(24.dp).padding(end = 8.dp)
                            )
                        } else {
                            val indicator = method.sortOrder ?: (index + 1)
                            Text(
                                text = "$indicator. ",
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        }
                        Text(
                            text = method.value,
                            textDecoration = if (isMethodChecked) TextDecoration.LineThrough else null,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (!method.ingredients.isNullOrEmpty()) {
                        Column(modifier = Modifier.padding(start = 28.dp, top = 4.dp, bottom = 4.dp)) {
                            method.ingredients?.forEach { ingredientInfo ->
                                val linkedIngredient = recipe.ingredients?.find { it.name == ingredientInfo.name }
                                if (linkedIngredient != null && linkedIngredient.name !in allDividerIngredientNames) { // Filtered here
                                    val isIngredientChecked =  linkedIngredient.name in checkedIngredients
                                    val ingredientTextDecoration = if (isIngredientChecked) TextDecoration.LineThrough else null
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 2.dp)
                                            .clickable {
                                                if (isIngredientChecked) {
                                                    checkedIngredients.remove(linkedIngredient.name)
                                                    // If unchecking an ingredient unchecks the parent method step:
                                                    // checkedMethods.remove(index) // This behavior can be complex
                                                } else {
                                                    checkedIngredients.add(linkedIngredient.name)
                                                }
                                            },
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "• ${linkedIngredient.name}",
                                            modifier = Modifier.weight(1f),
                                            textDecoration = ingredientTextDecoration,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = "${linkedIngredient.value} ${linkedIngredient.measurement}",
                                            textDecoration = ingredientTextDecoration,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        // --- END MODIFIED MAIN METHODS SECTION ---


        if (recipe.url.isNotEmpty()) {
            val uriHandler = LocalUriHandler.current
            val isValidUrl = Patterns.WEB_URL.matcher(recipe.url).matches()

            Column(modifier = Modifier.padding(top = 20.dp)) {
                Text("Original recipe URL:")
                Text(
                    text = if (isValidUrl) recipe.url else recipe.url + " (invalid URL)",
                    modifier = Modifier.clickable(enabled = isValidUrl) { uriHandler.openUri(recipe.url) },
                    color = if (isValidUrl) Color.Blue else Color.Gray
                )
            }
        }
    }
}

@Composable
fun FullImage(
    imageUrl: String,
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val decodedBytes = Base64.decode(imageUrl, Base64.DEFAULT)
    val bitmap = byteArrayToBitmap(decodedBytes)
    val imageBitmap = bitmap.asImageBitmap()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val newScale = (scale * zoom).coerceIn(1f, 3f)
                    val maxOffsetX = (newScale - 1f) * size.width / 2f
                    val maxOffsetY = (newScale - 1f) * size.height / 2f
                    val newOffset = offset + pan
                    val coercedOffset = Offset(
                        x = newOffset.x.coerceIn(-maxOffsetX, maxOffsetX),
                        y = newOffset.y.coerceIn(-maxOffsetY, maxOffsetY)
                    )
                    offset = if (newScale == 1f) Offset.Zero else coercedOffset
                    scale = newScale
                }
            }
    ) {
        Image(
            bitmap = imageBitmap,
            contentDescription = "Full-size image",
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                )
        )
    }
}
