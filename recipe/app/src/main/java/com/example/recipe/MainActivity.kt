package com.example.recipe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.recipe.data.RecipeViewModel
import com.example.recipe.screens.RecipeListScreen
import com.example.recipe.screens.RecipeModifyScreen
import com.example.recipe.screens.RecipeViewScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Main(RecipeViewModel())
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
