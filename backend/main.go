package main

import (
	"database/sql"
	"encoding/json"
	"fmt"
	"log"
	"net/http"
	"strconv"
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
	ID          int    `json:"id"`
	Name        string `json:"name"`
	Measurement string `json:"measurement"`
	Value       string `json:"value"`
	RecipeID    int    `json:"recipe_id"`
	SortOrder   int    `json:"sortOrder"`
}

type Method struct {
	ID        int    `json:"id"`
	Value     string `json:"value"`
	SortOrder int    `json:"sortOrder"`
	RecipeID  int    `json:"recipe_id"`
}

type Recipe struct {
	ID          int          `json:"id"`
	Name        string       `json:"name"`
	Portion     *Portion     `json:"portion"`
	Url         string       `json:"url"`
	ImageUrl    string       `json:"imageUrl"`
	Ingredients []Ingredient `json:"ingredients"`
	Methods     []Method     `json:"methods"`
	CreatedAt   string       `json:"createdAt"`
}

var recipes []Recipe
var db *sql.DB

func getRecipes(w http.ResponseWriter, r *http.Request) {
	queryParams := r.URL.Query()
	searchString := queryParams.Get("search")
	var rows *sql.Rows
	var err error

	if searchString == "" {
		rows, err = db.Query(`
		SELECT * FROM recipe
	`)
	} else {
		searchPattern := "%" + searchString + "%"
		rows, err = db.Query(`
			SELECT * FROM recipe
			WHERE recipe.name LIKE ?
		`, searchPattern)
	}

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
			&recipe.ImageUrl,
			&recipe.CreatedAt,
		)

		recipe.Ingredients = getRecipeIngredients(recipe.ID, searchString)
		recipe.Methods = getRecipeMethods(recipe.ID)
		recipe.Portion = getRecipePortion(recipe.ID)

		recipes = append(recipes, recipe)
	}

	json.NewEncoder(w).Encode(recipes)
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
		SELECT * FROM recipe WHERE recipe.id = ?
	`, id)

	var recipe Recipe

	row.Scan(
		&recipe.ID,
		&recipe.Name,
		&recipe.Url,
		&recipe.ImageUrl,
		&recipe.CreatedAt,
	)

	recipe.Ingredients = getRecipeIngredients(id, "")
	recipe.Methods = getRecipeMethods(id)
	recipe.Portion = getRecipePortion(id)

	return recipe
}

func createRecipe(w http.ResponseWriter, r *http.Request) {
	var recipe Recipe
	json.NewDecoder(r.Body).Decode(&recipe)
	stmt, err := db.Prepare(`
		INSERT INTO recipe(name, url, imageUrl, createdAt) VALUES(?,?,?,?)
	`)
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}

	now := time.Now()
	result, err := stmt.Exec(recipe.Name, recipe.Url, recipe.ImageUrl, now.Format("2006-01-02 15:04:05"))
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
		UPDATE recipe
		SET name = ?,
			url = ?,
			imageUrl = ?,
			createdAt = ?
		WHERE id = ?
	`)
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}
	_, err = stmt.Exec(
		recipe.Name,
		recipe.Url,
		recipe.ImageUrl,
		recipe.CreatedAt,
		id,
	)
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}
	json.NewEncoder(w).Encode(recipe)
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

	stmt, err := db.Prepare("DELETE FROM recipe WHERE id = ?")
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

func getRecipePortion(recipeId int) *Portion {
	row := db.QueryRow(`
		SELECT * FROM portion
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
			INSERT INTO portion(value, measurement, recipe_id) VALUES(?,?,?)
		`, portion.Value, portion.Measurement, recipeId)
		if err != nil {
			http.Error(w, err.Error(), http.StatusInternalServerError)
			return
		}
	} else {
		_, err := db.Exec(`
			UPDATE portion SET value = ?, measurement = ? WHERE recipe_id = ?
		`, portion.Value, portion.Measurement, recipeId)
		if err != nil {
			http.Error(w, err.Error(), http.StatusInternalServerError)
			return
		}
	}

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

	stmt, err := db.Prepare("DELETE FROM portion WHERE id = ?")
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
		SELECT * FROM portion
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
		SELECT * FROM ingredient
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
		SELECT * FROM ingredient
		WHERE recipe_id = ?
	`, recipeId)
	} else {
		searchPattern := "%" + searchString + "%"
		rows, err = db.Query(`
		SELECT * FROM ingredient
		WHERE recipe_id = ? AND ingredient.name LIKE ?
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

				_, err := db.Exec("UPDATE ingredient SET name = ?, measurement = ?, value = ?, sortOrder = ? WHERE id = ?", passedIngredient.Name, passedIngredient.Measurement, passedIngredient.Value, sortOrder, passedIngredient.ID)
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

			_, err := db.Exec("INSERT INTO ingredient(name, measurement, value, sortOrder, recipe_id) VALUES(?,?,?,?,?)", passedIngredient.Name, passedIngredient.Measurement, passedIngredient.Value, sortOrder, recipeId)
			if err != nil {
				fmt.Println("Error inserting ingredient:", err)
				http.Error(w, err.Error(), http.StatusInternalServerError)
				return
			}
		}
	}

	json.NewEncoder(w).Encode(getRecipeById(recipeId))
}

func updateIngredient(w http.ResponseWriter, r *http.Request) {
	params := mux.Vars(r)
	idStr := params["id"]

	id, err := strconv.Atoi(idStr)
	if err != nil {
		http.Error(w, "Invalid ID parameter", http.StatusInternalServerError)
		return
	}

	var ingredient Ingredient
	json.NewDecoder(r.Body).Decode(&ingredient)
	stmt, err := db.Prepare(`
		UPDATE ingredient
		SET name = ?,
			measurement = ?,
			value = ?,
		WHERE id = ?
	`)
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}
	_, err = stmt.Exec(
		ingredient.Value,
		ingredient.Measurement,
		ingredient.Name,
		id,
	)
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}
	json.NewEncoder(w).Encode(ingredient)
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

	stmt, err := db.Prepare("DELETE FROM ingredient WHERE id = ?")
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
		SELECT * FROM method
		WHERE recipe_id = ?
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
			&method.SortOrder,
			&method.Value,
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

	rows, err := db.Query(`
		SELECT * FROM method WHERE recipe_id = ?
	`, recipeId)

	defer rows.Close()

	var existingMethods = getRecipeMethods(recipeId)

	var passedMethods []Method
	json.NewDecoder(r.Body).Decode(&passedMethods)

	for passedMethodIndex, passedMethod := range passedMethods {
		found := false

		for existingMethodIndex, existingMethod := range existingMethods {
			sortOrder := passedMethod.SortOrder
			if existingMethodIndex == 0 && passedMethod.SortOrder == 0 {
				sortOrder += 1
			} else if existingMethodIndex != 0 && passedMethod.SortOrder == 0 {
				sortOrder = existingMethodIndex + 1
			}
			if passedMethod.ID == existingMethod.ID {
				_, err := db.Exec("UPDATE method SET value = ?, sortOrder = ?, value = ? WHERE id = ?", passedMethod.Value, sortOrder, passedMethod.ID)
				if err != nil {
					fmt.Println("Error updating method:", err)
					http.Error(w, err.Error(), http.StatusInternalServerError)
					return
				}
				found = true
				break
			}
		}

		if !found {
			sortOrder := passedMethodIndex + 1 + len(existingMethods)
			_, err := db.Exec("INSERT INTO method(value, sortOrder, recipe_id) VALUES(?,?,?)", passedMethod.Value, sortOrder, recipeId)
			if err != nil {
				fmt.Println("Error inserting method:", err)
				http.Error(w, err.Error(), http.StatusInternalServerError)
				return
			}
		}
	}

	json.NewEncoder(w).Encode(getRecipeById(recipeId))
}

func updateMethod(w http.ResponseWriter, r *http.Request) {
	params := mux.Vars(r)
	idStr := params["id"]

	id, err := strconv.Atoi(idStr)
	if err != nil {
		http.Error(w, "Invalid ID parameter", http.StatusInternalServerError)
		return
	}

	var method Method
	json.NewDecoder(r.Body).Decode(&method)
	stmt, err := db.Prepare(`
		UPDATE method
		SET value = ?,
			sortOrder = ?,
		WHERE id = ?
	`)
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}
	_, err = stmt.Exec(
		method.Value,
		method.SortOrder,
		id,
	)
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}
	json.NewEncoder(w).Encode(method)
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

	stmt, err := db.Prepare("DELETE FROM method WHERE id = ?")
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

	// Portion routes
	router.HandleFunc("/portion/{recipe_id}", addPortion).Methods("POST")
	router.HandleFunc("/portion/{id}", deletePortion).Methods("DELETE")
	router.HandleFunc("/portions", getPortions).Methods("GET")

	// Ingredient routes
	router.HandleFunc("/ingredients/{recipe_id}", addIngredients).Methods("POST")
	router.HandleFunc("/ingredient/{id}", deleteIngredient).Methods("DELETE")
	router.HandleFunc("/ingredients", getIngredients).Methods("GET")

	// Method routes
	router.HandleFunc("/methods/{recipe_id}", addMethods).Methods("POST")
	router.HandleFunc("/method/{id}", deleteMethod).Methods("DELETE")

	fmt.Println("Starting server on :8080...")
	http.ListenAndServe(":8080", router)
}
