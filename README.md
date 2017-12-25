# Google Assistant Device Actions with Android Things (example)

## Setting up the Google Assistant

- Enable the Google Assistant API and create an OAuth Client ID (name=`androidthings-googleassistant`) (Read [here](https://developers.google.com/assistant/sdk/prototype/getting-started-other-platforms/config-dev-project-and-account) for help)
- Download the `client_secret_NNNN.json` file from the [credentials section of the Console](https://console.developers.google.com/apis/credentials)
- Use the [`google-oauthlib-tool`](https://github.com/GoogleCloudPlatform/google-auth-library-python-oauthlib) to generate credentials:
```
pip install google-auth-oauthlib[tool]
google-oauthlib-tool --client-secrets client_secret_NNNN.json \
					 --credentials ./google-assistant/src/main/res/raw/credentials.json \
					 --scope https://www.googleapis.com/auth/assistant-sdk-prototype \
					 --save
```
- Make sure to set the [Activity Controls](https://developers.google.com/assistant/sdk/prototype/getting-started-other-platforms/config-dev-project-and-account#set-activity-controls) for the Google Account using the application.


## Deploy the Android Things app

- On the first install, grant the sample required permissions for audio and internet access:
```bash
./gradlew :app:assembleDebug
adb install -g app/build/outputs/apk/debug/app-debug.apk
```
- On Android Studio, click on the "Run" button or on the command line, type:
```bash
adb shell am start com.nilhcem.androidthings.deviceactions/.MainActivity
```
