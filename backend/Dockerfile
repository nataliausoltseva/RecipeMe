# Use the official Golang 1.23.4 image as a build stage
FROM golang:1.23.4-alpine AS build

# Install dependencies required for CGO and SQLite
RUN apk add --no-cache gcc musl-dev sqlite

# Set the working directory
WORKDIR /app

# Copy go.mod and go.sum
COPY ./go.mod ./go.sum ./

# Download all dependencies
RUN go mod download

# Copy the source code into the container
COPY ./ .

# Enable CGO and build the application
ENV CGO_ENABLED=1
RUN go build -o backend ./main.go

# RUN mkdir -p ./database
# # Create the SQLite database and tables
# RUN sqlite3 ./database/database.db "CREATE TABLE recipes (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, url TEXT, createdAt TEXT, lastEditedAt TEXT, type TEXT, sortOrder INTEGER);"
# RUN sqlite3 ./database/database.db "CREATE TABLE portions (id INTEGER PRIMARY KEY AUTOINCREMENT, value DOUBLE, measurement TEXT, recipe_id INTEGER, FOREIGN KEY (recipe_id) REFERENCES recipe(id) ON DELETE CASCADE ON UPDATE NO ACTION);"
# RUN sqlite3 ./database/database.db "CREATE TABLE ingredients (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, measurement TEXT, value DOUBLE, sortOrder INTEGER, recipe_id INTEGER, FOREIGN KEY (recipe_id) REFERENCES recipe(id) ON DELETE CASCADE ON UPDATE NO ACTION);"
# RUN sqlite3 ./database/database.db "CREATE TABLE methods (id INTEGER PRIMARY KEY AUTOINCREMENT, value TEXT, sortOrder INTEGER, recipe_id INTEGER, FOREIGN KEY (recipe_id) REFERENCES recipe(id) ON DELETE CASCADE ON UPDATE NO ACTION);"
# RUN sqlite3 ./database/database.db "CREATE TABLE images (id INTEGER PRIMARY KEY AUTOINCREMENT, url string, filename TEXT, recipe_id INTEGER, FOREIGN KEY (recipe_id) REFERENCES recipe(id) ON DELETE CASCADE ON UPDATE NO ACTION);"

# Debugging step to check if the database file is created
# RUN ls -l ./database

# Use a minimal image for running the application
FROM alpine:latest

# Install dependencies required for CGO
RUN apk add --no-cache libc6-compat sqlite

# Set the working directory
WORKDIR /root/

# Copy the built application
COPY --from=build /app/backend .
# COPY --from=build /app/database/database.db ./database/

COPY entrypoint.sh /root/entrypoint.sh
RUN chmod +x /root/entrypoint.sh


# Define a volume for persistent database storage
VOLUME /root/database

# Ensure the Go application has execute permissions
RUN chmod +x /root/backend

# Command to run the application
# CMD ["./backend"]
ENTRYPOINT ["/root/entrypoint.sh"]