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

    private fun getRecipes(search: String? = null) {
        viewModelScope.launch {
            try {
                val endpoints = Endpoints()
                var recipes: List<Recipe>? = endpoints.getRecipes(search)
                if (recipes == null) {
                    recipes = listOf()
                }
                _uiState.update {
                    it.copy(
                        recipes,
                        isLoading = false
                    )
                }

                getIngredients()
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

                if (recipe.id != 0 && recipeResponse != null) {
                    onSaveRecipe(recipeResponse)
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

    fun onSaveRecipe(recipe: Recipe) {
        _uiState.update {
            it.copy(
                isEditingRecipe = false,
                selectedRecipe = recipe
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

    fun onFilterOrSort(selectedIngredientNames: Array<String>, sortKey: String = "", sortDirection: String = "") {
        val combinedIngredientNames = selectedIngredientNames.joinToString(",")
        viewModelScope.launch {
            try {
                val endpoints = Endpoints()
                var recipes: List<Recipe>? = endpoints.getRecipes("", combinedIngredientNames, sortKey, sortDirection)
                if (recipes == null) {
                    recipes = listOf()
                }

                _uiState.update {
                    it.copy(
                        recipes,
                        isLoading = false,
                        selectedSortDirection = sortDirection,
                        selectedSortKey = sortKey
                    )
                }

            } catch (e: Exception) {
                println("Error: ${e.message}")
            }
        }
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

    fun onReorder() {
        _uiState.update {
            it.copy(
                isReordered = true
            )
        }
    }

    fun onReset() {
        _uiState.update {
            it.copy(
                shouldReset = true,
            )
        }
    }

    fun onSaveReset() {
        _uiState.update {
            it.copy(
                shouldReset = false,
                isReordered = false
            )
        }
    }

    fun onSaveReorder(newOrderRecipes: List<Recipe>) {
        viewModelScope.launch {
            try {
                val endpoints = Endpoints()
                println(newOrderRecipes)
                var recipes: List<Recipe>? = endpoints.reorderRecipes(newOrderRecipes)
                println(recipes)
                if (recipes == null) {
                    recipes = listOf()
                }
                _uiState.update {
                    it.copy(
                        recipes,
                        isLoading = false,
                        shouldReset = false,
                        isReordered = false
                    )
                }
            } catch (e: Exception) {
                println("Error: ${e.message}")
            }
        }
    }

    init {
        getRecipes()
    }
}