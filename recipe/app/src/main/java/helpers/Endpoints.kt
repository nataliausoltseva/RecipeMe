package helpers

import com.example.recipe.helpers.ApiInterface

class Endpoints {
    private var apiInterface: ApiInterface = RetrofitInstance.getInstance().create(ApiInterface::class.java)

    suspend fun getSpecies(page: Number = 1): List<Species>? {
        try {
            val response = apiInterface.getSpecies(page)
            if (response.isSuccessful) {
                val data = response.body()
                return data
            } else {
                println("Response error: $response")
                return null
            }
        } catch (e: Exception) {
            println("Exception error getSpecies: ${e.message}. ${e.printStackTrace()}")
            return null
        }
    }

    suspend fun searchSpecies(searchQuery: String = "", page: Number = 1): List<Species>? {
        try {
            val response = apiInterface.searchSpecies(searchQuery, page)
            if (response.isSuccessful) {
                val data = response.body()
                return data
            } else {
                println("Response error: $response")
                return null
            }
        } catch (e: Exception) {
            println("Exception error: ${e.message}")
            return null
        }
    }
}