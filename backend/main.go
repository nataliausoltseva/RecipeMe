package main

import (
	"crypto/rand"
	"database/sql"
	"encoding/base64"
	"encoding/hex"
	"encoding/json"
	"fmt"
	"io"
	"log"
	"net/http"
	"sort"
	"strconv"
	"strings"
	"time"

	"github.com/gorilla/mux"
	_ "github.com/mattn/go-sqlite3"
)

type Portion struct {
	ID          int     `json:"id"`
	Value       float32 `json:"value"`
	Measurement string  `json:"measurement"`
	RecipeID    int     `json:"recipe_id"`
}

type Ingredient struct {
	ID          int     `json:"id"`
	Name        string  `json:"name"`
	Measurement string  `json:"measurement"`
	Value       float32 `json:"value"`
	RecipeID    int     `json:"recipe_id"`
	SortOrder   int     `json:"sortOrder"`
}

type Method struct {
	ID        int    `json:"id"`
	Value     string `json:"value"`
	SortOrder int    `json:"sortOrder"`
	RecipeID  int    `json:"recipe_id"`
}

type Image struct {
	ID       int    `json:"id"`
	Url      string `json:"url"`
	Filename string `json:"filename"`
	RecipeID int    `json:"recipe_id"`
}

type Recipe struct {
	ID           int          `json:"id"`
	Name         string       `json:"name"`
	Portion      *Portion     `json:"portion"`
	Image        *Image       `json:"image"`
	Url          string       `json:"url"`
	Ingredients  []Ingredient `json:"ingredients"`
	Methods      []Method     `json:"methods"`
	CreatedAt    string       `json:"createdAt"`
	LastEditedAt string       `json:"lastEditedAt"`
	Type         string       `json:"type"`
	SortOrder    int          `json:"sortOrder"`
}

var recipes []Recipe
var db *sql.DB

func getRecipes(w http.ResponseWriter, r *http.Request) {
	queryParams := r.URL.Query()
	searchString := queryParams.Get("search")
	sortKey := queryParams.Get("sortKey")
	sortDirection := queryParams.Get("sortDirection")

	if sortDirection == "" {
		sortDirection = "DESC"
	}

	if sortKey == "" {
		sortKey = "sortOrder"
	}

	var rows *sql.Rows
	var err error
	query := "SELECT * FROM recipes"

	if searchString != "" {
		searchPattern := "%" + searchString + "%"
		query += fmt.Sprintf(" WHERE recipes.name LIKE %s", searchPattern)
	}

	if sortKey != "" && sortKey != "portion" {
		query += fmt.Sprintf(" ORDER BY %s %s", sortKey, sortDirection)
	}

	rows, err = db.Query(query)

	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}
	defer rows.Close()

	var recipes []Recipe

	for rows.Next() {
		var recipe Recipe
		recipe.Ingredients = []Ingredient{}
		recipe.Methods = []Method{}

		// Scan the current row
		rows.Scan(
			&recipe.ID,
			&recipe.Name,
			&recipe.Url,
			&recipe.CreatedAt,
			&recipe.LastEditedAt,
			&recipe.Type,
			&recipe.SortOrder,
		)

		recipe.Ingredients = getRecipeIngredients(recipe.ID, searchString)
		recipe.Methods = getRecipeMethods(recipe.ID)
		recipe.Portion = getRecipePortion(recipe.ID)
		recipe.Image = getRecipeImage(recipe.ID)

		recipes = append(recipes, recipe)
	}
	ingredientNamesString := queryParams.Get("ingredientNames")
	if ingredientNamesString != "" {
		ingredientNames := strings.Split(ingredientNamesString, ",")
		lowerIngredientNames := make([]string, len(ingredientNames))
		for i, name := range ingredientNames {
			lowerIngredientNames[i] = strings.ToLower(name)
		}

		var filteredRecieps []Recipe
	outerLoop:
		for _, recipe := range recipes {
			for _, ingredient := range recipe.Ingredients {
				lowerValue := strings.ToLower(ingredient.Name)
				for _, v := range lowerIngredientNames {
					if v == lowerValue {
						filteredRecieps = append(filteredRecieps, recipe)
						continue outerLoop
					}
				}
			}
		}
		if sortKey == "portion" {
			json.NewEncoder(w).Encode(sortRecipesByPortion(filteredRecieps, sortDirection))
			return
		}
		json.NewEncoder(w).Encode(filteredRecieps)
	} else {
		if sortKey == "portion" {
			json.NewEncoder(w).Encode(sortRecipesByPortion(recipes, sortDirection))
			return
		}
		json.NewEncoder(w).Encode(recipes)
	}
}

func sortRecipesByPortion(recipes []Recipe, sortDirection string) []Recipe {
	var isAscending = sortDirection == "asc"
	sort.Slice(recipes, func(i, j int) bool {
		if recipes[i].Portion == nil && recipes[j].Portion != nil {
			return false // Place i after j
		}
		if recipes[i].Portion != nil && recipes[j].Portion == nil {
			return true // Place i before j
		}
		if recipes[i].Portion != nil && recipes[j].Portion != nil {
			if isAscending {
				return recipes[i].Portion.Value < recipes[j].Portion.Value
			}
			return recipes[i].Portion.Value > recipes[j].Portion.Value
		}
		return false // Keep order unchanged for two nils
	})

	return recipes
}

func getRecipe(w http.ResponseWriter, r *http.Request) {
	params := mux.Vars(r)
	idStr, ok := params["id"]
	if !ok {
		http.Error(w, "Missing ID parameter", http.StatusInternalServerError)
		return
	}

	id, _ := strconv.Atoi(idStr)

	json.NewEncoder(w).Encode(getRecipeById(id))
}

func getRecipeById(id int) Recipe {
	row := db.QueryRow(`
		SELECT * FROM recipes WHERE id = ?
	`, id)

	var recipe Recipe

	row.Scan(
		&recipe.ID,
		&recipe.Name,
		&recipe.Url,
		&recipe.CreatedAt,
		&recipe.LastEditedAt,
		&recipe.Type,
		&recipe.SortOrder,
	)

	recipe.Ingredients = getRecipeIngredients(id, "")
	recipe.Methods = getRecipeMethods(id)
	recipe.Portion = getRecipePortion(id)
	recipe.Image = getRecipeImage(id)

	return recipe
}

func createRecipe(w http.ResponseWriter, r *http.Request) {
	var recipe Recipe
	json.NewDecoder(r.Body).Decode(&recipe)

	existingRecipes := getAllRecipes()

	sortOrder := max(1+len(existingRecipes), 1)
	var name = ""
	if recipe.Name != "" {
		name = recipe.Name
	}

	var url = ""
	if recipe.Url != "" {
		url = recipe.Url
	}

	var recipeType = ""
	if recipe.Type != "" {
		recipeType = recipe.Type
	}

	stmt, err := db.Prepare(`
		INSERT INTO recipes(name, url, createdAt, lastEditedAt,  type, sortOrder) VALUES(?,?,?,?,?,?)
	`)
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}

	now := time.Now()
	result, err := stmt.Exec(name, url, now.Format("2006-01-02 15:04:05"), now.Format("2006-01-02 15:04:05"), recipeType, sortOrder)
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}
	recipeId, _ := result.LastInsertId()
	json.NewEncoder(w).Encode(getRecipeById(int(recipeId)))
}

func updateRecipe(w http.ResponseWriter, r *http.Request) {
	params := mux.Vars(r)
	idStr := params["id"]

	id, err := strconv.Atoi(idStr)
	if err != nil {
		http.Error(w, "Invalid ID parameter", http.StatusInternalServerError)
		return
	}

	var recipe Recipe
	json.NewDecoder(r.Body).Decode(&recipe)
	stmt, err := db.Prepare(`
		UPDATE recipes
		SET name = ?,
			url = ?,
			lastEditedAt = ?,
			type = ?
		WHERE id = ?
	`)
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}

	_, err = stmt.Exec(
		recipe.Name,
		recipe.Url,
		time.Now().Format("2006-01-02 15:04:05"),
		recipe.Type,
		id,
	)
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}
	json.NewEncoder(w).Encode(recipe)
}

func updateRecipeLastEdited(recipeId int) {
	_, err := db.Exec(`
		UPDATE recipes SET lastEditedAt = ? WHERE id = ?
	`, time.Now().Format("2006-01-02 15:04:05"), recipeId)
	if err != nil {
		fmt.Println(err.Error())
	}
}

func deleteRecipe(w http.ResponseWriter, r *http.Request) {
	params := mux.Vars(r)
	idStr, ok := params["id"]
	if !ok {
		http.Error(w, "Missing ID parameter", http.StatusInternalServerError)
		return
	}

	id, err := strconv.Atoi(idStr)
	if err != nil {
		http.Error(w, "Invalid ID parameter", http.StatusInternalServerError)
		return
	}

	stmt, err := db.Prepare("DELETE FROM recipes WHERE id = ?")
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}

	_, err = stmt.Exec(id)
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}

	json.NewEncoder(w).Encode(recipes)
}

func reorderRecipes(w http.ResponseWriter, r *http.Request) {
	var passedRecipes []Recipe
	json.NewDecoder(r.Body).Decode(&passedRecipes)

	var existingRecipes = getAllRecipes()

	if existingRecipes != nil {
		for passedRecipeIndex, passedRecipe := range passedRecipes {
			for _, existingRecipe := range existingRecipes {
				if passedRecipe.ID == existingRecipe.ID {
					sortOrder := passedRecipeIndex + 1

					_, err := db.Exec("UPDATE recipes SET sortOrder = ? WHERE id = ?", sortOrder, passedRecipe.ID)
					if err != nil {
						fmt.Println("Error updating recipe:", err)
						http.Error(w, err.Error(), http.StatusInternalServerError)
						return
					}
					break
				}
			}
		}
	}

	json.NewEncoder(w).Encode(getAllRecipes())
}

func getAllRecipes() []Recipe {
	rows, err := db.Query("SELECT * FROM recipes")

	if err != nil {
		return nil
	}

	defer rows.Close()

	var recipes []Recipe
	for rows.Next() {
		var recipe Recipe
		recipe.Ingredients = []Ingredient{}
		recipe.Methods = []Method{}

		rows.Scan(
			&recipe.ID,
			&recipe.Name,
			&recipe.Url,
			&recipe.CreatedAt,
			&recipe.LastEditedAt,
			&recipe.Type,
			&recipe.SortOrder,
		)

		recipe.Ingredients = getRecipeIngredients(recipe.ID, "")
		recipe.Methods = getRecipeMethods(recipe.ID)
		recipe.Portion = getRecipePortion(recipe.ID)
		recipe.Image = getRecipeImage(recipe.ID)

		recipes = append(recipes, recipe)
	}
	return recipes
}

func getRecipePortion(recipeId int) *Portion {
	row := db.QueryRow(`
		SELECT * FROM portions
		WHERE recipe_id = ?
	`, recipeId)

	var portion Portion
	row.Scan(
		&portion.ID,
		&portion.Value,
		&portion.Measurement,
		&portion.RecipeID,
	)

	if portion.ID == 0 {
		return nil
	}

	return &portion
}

func addPortion(w http.ResponseWriter, r *http.Request) {
	params := mux.Vars(r)
	idStr := params["recipe_id"]

	recipeId, err := strconv.Atoi(idStr)
	if err != nil {
		http.Error(w, "Invalid ID parameter", http.StatusInternalServerError)
		return
	}

	recipe := getRecipeById(recipeId)

	if recipe.ID == 0 {
		json.NewEncoder(w).Encode(nil)
		return
	}

	var portion Portion
	json.NewDecoder(r.Body).Decode(&portion)

	recipePortion := getRecipePortion(recipeId)

	if recipePortion == nil {
		_, err := db.Exec(`
			INSERT INTO portions(value, measurement, recipe_id) VALUES(?,?,?)
		`, portion.Value, portion.Measurement, recipeId)
		if err != nil {
			http.Error(w, err.Error(), http.StatusInternalServerError)
			return
		}
	} else {
		_, err := db.Exec(`
			UPDATE portions SET value = ?, measurement = ? WHERE recipe_id = ?
		`, portion.Value, portion.Measurement, recipeId)
		if err != nil {
			http.Error(w, err.Error(), http.StatusInternalServerError)
			return
		}
	}

	updateRecipeLastEdited(recipeId)

	json.NewEncoder(w).Encode(getRecipeById(recipeId))
}

func deletePortion(w http.ResponseWriter, r *http.Request) {
	params := mux.Vars(r)
	idStr, ok := params["id"]
	if !ok {
		http.Error(w, "Missing ID parameter", http.StatusInternalServerError)
		return
	}

	id, err := strconv.Atoi(idStr)
	if err != nil {
		http.Error(w, "Invalid ID parameter", http.StatusInternalServerError)
		return
	}

	stmt, err := db.Prepare("DELETE FROM portions WHERE id = ?")
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}

	_, err = stmt.Exec(id)
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}
}

func getPortions(w http.ResponseWriter, r *http.Request) {
	rows, err := db.Query(`
		SELECT * FROM portions
	`)

	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}
	defer rows.Close()
	var portions []Portion
	for rows.Next() {
		var portion Portion
		rows.Scan(
			&portion.ID,
			&portion.Value,
			&portion.Measurement,
			&portion.RecipeID,
		)

		portions = append(portions, portion)
	}

	json.NewEncoder(w).Encode(portions)
}

func getIngredients(w http.ResponseWriter, r *http.Request) {
	rows, err := db.Query(`
		SELECT * FROM ingredients
	`)

	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}
	defer rows.Close()
	var ingredients []Ingredient
	for rows.Next() {
		var ingredient Ingredient
		rows.Scan(
			&ingredient.ID,
			&ingredient.Name,
			&ingredient.Measurement,
			&ingredient.Value,
			&ingredient.SortOrder,
			&ingredient.RecipeID,
		)

		ingredients = append(ingredients, ingredient)
	}

	json.NewEncoder(w).Encode(ingredients)
}

func getRecipeIngredients(recipeId int, searchString string) []Ingredient {
	var rows *sql.Rows
	var err error

	if searchString == "" {
		rows, err = db.Query(`
		SELECT * FROM ingredients
		WHERE recipe_id = ?
		ORDER BY SortOrder ASC
	`, recipeId)
	} else {
		searchPattern := "%" + searchString + "%"
		rows, err = db.Query(`
		SELECT * FROM ingredients
		WHERE recipe_id = ? AND name LIKE ?
		ORDER BY SortOrder ASC
		`, recipeId, searchPattern)
	}

	if err != nil {
		return []Ingredient{}
	}

	defer rows.Close()

	var ingredients []Ingredient
	for rows.Next() {
		var ingredient Ingredient
		rows.Scan(
			&ingredient.ID,
			&ingredient.Name,
			&ingredient.Measurement,
			&ingredient.Value,
			&ingredient.SortOrder,
			&ingredient.RecipeID,
		)

		ingredients = append(ingredients, ingredient)
	}

	return ingredients
}

func addIngredients(w http.ResponseWriter, r *http.Request) {
	params := mux.Vars(r)
	idStr := params["recipe_id"]

	recipeId, err := strconv.Atoi(idStr)
	if err != nil {
		http.Error(w, "Invalid ID parameter", http.StatusInternalServerError)
		return
	}

	recipe := getRecipeById(recipeId)
	if recipe.ID == 0 {
		json.NewEncoder(w).Encode(nil)
		return
	}

	var existingIngredients = getRecipeIngredients(recipeId, "")
	var passedIngredients []Ingredient
	json.NewDecoder(r.Body).Decode(&passedIngredients)

	for passedIngredientIndex, passedIngredient := range passedIngredients {
		found := false
		for existingIngredientIndex, existingIngredient := range existingIngredients {
			if passedIngredient.ID == existingIngredient.ID {
				sortOrder := passedIngredient.SortOrder
				if existingIngredientIndex == 0 && passedIngredient.SortOrder == 0 {
					sortOrder += 1
				} else if existingIngredientIndex != 0 && passedIngredient.SortOrder == 0 {
					sortOrder = existingIngredientIndex + 1
				}

				_, err := db.Exec("UPDATE ingredients SET name = ?, measurement = ?, value = ?, sortOrder = ? WHERE id = ?", passedIngredient.Name, passedIngredient.Measurement, passedIngredient.Value, sortOrder, passedIngredient.ID)
				if err != nil {
					fmt.Println("Error updating ingredient:", err)
					http.Error(w, err.Error(), http.StatusInternalServerError)
					return
				}
				found = true
				break
			}
		}
		if !found {
			sortOrder := passedIngredientIndex + 1 + len(existingIngredients)

			_, err := db.Exec("INSERT INTO ingredients(name, measurement, value, sortOrder, recipe_id) VALUES(?,?,?,?,?)", passedIngredient.Name, passedIngredient.Measurement, passedIngredient.Value, sortOrder, recipeId)
			if err != nil {
				fmt.Println("Error inserting ingredient:", err)
				http.Error(w, err.Error(), http.StatusInternalServerError)
				return
			}
		}
	}

	updateRecipeLastEdited(recipeId)

	json.NewEncoder(w).Encode(getRecipeById(recipeId))
}

func addIngredient(w http.ResponseWriter, r *http.Request) {
	params := mux.Vars(r)
	idStr := params["recipe_id"]

	recipeId, err := strconv.Atoi(idStr)
	if err != nil {
		http.Error(w, "Invalid ID parameter", http.StatusInternalServerError)
		return
	}

	recipe := getRecipeById(recipeId)
	if recipe.ID == 0 {
		json.NewEncoder(w).Encode(nil)
		return
	}

	var existingIngredients = getRecipeIngredients(recipeId, "")
	var passedIngredient Ingredient
	json.NewDecoder(r.Body).Decode(&passedIngredient)
	found := false
	for existingIngredientIndex, existingIngredient := range existingIngredients {
		if passedIngredient.ID == existingIngredient.ID {
			sortOrder := passedIngredient.SortOrder
			if existingIngredientIndex == 0 && passedIngredient.SortOrder == 0 {
				sortOrder += 1
			} else if existingIngredientIndex != 0 && passedIngredient.SortOrder == 0 {
				sortOrder = existingIngredientIndex + 1
			}

			_, err := db.Exec("UPDATE ingredients SET name = ?, measurement = ?, value = ?, sortOrder = ? WHERE id = ?", passedIngredient.Name, passedIngredient.Measurement, passedIngredient.Value, sortOrder, passedIngredient.ID)
			if err != nil {
				fmt.Println("Error updating ingredient:", err)
				http.Error(w, err.Error(), http.StatusInternalServerError)
				return
			}
			found = true
			break
		}
	}
	if !found {
		sortOrder := 1 + len(existingIngredients)

		_, err := db.Exec("INSERT INTO ingredients(name, measurement, value, sortOrder, recipe_id) VALUES(?,?,?,?,?)", passedIngredient.Name, passedIngredient.Measurement, passedIngredient.Value, sortOrder, recipeId)
		if err != nil {
			fmt.Println("Error inserting ingredient:", err)
			http.Error(w, err.Error(), http.StatusInternalServerError)
			return
		}
	}

	updateRecipeLastEdited(recipeId)

	json.NewEncoder(w).Encode(getRecipeById(recipeId))
}

func deleteIngredient(w http.ResponseWriter, r *http.Request) {
	params := mux.Vars(r)
	idStr, ok := params["id"]
	if !ok {
		http.Error(w, "Missing ID parameter", http.StatusInternalServerError)
		return
	}

	id, err := strconv.Atoi(idStr)
	if err != nil {
		http.Error(w, "Invalid ID parameter", http.StatusInternalServerError)
		return
	}

	stmt, err := db.Prepare("DELETE FROM ingredients WHERE id = ?")
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}

	_, err = stmt.Exec(id)
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}
}

func getRecipeMethods(recipeId int) []Method {
	rows, err := db.Query(`
		SELECT * FROM methods
		WHERE recipe_id = ?
		ORDER BY SortOrder ASC
	`, recipeId)

	if err != nil {
		return []Method{}
	}

	defer rows.Close()

	var methods []Method
	for rows.Next() {
		var method Method
		rows.Scan(
			&method.ID,
			&method.Value,
			&method.SortOrder,
			&method.RecipeID,
		)

		methods = append(methods, method)
	}

	return methods
}

func addMethods(w http.ResponseWriter, r *http.Request) {
	params := mux.Vars(r)
	idStr := params["recipe_id"]

	recipeId, err := strconv.Atoi(idStr)
	if err != nil {
		http.Error(w, "Invalid ID parameter", http.StatusInternalServerError)
		return
	}

	recipe := getRecipeById(recipeId)
	if recipe.ID == 0 {
		json.NewEncoder(w).Encode(nil)
		return
	}

	var existingMethods = getRecipeMethods(recipeId)

	var passedMethods []Method
	json.NewDecoder(r.Body).Decode(&passedMethods)

	for passedMethodIndex, passedMethod := range passedMethods {
		found := false

		for existingMethodIndex, existingMethod := range existingMethods {
			sortOrder := passedMethod.SortOrder
			if existingMethodIndex == 0 && sortOrder == 0 {
				sortOrder += 1
			} else if existingMethodIndex != 0 && sortOrder == 0 {
				sortOrder = existingMethodIndex + 1
			}

			if passedMethod.ID == existingMethod.ID {
				_, err := db.Exec("UPDATE methods SET value = ?, sortOrder = ? WHERE id = ?", passedMethod.Value, sortOrder, passedMethod.ID)
				if err != nil {
					http.Error(w, err.Error(), http.StatusInternalServerError)
					return
				}
				found = true
				break
			}
		}

		if !found {
			sortOrder := passedMethodIndex + 1 + len(existingMethods)
			_, err := db.Exec("INSERT INTO methods(value, sortOrder, recipe_id) VALUES(?,?,?)", passedMethod.Value, sortOrder, recipeId)
			if err != nil {
				http.Error(w, err.Error(), http.StatusInternalServerError)
				return
			}
		}
	}

	updateRecipeLastEdited(recipeId)

	json.NewEncoder(w).Encode(getRecipeById(recipeId))
}

func addMethod(w http.ResponseWriter, r *http.Request) {
	params := mux.Vars(r)
	idStr := params["recipe_id"]

	recipeId, err := strconv.Atoi(idStr)
	if err != nil {
		http.Error(w, "Invalid ID parameter", http.StatusInternalServerError)
		return
	}

	recipe := getRecipeById(recipeId)
	if recipe.ID == 0 {
		json.NewEncoder(w).Encode(nil)
		return
	}

	var existingMethods = getRecipeMethods(recipeId)

	var passedMethod Method
	json.NewDecoder(r.Body).Decode(&passedMethod)

	found := false

	for existingMethodIndex, existingMethod := range existingMethods {
		sortOrder := passedMethod.SortOrder
		if existingMethodIndex == 0 && sortOrder == 0 {
			sortOrder += 1
		} else if existingMethodIndex != 0 && sortOrder == 0 {
			sortOrder = existingMethodIndex + 1
		}

		if passedMethod.ID == existingMethod.ID {
			_, err := db.Exec("UPDATE methods SET value = ?, sortOrder = ? WHERE id = ?", passedMethod.Value, sortOrder, passedMethod.ID)
			if err != nil {
				http.Error(w, err.Error(), http.StatusInternalServerError)
				return
			}
			found = true
			break
		}
	}

	if !found {
		sortOrder := 1 + len(existingMethods)
		_, err := db.Exec("INSERT INTO methods(value, sortOrder, recipe_id) VALUES(?,?,?)", passedMethod.Value, sortOrder, recipeId)
		if err != nil {
			http.Error(w, err.Error(), http.StatusInternalServerError)
			return
		}
	}

	updateRecipeLastEdited(recipeId)

	json.NewEncoder(w).Encode(getRecipeById(recipeId))
}

func deleteMethod(w http.ResponseWriter, r *http.Request) {
	params := mux.Vars(r)
	idStr, ok := params["id"]
	if !ok {
		http.Error(w, "Missing ID parameter", http.StatusInternalServerError)
		return
	}

	id, err := strconv.Atoi(idStr)
	if err != nil {
		http.Error(w, "Invalid ID parameter", http.StatusInternalServerError)
		return
	}

	stmt, err := db.Prepare("DELETE FROM methods WHERE id = ?")
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}

	_, err = stmt.Exec(id)
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}
}

func updateImage(w http.ResponseWriter, r *http.Request) {
	file, _, err := r.FormFile("image")
	if err != nil {
		http.Error(w, err.Error(), http.StatusBadRequest)
		return
	}
	defer file.Close()

	params := mux.Vars(r)
	idStr := params["recipe_id"]

	recipeId, err := strconv.Atoi(idStr)
	if err != nil {
		http.Error(w, "Invalid ID parameter", http.StatusInternalServerError)
		return
	}

	recipe := getRecipeById(recipeId)

	if recipe.ID == 0 {
		json.NewEncoder(w).Encode(nil)
		return
	}

	imgBytes, err := io.ReadAll(file)
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}

	err = saveImage(imgBytes, recipeId, recipe.Image != nil)
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}

	updateRecipeLastEdited(recipeId)

	json.NewEncoder(w).Encode(getRecipeById(recipeId))
}

func saveImage(data []byte, recipeId int, isReplace bool) error {
	url := base64.StdEncoding.EncodeToString(data)

	if isReplace {
		_, err := db.Exec("UPDATE images SET url = ? WHERE recipe_id = ?",
			url, recipeId,
		)
		return err
	} else {
		_, err := db.Exec("INSERT INTO images(filename, url, recipe_id) VALUES(?,?,?)",
			generateRandomFilename("png"), url, recipeId,
		)
		return err
	}
}

func generateRandomFilename(ext string) string {
	b := make([]byte, 16)
	_, err := rand.Read(b)
	if err != nil {
		panic(err)
	}
	return fmt.Sprintf("%s.%s", hex.EncodeToString(b), ext)
}

func getRecipeImage(recipeId int) *Image {
	row := db.QueryRow(`
		SELECT * FROM images
		WHERE recipe_id = ?
	`, recipeId)

	var image Image
	row.Scan(
		&image.ID,
		&image.Url,
		&image.Filename,
		&image.RecipeID,
	)

	if image.ID == 0 {
		return nil
	}

	return &image
}

func getImages(w http.ResponseWriter, r *http.Request) {
	rows, err := db.Query(`
		SELECT * FROM images
	`)

	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}
	defer rows.Close()
	var images []Image
	for rows.Next() {
		var image Image
		rows.Scan(
			&image.ID,
			&image.Url,
			&image.Filename,
			&image.RecipeID,
		)

		images = append(images, image)
	}

	json.NewEncoder(w).Encode(images)
}

func main() {
	var err error
	db, err = sql.Open("sqlite3", "./database/database.db")
	if err != nil {
		log.Fatal(err)
	}
	defer db.Close()

	router := mux.NewRouter().StrictSlash(true)
	// Recipe routes
	router.HandleFunc("/recipes", getRecipes).Methods("GET")
	router.HandleFunc("/recipe/{id}", getRecipe).Methods("GET")
	router.HandleFunc("/recipe", createRecipe).Methods("POST")
	router.HandleFunc("/recipe/{id}", updateRecipe).Methods("PUT")
	router.HandleFunc("/recipe/{id}", deleteRecipe).Methods("DELETE")
	router.HandleFunc("/recipes", reorderRecipes).Methods("PUT")

	// Portion routes
	router.HandleFunc("/portion/{recipe_id}", addPortion).Methods("POST")
	router.HandleFunc("/portion/{id}", deletePortion).Methods("DELETE")
	router.HandleFunc("/portions", getPortions).Methods("GET")

	// Ingredient routes
	router.HandleFunc("/ingredients/{recipe_id}", addIngredients).Methods("POST")
	router.HandleFunc("/ingredient/{recipe_id}", addIngredient).Methods("POST")
	router.HandleFunc("/ingredient/{id}", deleteIngredient).Methods("DELETE")
	router.HandleFunc("/ingredients", getIngredients).Methods("GET")

	// Method routes
	router.HandleFunc("/methods/{recipe_id}", addMethods).Methods("POST")
	router.HandleFunc("/method/{recipe_id}", addMethod).Methods("POST")
	router.HandleFunc("/method/{id}", deleteMethod).Methods("DELETE")

	// Image routes
	router.HandleFunc("/image/{recipe_id}", updateImage).Methods("POST")
	router.HandleFunc("/images", getImages).Methods("GET")

	fmt.Println("Starting server on :8080...")
	http.ListenAndServe(":8080", router)
}
