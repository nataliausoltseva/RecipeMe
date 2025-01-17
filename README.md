# RecipeMe

## API and database
SQLite is used for this application as the database engine. The API is using [go-sqlite3](https://github.com/mattn/go-sqlite3) for storing/retrieving the data from the endpoints.

All backend logic is located in the `/backend` directory. It also includes the `docker-compose.yml` and `Dockerfile` that setup the container for the API. On the docker container, the `/database` directory is created that includes `database.db` file. So that, there is no need to install SQLite on your machine and handle that on the docker instead. By default, the following tables are created:

1. The **recipe** table:
```sqlite
CREATE TABLE recipe(
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT,
    url TEXT,
    imageUrl TEXT,
    methods TEXT,
    createdAt TEXT
);
```

2. The **portion** table:
```sqlite
CREATE TABLE portion(
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    value TEXT,
    measurement TEXT,
    FOREIGN KEY (recipe_id) REFERENCES recipe(id) ON DELETE CASCADE ON UPDATE NO ACTION
);
```

3. The **ingredient** table:
```sqlite
CREATE TABLE ingredient(
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT,
    measurement TEXT,
    value TEXT,
    recipe_id INTEGER,
    FOREIGN KEY (recipe_id) REFERENCES recipe(id) ON DELETE CASCADE ON UPDATE NO ACTION
);
```

The **recipe** table has a relationsip `has_one` **portion** and `has_many` **ingredient**s.


## Idea:
A way to host the storage on your machine and have full control over where the data is saved, how manipulated. No ads, all features available.

## Roadmap:
- Create API in Go. Basic CRUD will do for now. The data should be saved to the locally stored database.
```json
example of possible data for API
{
    "name": "Test",
    "portion": {
        "value": 2,
        "measurement": "days", // can be just "days" for now
    },
    "url": "", // the URL of the original recipe
    "image": "",
    "ingredients":  [
        {
            "name": "Onion",
            "measurement": "items", // can be items, grams, kgs, ml, cups, cans 
            "value": 1,
        }
    ],
    "methods": [
        "Turn the stove on",
        "Crack an egg",
        "Tun off the stove",
        "Clean the stove"
    ],
    "createdAt": "",
}
```
- Setup basic mobile app using Kotlin. The app should be able to connect to the API and the CRUD functionality works as expected.
- Implement and update the design of the application.
- Add functionality of increasing or decreasing the portion which results in the updated list of ingredients. 
- Exporting functionality of the recipe as a long screenshot.
- Make a companion watch app.
- Add Sort functionality in the app by:
    1. Name
    2. Portion
    3. Create Date
- Filter out the reipecs by ingredients. Being able to uncheck the ingredients and hide the recipes.
- Look into Gemini integration so that when asking to look for a recipe, it looks through this app first.
- Ability to have "Cooking mode" so that the phone screen does not turn off.
    1. Look into this feature on the watch as well.
<details>
    <summary><strong>Completed items</strong></summary>
- Setup docker container for backend (goLang is going to be used) ✅
</details>