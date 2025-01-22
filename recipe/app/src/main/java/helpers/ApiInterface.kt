package com.example.recipe.helpers

import data.Ingredient
import data.Method
import data.Portion
import data.Recipe
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

data class RecipeRequest(
    val name: String,
    val url: String,
    val imageUrl: String,
    val createdAt: String
)

data class PortionRequest(
    val value: String,
    val measurement: String,
)

data class MethodRequest(
    val value: String,
    val sortOrder: Number,
)

data class IngredientRequest(
    val name: String,
    val measurement: String,
    val value: String,
)

interface ApiInterface {
    // Recipe endpoints
    @GET("/recipes")
    suspend fun getRecipes(): Response<List<Recipe>>

    @GET("/recipe/{id}")
    suspend fun getRecipe(@Path("id") id: Int): Response<Recipe>

    @POST("/recipe")
    suspend fun createRecipe(@Body request: RecipeRequest): Response<Recipe>

    @PUT("/recipe/{id}")
    suspend fun updateRecipe(@Path("id") id: Int, @Body request: RecipeRequest): Response<Recipe>

    @DELETE("/recipe/{id}")
    suspend fun deleteRecipe(@Path("id") id: Int): Response<Void>

    // Portion endpoints
    @POST("/portion/{recipeId}")
    suspend fun createPortion(@Path("recipeId") id: Int, @Body request: PortionRequest): Response<Portion>

    @DELETE("/portion/{id}")
    suspend fun deletePortion(@Path("id") id: Int): Response<Void>

    // Method endpoints
    @POST("/method/{recipeId}")
    suspend fun createMethod(@Path("recipeId") id: Int, @Body request: MethodRequest): Response<Method>

    @DELETE("/method/{id}")
    suspend fun deleteMethod(@Path("id") id: Int): Response<Void>

    // Ingredient endpoints
    @POST("/ingredient/{recipeId}")
    suspend fun createIngredient(@Path("recipeId") id: Int, @Body request: IngredientRequest): Response<Ingredient>

    @DELETE("/ingredient/{id}")
    suspend fun deleteIngredient(@Path("id") id: Int): Response<Void>
}