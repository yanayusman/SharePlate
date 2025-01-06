# SharePlate Android App

SharePlate is an Android application built with modern Android development practices. The app appears to focus on food sharing or plate-related social interactions, based on the branding elements visible in the main activity layout.

## Features

- Modern Material Design UI
- Responsive layouts using ConstraintLayout
- User authentication system (Login/Register flow)

## Tech Stack

- Java
- Android SDK
- Gradle Build System
- Firebase Integration (Google Services)
- AndroidX Components
- Material Design Components

## Project Setup

### Prerequisites

- Android Studio Arctic Fox or newer
- JDK 11 or higher
- Android SDK with minimum API level as specified in build.gradle
- Google Services configuration

### Building the Project

1. Clone the repository
2. Open the project in Android Studio
3. Sync Gradle files
4. Run the application on an emulator or physical device

### Gradle Configuration

The project uses Gradle with Kotlin DSL for build automation. Key configurations can be found in:

kotlin:build.gradle.kts
startLine: 1
endLine: 18

### Main UI Components

The app features a clean, modern UI with:
- Circular logo container
- Bottom sheet design
- Material Design buttons

The main layout can be found in:

xml:app/src/main/res/layout/activity_main.xml
startLine: 1
endLine: 145

## ProGuard Rules

The project includes ProGuard rules for app optimization and security. See the configuration in:

text:app/proguard-rules.pro
startLine: 1
endLine: 21

## License

This project is licensed under the Apache License, Version 2.0. See the license header in:

text:gradlew.bat
startLine: 1
endLine: 14

## Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## Contact

Project Link: [https://github.com/yanayusman/SharePlate]
