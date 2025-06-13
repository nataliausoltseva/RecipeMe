package com.example.recipe.data

data class State(
    val recipes: List<Recipe> = listOf(),
    val isLoading: Boolean = false,
    val selectedIngredientNames: List<String> = emptyList(),
    val availableIngredients: List<String> = emptyList(),
    val selectedSortDirection: String = "asc",
    val selectedSortKey: String = "sortOrder",
    val isReordered: Boolean = false,
    val shouldReset: Boolean = false,
    val isTypeSplitView: Boolean = false,
)
