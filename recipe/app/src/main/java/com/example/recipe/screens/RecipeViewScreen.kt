package com.example.recipe.screens

import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.recipe.byteArrayToBitmap
import com.example.recipe.data.Recipe
import com.example.recipe.data.RecipeViewModel

@Composable
fun RecipeViewScreen(
    recipeViewModel: RecipeViewModel,
) {
    val recipesUIState by recipeViewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Go back",
                modifier = Modifier
                    .clickable { recipeViewModel.backToListView() }
                    .size(50.dp, 50.dp)
            )
            Icon(
                imageVector = Icons.Filled.Edit,
                contentDescription = "Edit button",
                modifier = Modifier
                    .clickable { recipeViewModel.onEditRecipe() }
                    .padding(end = 10.dp)
                    .size(30.dp, 30.dp)
            )
        }
        Recipe(
            recipesUIState.selectedRecipe!!
        )

    }
}

@Composable
fun Recipe(
    recipe: Recipe
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
                Image(
                    bitmap = imageBitmap,
                    contentDescription = recipe.name + " image",
                    modifier = Modifier
                        .size(200.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
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
    }
}