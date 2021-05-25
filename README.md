# ActiveCaptain Community SDK - Android
The ActiveCaptain Community SDK contains functions for storing and rendering data from a SQLite database exported from [ActiveCaptain Community](https://activecaptain.garmin.com).

# Quick Start
* Download the debug or release .aar file from the [latest release](https://github.com/garmin/ActiveCaptainCommunitySDK-android/releases/latest).
* Put the .aar file somewhere in your project folder.  If your module is called "app", you might put it in app/libs.
* In your module's build.gradle file (app/build.gradle), add a dependency on the .aar file:
  ```Groovy
  dependencies {
      implementation files('LIBS_PATH_HERE/activecaptaincommunitysdk-release-VERSION_NUMBER_HERE.aar')
  }
  ```
  Replace LIBS_PATH_HERE with the relative path to the .aar file and VERSION_NUMBER_HERE with the version number from the filename.  For example: ```libs/activecaptaincommunitysdk-release-2.0.1.aar```

# Requesting a Stage API Key
* Create an account on [ActiveCaptain Community](https://activecaptain.garmin.com).  If you already have a personal account, create a separate account for app development only.
* Go to the [Developer page](https://activecaptain.garmin.com/Developer) and click the Request Access button.  Fill out the information form and agree to the terms and conditions.
* Once you have access to the Developer Portal, you can access it [here](https://activecaptain.garmin.com/Profile/DeveloperPortal).
* In the Developer Portal, click "Add Application" and give your application a name.
* Your app will be assigned a Stage API key.  Click the eye icon to view it.

# Building and Running Sample App
* Clone the repository.
* Initialize submodules recursively: ```git submodule update --init --recursive```
* In Android Studio, open the repository directory.
* In app\src\debug\java\com\garmin\marine\activecaptainsample\ActiveCaptainConfiguration.java, replace "STAGE_API_KEY_HERE" with your Stage API key.
* Build > Make Project
* Run > Debug 'app'

# Using the Sample App
* Log in to Garmin SSO.  Create a new account if needed.
* Wait a few minutes for data exports to be downloaded and installed.
* Initial marker will be displayed.
  * Use the magnifying glass to search for other markers by name.
  * Use the links on the page to display additional information or edit the marker.
