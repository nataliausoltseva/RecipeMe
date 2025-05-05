package com.example.recipe.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recipe.helpers.Endpoints
import com.example.recipe.helpers.RecipeRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class RecipeViewModel: ViewModel() {
    private val _uiState = MutableStateFlow(State())
    val uiState: StateFlow<State> = _uiState.asStateFlow()

    fun reloadRecipes() {
        getRecipes()
    }

    private fun getRecipes(search: String? = null) {
        viewModelScope.launch {
            try {
                val endpoints = Endpoints()
                val recipes: List<Recipe>? = endpoints.getRecipes(search, "")
                if (recipes != null) {
                    _uiState.update {
                        it.copy(
                            recipes,
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                println("Error: ${e.message}")
            }
        }
    }

    fun viewRecipe(recipe: Recipe) {
        _uiState.update {
            it.copy(
                isFullScreen = true,
                selectedRecipe = recipe,
                isEditingRecipe = false
            )
        }
    }

    fun createRecipe() {
        _uiState.update {
            it.copy(
                isFullScreen = true,
            )
        }
    }

    fun saveRecipe(
        recipe: RecipeRequest,
        portion: Portion,
        ingredients: List<Ingredient>,
        methods: List<Method>,
        imageBytes: ByteArray? = null
    ) {
        viewModelScope.launch {
            try {
                val endpoints = Endpoints()
                val recipeResponse = endpoints.updateOrCreateRecipe(recipe)
                if (recipeResponse != null) {
                    endpoints.updateOrCreatePortion(portion, recipeResponse.id)
                    endpoints.addOrUpdateIngredients(ingredients, recipeResponse.id)
                    endpoints.addOrUpdateMethods(methods, recipeResponse.id)
                    if (imageBytes != null) {
                        endpoints.addImage(
                            createMultipartFromBytes(imageBytes, recipeResponse.id),
                            recipeResponse.id
                        )
                    }
                }

                getRecipes()
                getIngredients()

                if (recipe.id != 0 && recipeResponse != null) {
                    onSaveRecipe()
                } else {
                    backToListView()
                }
            } catch (e: Exception) {
                println("Error: ${e.message}")
            }
        }
    }

    fun backToListView() {
        _uiState.update {
            it.copy(
                isFullScreen = false,
                selectedRecipe = null
            )
        }
    }

    fun onEditRecipe() {
        _uiState.update {
            it.copy(
                isEditingRecipe = true
            )
        }
    }

    fun onSaveRecipe() {
        _uiState.update {
            it.copy(
                isEditingRecipe = false,
            )
        }
    }

    fun onSearch(search: String) {
        getRecipes(search)
    }

    fun createMultipartFromBytes(imageBytes: ByteArray, recipeId: Int): MultipartBody.Part {
        val requestBody = imageBytes.toRequestBody("image/png".toMediaTypeOrNull())
        return MultipartBody.Part.createFormData("image", "recipe-$recipeId.png", requestBody)
    }

    fun onFilterOrSort(selectedIngredientNames: Array<String>, sortKey: String) {
        if (selectedIngredientNames.isNotEmpty()) {
            val combinedIngredientNames = selectedIngredientNames.joinToString(",")
            viewModelScope.launch {
                try {
                    val endpoints = Endpoints()
                    val recipes: List<Recipe>? = endpoints.getRecipes("", combinedIngredientNames)
                    if (recipes != null) {
                        _uiState.update {
                            it.copy(
                                recipes,
                                isLoading = false
                            )
                        }
                    }
                } catch (e: Exception) {
                    println("Error: ${e.message}")
                }
            }
        }
        // TODO: check if selected ingredient names are different to saved
        // TODO: check if sortkey is diff
        // TODO: based on what is different - do api call
        // TODO: if nothing is different, do not make api call
    }

    fun getIngredients() {
        viewModelScope.launch {
            try {
                val endpoints = Endpoints()
                val ingredients: List<Ingredient>? = endpoints.getIngredients()
                if (ingredients != null) {
                    val uniqueIngredientNames = ingredients.map { it.name }.distinct()
                    _uiState.update {
                        it.copy(
                            availableIngredients = uniqueIngredientNames.toTypedArray(),
                        )
                    }
                }
            } catch (e: Exception) {
                println("Error: ${e.message}")
            }
        }
    }

    init {
        getRecipes()
        getIngredients()
    }
}