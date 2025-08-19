package com.example.recipe.data

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recipe.helpers.Endpoints
import com.example.recipe.helpers.RecipeParser
import com.example.recipe.helpers.RecipeRequest
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class GeminiPromptModel : ViewModel() {
    private val _parsedRecipe = MutableStateFlow<RecipeParserInput?>(null)
    val parsedRecipe: StateFlow<RecipeParserInput?> = _parsedRecipe.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isLoadingImport = MutableStateFlow(false)
    val isLoadingImport: StateFlow<Boolean> = _isLoadingImport.asStateFlow()

    private val jsonParser = Json {
        ignoreUnknownKeys = true // Be flexible with extra fields, if any
        isLenient = true         // Allow some minor JSON format deviations if necessary
        prettyPrint = true       // For logging/debugging the JSON output
    }

    val promptCompleted = MutableSharedFlow<Unit>()

    fun convertTextToRecipe(title: String, plainText: String, context: Context) {
        viewModelScope.launch {
            // 1. Set loading to true at the very beginning
            _isLoadingImport.value = true
            try {
                _parsedRecipe.value = null
                _errorMessage.value = null

                val parser = RecipeParser()
                val result = parser.convertTextToRecipeJson(plainText, context)

                result.fold(
                    onSuccess = { jsonString ->
                        try {
                            if (jsonString != "") {
                                val recipe = jsonParser.decodeFromString<RecipeParserInput>(jsonString.trim())
                                _parsedRecipe.value = recipe
                            }
                        } catch (e: Exception) {
                            _errorMessage.value = "Failed to parse recipe from text. The model might have returned an unexpected format. Raw output: $jsonString"
                        }
                    },
                    onFailure = { exception ->
                        _errorMessage.value = "Error converting text: ${exception.message}"
                    }
                )
                if (parsedRecipe.value != null) {
                    val ingredients = mutableListOf<Ingredient>()

                    for (ingredientInput in parsedRecipe.value?.ingredients ?: listOf()) {
                        val ingredient = Ingredient(
                            name = ingredientInput.name ?: "",
                            measurement = ingredientInput.measurement ?: "",
                            value = ingredientInput.value ?: 1f,
                            id = 0,
                            sortOrder = 0,
                        )
                        ingredients.add(ingredient)
                    }

                    val methods = mutableListOf<Method>()

                    for (methodInput in parsedRecipe.value?.methods ?: listOf()) {
                        val method = Method(
                            value = methodInput.value ?: "",
                            id = 0,
                            sortOrder = 0,
                        )
                        methods.add(method)
                    }
                    try {
                        val endpoints = Endpoints()
                        endpoints.saveRecipe(
                            RecipeRequest(
                                id = 0,
                                name = title,
                                type = "",
                                url = parsedRecipe.value?.url ?: ""
                            ),
                            Portion(
                                id = 0,
                                value = parsedRecipe.value!!.portion?.value ?: 1f,
                                measurement = "portion"
                            ),
                            ingredients,
                            methods,
                            null
                        )
                        promptCompleted.emit(Unit)
                    } catch (e: Exception) {
                        println(e.message)
                    }
                }
            } catch (e: Exception) {
                println("-------------ERROR MESSAGE-----------")
                println(e.message)
                println("--------------------------------------")
                _errorMessage.value = "There was an error with processing this recipe. Try again later."
            } finally {
                _isLoadingImport.value = false
            }
        }
    }

    fun onResetGeminiParsedRecipe() {
        _parsedRecipe.value = null
        _errorMessage.value = null
    }
}