package com.example.recipe.helpers

import com.example.recipe.data.Ingredient
import com.example.recipe.data.Method
import com.example.recipe.data.Portion
import com.example.recipe.data.Recipe
import okhttp3.MultipartBody

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

    suspend fun updateOrCreatePortion(portion: Portion, recipeId: Int) {
        try {
            apiInterface.addOrUpdatePortion(recipeId, portion)
        } catch (e: Exception) {
            println("Exception error updateOrCreatePortion: ${e.message}. ${e.printStackTrace()}")
        }
    }

    suspend fun addOrUpdateIngredients(ingredients: List<Ingredient>, recipeId: Int) {
        try {
            apiInterface.addOrUpdateIngredients(recipeId, ingredients)
        } catch (e: Exception) {
            println("Exception error updateOrCreatePortion: ${e.message}. ${e.printStackTrace()}")
        }
    }

    suspend fun addOrUpdateMethods(methods: List<Method>, recipeId: Int) {
        try {
            apiInterface.addOrUpdateMethods(recipeId, methods)
        } catch (e: Exception) {
            println("Exception error updateOrCreatePortion: ${e.message}. ${e.printStackTrace()}")
        }
    }

    suspend fun addImage(body: MultipartBody.Part, recipeId: Int) {
        try {
            apiInterface.uploadImage(recipeId, body)
        } catch (e: Exception) {
            println("Exception error updateOrCreatePortion: ${e.message}. ${e.printStackTrace()}")
        }
    }

    suspend fun getIngredients(): List<Ingredient>? {
        try {
            val response = apiInterface.getIngredients()
            if (response.isSuccessful) {
                val data = response.body()
                return data
            } else {
                println("Response error: $response")
                return null
            }
        } catch (e: Exception) {
            println("Exception error getIngredients: ${e.message}. ${e.printStackTrace()}")
            return null
        }
    }
}