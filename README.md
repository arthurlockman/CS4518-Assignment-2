# WPI CS 4518 Assignment 2

The goal for this project was to gain familiarity with developing Android apps that use the Android Camera, images, and local storage.

## Project Requirements

The sections below detail how each of the project requirements were fulfilled in this project implementation.

### 3.1: Fix gradle compile error (10 points)

The goal of this section was to fix a bug in the gradle build file that was preventing the application from building and installing on devices.

The issue in the gradle build file `./app/build.gradle` was that the `compileSdkVersion` was set to 21, which is greater than the `buildToolsVersion`, which was set to 20.0.0 initially. To fix this issue, I incremented the `buildToolsVersion` to 21.1.2, which is the version recommended by Android Studio over using 21.0.0.

### 3.2: Display more images (30 points)

The goal of this section was to make it so that the app could take more images. It required adding three additional image slots below the application view, and filling those with new images that are taken.

To display more images, I first added three more `ImageView` elements to the bottom of the view, along with a label. 

### 3.3: Recycle views (30 points)

The goal of this portion was to modify how images were taken, so that only 4 additional images could be taken and stored as part of the application. If a user took more than 4 images, subsequent images would replace the previous ones in order of their position on screen.

### 3.4: Save captured images to the gallery (30 points)

The goal of this section is to make it so that images that are taken in the app are also saved to the gallery, in addition to being stored in the app itself.