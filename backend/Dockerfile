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

# Use a minimal image for running the application
FROM alpine:latest

# Install dependencies required for CGO
RUN apk add --no-cache libc6-compat sqlite

# Set the working directory
WORKDIR /root/

# Copy the built application
COPY --from=build /app/backend .

# Copy the entrypoint shell script 
COPY entrypoint.sh /root/entrypoint.sh

# Give permissions to the script for it to run successfully in the docker container
RUN chmod +x /root/entrypoint.sh

# Define a volume for persistent database storage
VOLUME /root/database

# Ensure the Go application has execute permissions
RUN chmod +x /root/backend

# Point the container to run the script first
ENTRYPOINT ["/root/entrypoint.sh"]