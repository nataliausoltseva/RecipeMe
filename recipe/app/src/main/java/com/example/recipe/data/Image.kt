package com.example.recipe.data

data class Image(
    val id: Int,
    val url: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Image

        if (id != other.id) return false
        if (!url.contentEquals(other.url)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + url.contentHashCode()
        return result
    }
}
