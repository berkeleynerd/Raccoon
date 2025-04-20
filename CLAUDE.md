# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test Commands
- Build: `./gradlew build`
- Clean build: `./gradlew clean build`
- Run tests: `./gradlew test`
- Run single test: `./gradlew :module-path:test --tests "com.livefast.eattrash.TestClass.testMethod"`
- Check test coverage: `./gradlew koverReport`
- Generate APK: `./gradlew assembleDebug`

## Code Style Guidelines
- Use Kotlin's official coding conventions
- Package structure: `com.livefast.eattrash.raccoonforfriendica`
- Use dependency injection with Kodein
- Navigation via Voyager (Screen/ScreenModel pattern)
- KMP structure - place platform-specific code in respective sourcesets
- Coroutines for async operations with Flow for reactive data
- Use sealed classes for UI state management
- For tests, use Turbine and Mokkery for mocking
- Multiplatform structure follows core/domain/feature module organization