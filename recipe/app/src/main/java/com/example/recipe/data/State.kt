package com.example.recipe.data

data class State(
    val recipes: List<Recipe> = listOf(),
    val isLoading: Boolean = false,
    val isFullScreen: Boolean = false,
    val selectedRecipe: Recipe? = null,
    val isEditingRecipe: Boolean = false,
    val selectedIngredientNames: Array<String> = arrayOf(),
    val availableIngredients: Array<String> = arrayOf(),
    val selectedSortDirection: String = "asc",
    val selectedSortKey: String = "lastEditedAt",

) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as State

        if (isLoading != other.isLoading) return false
        if (isFullScreen != other.isFullScreen) return false
        if (isEditingRecipe != other.isEditingRecipe) return false
        if (recipes != other.recipes) return false
        if (selectedRecipe != other.selectedRecipe) return false
        if (!selectedIngredientNames.contentEquals(other.selectedIngredientNames)) return false
        if (!availableIngredients.contentEquals(other.availableIngredients)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = isLoading.hashCode()
        result = 31 * result + isFullScreen.hashCode()
        result = 31 * result + isEditingRecipe.hashCode()
        result = 31 * result + recipes.hashCode()
        result = 31 * result + (selectedRecipe?.hashCode() ?: 0)
        result = 31 * result + selectedIngredientNames.contentHashCode()
        result = 31 * result + availableIngredients.contentHashCode()
        return result
    }
}
