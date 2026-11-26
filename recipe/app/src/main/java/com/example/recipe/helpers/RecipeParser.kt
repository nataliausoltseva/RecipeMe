package com.example.recipe.helpers

import android.content.Context
import android.graphics.Bitmap
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONArray
import org.json.JSONObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class RecipeParser {

    /**
     * Extracts plain text from an image/photo of a recipe using on-device ML Kit Text Recognition.
     */
    suspend fun extractTextFromImage(bitmap: Bitmap): String = suspendCancellableCoroutine { continuation ->
        try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    continuation.resume(visionText.text)
                }
                .addOnFailureListener { e ->
                    continuation.resumeWithException(e)
                }
        } catch (e: Exception) {
            continuation.resumeWithException(e)
        }
    }

    /**
     * Converts a photo/image of a recipe directly into JSON structure.
     */
    suspend fun convertImageToRecipeJson(bitmap: Bitmap, context: Context): Result<String> {
        return try {
            val extractedText = extractTextFromImage(bitmap)
            if (extractedText.isBlank()) {
                Result.failure(Exception("No text could be recognized in the image. Please try a clearer photo."))
            } else {
                convertTextToRecipeJson(extractedText, context)
            }
        } catch (e: Exception) {
            Result.failure(Exception("Failed to read recipe from image: ${e.message}"))
        }
    }

    private fun buildPrompt(plainTextRecipe: String, targetJsonSchema: String): String {
        return """
        You are an expert recipe parser. Convert the following plain text recipe into a valid JSON object.
        Extract the recipe title if present.
        The JSON object MUST strictly adhere to the following JSON schema:
        $targetJsonSchema

        Plain text recipe:
        "$plainTextRecipe"

        JSON output:
        """.trimIndent()
    }

    /**
     * Converts plain text recipe into structured JSON using On-Device Gemini Nano (ML Kit Prompt API),
     * with an intelligent local parser fallback when on-device model is unavailable or warming up.
     */
    suspend fun convertTextToRecipeJson(plainTextRecipe: String, context: Context): Result<String> {
        val recipeSchema = """
        {
          "type": "object",
          "properties": {
            "title": { "type": "string", "description": "The title or name of the recipe." },
            "url": { "type": "string", "format": "uri", "nullable": true },
            "portion": {
              "type": "object",
              "properties": {
                "value": { "type": "number" },
                "measurement": { "type": "string" }
              }
            },
            "ingredients": {
              "type": "array",
              "items": {
                "type": "object",
                "properties": {
                  "name": { "type": "string" },
                  "value": { "type": "number" },
                  "measurement": { "type": "string", "description": "Allowed: bottle, can, item, g, kg, mL, L, tbsp, tsp, cup, to taste." }
                }
              }
            },
            "methods": {
              "type": "array",
              "items": {
                "type": "object",
                "properties": {
                  "value": { "type": "string" }
                }
              }
            },
            "equipments": {
              "type": "array",
              "items": {
                "type": "object",
                "properties": {
                  "name": { "type": "string" }
                }
              }
            }
          }
        }
        """.trimIndent()

        // 1. Try On-Device Gemini Nano via ML Kit GenAI Prompt API
        try {
            val prompt = buildPrompt(plainTextRecipe, recipeSchema)
            val generativeModel = Generation.getClient()
            val response = generativeModel.generateContent(prompt)
            generativeModel.close()

            val candidates = response.candidates
            if (candidates.isNotEmpty()) {
                val text = candidates[0].text
                var cleanedJson = text.trim()
                if (cleanedJson.startsWith("```json")) {
                    cleanedJson = cleanedJson.removePrefix("```json").trimStart()
                }
                if (cleanedJson.startsWith("```")) {
                    cleanedJson = cleanedJson.removePrefix("```").trimStart()
                }
                if (cleanedJson.endsWith("```")) {
                    cleanedJson = cleanedJson.removeSuffix("```").trimEnd()
                }
                if (cleanedJson.isNotBlank() && cleanedJson.startsWith("{")) {
                    val sanitized = sanitizeRecipeJson(cleanedJson, plainTextRecipe)
                    return Result.success(sanitized)
                }
            }
        } catch (_: Exception) {
            // On-device LLM model not available, warming up, or prompt failed -> proceed to local parser fallback
        }

        // 2. Intelligent local fallback parser (100% offline, guaranteed)
        return try {
            val jsonResult = parseRecipeTextLocally(plainTextRecipe)
            Result.success(jsonResult)
        } catch (e: Exception) {
            Result.failure(Exception("Could not parse recipe format: ${e.message}"))
        }
    }

    /**
     * Sanitizes raw JSON string to ensure all types match Kotlin data classes.
     */
    private fun sanitizeRecipeJson(rawJson: String, originalText: String): String {
        return try {
            val root = JSONObject(rawJson)

            // Extract title if missing
            if (!root.has("title") || root.optString("title").isBlank()) {
                root.put("title", extractTitleFromText(originalText))
            }

            // Sanitize ingredients array
            if (root.has("ingredients")) {
                val ingArr = root.getJSONArray("ingredients")
                val cleanIngArr = JSONArray()
                for (i in 0 until ingArr.length()) {
                    val item = ingArr.optJSONObject(i) ?: continue
                    val cleanItem = JSONObject()
                    cleanItem.put("name", item.optString("name", ""))
                    val rawVal = item.opt("value")
                    val numericVal = when (rawVal) {
                        is Number -> rawVal.toFloat()
                        is String -> parseQuantity(rawVal)
                        else -> 1f
                    }
                    cleanItem.put("value", numericVal)
                    cleanItem.put("measurement", matchMeasurement(item.optString("measurement", "item")))
                    cleanIngArr.put(cleanItem)
                }
                root.put("ingredients", cleanIngArr)
            }

            // Sanitize methods array
            if (root.has("methods")) {
                val methodArr = root.getJSONArray("methods")
                val cleanMethodArr = JSONArray()
                for (i in 0 until methodArr.length()) {
                    val item = methodArr.opt(i)
                    val cleanItem = JSONObject()
                    if (item is JSONObject) {
                        cleanItem.put("value", item.optString("value", ""))
                    } else if (item is String) {
                        cleanItem.put("value", item)
                    }
                    if (cleanItem.optString("value").isNotBlank()) {
                        cleanMethodArr.put(cleanItem)
                    }
                }
                root.put("methods", cleanMethodArr)
            }

            root.toString(2)
        } catch (_: Exception) {
            rawJson
        }
    }

    private fun extractTitleFromText(plainText: String): String {
        val lines = plainText.lines().map { it.trim() }.filter { it.isNotBlank() }
        for (line in lines) {
            val lower = line.lowercase()
            if (!lower.contains("serves") &&
                !lower.contains("portion") &&
                !lower.startsWith("ingredient") &&
                !lower.startsWith("method") &&
                !lower.startsWith("instruction") &&
                !lower.startsWith("step") &&
                !lower.startsWith("equipment")
            ) {
                return line.replace(Regex("""^[#*\s]+"""), "").trim()
            }
        }
        return ""
    }

    /**
     * Parses ingredients, amounts, units, portions, and methods locally from raw recipe text.
     */
    private fun parseRecipeTextLocally(plainText: String): String {
        val root = JSONObject()
        val ingredientsArray = JSONArray()
        val methodsArray = JSONArray()

        val lines = plainText.lines().map { it.trim() }.filter { it.isNotBlank() }

        val title = extractTitleFromText(plainText)
        root.put("title", title)

        var inMethodsSection = false
        var portionValue = 4f
        var portionUnit = "portion"

        val allowedUnits = listOf("bottle", "can", "item", "g", "kg", "mL", "L", "tbsp", "tsp", "cup", "to taste")

        val instructionVerbs = listOf(
            "preheat", "mix", "bake", "stir", "whisk", "heat", "place", "add", "combine",
            "cook", "pour", "serve", "chop", "slice", "dice", "boil", "simmer", "cover",
            "remove", "transfer", "set aside", "top with", "garnish", "blend", "grease",
            "line", "season", "drain", "melt", "fold", "knead", "roast", "fry", "saute"
        )

        for (line in lines) {
            // Skip title line if matched
            if (line.equals(title, ignoreCase = true) && lines.size > 2) {
                continue
            }

            val lower = line.lowercase()

            // Detect portions (e.g., "Serves 4", "Portions: 2")
            if (lower.contains("serves") || lower.contains("portion") || lower.contains("yield")) {
                val numMatch = Regex("""\d+""").find(line)
                if (numMatch != null) {
                    portionValue = numMatch.value.toFloatOrNull() ?: 4f
                }
                continue
            }

            // Detect instructions / method headers
            if (lower.startsWith("method") || lower.startsWith("instructions") || lower.startsWith("directions") || lower.startsWith("steps") || lower.startsWith("preparation")) {
                inMethodsSection = true
                continue
            }

            if (lower.startsWith("ingredients")) {
                inMethodsSection = false
                continue
            }

            // Check if line is clearly an instruction step (starts with step number or instruction verb)
            val isExplicitStep = Regex("""^\d+[\.\)]\s*""").containsMatchIn(line) ||
                    Regex("""^step\s+\d+""", RegexOption.IGNORE_CASE).containsMatchIn(line)
            val startsWithVerb = instructionVerbs.any { lower.startsWith(it) }

            if (inMethodsSection || isExplicitStep || startsWithVerb) {
                val methodObj = JSONObject()
                val cleanedLine = line.replace(Regex("""^\d+[\.\)]\s*"""), "").replace(Regex("""^step\s+\d+:\s*""", RegexOption.IGNORE_CASE), "")
                if (cleanedLine.isNotBlank()) {
                    methodObj.put("value", cleanedLine)
                    methodsArray.put(methodObj)
                }
            } else {
                // Ingredient parsing
                val ingredientObj = JSONObject()
                val regex = Regex("""^([\d/.\s]+)?\s*([a-zA-Z]+)?\s+(.+)""")
                val match = regex.find(line)

                if (match != null) {
                    val rawVal = match.groupValues[1].trim()
                    val rawUnit = match.groupValues[2].trim()
                    val rawName = match.groupValues[3].trim()

                    val numVal = parseQuantity(rawVal)
                    val unit = if (allowedUnits.contains(rawUnit.lowercase())) rawUnit else matchMeasurement(rawUnit)

                    ingredientObj.put("name", if (rawName.isNotBlank()) rawName else line)
                    ingredientObj.put("value", numVal)
                    ingredientObj.put("measurement", unit)
                } else {
                    ingredientObj.put("name", line)
                    ingredientObj.put("value", 1)
                    ingredientObj.put("measurement", "item")
                }
                ingredientsArray.put(ingredientObj)
            }
        }

        // Auto-detect common equipment mentioned in full text
        val equipmentsArray = JSONArray()
        val detectedEquipments = mutableSetOf<String>()

        val knownEquipmentKeywords = mapOf(
            "baking paper" to "Baking paper",
            "parchment paper" to "Baking paper",
            "baking tray" to "Baking tray",
            "baking sheet" to "Baking tray",
            "baking dish" to "Baking dish",
            "oven dish" to "Baking dish",
            "blender" to "Blender",
            "food processor" to "Food processor",
            "whisk" to "Whisk",
            "mixing bowl" to "Mixing bowl",
            "frying pan" to "Frying pan",
            "skillet" to "Frying pan",
            "rolling pin" to "Rolling pin",
            "saucepan" to "Saucepan",
            "muffin tin" to "Muffin tin",
            "spatula" to "Spatula",
            "grater" to "Grater"
        )

        val fullTextLower = plainText.lowercase()
        for ((keyword, displayName) in knownEquipmentKeywords) {
            if (fullTextLower.contains(keyword) && !detectedEquipments.contains(displayName)) {
                detectedEquipments.add(displayName)
                val eqObj = JSONObject()
                eqObj.put("name", displayName)
                equipmentsArray.put(eqObj)
            }
        }

        val portionObj = JSONObject()
        portionObj.put("value", portionValue)
        portionObj.put("measurement", portionUnit)

        root.put("portion", portionObj)
        root.put("ingredients", ingredientsArray)
        root.put("methods", methodsArray)
        root.put("equipments", equipmentsArray)

        return root.toString(2)
    }

    private fun parseQuantity(raw: String): Float {
        if (raw.isBlank()) return 1f
        if (raw.contains("/")) {
            val parts = raw.split("/")
            if (parts.size == 2) {
                val num = parts[0].trim().toFloatOrNull()
                val den = parts[1].trim().toFloatOrNull()
                if (num != null && den != null && den != 0f) {
                    return num / den
                }
            }
        }
        return raw.toFloatOrNull() ?: 1f
    }

    private fun matchMeasurement(unit: String): String {
        return when (unit.lowercase()) {
            "gram", "grams", "gr" -> "g"
            "kilogram", "kilograms", "kg" -> "kg"
            "ml", "milliliter", "milliliters" -> "mL"
            "liter", "liters", "l" -> "L"
            "tbsp", "tablespoon", "tablespoons" -> "tbsp"
            "tsp", "teaspoon", "teaspoons" -> "tsp"
            "cup", "cups" -> "cup"
            "can", "cans" -> "can"
            "bottle", "bottles" -> "bottle"
            else -> "item"
        }
    }
}
