# SharePlate Android App

SharePlate is a community-driven donation platform that facilitates food and non-food donations. The app connects donors with recipients and includes features for community events and volunteering opportunities.

## Features

### Core Features
- Food and non-food donation management
- Request system for those in need
- Community events and campaigns
- Real-time chat with AI assistant
- User profile management
- Image upload and management
- Location-based services
- Push notifications

### User Interface
- Modern Material Design 3 implementation
- Responsive layouts using ConstraintLayout
- Bottom navigation with Home, Community, Actions, and Profile sections
- Image handling with Glide
- Custom dialogs and loading indicators
- Dark mode support

## Tech Stack

### Core Technologies
- Language: Java & Kotlin
- Platform: Android (min SDK 34, target SDK 34)
- Build System: Gradle with Kotlin DSL

### Firebase Integration
- Authentication (Email/Password & Google Sign-in)
- Cloud Firestore for data storage
- Firebase Storage for media
- Firebase Cloud Messaging for notifications

### Key Dependencies
```kotlin
dependencies {
    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.firebase:firebase-messaging:24.1.0")

    // UI Components
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")
    
    // Image Loading
    implementation("com.github.bumptech.glide:glide:4.16.0")
    
    // Location Services
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("com.google.android.libraries.places:places:3.3.0")
    
    // AI Integration
    implementation("com.google.ai.client.generativeai:generativeai:0.1.1")
}
```

## Project Setup

### Prerequisites
- Android Studio Hedgehog or newer
- JDK 17 or higher
- Android SDK with minimum API level 34
- Google Services configuration
- Gemini API key for AI features

### Building the Project
1. Clone the repository
```bash
git clone https://github.com/yanayusman/SharePlate
```

2. Add required API keys:
   - Create a `local.properties` file in the project root
   - Add your Gemini API key:
   ```properties
   GEMINI_API_KEY=your_api_key_here
   ```

3. Add Firebase configuration:
   - Add your `google-services.json` to the app directory
   - Configure Firebase services in the Firebase Console

4. Build and run:
   - Open in Android Studio
   - Sync Gradle files
   - Run on an emulator or physical device

## Architecture

### MVVM Pattern
- View Layer: Activities and Fragments
- ViewModel: Data handling and business logic
- Repository: Data access and management
- Model: Data classes and entities

### Key Components
- `SharePlateChatbot`: AI-powered chat assistant
- `DonationItemRepository`: Manages donation data
- `UserRepo`: Handles user management
- `EventRepo`: Manages community events
- `NotificationRepo`: Handles push notifications

## Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## Security

- Implements Firebase Authentication
- Secure file storage with Firebase Storage
- Protected database access rules
- Encrypted user data
- Safe location sharing

## License

This project is licensed under the Apache License, Version 2.0. See [LICENSE](LICENSE) for more information.

## Contact

Project Link: [https://github.com/yanayusman/SharePlate]

## Acknowledgments

- Material Design Components
- Firebase Platform
- Google AI (Gemini)
- Android Jetpack Libraries
