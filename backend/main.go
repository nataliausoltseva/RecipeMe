package main

import (
	"database/sql"
	"encoding/json"
	"fmt"
	"log"
	"net/http"
	"strconv"

	"github.com/gorilla/mux"
	_ "github.com/mattn/go-sqlite3"
)

type Portion struct {
	ID          int    `json:"id"`
	Value       string `json:"value"`
	Measurement string `json:"measurement"`
	RecipeID    int    `json:"recipe_id"`
}

type Ingerdient struct {
	ID          int    `json:"id"`
	Name        string `json:"name"`
	Measurement string `json:"measurement"`
	Value       string `json:"value"`
	RecipeID    int    `json:"recipe_id"`
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
	Ingredients []Ingerdient `json:"ingredients"`
	Methods     []Method     `json:"methods"`
	CreatedAt   string       `json:"createdAt"`
}

var recipes []Recipe
var db *sql.DB

func getRecipes(w http.ResponseWriter, r *http.Request) {
	rows, err := db.Query(`
		SELECT *
		FROM recipe
		LEFT JOIN ingredient ON recipe.id = ingredient.recipe_id
		LEFT JOIN portion ON recipe.id = portion.recipe_id
		LEFT JOIN method ON recipe.id = method.recipe_id
	`)
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}
	defer rows.Close()
	var recipes []Recipe
	for rows.Next() {
		var recipe Recipe
		var portion Portion
		var ingredient Ingerdient
		var method Method
		recipe.Ingredients = []Ingerdient{}
		recipe.Methods = []Method{}
		rows.Scan(
			&recipe.ID,
			&recipe.Name,
			&recipe.Url,
			&recipe.ImageUrl,
			&recipe.CreatedAt,
			&portion.ID,
			&portion.Value,
			&portion.Measurement,
			&portion.RecipeID,
			&ingredient.ID,
			&ingredient.Name,
			&ingredient.Measurement,
			&ingredient.Value,
			&ingredient.RecipeID,
			&method.ID,
			&method.SortOrder,
			&ingredient.Value,
			&ingredient.RecipeID,
		)

		if portion.ID != 0 {
			recipe.Portion = &portion
		}

		if ingredient.ID != 0 {
			recipe.Ingredients = append(recipe.Ingredients, ingredient)
		}

		if method.ID != 0 {
			recipe.Methods = append(recipe.Methods, method)
		}

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

	row := db.QueryRow(`
		SELECT
			recipe.*,
			COALESCE(portion.id, 0) AS portion_id,
			portion.value,
			portion.measurement,
			portion.recipe_id,
			ingredient.*,
			method.*
		FROM recipe
		LEFT JOIN ingredient ON recipe.id = ingredient.recipe_id
		LEFT JOIN portion ON recipe.id = portion.recipe_id
		LEFT JOIN method ON recipe.id = method.recipe_id
		WHERE recipe.id = ?
	`, id)

	var recipe Recipe
	var portion Portion
	var ingredient Ingerdient
	var method Method
	recipe.Ingredients = []Ingerdient{}
	recipe.Methods = []Method{}
	var portionId int

	err := row.Scan(
		&recipe.ID,
		&recipe.Name,
		&recipe.Url,
		&recipe.ImageUrl,
		&recipe.CreatedAt,
		&portion.ID,
		&portion.Value,
		&portion.Measurement,
		&portion.RecipeID,
		&ingredient.ID,
		&ingredient.Name,
		&ingredient.Measurement,
		&ingredient.Value,
		&ingredient.RecipeID,
		&method.ID,
		&method.SortOrder,
		&method.Value,
		&method.RecipeID,
		portionId,
	)

	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
	}

	if portionId != 0 {
		recipe.Portion = &portion
	}

	if ingredient.ID != 0 {
		recipe.Ingredients = append(recipe.Ingredients, ingredient)
	}

	if method.ID != 0 {
		recipe.Methods = append(recipe.Methods, method)
	}

	json.NewEncoder(w).Encode(recipe)
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
	_, err = stmt.Exec(recipe.Name, recipe.Url, recipe.ImageUrl, recipe.CreatedAt)
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}
	w.WriteHeader(http.StatusCreated)
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
	w.WriteHeader(http.StatusOK)
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

func createPortion(w http.ResponseWriter, r *http.Request) {
	params := mux.Vars(r)
	idStr := params["recipe_id"]

	recipeId, err := strconv.Atoi(idStr)
	if err != nil {
		http.Error(w, "Invalid ID parameter", http.StatusInternalServerError)
		return
	}

	var portion Portion
	json.NewDecoder(r.Body).Decode(&portion)
	stmt, err := db.Prepare(`
		INSERT INTO portion(value, measurement, recipe_id) VALUES(?,?,?)
	`)
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}
	_, err = stmt.Exec(portion.Value, portion.Measurement, recipeId)
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}
	w.WriteHeader(http.StatusCreated)
}

func updatePortion(w http.ResponseWriter, r *http.Request) {
	params := mux.Vars(r)
	idStr := params["id"]

	id, err := strconv.Atoi(idStr)
	if err != nil {
		http.Error(w, "Invalid ID parameter", http.StatusInternalServerError)
		return
	}

	var portion Portion
	json.NewDecoder(r.Body).Decode(&portion)
	stmt, err := db.Prepare(`
		UPDATE portion
		SET value = ?,
			measurement = ?,
		WHERE id = ?
	`)
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}
	_, err = stmt.Exec(
		portion.Value,
		portion.Measurement,
		id,
	)
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}
	w.WriteHeader(http.StatusOK)
}

func createIngredient(w http.ResponseWriter, r *http.Request) {
	params := mux.Vars(r)
	idStr := params["recipe_id"]

	recipeId, err := strconv.Atoi(idStr)
	if err != nil {
		http.Error(w, "Invalid ID parameter", http.StatusInternalServerError)
		return
	}

	var ingredient Ingerdient
	json.NewDecoder(r.Body).Decode(&ingredient)
	stmt, err := db.Prepare(`
		INSERT INTO portion(name, measurement, value, recipe_id) VALUES(?,?,?,?)
	`)
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}
	_, err = stmt.Exec(ingredient.Name, ingredient.Measurement, ingredient.Value, recipeId)
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}
	w.WriteHeader(http.StatusCreated)
}

func updateIngredient(w http.ResponseWriter, r *http.Request) {
	params := mux.Vars(r)
	idStr := params["id"]

	id, err := strconv.Atoi(idStr)
	if err != nil {
		http.Error(w, "Invalid ID parameter", http.StatusInternalServerError)
		return
	}

	var ingredient Ingerdient
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
	w.WriteHeader(http.StatusOK)
}

func createMethod(w http.ResponseWriter, r *http.Request) {
	params := mux.Vars(r)
	idStr := params["recipe_id"]

	recipeId, err := strconv.Atoi(idStr)
	if err != nil {
		http.Error(w, "Invalid ID parameter", http.StatusInternalServerError)
		return
	}

	var method Method
	json.NewDecoder(r.Body).Decode(&method)
	stmt, err := db.Prepare(`
		INSERT INTO portion(value, sortOrder, recipe_id) VALUES(?,?,?)
	`)
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}
	_, err = stmt.Exec(method.Value, method.SortOrder, recipeId)
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}
	w.WriteHeader(http.StatusCreated)
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
	w.WriteHeader(http.StatusOK)
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
	router.HandleFunc("/portion/{recipe_id}", createPortion).Methods("POST")
	router.HandleFunc("/portion/{id}", updatePortion).Methods("PUT")

	// Ingredient routes
	router.HandleFunc("/ingredient/{recipe_id}", createIngredient).Methods("POST")
	router.HandleFunc("/ingredient/{id}", updateIngredient).Methods("PUT")

	// Method routes
	router.HandleFunc("/mehthod/{recipe_id}", createMethod).Methods("POST")
	router.HandleFunc("/method/{id}", updateMethod).Methods("PUT")

	fmt.Println("Starting server on :8080...")
	http.ListenAndServe(":8080", router)
}
