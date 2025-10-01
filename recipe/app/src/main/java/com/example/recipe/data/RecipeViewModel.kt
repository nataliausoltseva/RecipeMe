package com.example.recipe.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recipe.helpers.DividerIngredientsRequest
import com.example.recipe.helpers.Endpoints
import com.example.recipe.helpers.RecipeParser
import com.example.recipe.helpers.RecipeRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.serialization.json.Json

class RecipeViewModel: ViewModel() {
    private val _uiState = MutableStateFlow(State())
    val uiState: StateFlow<State> = _uiState.asStateFlow()

    private val _plainTextInput = MutableStateFlow("")
    val plainTextInput: StateFlow<String> = _plainTextInput.asStateFlow()

    private val _parsedRecipe = MutableStateFlow<RecipeParserInput?>(null)
    val parsedRecipe: StateFlow<RecipeParserInput?> = _parsedRecipe.asStateFlow()

    private val _jsonOutput = MutableStateFlow<String?>(null)
    val jsonOutput: StateFlow<String?> = _jsonOutput.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isLoadingImport = MutableStateFlow(false)
    val isLoadingImport: StateFlow<Boolean> = _isLoadingImport.asStateFlow()

    private val jsonParser = Json {
        ignoreUnknownKeys = true // Be flexible with extra fields, if any
        isLenient = true         // Allow some minor JSON format deviations if necessary
        prettyPrint = true       // For logging/debugging the JSON output
    }

    fun convertTextToRecipe(title: String, plainText: String, context: Context) {
        viewModelScope.launch {
            // 1. Set loading to true at the very beginning
            _isLoadingImport.value = true
            try {
                _parsedRecipe.value = null
                _jsonOutput.value = null
                _errorMessage.value = null

                val parser = RecipeParser()
                val result = parser.convertTextToRecipeJson(plainText, context)

                result.fold(
                    onSuccess = { jsonString ->
                        _jsonOutput.value = jsonString // Store the raw JSON output
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

                    saveRecipe(
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
                        dividers = null
                    )
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

    private fun getRecipes(search: String? = null) {
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
        imageBytes: ByteArray? = null,
        dividers: List<Divider>?
    ) {
        viewModelScope.launch {
            try {
                val endpoints = Endpoints()
                var recipeResponse: Recipe? = null
                var recipeIdToUse = recipe.id

                recipeResponse = endpoints.updateOrCreateRecipe(recipe)
                if (recipeResponse != null) {
                    recipeIdToUse = recipeResponse.id
                }

                endpoints.updateOrCreatePortion(portion, recipeIdToUse)
                endpoints.addOrUpdateIngredients(ingredients, recipeIdToUse)
                endpoints.addOrUpdateMethods(methods, recipeIdToUse)
                if (imageBytes != null) {
                    endpoints.addImage(
                        createMultipartFromBytes(imageBytes, recipeIdToUse),
                        recipeIdToUse
                    )
                }

                if (dividers != null) {
                    for (divider in dividers) {
                        val dividerResponse = endpoints.addDivider(recipeIdToUse, divider)
                        if (dividerResponse != null) {
                            val dividerIngredients = divider.ingredients.orEmpty()
                            if (dividerIngredients.isNotEmpty()) {
                                endpoints.addIngredientsToDivider(recipeIdToUse, divider.id, dividerIngredients)
                            }
                        }
                    }
                }
                getRecipes()
            } catch (e: Exception) {
                println("Error: ${e.message}")
            }
        }
    }

    fun onSearch(search: String) {
        getRecipes(search)
    }

    fun createMultipartFromBytes(imageBytes: ByteArray, recipeId: Int): MultipartBody.Part {
        val requestBody = imageBytes.toRequestBody("image/png".toMediaTypeOrNull())
        return MultipartBody.Part.createFormData("image", "recipe-$recipeId.png", requestBody)
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

    fun onResetGeminiParsedRecipe() {
        _parsedRecipe.value = null
        _jsonOutput.value = null
        _errorMessage.value = null
    }

    init {
        getRecipes()
    }
}

fun byteArrayToBitmap(bytes: ByteArray): Bitmap {
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}