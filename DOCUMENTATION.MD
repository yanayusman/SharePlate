# SharePlate - Food and Non-Food Donation Platform

## Overview
SharePlate is an Android application designed to facilitate food and non-food donations within communities. The app connects donors with recipients and includes features for community events and volunteering opportunities.

## Technical Stack

### Core Technologies
- Language: Java & Kotlin
- Platform: Android (min SDK 30, target SDK 34)
- Backend: Firebase (Authentication, Firestore, Storage, Cloud Messaging)
- Build System: Gradle with Kotlin DSL

### Key Dependencies
```kotlin:app/build.gradle.kts
startLine: 44
endLine: 45
```

## Application Architecture

### 1. Core Components

#### Authentication
- Email/Password authentication
- Google Sign-in integration
- Facebook integration
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
- Profile image upload
- Location preferences

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
startLine: 5
endLine: 14
```

### 4. User Interface

#### Material Design
- Implements Material Design 3 components
- Consistent theming across the app
- Responsive layouts
- Custom styles and themes
- Dark mode support
- Custom fonts (Poppins)

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

## Build and Deployment
- Minimum SDK: 30
- Target SDK: 34
- Build using Gradle with Kotlin DSL
- Firebase configuration required
- Gemini API key required
- ProGuard rules for release builds
- Facebook Developer credentials required