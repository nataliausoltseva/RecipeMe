package com.example.recipe.data

data class Recipe (
    val id: Int,
    val name: String,
    val portion: Portion?,
    val imageUrl: String,
    val image: String,
    val ingredients: Array<Ingredient>?,
    val methods: Array<Method>?,
    val createdAt: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Recipe

        if (id != other.id) return false
        if (name != other.name) return false
        if (portion != other.portion) return false
        if (imageUrl != other.imageUrl) return false
        if (image != other.image) return false
        if (!ingredients.contentEquals(other.ingredients)) return false
        if (!methods.contentEquals(other.methods)) return false
        if (createdAt != other.createdAt) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + portion.hashCode()
        result = 31 * result + imageUrl.hashCode()
        result = 31 * result + image.hashCode()
        result = 31 * result + ingredients.contentHashCode()
        result = 31 * result + methods.contentHashCode()
        result = 31 * result + createdAt.hashCode()
        return result
    }
}
