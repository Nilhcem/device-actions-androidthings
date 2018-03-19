# Google Assistant Device Actions with Android Things (example)

Blog post:  
[http://nilhcem.com/iot/an-introduction-to-device-actions-for-the-google-assistant](http://nilhcem.com/iot/an-introduction-to-device-actions-for-the-google-assistant)


## Hardware

- Raspberry Pi 3
- Blinkt hat
- MPR121 connected to I2C1, touch button on pin #11

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

- Register a device model. In the command below, replace `your-project-id` with your Google Developer Project ID as the model must be globally unique (so it's a good idea to prefix it to help avoid collisions).  
Make sure you run the tool in the same directory you used to generate your credentials:  
```
googlesamples-assistant-devicetool register-model --manufacturer "Assistant SDK developer" \
          --product-name "3D Lamp Assistant" --type LIGHT \
          --trait action.devices.traits.OnOff \
          --trait action.devices.traits.ColorSpectrum \
          --trait action.devices.traits.Brightness \
          --model your-project-id-3dlamp
```

- Verify that the traits are registered:  
```
googlesamples-assistant-devicetool list --model
```

- Register the device instance. In the command below, replace `your-project-id` with your Google Developer Project ID, and change `my-device-id` to a unique device instance id within all of the devices registered under the same Google Developer project. (e.g.: `device01`):  
```
googlesamples-assistant-devicetool register-device --client-type SERVICE \
          --model your-project-id-3dlamp \
          --device my-device-id
```

- Finally, update the `google-assistant/build.gradle` file to change the `ASSISTANT_DEVICE_INSTANCE_ID` and `ASSISTANT_DEVICE_MODEL_ID` buildConfigField to your own values
