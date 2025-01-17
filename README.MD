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
- Feedback system for completed donations
- Facebook integration

### User Interface
- Modern Material Design 3 implementation
- Responsive layouts using ConstraintLayout
- Bottom navigation with Home, Community, Actions, and Profile sections
- Image handling with Glide
- Custom dialogs and loading indicators
- Dark mode support
- Custom fonts (Poppins)

## Tech Stack

### Core Technologies
- Language: Java & Kotlin
- Platform: Android (min SDK 30, target SDK 34)
- Build System: Gradle with Kotlin DSL

### Firebase Integration
- Authentication (Email/Password, Google Sign-in & Facebook)
- Cloud Firestore for data storage
- Firebase Storage for media
- Firebase Cloud Messaging for notifications
- Real-time data synchronization

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
    
    // Facebook SDK
    implementation("com.facebook.android:facebook-login:latest.release")
    implementation("com.facebook.android:facebook-android-sdk:latest.release")
}
```

## Project Setup

### Prerequisites
- Android Studio Hedgehog or newer
- JDK 11 or higher
- Android SDK with minimum API level 30
- Google Services configuration
- Gemini API key for AI features
- Facebook Developer credentials

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

4. Configure Facebook SDK:
   - Add Facebook App ID and Client Token in strings.xml
   - Configure Facebook Developer settings

5. Build and run:
   - Open in Android Studio
   - Sync Gradle files
   - Run on an emulator or physical device (API 30+)

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

## Security
- Firebase Authentication with multiple providers
- Secure file storage with Firebase Storage
- Protected database access rules
- Encrypted user data
- Safe location sharing
- Runtime permissions handling

## License

This project is licensed under the Apache License, Version 2.0. See [LICENSE](LICENSE) for more information.

## Contact

Project Link: [https://github.com/yanayusman/SharePlate]

## Acknowledgments
- Material Design Components
- Firebase Platform
- Google AI (Gemini)
- Android Jetpack Libraries
- Facebook SDK