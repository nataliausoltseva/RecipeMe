package com.example.recipe.data

data class Divider(
    val id: Int,
    var name: String,
    var recipeId: Int,
    var sortOrder: Int,
    var methods: List<Ingredient>? = emptyList(),
    var ingredients: List<Ingredient>? = emptyList()
)
