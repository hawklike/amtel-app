[![AMTEL Opava App](https://img.shields.io/badge/AMTEL%20Opava-APK-red.svg?style=for-the-badge&logo=android)](https://github.com/hawklike/amtel-app/blob/master/AMTEL_Opava_1.2-beta.apk)

## About the app 📱
*AMTEL Opava* application - the Android application which helps to organize the amateur tennis league. The application is a practical part of my Bachelor thesis at CTU in Prague.

The application itself provides an interface for creating new matches, editing groups in which the teams play, inputting matches scores, showing the actual ranking of teams within groups, presenting teams and players statistics, chatting with other players and many more. The app supports authentication and persistent data layer on the cloud.

## Used technologies ⚙️
* Written in Kotlin with coroutines
* Android Jetpack - LiveData, ViewModel, ViewBinding
* Designed with Material Components
* Firebase - Firebase Authentication and Firebase Cloud Firestore as backend services
* For testing and analysis (plus app distribution) - Firebase Test Lab, Firebase Crashlytics, Firebase App Distribution
* Google Maps API for maps

## Used external libraries 📚
* [libphonenumber](https://github.com/google/libphonenumber) - Formatting and validating phone numbers.
* [FirebaseUI](https://github.com/firebase/FirebaseUI-Android) - UI Bindings for Firebase.
* [MPAndroidChart](https://github.com/PhilJay/MPAndroidChart) - A powerful & easy to use chart library for Android.
* [GmailBackground](https://github.com/luongvo/GmailBackground) - Library to sending emails in background without user interaction.
* [MaterialDrawer](https://github.com/mikepenz/MaterialDrawer) - The flexible, easy to use, all in one drawer library for your Android project.
* [MaterialDialogs](https://github.com/afollestad/material-dialogs) - A beautiful, fluid, and extensible dialogs API for Kotlin & Android.

## Some screenshots 🖼️
<p float="left">
  <img src="/account_TM.png" width="200" />
  <img src="/chat.png" width="200" /> 
  <img src="/group_results.png" width="200" />
</p>

<p float="left">
  <img src="/match_input.png" width="200" />
  <img src="/match_result.png" width="200" /> 
  <img src="/team_profile.png" width="200" />
</p>
