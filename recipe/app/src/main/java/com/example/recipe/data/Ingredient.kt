package com.example.recipe.data

data class Ingredient(
    val id: Int,
    var name: String,
    var measurement: String,
    var value: Number,
    val sortOrder: Int?,
)
