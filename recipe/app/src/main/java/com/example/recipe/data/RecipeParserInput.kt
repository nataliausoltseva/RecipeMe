package com.example.recipe.data

import kotlinx.serialization.Serializable

@Serializable
data class RecipeParserInput(
    val name: String?,
    val url: String?,
    val methods: List<RecipeMethodInput>?,
    val ingredients: List<RecipeIngredientInput>?,
    val portion: RecipePortionInput?,
)

@Serializable
data class RecipeMethodInput(
    var value: String
)

@Serializable
data class RecipeIngredientInput(
    var name: String,
    var measurement: String,
    var value: Float,
)

@Serializable
data class RecipePortionInput(
    val value: Float,
    val measurement: String,
)
