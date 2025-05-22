package com.example.recipe

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.example.recipe.data.RecipeViewModel
import com.example.recipe.screens.RecipeListScreen
import com.example.recipe.screens.RecipeModifyScreen
import com.example.recipe.screens.RecipeViewScreen

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
    if (recipesUIState.isFullScreen) {
        if (recipesUIState.isEditingRecipe || recipesUIState.selectedRecipe == null) {
            RecipeModifyScreen(recipeViewModel)
        } else {
            RecipeViewScreen(recipeViewModel)
        }
    } else {
        RecipeListScreen(recipeViewModel)
    }
}

fun byteArrayToBitmap(bytes: ByteArray): Bitmap {
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}
