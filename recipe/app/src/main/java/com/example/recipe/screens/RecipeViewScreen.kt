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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.recipe.data.byteArrayToBitmap
import com.example.recipe.data.Recipe
import com.example.recipe.data.RecipeViewModel

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
                    text = recipe?.name ?: "",
                    fontWeight = FontWeight.Bold,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 22.sp
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
                            .clickable { onNavigateRecipeEdit(recipe!!.id.toString()) }
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

        if (showFullImage.value && recipe!!.image != null) {
            FullImage(recipe.image.url)
        } else {
            Recipe(
                recipe!!,
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
    Column(
        modifier = Modifier
            .padding(10.dp),
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
                modifier = Modifier.padding(top = 20.dp),
                fontWeight = FontWeight.Bold
            )

            val chunkedItems = recipe.ingredients.toList().chunked((recipe.ingredients.size + 1) / 2) // Splits into two roughly equal lists

            Row(modifier = Modifier.fillMaxWidth()) {
                chunkedItems.forEach { columnItems ->
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        columnItems.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 5.dp, end = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(20.dp)
                            ) {
                                Text(text = item.name)
                                Text(
                                    text = item.value.toString() + " " + item.measurement,
                                )
                            }
                        }
                    }
                }
            }

        }

        if (recipe.methods != null) {
            Text(
                text = "Methods:",
                modifier = Modifier.padding(top = 20.dp)
            )

            for (method in recipe.methods) {
                val indicator = method.sortOrder ?: 1;
                Text(
                    text = indicator.toString() + ". " + method.value,
                )
            }
        }

        if (recipe.url != "") {
            val uriHandler = LocalUriHandler.current
            val isValidUrl = Patterns.WEB_URL.matcher(recipe.url).matches()

            Column {
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
                    // Calculate the new scale value, clamping it between 1f and a max value (e.g., 3f)
                    val newScale = (scale * zoom).coerceIn(1f, 3f)

                    // Calculate the maximum allowed offset to prevent panning outside the image bounds
                    val maxOffsetX = (newScale - 1f) * size.width / 2f
                    val maxOffsetY = (newScale - 1f) * size.height / 2f

                    // Calculate the new offset, clamping it to the allowed bounds
                    val newOffset = offset + pan
                    val coercedOffset = Offset(
                        x = newOffset.x.coerceIn(-maxOffsetX, maxOffsetX),
                        y = newOffset.y.coerceIn(-maxOffsetY, maxOffsetY)
                    )

                    // If scale is 1f, reset the offset. Otherwise, apply the corrected offset.
                    offset = if (newScale == 1f) {
                        Offset.Zero
                    } else {
                        coercedOffset
                    }

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