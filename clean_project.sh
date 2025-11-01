#!/bin/bash

echo "Cleaning Gradle cache and build directories..."

# Stop any running Gradle daemons
./gradlew --stop

# Clean the project
./gradlew clean

# Remove build directories
rm -rf build
rm -rf app/build
rm -rf .gradle

# Remove Gradle caches
rm -rf ~/.gradle/caches/

echo "Clean complete!"
