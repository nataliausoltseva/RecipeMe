package com.example.recipe.data

import kotlinx.serialization.Serializable

@Serializable
data class Equipment(
    val id: Int = 0,
    val name: String = "",
    val sortOrder: Int = 0
)
