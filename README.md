# Google Assistant Device Actions with Android Things (example)

## Setting up the Google Assistant

- Enable the Google Assistant API and create an OAuth Client ID (name=`androidthings-googleassistant`) (Read [here](https://developers.google.com/assistant/sdk/guides/service/python/embed/config-dev-project-and-account) for help)
- Download the `client_secret_NNNN.json` file from the [credentials section of the Console](https://console.developers.google.com/apis/credentials)
- Use the [`google-oauthlib-tool`](https://github.com/GoogleCloudPlatform/google-auth-library-python-oauthlib) to generate credentials:
```
pip install google-auth-oauthlib[tool]
google-oauthlib-tool --scope https://www.googleapis.com/auth/assistant-sdk-prototype \
          --save --headless --client-secrets /path/to/client_secret_client-id.json
```

- Copy the saved credentials (`~/.config/google-oauthlib-tool/credentials.json`) to `./google-assistant/src/main/res/raw/`

- Open the [Activity Controls](https://developers.google.com/assistant/sdk/prototype/getting-started-other-platforms/config-dev-project-and-account#set-activity-controls) page for the Google account that you want to use with the Assistant, and ensure the following toggle switches are enabled: Web & App Activity, Device Information, Voice & Audio Activity


## Register the device

- Install the Google Assistant SDK package:  
```
pip install --upgrade google-assistant-sdk
```

- Register a device model, in order for the Google Assistant to respond to commands appropriate to your device and the given context.  
In the following command, replace `my-model` with whatever you want to call your model. Note that this name must be globally unique so you should use your Google Developer Project ID as a prefix to help avoid collisions (for example, my-dev-project-my-model1).  
Make sure you run the tool in the same directory you used to generate your credentials:  
```
googlesamples-assistant-devicetool register-model --manufacturer "Assistant SDK developer" \
          --product-name "Assistant SDK light" --type LIGHT --model my-model
```

- Now query the server for the model you just created:  
```
googlesamples-assistant-devicetool get --model my-model
```


## Register Traits for the Device

- Add an OnOff, and a ColorSpectrum trait:  
```
googlesamples-assistant-devicetool register-model --manufacturer "Assistant SDK developer" \
          --product-name "Assistant SDK light" --type LIGHT --trait action.devices.traits.OnOff \
          --trait action.devices.traits.ColorSpectrum \
          --model my-model
```

- Verify that the traits are registered:  
```
googlesamples-assistant-devicetool list --model
```

- Register the device instance
In the command below, change `my-model` to the name you used for your model, and change `my-device-id` to a unique device instance id within all of the devices registered under the same Google Developer project. (e.g.: `device01`):  
```
googlesamples-assistant-devicetool register-device --client-type SERVICE \
          --model my-model --device my-device-id
```

- Update the `google-assistant/build.gradle` file to change the `ASSISTANT_DEVICE_ID` and `ASSISTANT_DEVICE_MODEL_ID` buildConfigField to your own values


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
