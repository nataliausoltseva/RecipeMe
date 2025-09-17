package com.example.recipe.data

data class Method(
    val id: Int,
    val sortOrder: Int?,
    var value: String,
    var ingredients: List<Ingredient>? = emptyList()
)
