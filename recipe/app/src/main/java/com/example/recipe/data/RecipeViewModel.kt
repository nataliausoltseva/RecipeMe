package com.example.recipe.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recipe.helpers.Endpoints
import com.example.recipe.helpers.IngredientRequest
import com.example.recipe.helpers.MethodRequest
import com.example.recipe.helpers.PortionRequest
import com.example.recipe.helpers.RecipeRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
                val recipes: List<Recipe>? = endpoints.getRecipes(search)
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
                selectedRecipe = recipe
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
        portion: PortionRequest,
        ingredients: List<IngredientRequest>,
        methods: List<MethodRequest>,
    ) {
        viewModelScope.launch {
            try {
                val endpoints = Endpoints()
                val recipeResponse = endpoints.updateOrCreateRecipe(recipe)
                if (recipeResponse != null) {
                    endpoints.updateOrCreatePortion(portion, recipeResponse.id)
                    endpoints.addOrUpdateIngredients(ingredients, recipeResponse.id)
                    endpoints.addOrUpdateMethods(methods, recipeResponse.id)
                }

                getRecipes()

                if (recipe.id != 0 && recipeResponse != null) {
                    viewRecipe(recipeResponse)
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

    init {
        getRecipes()
    }
}