# WPI CS 4518 Assignment 2

The goal for this project was to gain familiarity with developing Android apps that use the Android Camera, images, and local storage.

## Project Requirements

The sections below detail how each of the project requirements were fulfilled in this project implementation.

### 3.1: Fix gradle compile error (10 points)

The goal of this section was to fix a bug in the gradle build file that was preventing the application from building and installing on devices.

The issue in the gradle build file `./app/build.gradle` was that the `compileSdkVersion` was set to 21, which is greater than the `buildToolsVersion`, which was set to 20.0.0 initially. To fix this issue, I incremented the `buildToolsVersion` to 21.1.2, which is the version recommended by Android Studio over using 21.0.0.

### 3.2: Display more images (30 points)

The goal of this section was to make it so that the app could take more images. It required adding three additional image slots below the application view, and filling those with new images that are taken.

To display more images, I first added three more `ImageView` elements to the bottom of the view, along with a label to indicate that there were more images. I also wrapped the entire view in a `ScrollView`, so that in landscape mode the images wouldn't get cut off and the view would scroll to reveal them.

I then modified the `getPhotoFilename()` method in the Crime class to take in an image index, to account for saving more images. This generates a new image filename for each new image slot, provided you give it a different index number for each. I also modofied `getPhotoFile()` inside CrimeLab to handle the same index.

In CrimeFragment, I wrote the backing code to handle the image views, and to get them to grab their proper images from the CrimeLab. 

Finally, I set the photo button in the application to figure out which image it was supposed to be taking in sequential order. The last image slot filled is stored in the Crime element, and set through the CrimeLab. Each time the photo button is pressed, this number is incremented (from 0 to 3, reset after 3) so that it always fills the next available image slot.

The image previews are updated in `CrimeFragment.updatePhotoView()`, ensuring that they're all updated every time a new image is taken or the activity is created.

### 3.3: Recycle views (30 points)

The goal of this portion was to modify how images were taken, so that only 4 additional images could be taken and stored as part of the application. If a user took more than 4 images, subsequent images would replace the previous ones in order of their position on screen.

The code from the previous section handles this. The code that fills the next image in `CrimeFragment.onCreateView()` also handles cycling through the photo views:

    mPhotoButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
          //On click, select a new photo slot to fill
          mLastPhoto = (mLastPhoto >= 3) ? 0 : mLastPhoto + 1;
          File photoFile;
          switch (mLastPhoto) //Switch which photo slot to fill
          {
              case 0:
                  photoFile = mPhotoFilePrimary;
                  break;
              case 1:
                  photoFile = mPhotoFileSecondary1;
                  break;
              case 2:
                  photoFile = mPhotoFileSecondary2;
                  break;
              case 3:
                  photoFile = mPhotoFileSecondary3;
                  break;
              default:
                  photoFile = mPhotoFilePrimary;
                  break;
          }
          Uri uri = Uri.fromFile(photoFile); //Fill selected slot
          CrimeLab.get(getActivity()).setLastPhoto(mCrime, mLastPhoto); //Store last taken photo
          captureImage.putExtra(MediaStore.EXTRA_OUTPUT, uri);
          startActivityForResult(captureImage, REQUEST_PHOTO);
      }
    });
This code is fired whenever the photo button is clicked. The conditional statement and switch statement handle cycling between the image views as images are taken. This ensures that the photos replace the older photos in the correct order.

### 3.4: Save captured images to the gallery (30 points)

The goal of this section is to make it so that images that are taken in the app are also saved to the gallery, in addition to being stored in the app itself.

Implementing this required two changes to the application. The first was in the Android manifest file. In addition to having the `READ_EXTERNAL_STORAGE` permission, the application now needed the `WRITE_EXTERNAL_STORAGE` permission as well to be able to save images to the gallery.

The second change was within `CrimeFragment.onActivityResult()`. Previously, when an image result came in this method only refreshed the image views. To get the images saved to the gallery, a line needed to be added (with some additional logic) to save the images to the gallery:

```
MediaStore.Images.Media.insertImage(resolver, photoFile.getPath(), "Crime Photo", "Taken with the CriminalIntent app.");
```

This line saves the selected image to the gallery through the MediaStore API.

On my testing device, images taken with the `MediaStore.ACTION_IMAGE_CAPTURE` intent automatically were saved to the gallery by the camera intent. I suspect this was a fault of Google Photos intercepting all photos taken on the device. This leads to each image being saved in the gallery twice when taken with the CriminalIntent app, even though the app is only saving each once.

### 3.5: Face Detection (Extra 15 points)

The goal of this section is to enable face detection in the image preview window.

### 3.6: Target API Level 25 (Extra 10 points)

The goal of this section was to update the app to be compliant with the latest API level of 25. 