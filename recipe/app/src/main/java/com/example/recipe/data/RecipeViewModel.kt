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

class RecipeViewModel: ViewModel() {
    private val _uiState = MutableStateFlow(State())
    val uiState: StateFlow<State> = _uiState.asStateFlow()

    fun reloadRecipes() {
        getRecipes()
    }

    private fun getRecipes() {
        viewModelScope.launch {
            try {
                val endpoints = Endpoints()
                val recipes: List<Recipe>? = endpoints.getRecipes()
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

    fun saveRecipe(recipe: RecipeRequest) {
        viewModelScope.launch {
            try {
                val endpoints = Endpoints()
                endpoints.createRecipe(recipe)
                getRecipes()
                backToListView()
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

    init {
        getRecipes()
    }
}