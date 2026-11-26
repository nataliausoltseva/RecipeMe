package com.example.recipe.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
        coerceInputValues = true // Coerce nulls or invalid types to defaults
    }

    fun convertImageToRecipe(title: String, bitmap: Bitmap, context: Context) {
        viewModelScope.launch {
            _isLoadingImport.value = true
            try {
                _parsedRecipe.value = null
                _jsonOutput.value = null
                _errorMessage.value = null

                val parser = RecipeParser()
                val result = parser.convertImageToRecipeJson(bitmap, context)

                val outputStream = java.io.ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, outputStream)
                val imageBytes = outputStream.toByteArray()

                handleParserResult(title, result, imageBytes)
            } catch (e: Exception) {
                _errorMessage.value = "Error reading recipe from image: ${e.message}"
            } finally {
                _isLoadingImport.value = false
            }
        }
    }

    fun convertTextToRecipe(title: String, plainText: String, context: Context) {
        viewModelScope.launch {
            _isLoadingImport.value = true
            try {
                _parsedRecipe.value = null
                _jsonOutput.value = null
                _errorMessage.value = null

                val parser = RecipeParser()
                val result = parser.convertTextToRecipeJson(plainText, context)

                handleParserResult(title, result, null)
            } catch (e: Exception) {
                _errorMessage.value = "There was an error with processing this recipe. Try again later."
            } finally {
                _isLoadingImport.value = false
            }
        }
    }

    private fun handleParserResult(userTitle: String, result: Result<String>, imageBytes: ByteArray? = null) {
        result.fold(
            onSuccess = { jsonString ->
                _jsonOutput.value = jsonString
                try {
                    if (jsonString.isNotBlank()) {
                        val recipe = jsonParser.decodeFromString<RecipeParserInput>(jsonString.trim())
                        _parsedRecipe.value = recipe
                        val recipeTitle = userTitle.ifBlank { recipe.title ?: "" }.ifBlank { "Imported Recipe" }
                        saveParsedRecipe(recipeTitle, recipe, imageBytes)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    _errorMessage.value = "Failed to parse recipe data. Unexpected format received."
                }
            },
            onFailure = { exception ->
                _errorMessage.value = exception.message ?: "Error converting recipe."
            }
        )
    }

    private fun saveParsedRecipe(title: String, parsed: RecipeParserInput, imageBytes: ByteArray? = null) {
        val ingredients = parsed.ingredients.orEmpty().map { ingredientInput ->
            Ingredient(
                name = ingredientInput.name ?: "",
                measurement = ingredientInput.measurement ?: "",
                value = ingredientInput.value ?: 1f,
                id = 0,
                sortOrder = 0,
            )
        }

        val methods = parsed.methods.orEmpty().map { methodInput ->
            Method(
                value = methodInput.value ?: "",
                id = 0,
                sortOrder = 0,
            )
        }

        val dividers = parsed.dividers.orEmpty().map { dividerInput ->
            val dividerIngredients = dividerInput.ingredients.orEmpty().map { dividerIngredientInput ->
                Ingredient(
                    name = dividerIngredientInput.name ?: "",
                    measurement = dividerIngredientInput.measurement ?: "",
                    value = dividerIngredientInput.value ?: 1f,
                    id = 0,
                    sortOrder = 0,
                )
            }

            Divider(
                id = 0,
                title = dividerInput.title ?: "",
                sortOrder = 0,
                ingredients = dividerIngredients,
                recipeId = 0
            )
        }

        val equipments = parsed.equipments.orEmpty().mapIndexed { idx, eqInput ->
            Equipment(
                id = 0,
                name = eqInput.name ?: "",
                sortOrder = idx
            )
        }

        saveRecipe(
            RecipeRequest(
                id = 0,
                name = title.ifBlank { "Imported Recipe" },
                type = "",
                url = parsed.url ?: ""
            ),
            Portion(
                id = 0,
                value = parsed.portion?.value ?: 1f,
                measurement = "portion"
            ),
            ingredients,
            methods,
            imageBytes,
            dividers,
            equipments
        )
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
        dividers: List<Divider>?,
        equipments: List<Equipment>? = null
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
                if (!equipments.isNullOrEmpty()) {
                    endpoints.addOrUpdateEquipment(equipments, recipeIdToUse)
                }
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
                    if (dividers.isEmpty()) {
                        endpoints.deleteDividers(recipeIdToUse)
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

    fun onResetParsedRecipe() {
        _parsedRecipe.value = null
        _jsonOutput.value = null
        _errorMessage.value = null
    }

    init {
        getRecipes()
    }
}

fun byteArrayToBitmap(bytes: ByteArray): Bitmap {
    return com.example.recipe.helpers.decodeByteArraySafely(bytes, 1024) ?: Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
}