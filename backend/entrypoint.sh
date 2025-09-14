#!/bin/sh

# Ensure database folder exists
mkdir -p /root/database

# Only create database if it doesn't exist
if [ ! -f /root/database/database.db ]; then
    echo "Initializing database..."
    sqlite3 /root/database/database.db <<EOF
    CREATE TABLE recipes (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        name TEXT,
        url TEXT,
        createdAt TEXT,
        lastEditedAt TEXT,
        type TEXT,
        sortOrder INTEGER
    );

    CREATE TABLE portions (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        value DOUBLE, measurement TEXT,
        recipe_id INTEGER,
        FOREIGN KEY (recipe_id) REFERENCES recipe(id) ON DELETE CASCADE ON UPDATE NO ACTION
    );

    CREATE TABLE ingredients (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        name TEXT,
        measurement TEXT,
        value DOUBLE,
        sortOrder INTEGER,
        recipe_id INTEGER,
        FOREIGN KEY (recipe_id) REFERENCES recipe(id) ON DELETE CASCADE ON UPDATE NO ACTION
    );

    CREATE TABLE methods (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        value TEXT,
        sortOrder INTEGER,
        recipe_id INTEGER,
        FOREIGN KEY (recipe_id) REFERENCES recipe(id) ON DELETE CASCADE ON UPDATE NO ACTION
    );

    CREATE TABLE images (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        url string,
        filename TEXT,
        recipe_id INTEGER,
        FOREIGN KEY (recipe_id) REFERENCES recipe(id) ON DELETE CASCADE ON UPDATE NO ACTION
    );

    CREATE TABLE method_ingredients (
        method_id INTEGER NOT NULL,
        ingredient_id INTEGER NOT NULL,
        FOREIGN KEY (method_id) REFERENCES methods(id) ON DELETE CASCADE,
        FOREIGN KEY (ingredient_id) REFERENCES ingredients(id) ON DELETE CASCADE,
        PRIMARY KEY (method_id, ingredient_id)
    );
EOF
fi


# Check if custom command is provided to modify tables
if [ "$#" -eq 3 ]; then
    echo "Adding column '$2' of type '$3' to table '$1'..."
    sqlite3 /root/database/database.db "ALTER TABLE $1 ADD COLUMN $2 $3;"
fi

# Run the backend application
exec ./backend
