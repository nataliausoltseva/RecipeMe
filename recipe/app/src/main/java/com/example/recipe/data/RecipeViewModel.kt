package com.example.recipe.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recipe.helpers.Endpoints
import com.example.recipe.helpers.RecipeRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RecipeViewModel: ViewModel() {
    private val _uiState = MutableStateFlow(State())
    val uiState: StateFlow<State> = _uiState.asStateFlow()

    fun getRecipes(search: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _uiState.update {
                    it.copy(
                        isLoading = true
                    )
                }
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
                endpoints.saveRecipe(recipe, portion, ingredients, methods, imageBytes)
                getRecipes()
            } catch (e: Exception) {}
        }
    }

    fun onSearch(search: String) {
        getRecipes(search)
    }

    fun onFilterOrSort(selectedIngredientNames: List<String>, sortKey: String = "", sortDirection: String = "") {
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
                            availableIngredients = uniqueIngredientNames,
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
                var recipes: List<Recipe>? = endpoints.reorderRecipes(newOrderRecipes)
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

    fun onSplitViewToggle() {
        _uiState.update { currentState ->
            currentState.copy(isTypeSplitView = !currentState.isTypeSplitView)
        }
    }

    fun onDeleteRecipe(recipe: Recipe) {
        viewModelScope.launch {
            try {
                val endpoints = Endpoints()
                endpoints.deleteRecipe(recipe.id)
                var recipes = endpoints.getRecipes("")
                if (recipes == null) {
                    recipes = listOf()
                }
                _uiState.update {
                    it.copy(
                        recipes
                    )
                }
            } catch (e: Exception) {
                println("Error: ${e.message}")
            }
        }
    }

    fun onCookingModeToggle() {
        _uiState.update { currentState ->
            currentState.copy(isCookingModeOn = !currentState.isCookingModeOn)
        }
    }

    init {
        getRecipes()
    }
}

fun byteArrayToBitmap(bytes: ByteArray): Bitmap {
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}