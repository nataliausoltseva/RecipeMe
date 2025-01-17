package main

import (
	"database/sql"
	"encoding/json"
	"fmt"
	"log"
	"net/http"

	"github.com/gorilla/mux"
	_ "github.com/mattn/go-sqlite3"
)

type Portion struct {
	Value       string `json:"value"`
	Measurement string `json:"measurement"`
}

type Ingerdient struct {
	Name        string `json:"name"`
	Measurement string `json:"measurement"`
	Value       string `json:"value"`
}

type Recipe struct {
	ID          string       `json:"id"`
	Name        string       `json:"name"`
	Portion     Portion      `json:"portion"`
	Url         string       `json:"url"`
	ImageUrl    string       `json:"imageUrl"`
	Ingredients []Ingerdient `json:"ingredients"`
	Methods     []string     `json:"methods"`
	CreatedAt   string       `json:"createdAt"`
}

var recipes []Recipe
var db *sql.DB

func getRecipes(w http.ResponseWriter, r *http.Request) {
	// json.NewEncoder(w).Encode(recipes)
	rows, err := db.Query("SELECT id, name, url, imageUrl, createdAt FROM recipes")
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}
	defer rows.Close()
	var recipes []Recipe
	for rows.Next() {
		var recipe Recipe
		rows.Scan(&recipe.ID, &recipe.Name, &recipe.Url, &recipe.ImageUrl, &recipe.CreatedAt)
		recipes = append(recipes, recipe)
	}
	json.NewEncoder(w).Encode(recipes)
}

func GetRecipe(w http.ResponseWriter, r *http.Request) {
	// params := mux.Vars(r)
	// for _, recipe := range recipes {
	// 	if recipe.ID == params["id"] {
	// 		json.NewEncoder(w).Encode(recipe)
	// 		return
	// 	}
	// }
	// http.NotFound(w, r)
	params := mux.Vars(r)
	id := params["id"]
	row := db.QueryRow("SELECT id, name, url, imageUrl, createdAt FROM recipes WHERE id = ?", id)
	var recipe Recipe
	err := row.Scan(&recipe.ID, &recipe.Name, &recipe.Url, &recipe.ImageUrl, &recipe.CreatedAt)
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}
	json.NewEncoder(w).Encode(recipe)
}

func createRecipe(w http.ResponseWriter, r *http.Request) {
	var recipe Recipe
	json.NewDecoder(r.Body).Decode(&recipe)
	// recipe.ID = string(len(recipes) + 1)
	// recipes = append(recipes, recipe)
	// json.NewEncoder(w).Encode(recipe)

	// TODO need to add portion, ingredients, methods later
	stmt, err := db.Prepare("INSERT INTO recipes(name) VALUES(?)")
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}
	_, err = stmt.Exec(recipe.Name)
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}
	w.WriteHeader(http.StatusCreated)
}

func updateItem(w http.ResponseWriter, r *http.Request) {
	// params := mux.Vars(r)
	// for index, recipe := range recipes {
	// 	if recipe.ID == params["id"] {
	// 		recipes = append(recipes[:index], recipes[index+1:]...)
	// 		var newRecipe Recipe
	// 		_ = json.NewDecoder(r.Body).Decode(&newRecipe)
	// 		newRecipe.ID = params["id"]
	// 		recipes = append(recipes, newRecipe)
	// 		json.NewEncoder(w).Encode(newRecipe)
	// 		return
	// 	}
	// }
	// http.NotFound(w, r)

	params := mux.Vars(r)
	id := params["id"]
	var recipe Recipe
	json.NewDecoder(r.Body).Decode(&recipe)
	stmt, err := db.Prepare("UPDATE recipes SET Name = ?, Url = ?, ImageUrl = ?, CreatedAt = ? WHERE id = ?")
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}
	_, err = stmt.Exec(recipe.Name, recipe.Url, recipe.ImageUrl, recipe.CreatedAt, id)
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}
	w.WriteHeader(http.StatusOK)
}

func deleteRecipe(w http.ResponseWriter, r *http.Request) {
	params := mux.Vars(r)
	for index, recipe := range recipes {
		if recipe.ID == params["id"] {
			recipes = append(recipes[:index], recipes[index+1:]...)
			break
		}
	}
	json.NewEncoder(w).Encode(recipes)
}

func main() {
	var err error
	db, err = sql.Open("sqlite3", "./database/database.db")
	if err != nil {
		log.Fatal(err)
	}
	defer db.Close()

	router := mux.NewRouter().StrictSlash(true)
	router.HandleFunc("/recipes", getRecipes).Methods("GET")
	router.HandleFunc("/recipe/{id}", GetRecipe).Methods("GET")
	router.HandleFunc("/recipe", createRecipe).Methods("POST")
	router.HandleFunc("/recipe/{id}", updateItem).Methods("PUT")
	router.HandleFunc("/recipe/{id}", deleteRecipe).Methods("DELETE")
	fmt.Println("Starting server on :8080...")
	http.ListenAndServe(":8080", router)
}
