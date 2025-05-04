package com.example.recipe.helpers

import com.example.recipe.data.Ingredient
import com.example.recipe.data.Method
import com.example.recipe.data.Portion
import com.example.recipe.data.Recipe
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

data class RecipeRequest(
    var id: Int,
    var name: String,
)

interface ApiInterface {
    // Recipe endpoints
    @GET("/recipes")
    suspend fun getRecipes(@Query("search") search: String? = null): Response<List<Recipe>>

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
    suspend fun addOrUpdatePortion(@Path("recipeId") id: Int, @Body request: Portion): Response<Portion>

    @DELETE("/portion/{id}")
    suspend fun deletePortion(@Path("id") id: Int): Response<Void>

    // Ingredient endpoints
    @POST("/ingredients/{recipeId}")
    suspend fun addOrUpdateIngredients(@Path("recipeId") id: Int, @Body request: List<Ingredient>): Response<Recipe>

    @DELETE("/ingredient/{id}")
    suspend fun deleteIngredient(@Path("id") id: Int): Response<Void>

    @GET("/ingredients")
    suspend fun getIngredients(): Response<List<Ingredient>>

    // Methods endpoints
    @POST("/methods/{recipeId}")
    suspend fun addOrUpdateMethods(@Path("recipeId") id: Int, @Body request: List<Method>): Response<Recipe>

    @DELETE("/method/{id}")
    suspend fun deleteMethod(@Path("id") id: Int): Response<Void>

    // Image endpoints
    @Multipart
    @POST("/image/{recipeId}")
    suspend fun uploadImage(@Path("recipeId") id: Int, @Part image: MultipartBody.Part): Response<Recipe>
}