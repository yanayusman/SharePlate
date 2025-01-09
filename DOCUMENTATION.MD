# SharePlate - Food and Non-Food Donation Platform

## Overview
SharePlate is an Android application designed to facilitate food and non-food donations within communities. The app connects donors with recipients and includes features for community events and volunteering opportunities.

## Technical Stack

### Core Technologies
- Language: Java & Kotlin
- Platform: Android (min SDK 34, target SDK 34)
- Backend: Firebase (Authentication, Firestore, Storage, Cloud Messaging)
- Build System: Gradle with Kotlin DSL

### Key Dependencies

```kotlin:app/build.gradle.kts
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

## Application Architecture

### 1. Core Components

#### Authentication
- Email/Password authentication
- Google Sign-in integration
- Email verification system
- User profile management

#### Data Models
- DonationItem: Represents food donations
- NonFoodItem: Represents non-food donations
- Event: Represents community events
- RequestFood/RequestNonFood: Represents donation requests

#### Repository Pattern
The app implements the repository pattern for data management:
- DonationItemRepository: Manages food donations
- NonFoodItemRepository: Manages non-food donations
- EventRepo: Manages community events
- UserRepo: Manages user data
- RequestFoodRepo/RequestNonFoodRepo: Manages donation requests

### 2. Features

#### Donation Management
- Create food and non-food donations
- Upload images with camera or gallery integration
- Track donation status
- Edit and delete donations
- Search and filter donations
- Real-time updates with Firebase

#### Request System
- Create food and non-food requests
- Specify urgency levels
- Track request status
- Location-based matching
- Request notifications

#### Community Features
- Event creation and management
- Volunteering opportunities
- Community campaigns
- User interaction and engagement
- Event status tracking

#### Profile Management
- User profile customization
- Donation history tracking
- Activity statistics
- Favorites management
- Profile image upload

#### AI Integration
- AI-powered chat assistant using Gemini
- Smart donation recommendations
- Content moderation
- User support

### 3. Security

#### Firebase Security
- Secure authentication flow
- Protected database access
- Encrypted data storage
- File storage security
- Real-time database rules

#### Permissions
```xml:app/src/main/AndroidManifest.xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" android:maxSdkVersion="32" />
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
```

### 4. User Interface

#### Material Design
- Implements Material Design 3 components
- Consistent theming across the app
- Responsive layouts
- Custom styles and themes
- Dark mode support

#### Key UI Components
- Custom toolbar with search functionality
- Image handling with Glide
- Progress indicators
- Custom dialogs
- Rating interface
- Bottom navigation

## Development Guidelines

### Code Style
- Follow Java/Kotlin naming conventions
- Use camelCase for methods and variables
- Use PascalCase for class names
- Constants in ALL_CAPS

### Best Practices
- Implement proper error handling
- Use loading indicators for async operations
- Follow MVVM architecture pattern
- Implement proper lifecycle management
- Use Firebase best practices for data structure
- Handle configuration changes properly

### Testing
- Unit tests with JUnit
- Instrumented tests for Android components
- UI testing with Espresso
- Firebase Test Lab integration

## Firebase Structure

### Collections
1. users
   - User profiles
   - Authentication data
   - Preferences
   - Activity statistics

2. allDonationItems
   - Food and non-food donations
   - Status tracking
   - Image references
   - Donor information

3. foodRequest/nonFoodRequest
   - Food and non-food requests
   - Request status
   - User references
   - Location data

4. events
   - Community events
   - Volunteering opportunities
   - Campaign information
   - Event status

## Future Enhancements
1. Enhanced push notifications
2. Real-time chat between users
3. Advanced search and filtering
4. Analytics dashboard
5. Multi-language support
6. Offline capabilities
7. Enhanced security measures
8. Social media integration

## Build and Deployment
- Minimum SDK: 34
- Target SDK: 34
- Build using Gradle with Kotlin DSL
- Firebase configuration required
- Gemini API key required
- ProGuard rules for release builds