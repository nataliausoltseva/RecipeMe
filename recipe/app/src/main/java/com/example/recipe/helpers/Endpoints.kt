package com.example.recipe.helpers

import com.example.recipe.data.Recipe

class Endpoints {
    private var apiInterface: ApiInterface = RetrofitInstance.getInstance().create(ApiInterface::class.java)

    suspend fun getRecipes(): List<Recipe>? {
        try {
            val response = apiInterface.getRecipes()
            if (response.isSuccessful) {
                val data = response.body()
                return data
            } else {
                println("Response error: $response")
                return null
            }
        } catch (e: Exception) {
            println("Exception error getSpecies: ${e.message}. ${e.printStackTrace()}")
            return null
        }
    }

    suspend fun createRecipe(recipe: RecipeRequest) {
        try {
            apiInterface.createRecipe(recipe)
        } catch (e: Exception) {
            println("Exception error getSpecies: ${e.message}. ${e.printStackTrace()}")
        }
    }
}