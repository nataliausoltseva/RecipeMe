package com.example.recipe.data

data class Recipe (
    val id: Int,
    val name: String,
    val portion: Portion?,
    val ingredients: Array<Ingredient>?,
    val methods: Array<Method>?,
    val createdAt: String,
    val editedAt: String,
    val image: Image?,
    val type: String,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Recipe

        if (id != other.id) return false
        if (name != other.name) return false
        if (portion != other.portion) return false
        if (!ingredients.contentEquals(other.ingredients)) return false
        if (!methods.contentEquals(other.methods)) return false
        if (createdAt != other.createdAt) return false
        if (editedAt != other.editedAt) return false
        if (image != other.image) return false
        if (type != other.type) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + name.hashCode()
        result = 31 * result + (portion?.hashCode() ?: 0)
        result = 31 * result + (ingredients?.contentHashCode() ?: 0)
        result = 31 * result + (methods?.contentHashCode() ?: 0)
        result = 31 * result + createdAt.hashCode()
        result = 31 * result + editedAt.hashCode()
        result = 31 * result + (image?.hashCode() ?: 0)
        result = 31 * result + type.hashCode()
        return result
    }
}
