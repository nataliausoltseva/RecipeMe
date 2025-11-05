package com.example.recipe.data

data class Divider(
    val id: Int,
    var title: String,
    var recipeId: Int,
    var sortOrder: Int,
    var ingredients: List<Ingredient>? = emptyList()
)
