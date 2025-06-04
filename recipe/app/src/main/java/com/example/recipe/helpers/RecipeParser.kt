package com.example.recipe.helpers

import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import com.google.ai.client.generativeai.GenerativeModel

class RecipeParser {
    private fun buildPrompt(plainTextRecipe: String, targetJsonSchema: String): String {
        return """
        You are an expert recipe parser. Convert the following plain text recipe into a valid JSON object.
        The JSON object MUST strictly adhere to the following JSON schema:
        $targetJsonSchema

        Plain text recipe:
        "$plainTextRecipe"

        JSON output:
        """.trimIndent()
    }

    suspend fun convertTextToRecipeJson(plainTextRecipe: String, context: Context): Result<String> {
        val recipeSchema = """
        {
          "type": "object",
          "properties": {
            "url": { "type": "string", "format": "uri", "nullable": true, "description": "Optional URL of the recipe source." },
            "methods": {
              "type": "array",
              "items": {
                "type": "object",
                "properties": {
                  "value": { "type": "string", "description": "The instruction for this method step." }
                }
              },
              "nullable": true
            },
            "ingredients": {
              "type": "array",
              "items": {
                "type": "object",
                "properties": {
                  "name": { "type": "string", "description": "Name of the ingredient." },
                  "value": { "type": "number", "description": "Quantity of the ingredient." },
                  "measurement": { "type": "string", "description": "Unit of measurement for the quantity (e.g., grams, ml, cups)." }
                }
              },
              "nullable": true
            },
            "portion": {
              "type": "object",
              "properties": {
                "value": { "type": "number", "description": "Numeric value of the portion (e.g., 4)." },
                "measurement": { "type": "string", "description": "Unit of measurement for the portion (e.g., servings, people)." }
              },
              "nullable": true
            }
          }
        }
        """.trimIndent()

        val prompt = buildPrompt(plainTextRecipe, recipeSchema)

        val ai = context.packageManager.getApplicationInfo(
            context.packageName,
            PackageManager.GET_META_DATA
        )
        val bundle: Bundle = ai.metaData

        try {
            val model = GenerativeModel(modelName = "gemini-2.0-flash", apiKey = bundle.getString("GEMINI_API_KEY") ?: "")

            val response = model.generateContent(prompt)
            var responseText = response.text ?: ""
            var cleanedJson = responseText.trim()
            if (cleanedJson.startsWith("```json")) {
                cleanedJson = cleanedJson.removePrefix("```json").trimStart()
            }
            if (cleanedJson.startsWith("```")) { // Handle cases where just ``` is used
                cleanedJson = cleanedJson.removePrefix("```").trimStart()
            }
            if (cleanedJson.endsWith("```")) {
                cleanedJson = cleanedJson.removeSuffix("```").trimEnd()
            }

            return Result.success(cleanedJson)
        } catch (e: Exception) {
            println(e.message)
            return Result.success("")
        }
        return Result.success("")
    }
}