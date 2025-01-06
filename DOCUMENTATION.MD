# SharePlate - Food and Non-Food Donation Platform

## Overview
SharePlate is an Android application designed to facilitate food and non-food donations within communities. The app connects donors with recipients and includes features for community events and volunteering opportunities.

## Technical Stack

### Core Technologies
- Language: Java
- Platform: Android (min SDK 34, target SDK 34)
- Backend: Firebase (Authentication, Firestore, Storage)
- Build System: Gradle

### Key Dependencies

```44:102:app/build.gradle.kts
dependencies {
    // Firebase BoM - Use this to manage Firebase dependency versions
    implementation(platform("com.google.firebase:firebase-bom:33.7.0")) // Use the latest version
...

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // Other Libraries
    implementation("com.makeramen:roundedimageview:2.3.0")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.0.0")

    // Google Play Services Tasks (ensure latest)
    implementation("com.google.android.gms:play-services-tasks:18.2.0")

    // Firebase Auth and Google Play Services
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.android.gms:play-services-auth:21.3.0")

    // SwipeRefreshLayout
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    // Add Firebase Storage
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation("com.google.firebase:firebase-storage:21.0.1")

    // Add Glide for image loading (optional but recommended)
    implementation("com.github.bumptech.glide:glide:4.16.0")
    
     // LocalBroadcastManager
    implementation("androidx.localbroadcastmanager:localbroadcastmanager:1.1.0")
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

### 2. Features

#### Donation Management
- Create food and non-food donations
- Upload images with camera or gallery integration
- Track donation status
- Edit and delete donations
- Search and filter donations

#### Request System
- Create food and non-food requests
- Specify urgency levels
- Track request status
- Location-based matching

#### Community Features
- Event creation and management
- Volunteering opportunities
- Community campaigns
- User interaction and engagement

#### Profile Management
- User profile customization
- Donation history tracking
- Activity statistics
- Favorites management

### 3. Security

#### Firebase Security
- Secure authentication flow
- Protected database access
- Encrypted data storage

```171:179:app/src/main/java/com/example/shareplate/SignupActivity.java
                                        // Send verification email
                                        user.sendEmailVerification()
                                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<Void> task) {
                                                        signupButton.setEnabled(true);
                                                        if (task.isSuccessful()) {
                                                            Toast.makeText(SignupActivity.this,
```


#### Permissions
The app requires and handles the following permissions:

```5:14:app/src/main/AndroidManifest.xml
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"
        android:maxSdkVersion="32" />
    <uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />

    <uses-feature android:name="android.hardware.camera" android:required="false" />
```


### 4. User Interface

#### Material Design
- Implements Material Design 3 components
- Consistent theming across the app
- Responsive layouts
- Custom styles and themes

#### Key UI Components
- Custom toolbar with search functionality
- Image handling with Glide
- Progress indicators
- Custom dialogs
- Rating interface

## Development Guidelines

### Code Style
- Follow Java naming conventions
- Use camelCase for methods and variables
- Use PascalCase for class names
- Constants in ALL_CAPS

### Best Practices
- Implement proper error handling
- Use loading indicators for async operations
- Follow MVVM architecture pattern
- Implement proper lifecycle management
- Use Firebase best practices for data structure

### Testing
- Unit tests with JUnit
- Instrumented tests for Android components
- UI testing with Espresso

## Firebase Structure

### Collections
1. users
   - User profiles
   - Authentication data
   - Preferences

2. allDonationItems
   - Food and non-food donations
   - Status tracking
   - Image references

3. foodRequest
   - Food and non-food requests
   - Request status
   - User references

4. events
   - Community events
   - Volunteering opportunities
   - Campaign information

## Future Enhancements
1. Implement push notifications
2. Add real-time chat functionality
3. Enhance search and filtering capabilities
4. Add analytics and reporting
5. Implement multi-language support
6. Add offline capabilities
7. Enhance security measures

## Build and Deployment
- Minimum SDK: 34
- Target SDK: 34
- Build using Gradle
- Firebase configuration required
- ProGuard rules for release builds
