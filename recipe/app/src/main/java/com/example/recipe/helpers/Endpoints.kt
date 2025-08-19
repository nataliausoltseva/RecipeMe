package com.example.recipe.helpers

import com.example.recipe.data.Ingredient
import com.example.recipe.data.Method
import com.example.recipe.data.Portion
import com.example.recipe.data.Recipe
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class Endpoints {
    private var apiInterface: ApiInterface = RetrofitInstance.getInstance().create(ApiInterface::class.java)

    suspend fun saveRecipe(
        recipe: RecipeRequest,
        portion: Portion,
        ingredients: List<Ingredient>,
        methods: List<Method>,
        imageBytes: ByteArray? = null
    ) {
        try {
            val endpoints = Endpoints()
            var recipeResponse: Recipe? = null
            var recipeIdToUse = recipe.id

            recipeResponse = endpoints.updateOrCreateRecipe(recipe)
            if (recipeResponse != null) {
                recipeIdToUse = recipeResponse.id
            }

            updateOrCreatePortion(portion, recipeIdToUse)
            endpoints.addOrUpdateIngredients(ingredients, recipeIdToUse)
            endpoints.addOrUpdateMethods(methods, recipeIdToUse)
            if (imageBytes != null) {
                endpoints.addImage(
                    createMultipartFromBytes(imageBytes, recipeIdToUse),
                    recipeIdToUse
                )
            }

            recipeResponse = endpoints.getRecipe(recipeIdToUse)
        } catch (e: Exception) {
            println("Error: ${e.message}")
        }
    }

    suspend fun getRecipes(search: String?, selectedIngredients: String = "", sortKey: String = "", sortDirection: String = ""): List<Recipe>? {
        try {
            val response = apiInterface.getRecipes(search, selectedIngredients, sortKey, sortDirection)
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

    suspend fun getRecipe(recipeId: Int): Recipe? {
        try {
            val response = apiInterface.getRecipe(recipeId)
            if (response.isSuccessful) {
                val data = response.body()
                return data
            } else {
                println("Response error: $response")
                return null
            }
        } catch (e: Exception) {
            println("Exception error getRecipe: ${e.message}. ${e.printStackTrace()}")
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

    suspend fun reorderRecipes(recipes: List<Recipe>): List<Recipe>? {
        try {
            val response = apiInterface.reorderRecipes(recipes)
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

    suspend fun deleteRecipe(recipeId: Int) {
        try {
            val response = apiInterface.deleteRecipe(recipeId)
            if (!response.isSuccessful) {
                println("Response error: $response")
            }
        } catch (e: Exception) {
            println("Exception error getRecipes: ${e.message}. ${e.printStackTrace()}")
        }
    }

    fun createMultipartFromBytes(imageBytes: ByteArray, recipeId: Int): MultipartBody.Part {
        val requestBody = imageBytes.toRequestBody("image/png".toMediaTypeOrNull())
        return MultipartBody.Part.createFormData("image", "recipe-$recipeId.png", requestBody)
    }
}