# RecipeMe

## Idea:
A way to host the storage on your machine and have full control over where the data is saved, how manipulated. No ads, all features available.

## Roadmap:
1. Setup docker container for backend (goLang is going to be used)
2. Create API in Go. Basic CRUD will do for now. The data should be saved to the locally stored database.
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
3. Setup basic mobile app using Kotlin. The app should be able to connect to the API and the CRUD functionality works as expected.
4. Implement and update the design of the application.
5. Add functionality of increasing or decreasing the portion which results in the updated list of ingredients. 
6. Exporting functionality of the recipe as a long screenshot.
7. Make a companion watch app.
8. Add Sort functionality in the app by:
    1. Name
    2. Portion
    3. Create Date
9. Filter out the recipes by ingredients. Being able to uncheck the ingredients and hide the recipes.
10. Look into Gemini integration so that when asking to look for a recipe, it looks through this app first.