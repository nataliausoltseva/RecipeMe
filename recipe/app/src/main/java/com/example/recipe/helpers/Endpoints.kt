package com.example.recipe.helpers

import com.example.recipe.data.Recipe

class Endpoints {
    private var apiInterface: ApiInterface = RetrofitInstance.getInstance().create(ApiInterface::class.java)

    suspend fun getRecipes(search: String?): List<Recipe>? {
        try {
            val response = apiInterface.getRecipes(search)
            if (response.isSuccessful) {
                val data = response.body()
                return data
            } else {
                println("Response error: $response")
                return null
            }
        } catch (e: Exception) {
            println("Exception error getRecipes: ${e.message}. ${e.printStackTrace()}")
            return null
        }
    }

    suspend fun updateOrCreateRecipe(recipe: RecipeRequest): Recipe? {
        try {
            if (recipe.id == 0) {
                val response = apiInterface.createRecipe(recipe)
                if (response.isSuccessful) {
                    val data = response.body()
                    return data
                } else {
                    println("Response error: $response")
                    return null
                }
            } else {
                val response = apiInterface.updateRecipe(recipe.id, recipe)
                if (response.isSuccessful) {
                    val data = response.body()
                    return data
                } else {
                    println("Response error: $response")
                    return null
                }
            }
        } catch (e: Exception) {
            println("Exception error updateOrCreateRecipe: ${e.message}. ${e.printStackTrace()}")
            return null
        }
    }

    suspend fun updateOrCreatePortion(portion: PortionRequest, recipeId: Int) {
        try {
            apiInterface.addOrUpdate(recipeId, portion)
        } catch (e: Exception) {
            println("Exception error updateOrCreatePortion: ${e.message}. ${e.printStackTrace()}")
        }
    }

    suspend fun addOrUpdateIngredients(ingredients: List<IngredientRequest>, recipeId: Int) {
        try {
            apiInterface.addIngredients(recipeId, ingredients)
        } catch (e: Exception) {
            println("Exception error updateOrCreatePortion: ${e.message}. ${e.printStackTrace()}")
        }
    }

    suspend fun addOrUpdateMethods(methods: List<MethodRequest>, recipeId: Int) {
        try {
            apiInterface.addMethods(recipeId, methods)
        } catch (e: Exception) {
            println("Exception error updateOrCreatePortion: ${e.message}. ${e.printStackTrace()}")
        }
    }
}