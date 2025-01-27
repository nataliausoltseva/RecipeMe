package com.example.recipe.data

data class State(
    val recipes: List<Recipe> = listOf(),
    val isLoading: Boolean = false,
    val isFullScreen: Boolean = false,
    val selectedRecipe: Recipe? = null,
)
