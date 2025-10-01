package com.example.recipe.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.recipe.data.byteArrayToBitmap
import com.example.recipe.data.Divider
import com.example.recipe.data.Image
import com.example.recipe.data.Ingredient
import com.example.recipe.data.Method
import com.example.recipe.data.Portion
import com.example.recipe.data.RecipeViewModel
import com.example.recipe.helpers.RecipeRequest
import com.example.recipe.helpers.getResizedBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import sh.calvin.reorderable.ReorderableColumn
import java.io.ByteArrayOutputStream

@Composable
fun RecipeModifyScreen(
    recipeId: String?,
    recipeViewModel: RecipeViewModel,
    onNavigateBack: () -> Unit,
) {
    val recipesUIState by recipeViewModel.uiState.collectAsState()

    val recipe = recipesUIState.recipes.find { it.id.toString() == recipeId }

    var name by remember { mutableStateOf(recipe?.name ?: "") }
    val decodedBytes = Base64.decode(recipe?.image?.url ?: "", Base64.DEFAULT)
    var imageBytes by remember { mutableStateOf(if (recipe?.image?.url != null) decodedBytes else null) }
    var isExpandedTypeSelector by remember { mutableStateOf(false) }
    var typeSelection by remember { mutableStateOf((if(recipe?.type != "") recipe?.type else "Choose") ?: "Choose") }
    var recipeExternalUrl by remember { mutableStateOf(recipe?.url ?: "") }

    // portion handlers
    var isExpandedPortionSelector by remember { mutableStateOf(false) }
    var portionSelection by remember { mutableStateOf(recipe?.portion?.measurement ?: "day") }
    var placeholderPortionValue by remember {
        mutableStateOf(
            recipe?.portion?.value?.toString() ?: "1"
        )
    }

    // ingredients handlers
    var ingredients = remember { mutableStateOf<List<Ingredient>>(recipe?.ingredients ?: listOf()) }
    var showIngredientModal by remember { mutableStateOf(false) }
    val selectedIngredient = remember { mutableStateOf<Ingredient?>(null) }
    val selectedIngredientIndex = remember { mutableIntStateOf(0) }

    // methods handlers
    var methods = remember { mutableStateOf<List<Method>>(recipe?.methods ?: listOf()) }
    var showMethodModal by remember { mutableStateOf(false) }
    val selectedMethod = remember { mutableStateOf<Method?>(null) }
    val selectedMethodIndex = remember { mutableIntStateOf(0) }

    // Divider handlers
    var dividers = remember { mutableStateOf<List<Divider>>(recipe?.dividers ?: listOf()) }
    var showDividerModal by remember { mutableStateOf(false) }
    val selectedDivider = remember { mutableStateOf<Divider?>(null) }
    val selectedDividerIndex = remember { mutableIntStateOf(0) }

    // State for LinkIngredientsDialog
    var showLinkIngredientsDialog by remember { mutableStateOf(false) }
    var methodToLinkIngredients by remember { mutableStateOf<Method?>(null) }
    var methodToLinkIngredientsIndex by remember { mutableIntStateOf(-1) }


    BackHandler(enabled = true, onBack = {  onNavigateBack() })
    Column(
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Go back",
                modifier = Modifier
                    .clickable { onNavigateBack() }
                    .size(50.dp, 50.dp)
            )
            Button(
                onClick = {
                    recipeViewModel.saveRecipe(
                        RecipeRequest(
                            id = recipe?.id ?: 0,
                            name = name,
                            type = if (typeSelection == "Choose") "" else typeSelection,
                            url = recipeExternalUrl
                        ),
                        Portion(
                            id = recipe?.portion?.id ?: 0,
                            value = placeholderPortionValue.toFloatOrNull() ?: 1f,
                            measurement = if (portionSelection == "Choose") "days" else portionSelection
                        ),
                        ingredients.value,
                        methods.value,
                        imageBytes,
                        dividers.value
                    )
                    onNavigateBack()
                }
            ) {
                Text("Save")
            }
        }

        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(10.dp, 0.dp),
        ) {
            ImageUploader(
                onImageUpload = { imageBytes = it },
                image = recipe?.image
            )
            Row {
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                    ),
                    modifier = Modifier.fillMaxWidth(0.6F)
                )
                Column(
                    modifier = Modifier.padding(start = 20.dp)
                ) {
                    TextField(
                        value = placeholderPortionValue,
                        onValueChange = {
                            val filtered = it.filter { char -> char.isDigit() || char == '.' }
                            if (filtered.count { char -> char == '.' } <= 1) {
                                placeholderPortionValue = filtered
                            }
                        },
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                        ),
                    )

                    DropdownMenuItem(
                        text = { Text(portionSelection) },
                        onClick = { isExpandedPortionSelector = !isExpandedPortionSelector }
                    )
                    DropdownMenu(
                        expanded = isExpandedPortionSelector,
                        onDismissRequest = { isExpandedPortionSelector = !isExpandedPortionSelector }
                    ) {
                        for (portion in arrayOf("day", "portion", "gram", "kg", "bar", "item", "mL", "L")) {
                            DropdownMenuItem(
                                modifier = Modifier
                                    .background(if (portion == portionSelection) Color.LightGray else Color.Transparent),
                                text = { Text(portion) },
                                onClick = {
                                    portionSelection = portion
                                    isExpandedPortionSelector = false
                                },
                            )
                        }
                    }
                }
            }

            TextField(
                label = { Text("Original recipe URL:") },
                value = recipeExternalUrl,
                onValueChange = { recipeExternalUrl = it },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                ),
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Type: ")
                DropdownMenuItem(
                    text = { Text(typeSelection) },
                    onClick = { isExpandedTypeSelector = !isExpandedTypeSelector }
                )
                DropdownMenu(
                    expanded = isExpandedTypeSelector,
                    onDismissRequest = { isExpandedTypeSelector = !isExpandedTypeSelector }
                ) {
                    for (type in arrayOf("breakfast", "lunch", "dinner", "dessert", "snack")) {
                        DropdownMenuItem(
                            modifier = Modifier
                                .background(if (type == typeSelection) Color.LightGray else Color.Transparent),
                            text = { Text(type) },
                            onClick = {
                                typeSelection = type
                                isExpandedTypeSelector = false
                            },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))


            // --- Dividers Section ---
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 20.dp)
            ) {
                Text("Recipe Sections / Dividers")
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Add Divider",
                    modifier = Modifier
                        .padding(start = 20.dp)
                        .size(20.dp)
                        .clickable {
                            selectedDivider.value = null
                            showDividerModal = true
                        }
                )
            }

            ReorderableColumn(
                list = dividers.value,
                onSettle = { fromIndex, toIndex ->
                    dividers.value = dividers.value.toMutableList().apply {
                        add(toIndex, removeAt(fromIndex))
                    }.mapIndexed { index, divider -> divider.copy(sortOrder = index) }
                }
            ) { index, divider, isDragging ->
                key(divider.id.toString() + divider.title + index) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                divider.title ?: "",
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        selectedDivider.value = divider
                                        selectedDividerIndex.intValue = index
                                        showDividerModal = true
                                    }
                                    .padding(end = 8.dp)
                            )
                            Row {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Divider",
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clickable {
                                            dividers.value = dividers.value.toMutableList().apply {
                                                removeAt(index)
                                            }.mapIndexed { idx, d -> d.copy(sortOrder = idx) }
                                        }
                                )
                                Icon(
                                    imageVector = Icons.Default.DragHandle,
                                    contentDescription = "Reorder Divider",
                                    modifier = Modifier
                                        .padding(start = 15.dp)
                                        .draggableHandle(),
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 20.dp)
                        ) {
                            Text("Ingredients")
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = "Add Ingredient to Divider",
                                modifier = Modifier
                                    .padding(start = 20.dp)
                                    .size(20.dp)
                                    .clickable {
                                        showIngredientModal = true
                                        selectedIngredient.value = null
                                        selectedDivider.value = divider
                                        selectedDividerIndex.intValue = index
                                    }
                            )
                        }

                        // Reorderable divider ingredients
                        val dividerIngredients = divider.ingredients.orEmpty()
                        if (dividerIngredients.isNotEmpty()) {
                            ReorderableColumn(
                                list = dividerIngredients,
                                onSettle = { fromIndex, toIndex ->
                                    val updatedDividers = dividers.value.toMutableList()
                                    val updatedIngredients = dividerIngredients.toMutableList().apply {
                                        add(toIndex, removeAt(fromIndex))
                                    }
                                    updatedDividers[index] = divider.copy(ingredients = updatedIngredients)
                                    dividers.value = updatedDividers
                                }
                            ) { ingIndex, ingredient, isDragging ->
                                key(ingredient.name + ingIndex) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth().padding(start = 40.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(ingredient.name)
                                            Text(" - ")
                                            Text(ingredient.value.toString())
                                            Text(" ")
                                            Text(ingredient.measurement)
                                        }
                                        Row {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete ingredient from divider",
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .clickable {
                                                        val updatedDividers = dividers.value.toMutableList()
                                                        val updatedIngredients = dividerIngredients.toMutableList().apply {
                                                            remove(ingredient)
                                                        }
                                                        updatedDividers[index] = divider.copy(ingredients = updatedIngredients)
                                                        dividers.value = updatedDividers
                                                    }
                                            )
                                            Icon(
                                                imageVector = Icons.Default.DragHandle,
                                                contentDescription = "Reorder divider ingredient",
                                                modifier = Modifier
                                                    .padding(start = 15.dp)
                                                    .draggableHandle(),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (showDividerModal) {
                AddOrEditDividerDialog(
                    divider = selectedDivider.value,
                    onConfirmation = { confirmedDivider ->
                        val currentDividers = dividers.value.toMutableList()
                        if (selectedDivider.value != null) { // Editing existing
                            currentDividers[selectedDividerIndex.intValue] = confirmedDivider.copy(
                                id = selectedDivider.value!!.id,
                                recipeId = selectedDivider.value!!.recipeId,
                                sortOrder = selectedDivider.value!!.sortOrder,
                                ingredients = selectedDivider.value!!.ingredients,
                                methods = selectedDivider.value!!.methods
                            )
                        } else { // Adding new
                            currentDividers.add(confirmedDivider.copy(
                                id = (dividers.value.maxOfOrNull { it.id } ?: 0) + 1, // Simple local ID
                                recipeId = recipe?.id ?: 0,
                                sortOrder = dividers.value.size
                            ))
                        }
                        dividers.value = currentDividers.mapIndexed { index, d -> d.copy(sortOrder = index) }
                        selectedDivider.value = null
                        showDividerModal = false
                    },
                    onDismissRequest = {
                        selectedDivider.value = null
                        showDividerModal = false
                    },
                    dialogTitle = if (selectedDivider.value != null) "Edit Section Name" else "New Section Name"
                )
            }


            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 20.dp)
            ) {
                Text("Ingredients")
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Add Icon",
                    modifier = Modifier
                        .padding(start = 20.dp)
                        .size(20.dp)
                        .clickable {
                            showIngredientModal = true
                            selectedIngredient.value = null
                            selectedDivider.value = null // Add to main list, not divider
                        }
                )
            }
            // Exclude ingredients that are part of any divider
            val dividerIngredientNames = dividers.value.flatMap { it.ingredients.orEmpty() }.map { it.name }.toSet()
            val mainIngredients = ingredients.value.filter { it.name !in dividerIngredientNames }
            ReorderableColumn(
                list = mainIngredients,
                onSettle = { fromIndex, toIndex ->
                    ingredients.value = ingredients.value.toMutableList().apply {
                        add(toIndex, removeAt(fromIndex))
                    }
                }
            ) { index, ingredient, isDragging ->
                key(ingredient.name + index) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.clickable {
                                showIngredientModal = true
                                selectedIngredient.value = ingredient
                                selectedIngredientIndex.intValue = index
                                selectedDivider.value = null // Editing main ingredient
                            }
                        ) {
                            Text(ingredient.name)
                            Text(" - ")
                            Text(ingredient.value.toString())
                            Text(" ")
                            Text(ingredient.measurement)
                        }
                        Row {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete ingredient button",
                                modifier = Modifier
                                    .size(24.dp)
                                    .clickable {
                                        ingredients.value = ingredients.value
                                            .toMutableList()
                                            .apply {
                                                remove(ingredient)
                                            }
                                        methods.value = methods.value.map { method ->
                                            val updatedLinkedIngredients = method.ingredients?.filter { it.name != ingredient.name }
                                            method.copy(ingredients = updatedLinkedIngredients)
                                        }
                                    }
                            )
                            Icon(
                                imageVector = Icons.Default.DragHandle,
                                contentDescription = "Reorder ingredient button",
                                modifier = Modifier
                                    .padding(start = 15.dp)
                                    .draggableHandle(),
                            )
                        }
                    }
                }
            }


            if (showIngredientModal) {
                AddOrEditIngredient(
                    ingredient = selectedIngredient.value,
                    onConfirmation = { confirmedIngredient ->
                        val oldIngredientName = selectedIngredient.value?.name
                        if (selectedDivider.value != null) {
                            // Add to divider
                            val dividerIdx = selectedDividerIndex.intValue
                            val updatedDividers = dividers.value.toMutableList()
                            if (selectedIngredient.value != null) {
                                // Edit existing ingredient in divider
                                val updatedIngredients = updatedDividers[dividerIdx].ingredients?.toMutableList().apply {
                                    this?.set(selectedIngredientIndex.intValue, confirmedIngredient)
                                }
                                updatedDividers[dividerIdx] = updatedDividers[dividerIdx].copy(ingredients = updatedIngredients)
                            } else {
                                // Add new ingredient to divider
                                val currentIngredients = updatedDividers[dividerIdx].ingredients ?: emptyList()
                                val updatedIngredients = currentIngredients.toMutableList().apply {
                                    add(confirmedIngredient)
                                }
                                updatedDividers[dividerIdx] = updatedDividers[dividerIdx].copy(ingredients = updatedIngredients)
                            }
                            dividers.value = updatedDividers
                        } else {
                            // Add to main ingredient list
                            if (selectedIngredient.value != null) {
                                ingredients.value = ingredients.value.toMutableList().apply {
                                    this[selectedIngredientIndex.intValue] = confirmedIngredient
                                }
                                if (oldIngredientName != null && oldIngredientName != confirmedIngredient.name) {
                                    methods.value = methods.value.map { method ->
                                        val updatedLinkedIngredients = method.ingredients?.map {
                                            if (it.name == oldIngredientName) confirmedIngredient else it
                                        }
                                        method.copy(ingredients = updatedLinkedIngredients)
                                    }
                                }
                            } else {
                                ingredients.value = ingredients.value.toMutableList().apply {
                                    add(confirmedIngredient)
                                }
                            }
                        }
                        selectedIngredient.value = null
                        showIngredientModal = false
                        selectedDivider.value = null
                    },
                    onDismissRequest = {
                        showIngredientModal = false
                        selectedDivider.value = null
                    },
                    dialogTitle = if (selectedIngredient.value != null) "Edit ingredient" else "New ingredient"
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 20.dp)
            ) {
                Text("Methods")
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Add Icon",
                    modifier = Modifier
                        .padding(start = 20.dp)
                        .size(20.dp)
                        .clickable {
                            showMethodModal = true
                            selectedMethod.value = null
                        }
                )
            }

            ReorderableColumn(
                list = methods.value,
                onSettle = { fromIndex, toIndex ->
                    methods.value = methods.value.toMutableList().apply {
                        add(toIndex, removeAt(fromIndex))
                    }
                }
            ) { index, method, isDragging ->
                key(method.id.toString() + index) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val methodDisplayNumber = (method.sortOrder ?: (index + 1)).toString()
                            Text(
                                "$methodDisplayNumber. ${method.value}",
                                modifier = Modifier
                                    .clickable {
                                        showMethodModal = true
                                        selectedMethod.value = method
                                        selectedMethodIndex.intValue = index
                                    }
                                    .weight(1f)
                                    .padding(end = 8.dp)
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Link,
                                    contentDescription = "Link Ingredients",
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clickable {
                                            methodToLinkIngredients = method
                                            methodToLinkIngredientsIndex = index
                                            showLinkIngredientsDialog = true
                                        }
                                        .padding(end = 10.dp)
                                )
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete method button",
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clickable {
                                            methods.value = methods.value
                                                .toMutableList()
                                                .apply {
                                                    remove(method)
                                                }
                                        }
                                )
                                Icon(
                                    imageVector = Icons.Default.DragHandle,
                                    contentDescription = "Reorder method button",
                                    modifier = Modifier
                                        .padding(start = 10.dp)
                                        .draggableHandle(),
                                )
                            }
                        }
                        if (method.ingredients?.isNotEmpty() == true) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 24.dp, end = 8.dp, bottom = 4.dp)
                            ) {
                                method.ingredients?.forEach { linkedIngredient ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "• ${linkedIngredient.name}" + (if (linkedIngredient.value.toFloat() >  0) " (${linkedIngredient.value} ${linkedIngredient.measurement})" else ""),
                                            modifier = Modifier.weight(1f).padding(end = 4.dp)
                                        )
                                        Icon(
                                            imageVector = Icons.Filled.LinkOff,
                                            contentDescription = "Unlink ${linkedIngredient.name}",
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clickable {
                                                    val currentMethods = methods.value.toMutableList()
                                                    val methodToUpdate = currentMethods[index]
                                                    val updatedIngredientsForMethod = methodToUpdate.ingredients?.filterNot { it.name == linkedIngredient.name }

                                                    currentMethods[index] = methodToUpdate.copy(ingredients = updatedIngredientsForMethod)
                                                    methods.value = currentMethods
                                                }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (showMethodModal) {
                AddOrEditMethodStep(
                    method = selectedMethod.value,
                    onConfirmation = { confirmedMethod ->
                        if (selectedMethod.value != null) {
                            methods.value = methods.value.toMutableList().apply {
                                val originalMethod = this[selectedMethodIndex.intValue]
                                this[selectedMethodIndex.intValue] = confirmedMethod.copy(ingredients = originalMethod.ingredients)
                            }
                        } else {
                            val newMethodWithOrder = confirmedMethod.copy(
                                sortOrder = methods.value.size + 1,
                                ingredients = emptyList()
                            )
                            methods.value = methods.value.toMutableList().apply {
                                add(newMethodWithOrder)
                            }
                        }
                        selectedMethod.value = null
                        showMethodModal = false
                    },
                    onDismissRequest = { showMethodModal = false },
                    dialogTitle = if (selectedMethod.value != null) "Edit method step" else "New method step"
                )
            }

            if (showLinkIngredientsDialog && methodToLinkIngredients != null) {
                LinkIngredientsDialog(
                    allIngredients = ingredients.value,
                    currentlyLinkedIngredientNames = methodToLinkIngredients?.ingredients?.map { it.name } ?: emptyList(),
                    onDismissRequest = {
                        showLinkIngredientsDialog = false
                        methodToLinkIngredients = null
                        methodToLinkIngredientsIndex = -1
                                     },
                    onConfirm = { updatedLinkedIngredientNames ->
                        if (methodToLinkIngredientsIndex != -1) {
                            val updatedLinkedIngredients = ingredients.value.filter { ingredient ->
                                ingredient.name in updatedLinkedIngredientNames
                            }
                            methods.value = methods.value.toMutableList().apply {
                                val currentMethod = this[methodToLinkIngredientsIndex]
                                this[methodToLinkIngredientsIndex] = currentMethod.copy(ingredients = updatedLinkedIngredients)
                            }
                        }
                        showLinkIngredientsDialog = false
                        methodToLinkIngredients = null
                        methodToLinkIngredientsIndex = -1
                    }
                )
            }
        }
    }
}

@Composable
fun ImageUploader(
    onImageUpload: (byteArray: ByteArray) -> Unit,
    image: Image?
) {
    val context = LocalContext.current
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var bitmapState by remember { mutableStateOf<Bitmap?>(null) }
    val scope = rememberCoroutineScope()

    val clipboardManager = LocalClipboardManager.current

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            uri?.let {
                imageUri = it
                scope.launch(Dispatchers.IO) {
                    val resizedBitmap = getResizedBitmap(context, it, 800, 800)
                    resizedBitmap?.let { bmp ->
                        val outputStream = ByteArrayOutputStream()
                        bmp.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                        withContext(Dispatchers.Main) {
                            bitmapState = bmp
                            onImageUpload(outputStream.toByteArray())
                        }
                    }
                }
            }
        }
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            when {
                bitmapState != null -> {
                    Image(
                        bitmap = bitmapState!!.asImageBitmap(),
                        contentDescription = "Selected image",
                        modifier = Modifier.fillMaxSize()
                    )
                }
                image?.url != null -> {
                    val decodedBytes = Base64.decode(image.url, Base64.DEFAULT)
                    val bitmap = byteArrayToBitmap(decodedBytes)
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Existing image",
                        modifier = Modifier.fillMaxSize()
                    )
                }
                else -> {
                    Text("No image selected")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    pickImageLauncher.launch("image/*")
                }
            ) {
                Text("Upload Image")
            }

            Button(
                onClick = {
                    val clipData = clipboardManager.getClip()
                    clipData?.let {
                        if (it.clipData.itemCount > 0) {
                            val uri = it.clipData.getItemAt(0).uri
                            uri?.let { imageUriValue -> // Renamed to avoid conflict
                                val inputStream = context.contentResolver.openInputStream(imageUriValue)
                                bitmapState = BitmapFactory.decodeStream(inputStream)
                                val outputStream = ByteArrayOutputStream()
                                bitmapState?.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                                onImageUpload(outputStream.toByteArray())
                            }
                        }
                    }
                }
            ) {
                Text("Paste Image")
            }
        }
    }
}


@Composable
fun AddOrEditIngredient(
    ingredient: Ingredient?,
    onDismissRequest: () -> Unit,
    onConfirmation: (ingredient: Ingredient) -> Unit,
    dialogTitle: String,
) {
    var name by remember(ingredient) { mutableStateOf(ingredient?.name ?: "") }
    var amount by remember(ingredient) { mutableStateOf(ingredient?.value?.toString() ?: "1") }
    var measurement by remember(ingredient) { mutableStateOf(ingredient?.measurement ?: "item") }
    var isExpandedIngredientPortionSelector by remember { mutableStateOf(false) }

    AlertDialog(
        title = { Text(text = dialogTitle) },
        text = {
            Column {
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Ingredient Name") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                    ),
                )
                Row (
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Box(
                        Modifier.width(100.dp)
                    ) {
                        TextField(
                            value = amount,
                            onValueChange = { currentAmount ->
                                val filtered = currentAmount.filter { it.isDigit() || it == '.' }
                                if (filtered.count { it == '.' } <= 1) {
                                    amount = filtered
                                }
                            },
                            label = { Text("Amount") },
                            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                            ),
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(measurement) },
                        onClick = { isExpandedIngredientPortionSelector = !isExpandedIngredientPortionSelector }
                    )
                    DropdownMenu(
                        expanded = isExpandedIngredientPortionSelector,
                        onDismissRequest = { isExpandedIngredientPortionSelector = false }
                    ) {
                        for (portionOption in arrayOf(
                            "bottle", "can", "item", "g", "kg", "mL", "L", "tbsp", "tsp", "cup", "to taste"
                        )) {
                            DropdownMenuItem(
                                text = { Text(portionOption) },
                                onClick = {
                                    measurement = portionOption
                                    isExpandedIngredientPortionSelector = false
                                }
                            )
                        }
                    }
                }
            }
        },
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                onClick = {
                    val finalAmount = amount.toFloatOrNull() ?: 1f
                    val confirmedIngredient = Ingredient(
                        id = ingredient?.id ?: 0,
                        name = name.trim(),
                        value = finalAmount,
                        measurement = measurement,
                        sortOrder = ingredient?.sortOrder ?: 0
                    )
                    if (confirmedIngredient.name.isNotBlank()) {
                        onConfirmation(confirmedIngredient)
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AddOrEditMethodStep(
    method: Method?,
    onDismissRequest: () -> Unit,
    onConfirmation: (method: Method) -> Unit,
    dialogTitle: String,
) {
    var value by remember(method) { mutableStateOf(method?.value ?: "") }

    AlertDialog(
        title = { Text(text = dialogTitle) },
        text = {
            Column {
                TextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text("Method Description") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                onClick = {
                    val updatedMethod = method?.copy(
                        value = value.trim()
                    ) ?: Method(
                        id = 0,
                        value = value.trim(),
                        sortOrder = 0,
                        ingredients = emptyList()
                    )
                    if (updatedMethod.value.isNotBlank()) {
                        onConfirmation(updatedMethod)
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AddOrEditDividerDialog(
    divider: Divider?,
    onDismissRequest: () -> Unit,
    onConfirmation: (divider: Divider) -> Unit,
    dialogTitle: String
) {
    var name by remember(divider) { mutableStateOf(divider?.title ?: "") }

    AlertDialog(
        title = { Text(text = dialogTitle) },
        text = {
            Column {
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Section Name") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                onClick = {
                    val confirmedDivider = divider?.copy(
                        title = name.trim()
                    ) ?: Divider(
                        id = 0,
                        title = name.trim(),
                        recipeId = 0,
                        sortOrder = 0,
                        ingredients = emptyList(),
                        methods = emptyList() // Assuming Divider.methods is List<Method>?
                    )
                    if (confirmedDivider.title.isNotBlank()) {
                        onConfirmation(confirmedDivider)
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun LinkIngredientsDialog(
    allIngredients: List<Ingredient>,
    currentlyLinkedIngredientNames: List<String>,
    onDismissRequest: () -> Unit,
    onConfirm: (updatedLinkedIngredientNames: List<String>) -> Unit
) {
    val tempSelectedIngredientNames = remember { mutableStateOf(currentlyLinkedIngredientNames.toMutableSet()) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Link Ingredients to Method") },
        text = {
            if (allIngredients.isEmpty()) {
                Text("No ingredients available in the recipe to link.")
            } else {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .fillMaxHeight(0.7f)
                ) {
                    allIngredients.chunked(2).forEach { rowIngredients ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowIngredients.forEach { ingredient ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            val currentSet = tempSelectedIngredientNames.value.toMutableSet()
                                            if (ingredient.name in currentSet) {
                                                currentSet.remove(ingredient.name)
                                            } else {
                                                currentSet.add(ingredient.name)
                                            }
                                            tempSelectedIngredientNames.value = currentSet
                                        }
                                        .padding(vertical = 4.dp, horizontal = 4.dp)
                                ) {
                                    Checkbox(
                                        checked = ingredient.name in tempSelectedIngredientNames.value,
                                        onCheckedChange = { isChecked ->
                                            val currentSet = tempSelectedIngredientNames.value.toMutableSet()
                                            if (isChecked) {
                                                currentSet.add(ingredient.name)
                                            } else {
                                                currentSet.remove(ingredient.name)
                                            }
                                            tempSelectedIngredientNames.value = currentSet
                                        }
                                    )
                                    Text(
                                        text = ingredient.name,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                            }
                            if (rowIngredients.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(tempSelectedIngredientNames.value.toList()) }) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        }
    )
}
