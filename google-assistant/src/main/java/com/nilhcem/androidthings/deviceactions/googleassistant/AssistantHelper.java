package com.nilhcem.androidthings.deviceactions.googleassistant;

import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.OnLifecycleEvent;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.google.assistant.embedded.v1alpha2.AssistConfig;
import com.google.assistant.embedded.v1alpha2.AssistRequest;
import com.google.assistant.embedded.v1alpha2.AssistResponse;
import com.google.assistant.embedded.v1alpha2.AudioInConfig;
import com.google.assistant.embedded.v1alpha2.AudioOut;
import com.google.assistant.embedded.v1alpha2.AudioOutConfig;
import com.google.assistant.embedded.v1alpha2.DeviceConfig;
import com.google.assistant.embedded.v1alpha2.DialogStateIn;
import com.google.assistant.embedded.v1alpha2.DialogStateOut;
import com.google.assistant.embedded.v1alpha2.EmbeddedAssistantGrpc;
import com.google.assistant.embedded.v1alpha2.SpeechRecognitionResult;
import com.google.protobuf.ByteString;
import com.nilhcem.androidthings.deviceactions.googleassistant.model.DeviceAction;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import org.json.JSONException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.auth.MoreCallCredentials;
import io.grpc.stub.StreamObserver;

public class AssistantHelper implements LifecycleObserver {

    private static final String TAG = AssistantHelper.class.getSimpleName();

    // Device Action Json Adapter
    private static final JsonAdapter<DeviceAction> JSON_ADAPTER = new Moshi.Builder().build().adapter(DeviceAction.class);

    // Audio constants.
    private static final int SAMPLE_RATE = 16000;
    private static final int ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private static final AudioInConfig.Encoding ENCODING_INPUT = AudioInConfig.Encoding.LINEAR16;
    private static final AudioOutConfig.Encoding ENCODING_OUTPUT = AudioOutConfig.Encoding.LINEAR16;
    private static final AudioInConfig ASSISTANT_AUDIO_REQUEST_CONFIG =
            AudioInConfig.newBuilder()
                    .setEncoding(ENCODING_INPUT)
                    .setSampleRateHertz(SAMPLE_RATE)
                    .build();
    private static final AudioOutConfig ASSISTANT_AUDIO_RESPONSE_CONFIG =
            AudioOutConfig.newBuilder()
                    .setEncoding(ENCODING_OUTPUT)
                    .setSampleRateHertz(SAMPLE_RATE)
                    .build();
    private static final AudioFormat AUDIO_FORMAT_STEREO =
            new AudioFormat.Builder()
                    .setChannelMask(AudioFormat.CHANNEL_IN_STEREO)
                    .setEncoding(ENCODING)
                    .setSampleRate(SAMPLE_RATE)
                    .build();
    private static final AudioFormat AUDIO_FORMAT_OUT_MONO =
            new AudioFormat.Builder()
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .setEncoding(ENCODING)
                    .setSampleRate(SAMPLE_RATE)
                    .build();
    private static final AudioFormat AUDIO_FORMAT_IN_MONO =
            new AudioFormat.Builder()
                    .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                    .setEncoding(ENCODING)
                    .setSampleRate(SAMPLE_RATE)
                    .build();
    private static final int SAMPLE_BLOCK_SIZE = 1024;

    // Google Assistant API constants.
    private static final String ASSISTANT_ENDPOINT = "embeddedassistant.googleapis.com";

    // gRPC client and stream observers.
    private EmbeddedAssistantGrpc.EmbeddedAssistantStub mAssistantService;
    private StreamObserver<AssistRequest> mAssistantRequestObserver;
    private StreamObserver<AssistResponse> mAssistantResponseObserver =
            new StreamObserver<AssistResponse>() {
                @Override
                public void onNext(AssistResponse value) {
                    if (value.hasAudioOut()) {
                        AudioOut audioOut = value.getAudioOut();
                        ByteBuffer audioData = ByteBuffer.wrap(audioOut.getAudioData().toByteArray());
                        mAudioTrack.write(audioData, audioData.remaining(), AudioTrack.WRITE_BLOCKING);
                    }

                    // Handle device actions
                    if (value.hasDeviceAction()) {
                        String json = value.getDeviceAction().getDeviceRequestJson();
                        Log.d(TAG, "Device action: " + json);
                        try {
                            DeviceAction deviceAction = JSON_ADAPTER.fromJson(json);
                            mDeviceActionLiveData.postValue(deviceAction);
                        } catch (IOException e) {
                            Log.e(TAG, "Failed parsing json");
                        }
                    }

                    if (value.hasDialogStateOut()) {
                        DialogStateOut stateOut = value.getDialogStateOut();
                        Log.d(TAG, "Supplemental display text: " + stateOut.getSupplementalDisplayText());
                    }

                    // Show transcript, for debugging purposes
                    List<SpeechRecognitionResult> speechResultsList = value.getSpeechResultsList();
                    if (!speechResultsList.isEmpty()) {
                        for (SpeechRecognitionResult result : speechResultsList) {
                            Log.d(TAG, "Assistant request text: " + result.getTranscript());
                        }
                    }

                    if (value.getEventType() == AssistResponse.EventType.END_OF_UTTERANCE) {
                        Log.d(TAG, "End of utterance. Should stop recording");
                    }
                }

                @Override
                public void onError(Throwable t) {
                    Log.e(TAG, "converse error:", t);
                }

                @Override
                public void onCompleted() {
                    Log.i(TAG, "assistant response finished");
                }
            };

    // Audio playback and recording objects.
    private AudioTrack mAudioTrack;
    private AudioRecord mAudioRecord;

    // Assistant Thread and Runnables implementing the push-to-talk functionality.
    private HandlerThread mAssistantThread;
    private Handler mAssistantHandler;
    private Runnable mStartAssistantRequest = new Runnable() {
        @Override
        public void run() {
            Log.i(TAG, "starting assistant request");
            mAudioRecord.startRecording();
            mAssistantRequestObserver = mAssistantService.assist(mAssistantResponseObserver);
            mAssistantRequestObserver.onNext(AssistRequest.newBuilder().setConfig(
                    AssistConfig.newBuilder()
                            .setAudioInConfig(ASSISTANT_AUDIO_REQUEST_CONFIG)
                            .setAudioOutConfig(ASSISTANT_AUDIO_RESPONSE_CONFIG)
                            .setDialogStateIn(DialogStateIn.newBuilder()
                                    .setLanguageCode(BuildConfig.ASSISTANT_LANGUAGE_CODE)
                                    .build())
                            .setDeviceConfig(DeviceConfig.newBuilder()
                                    .setDeviceId(BuildConfig.ASSISTANT_DEVICE_ID)
                                    .setDeviceModelId(BuildConfig.ASSISTANT_DEVICE_MODEL_ID)
                                    .build())
                            .build()).build());
            mAssistantHandler.post(mStreamAssistantRequest);
        }
    };
    private Runnable mStreamAssistantRequest = new Runnable() {
        @Override
        public void run() {
            ByteBuffer audioData = ByteBuffer.allocateDirect(SAMPLE_BLOCK_SIZE);
            int result =
                    mAudioRecord.read(audioData, audioData.capacity(), AudioRecord.READ_BLOCKING);
            if (result < 0) {
                Log.e(TAG, "error reading from audio stream:" + result);
                return;
            }
            // Log.d(TAG, "streaming ConverseRequest: " + result);
            mAssistantRequestObserver.onNext(AssistRequest.newBuilder()
                    .setAudioIn(ByteString.copyFrom(audioData))
                    .build());
            mAssistantHandler.post(mStreamAssistantRequest);
        }
    };
    private Runnable mStopAssistantRequest = new Runnable() {
        @Override
        public void run() {
            Log.i(TAG, "ending assistant request");
            mAssistantHandler.removeCallbacks(mStreamAssistantRequest);
            if (mAssistantRequestObserver != null) {
                mAssistantRequestObserver.onCompleted();
                mAssistantRequestObserver = null;
            }
            mAudioRecord.stop();
            mAudioTrack.play();
        }
    };

    private AudioManager mAudioManager;
    private com.google.auth.Credentials mCreds;
    private DeviceActionLiveData mDeviceActionLiveData;

    public AssistantHelper(Context context) throws IOException, JSONException {
        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        try {
            mCreds = Credentials.fromResource(context, R.raw.credentials);
        } catch (IOException | JSONException e) {
            Log.e(TAG, "error creating assistant service:", e);
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    void onCreate() {
        Log.i(TAG, "starting assistant demo");

        mAssistantThread = new HandlerThread("assistantThread");
        mAssistantThread.start();
        mAssistantHandler = new Handler(mAssistantThread.getLooper());

        int maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        Log.i(TAG, "setting volume to: " + maxVolume);
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, 0);
        int outputBufferSize = AudioTrack.getMinBufferSize(AUDIO_FORMAT_OUT_MONO.getSampleRate(),
                AUDIO_FORMAT_OUT_MONO.getChannelMask(),
                AUDIO_FORMAT_OUT_MONO.getEncoding());
        mAudioTrack = new AudioTrack.Builder()
                .setAudioFormat(AUDIO_FORMAT_OUT_MONO)
                .setBufferSizeInBytes(outputBufferSize)
                .build();
        mAudioTrack.play();
        int inputBufferSize = AudioRecord.getMinBufferSize(AUDIO_FORMAT_STEREO.getSampleRate(),
                AUDIO_FORMAT_STEREO.getChannelMask(),
                AUDIO_FORMAT_STEREO.getEncoding());
        mAudioRecord = new AudioRecord.Builder()
                .setAudioSource(MediaRecorder.AudioSource.MIC)
                .setAudioFormat(AUDIO_FORMAT_IN_MONO)
                .setBufferSizeInBytes(inputBufferSize)
                .build();

        ManagedChannel channel = ManagedChannelBuilder.forTarget(ASSISTANT_ENDPOINT).build();
        mAssistantService = EmbeddedAssistantGrpc.newStub(channel)
                .withCallCredentials(MoreCallCredentials.from(mCreds)
                );
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    void onDestroy() {
        Log.i(TAG, "destroying assistant demo");
        if (mAudioRecord != null) {
            mAudioRecord.stop();
            mAudioRecord = null;
        }
        if (mAudioTrack != null) {
            mAudioTrack.stop();
            mAudioTrack = null;
        }

        mAssistantHandler.post(new Runnable() {
            @Override
            public void run() {
                mAssistantHandler.removeCallbacks(mStreamAssistantRequest);
            }
        });
        mAssistantThread.quitSafely();
    }

    public void onButtonPressed(boolean pressed) {
        if (pressed) {
            mAssistantHandler.post(mStartAssistantRequest);
        } else {
            mAssistantHandler.post(mStopAssistantRequest);
        }
    }

    public DeviceActionLiveData getDeviceActionLiveData() {
        if (mDeviceActionLiveData == null) {
            mDeviceActionLiveData = new DeviceActionLiveData();
        }
        return mDeviceActionLiveData;
    }
}
