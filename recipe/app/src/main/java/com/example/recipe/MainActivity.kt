package com.example.recipe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.recipe.data.RecipeViewModel
import com.example.recipe.screens.RecipeListScreen
import com.example.recipe.screens.RecipeModifyScreen
import com.example.recipe.screens.RecipeViewScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val recipeViewModel: RecipeViewModel = viewModel()
            Main(recipeViewModel)
        }
    }
}

@Composable
fun Main(recipeViewModel: RecipeViewModel) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            RecipeListScreen(
                recipeViewModel,
                onNavigateRecipeView = {
                    navController.navigate("view/$it")
                },
                onNavigateRecipeEdit = {
                    navController.navigate("edit/$it")
                }
            )
        }
        composable("view/{recipeId}") { backStackEntry ->
            val recipeId = backStackEntry.arguments?.getString("recipeId")
            RecipeViewScreen(
                recipeId,
                recipeViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateRecipeEdit = {
                    navController.navigate("edit/$it")
                }
            )
        }
        composable("edit/{recipeId}") { backStackEntry ->
            val recipeId = backStackEntry.arguments?.getString("recipeId")
            RecipeModifyScreen(
                recipeId,
                recipeViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
