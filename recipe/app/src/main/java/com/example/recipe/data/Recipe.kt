package com.example.recipe.data

data class Recipe (
    val id: Int,
    val name: String,
    val portion: Portion?,
    val ingredients: List<Ingredient>?,
    val methods: List<Method>?,
    val createdAt: String,
    val lastEditedAt: String,
    val image: Image?,
    val type: String,
    val sortOrder: Int,
    val url: String,
)
