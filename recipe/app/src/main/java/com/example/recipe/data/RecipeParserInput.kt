package com.example.recipe.data

import kotlinx.serialization.Serializable

@Serializable
data class RecipeParserInput(
    val url: String? = "",
    val methods: List<RecipeMethodInput>? = null,
    val ingredients: List<RecipeIngredientInput>? = null,
    val portion: RecipePortionInput? = null,
    val dividers: List<Divider>? = null
)

@Serializable
data class Divider(
    val id: Int? = null,
    val title: String? = null,
    val recipeId: Int? = null,
    val sortOrder: Int? = null,
    val methods: List<RecipeMethodInput>? = null,
    val ingredients: List<RecipeIngredientInput>? = null
)

@Serializable
data class RecipeMethodInput(
    var value: String?
)

@Serializable
data class RecipeIngredientInput(
    var name: String?,
    var measurement: String?,
    var value: Float?,
)

@Serializable
data class RecipePortionInput(
    val value: Float?,
    val measurement: String?,
)
