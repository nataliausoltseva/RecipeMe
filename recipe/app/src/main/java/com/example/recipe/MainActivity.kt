package com.example.recipe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.recipe.data.RecipeViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val recipesViewModel = remember { RecipeViewModel() }
            Recipes(
                recipesViewModel
            )
        }
    }
}

@Composable
fun Recipes(recipeViewModel: RecipeViewModel) {
    val recipesUIState by recipeViewModel.uiState.collectAsState()
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .fillMaxWidth()
    ) {
        for (recipe in recipesUIState.recipes) {
            Row {
                Column(
                    modifier = Modifier
                        .padding(0.dp, 10.dp)
                        .background(Color.Cyan)

                ) {
                    Row {
                        Text(
                            text = "Common name: ",
                        )
                        Text(
                            text = recipe.name,
                        )
                    }
                }
            }
        }
        Button(
            onClick = { recipeViewModel.reloadRecipes() },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Reload")
        }
    }
}