package edu.cmu.hcii.sugilite.verbal_instruction_demo.speech;

/**
 * @author toby
 * @date 3/15/19
 * @time 8:45 PM
 */

import android.app.AlertDialog;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.google.auth.Credentials;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.speech.v1.RecognitionAudio;
import com.google.cloud.speech.v1.RecognitionConfig;
import com.google.cloud.speech.v1.RecognizeRequest;
import com.google.cloud.speech.v1.RecognizeResponse;
import com.google.cloud.speech.v1.SpeechContext;
import com.google.cloud.speech.v1.SpeechGrpc;
import com.google.cloud.speech.v1.SpeechRecognitionAlternative;
import com.google.cloud.speech.v1.SpeechRecognitionResult;
import com.google.cloud.speech.v1.StreamingRecognitionConfig;
import com.google.cloud.speech.v1.StreamingRecognitionResult;
import com.google.cloud.speech.v1.StreamingRecognizeRequest;
import com.google.cloud.speech.v1.StreamingRecognizeResponse;
import com.google.protobuf.ByteString;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import edu.cmu.hcii.sugilite.R;
import edu.cmu.hcii.sugilite.ui.dialog.SugiliteProgressDialog;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ClientInterceptors;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.internal.DnsNameResolverProvider;
import io.grpc.okhttp.OkHttpChannelProvider;
import io.grpc.stub.StreamObserver;



public class GoogleCloudSpeechService extends Service {

    public interface Listener {

        /**
         * Called when a new piece of text was recognized by the Speech API.
         *
         * @param text    The text.
         * @param isFinal {@code true} when the API finished processing audio.
         */
        void onSpeechRecognized(String text, boolean isFinal);

    }

    private static final String TAG = "SpeechService";

    private static final String PREFS = "SpeechService";
    private static final String PREF_ACCESS_TOKEN_VALUE = "access_token_value";
    private static final String PREF_ACCESS_TOKEN_EXPIRATION_TIME = "access_token_expiration_time";

    /** We reuse an access token if its expiration time is longer than this. */
    private static final int ACCESS_TOKEN_EXPIRATION_TOLERANCE = 30 * 60 * 1000; // thirty minutes
    /** We refresh the current access token before it expires. */
    private static final int ACCESS_TOKEN_FETCH_MARGIN = 60 * 1000; // one minute

    public static final List<String> SCOPE =
            Collections.singletonList("https://www.googleapis.com/auth/cloud-platform");
    private static final String HOSTNAME = "speech.googleapis.com";
    private static final int PORT = 443;

    private final SpeechBinder mBinder = new SpeechBinder();
    private final ArrayList<Listener> mListeners = new ArrayList<>();
    private volatile AccessTokenTask mAccessTokenTask;
    private SpeechGrpc.SpeechStub mApi;
    private static Handler mHandler;

    private SugiliteProgressDialog progressDialog;
    private MediaPlayer speechRecognizedSoundMediaPlayer;

    private List<String> contextPhrases = new ArrayList<>();

    public void setContextPhrases(Collection<String> contextPhrases) {
        this.contextPhrases.clear();
        this.contextPhrases.addAll(contextPhrases);
    }

    private final StreamObserver<StreamingRecognizeResponse> mResponseObserver
            = new StreamObserver<StreamingRecognizeResponse>() {
        @Override
        public void onNext(StreamingRecognizeResponse response) {
            String text = null;
            boolean isFinal = false;
            if (response.getResultsCount() > 0) {
                final StreamingRecognitionResult result = response.getResults(0);
                isFinal = result.getIsFinal();
                if (result.getAlternativesCount() > 0) {
                    final SpeechRecognitionAlternative alternative = result.getAlternatives(0);
                    text = alternative.getTranscript();
                }
            }
            if (text != null) {
                for (Listener listener : mListeners) {
                    //hide the dialog
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (progressDialog != null && progressDialog.isShowing()) {
                                progressDialog.dismiss();
                            }
                        }
                    });
                    listener.onSpeechRecognized(text, isFinal);
                    if (isFinal) {
                        if (speechRecognizedSoundMediaPlayer != null) {
                            //play the "speech recognized" sound when getting the final result
                            speechRecognizedSoundMediaPlayer.start();
                        }                    }
                }
            }
        }

        @Override
        public void onError(Throwable t) {
            Log.e(TAG, "Error calling the API.", t);
        }

        @Override
        public void onCompleted() {
            Log.i(TAG, "API completed.");
        }

    };

    private final StreamObserver<RecognizeResponse> mFileResponseObserver
            = new StreamObserver<RecognizeResponse>() {
        @Override
        public void onNext(RecognizeResponse response) {
            String text = null;
            if (response.getResultsCount() > 0) {
                final SpeechRecognitionResult result = response.getResults(0);
                if (result.getAlternativesCount() > 0) {
                    final SpeechRecognitionAlternative alternative = result.getAlternatives(0);
                    text = alternative.getTranscript();
                }
            }
            if (text != null) {
                for (Listener listener : mListeners) {
                    listener.onSpeechRecognized(text, true);
                    if (speechRecognizedSoundMediaPlayer != null) {
                        //play the "speech recognized" sound when getting the final result
                        speechRecognizedSoundMediaPlayer.start();
                    }                }
            }
        }

        @Override
        public void onError(Throwable t) {
            Log.e(TAG, "Error calling the API.", t);
        }

        @Override
        public void onCompleted() {
            Log.i(TAG, "API completed.");
        }

    };

    private StreamObserver<StreamingRecognizeRequest> mRequestObserver;

    public static GoogleCloudSpeechService from(IBinder binder) {
        return ((SpeechBinder) binder).getService();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mHandler = new Handler();
        fetchAccessToken();
        this.speechRecognizedSoundMediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.speech_recognized);


    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacks(mFetchAccessTokenRunnable);
        mHandler = null;
        // Release the gRPC channel.
        if (mApi != null) {
            final ManagedChannel channel = (ManagedChannel) mApi.getChannel();
            if (channel != null && !channel.isShutdown()) {
                try {
                    channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Log.e(TAG, "Error shutting down the gRPC channel.", e);
                }
            }
            mApi = null;
        }

        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }

    private void fetchAccessToken() {
        if (mAccessTokenTask != null) {
            return;
        }
        mAccessTokenTask = new AccessTokenTask();
        mAccessTokenTask.execute();
    }

    private String getDefaultLanguageCode() {
        final Locale locale = Locale.getDefault();
        final StringBuilder language = new StringBuilder(locale.getLanguage());
        final String country = locale.getCountry();
        if (!TextUtils.isEmpty(country)) {
            language.append("-");
            language.append(country);
        }
        return language.toString();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public void clearListener() {
        mListeners.clear();
    }

    public void addListener(@NonNull Listener listener) {
        mListeners.add(listener);
    }

    public void removeListener(@NonNull Listener listener) {
        mListeners.remove(listener);
    }

    /**
     * Starts recognizing speech audio.
     *
     * @param sampleRate The sample rate of the audio.
     */
    public void startRecognizing(int sampleRate) {
        if (mApi == null) {
            Log.w(TAG, "API not ready. Ignoring the request.");
            return;
        }
        // Configure the API
        mRequestObserver = mApi.streamingRecognize(mResponseObserver);
        mRequestObserver.onNext(StreamingRecognizeRequest.newBuilder()
                .setStreamingConfig(StreamingRecognitionConfig.newBuilder()
                        .setConfig(RecognitionConfig.newBuilder()
                                .setLanguageCode(getDefaultLanguageCode())
                                .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
                                .setSampleRateHertz(sampleRate)
                                .addSpeechContexts(SpeechContext.newBuilder().addAllPhrases(contextPhrases).build())
                                .build())
                        .setInterimResults(true)
                        .setSingleUtterance(true)
                        .build())
                .build());
    }

    /**
     * Recognizes the speech audio. This method should be called every time a chunk of byte buffer
     * is ready.
     *
     * @param data The audio data.
     * @param size The number of elements that are actually relevant in the {@code data}.
     */
    public void recognize(byte[] data, int size) {
        if (mRequestObserver == null) {
            return;
        }
        // Call the streaming recognition API
        mRequestObserver.onNext(StreamingRecognizeRequest.newBuilder()
                .setAudioContent(ByteString.copyFrom(data, 0, size))
                .build());
    }

    /**
     * Finishes recognizing speech audio.
     */
    public void finishRecognizing() {
        //show the dialog
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (progressDialog != null) {
                    progressDialog.show();
                }
            }
        });

        if (mRequestObserver == null) {
            return;
        }
        mRequestObserver.onCompleted();
        mRequestObserver = null;
    }

    /**
     * Recognize all data from the specified {@link InputStream}.
     *
     * @param stream The audio data.
     */
    /*
    public void recognizeInputStream(InputStream stream) {
        try {
            mApi.recognize(
                    RecognizeRequest.newBuilder()
                            .setConfig(RecognitionConfig.newBuilder()
                                    .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
                                    .setLanguageCode("en-US")
                                    .setSampleRateHertz(16000)
                                    .addSpeechContexts(SpeechContext.newBuilder().addAllPhrases(contextPhrases).build())
                                    .build())
                            .setAudio(RecognitionAudio.newBuilder()
                                    .setContent(ByteString.readFrom(stream))
                                    .build())
                            .build(),
                    mFileResponseObserver);
        } catch (IOException e) {
            Log.e(TAG, "Error loading the input", e);
        }
    }
    */

    private class SpeechBinder extends Binder {

        GoogleCloudSpeechService getService() {
            return GoogleCloudSpeechService.this;
        }

    }

    private final Runnable mFetchAccessTokenRunnable = new Runnable() {
        @Override
        public void run() {
            fetchAccessToken();
        }
    };

    private class AccessTokenTask extends AsyncTask<Void, Void, AccessToken> {

        @Override
        protected AccessToken doInBackground(Void... voids) {

            final SharedPreferences prefs =
                    getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            String tokenValue = prefs.getString(PREF_ACCESS_TOKEN_VALUE, null);
            long expirationTime = prefs.getLong(PREF_ACCESS_TOKEN_EXPIRATION_TIME, -1);

            // Check if the current token is still valid for a while
            if (tokenValue != null && expirationTime > 0) {
                if (expirationTime
                        > System.currentTimeMillis() + ACCESS_TOKEN_EXPIRATION_TOLERANCE) {
                    return new AccessToken(tokenValue, new Date(expirationTime));
                }
            }

            // ***** WARNING *****
            // In this sample, we load the credential from a JSON file stored in a raw resource
            // folder of this client app. You should never do this in your app. Instead, store
            // the file in your server and obtain an access token from there.
            // *******************
            final InputStream stream = getResources().openRawResource(R.raw.credential);
            try {
                final GoogleCredentials credentials = GoogleCredentials.fromStream(stream)
                        .createScoped(SCOPE);
                final AccessToken token = credentials.refreshAccessToken();
                prefs.edit()
                        .putString(PREF_ACCESS_TOKEN_VALUE, token.getTokenValue())
                        .putLong(PREF_ACCESS_TOKEN_EXPIRATION_TIME,
                                token.getExpirationTime().getTime())
                        .apply();
                return token;
            } catch (IOException e) {
                Log.e(TAG, "Failed to obtain access token.", e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(AccessToken accessToken) {
            mAccessTokenTask = null;
            final ManagedChannel channel = new OkHttpChannelProvider()
                    .builderForAddress(HOSTNAME, PORT)
                    .nameResolverFactory(new DnsNameResolverProvider())
                    .intercept(new GoogleCredentialsInterceptor(new GoogleCredentials(accessToken)
                            .createScoped(SCOPE)))
                    .build();
            mApi = SpeechGrpc.newStub(channel);

            // Schedule access token refresh before it expires
            if (mHandler != null) {
                try {
                    mHandler.postDelayed(mFetchAccessTokenRunnable,
                            Math.max(accessToken.getExpirationTime().getTime()
                                    - System.currentTimeMillis()
                                    - ACCESS_TOKEN_FETCH_MARGIN, ACCESS_TOKEN_EXPIRATION_TOLERANCE));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Authenticates the gRPC channel using the specified {@link GoogleCredentials}.
     */
    private static class GoogleCredentialsInterceptor implements ClientInterceptor {

        private final Credentials mCredentials;

        private Metadata mCached;

        private Map<String, List<String>> mLastMetadata;

        GoogleCredentialsInterceptor(Credentials credentials) {
            mCredentials = credentials;
        }

        @Override
        public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
                final MethodDescriptor<ReqT, RespT> method, CallOptions callOptions,
                final Channel next) {
            return new ClientInterceptors.CheckedForwardingClientCall<ReqT, RespT>(
                    next.newCall(method, callOptions)) {
                @Override
                protected void checkedStart(Listener<RespT> responseListener, Metadata headers)
                        throws StatusException {
                    Metadata cachedSaved;
                    URI uri = serviceUri(next, method);
                    synchronized (this) {
                        Map<String, List<String>> latestMetadata = getRequestMetadata(uri);
                        if (mLastMetadata == null || mLastMetadata != latestMetadata) {
                            mLastMetadata = latestMetadata;
                            mCached = toHeaders(mLastMetadata);
                        }
                        cachedSaved = mCached;
                    }
                    headers.merge(cachedSaved);
                    delegate().start(responseListener, headers);
                }
            };
        }

        /**
         * Generate a JWT-specific service URI. The URI is simply an identifier with enough
         * information for a service to know that the JWT was intended for it. The URI will
         * commonly be verified with a simple string equality check.
         */
        private URI serviceUri(Channel channel, MethodDescriptor<?, ?> method)
                throws StatusException {
            String authority = channel.authority();
            if (authority == null) {
                throw Status.UNAUTHENTICATED
                        .withDescription("Channel has no authority")
                        .asException();
            }
            // Always use HTTPS, by definition.
            final String scheme = "https";
            final int defaultPort = 443;
            String path = "/" + MethodDescriptor.extractFullServiceName(method.getFullMethodName());
            URI uri;
            try {
                uri = new URI(scheme, authority, path, null, null);
            } catch (URISyntaxException e) {
                throw Status.UNAUTHENTICATED
                        .withDescription("Unable to construct service URI for auth")
                        .withCause(e).asException();
            }
            // The default port must not be present. Alternative ports should be present.
            if (uri.getPort() == defaultPort) {
                uri = removePort(uri);
            }
            return uri;
        }

        private URI removePort(URI uri) throws StatusException {
            try {
                return new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), -1 /* port */,
                        uri.getPath(), uri.getQuery(), uri.getFragment());
            } catch (URISyntaxException e) {
                throw Status.UNAUTHENTICATED
                        .withDescription("Unable to construct service URI after removing port")
                        .withCause(e).asException();
            }
        }

        private Map<String, List<String>> getRequestMetadata(URI uri) throws StatusException {
            try {
                return mCredentials.getRequestMetadata(uri);
            } catch (IOException e) {
                throw Status.UNAUTHENTICATED.withCause(e).asException();
            }
        }

        private static Metadata toHeaders(Map<String, List<String>> metadata) {
            Metadata headers = new Metadata();
            if (metadata != null) {
                for (String key : metadata.keySet()) {
                    Metadata.Key<String> headerKey = Metadata.Key.of(
                            key, Metadata.ASCII_STRING_MARSHALLER);
                    for (String value : metadata.get(key)) {
                        headers.put(headerKey, value);
                    }
                }
            }
            return headers;
        }

    }

    /**
     * run the runnable on the main UI thread
     * @param runnable the runnable to run
     */
    public void runOnUiThread(Runnable runnable) {
        Handler handler = new Handler(getApplicationContext().getMainLooper());
        handler.post(runnable);
    }

}

