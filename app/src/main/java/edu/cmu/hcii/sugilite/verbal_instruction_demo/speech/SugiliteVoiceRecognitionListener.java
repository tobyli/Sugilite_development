package edu.cmu.hcii.sugilite.verbal_instruction_demo.speech;

import android.accessibilityservice.AccessibilityService;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;

import edu.cmu.hcii.sugilite.SugiliteAccessibilityService;

/**
 * @author toby
 * @date 11/22/17
 * @time 4:49 PM
 */
public class SugiliteVoiceRecognitionListener implements RecognitionListener {


    private SpeechRecognizer speech = null;
    private Intent recognizerIntent;
    private String LOG_TAG = "VoiceRecognition";
    private Context context;
    private long lastStartListening = -1;
    private TextToSpeech tts;


    //the parent interface
    private SugiliteVoiceInterface sugiliteVoiceInterface;

    public SugiliteVoiceRecognitionListener(Context context, SugiliteVoiceInterface voiceInterface, TextToSpeech tts) {
        this.context = context;
        this.sugiliteVoiceInterface = voiceInterface;
        this.tts = tts;


        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,
                "en");
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,
                context.getPackageName());
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5);

        //not really working
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 3000);

    }

    public void setSugiliteVoiceInterface(SugiliteVoiceInterface sugiliteVoiceInterface) {
        this.sugiliteVoiceInterface = sugiliteVoiceInterface;
    }

    public void startListening(){
        speech = SpeechRecognizer.createSpeechRecognizer(context);
        speech.setRecognitionListener(this);
        speech.startListening(recognizerIntent);
        lastStartListening = Calendar.getInstance().getTimeInMillis();
        sugiliteVoiceInterface.listeningStarted();
    }

    public void stopListening(){
        if(speech != null) {
            speech.stopListening();
            speech.destroy();
            sugiliteVoiceInterface.listeningEnded();
        }
    }

    private void runOnUiThread(Runnable runnable) throws Exception{
        if(context instanceof SugiliteAccessibilityService) {
            ((SugiliteAccessibilityService) context).runOnUiThread(runnable);
        }

        else if(context instanceof Activity) {
            ((Activity) context).runOnUiThread(runnable);
        }

        else {
            throw new Exception("no context available for running on ui thread");
        }
    }

    public void speak(String content, String utteranceId, Runnable onDone){
        HashMap<String, String> params = new HashMap<String, String>();
        String originalUtteranceId = utteranceId;
        params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,utteranceId);
        if (tts != null) {
            tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onStart(String utteranceId) {
                    if (sugiliteVoiceInterface != null) {
                        try {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    sugiliteVoiceInterface.speakingStarted();
                                }
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }

                @Override
                public void onDone(String utteranceId) {
                    if (utteranceId.equals(originalUtteranceId)) {
                        try {
                            runOnUiThread(onDone);
                            if (sugiliteVoiceInterface != null) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        sugiliteVoiceInterface.speakingEnded();
                                    }
                                });
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        System.out.println("TTS IS DONE: " + utteranceId);
                    }
                }

                @Override
                public void onError(String utteranceId) {
                    if (sugiliteVoiceInterface != null) {
                        try {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    sugiliteVoiceInterface.speakingEnded();
                                }
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
            tts.speak(content, TextToSpeech.QUEUE_ADD, params);
        } else {
            System.out.println("ERROR: TTS is null!");
        }
    }

    public void stopTTS(){
        if(tts != null) {
            if (tts.isSpeaking()) {
                tts.stop();
                if (sugiliteVoiceInterface != null) {
                    try {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                sugiliteVoiceInterface.speakingEnded();
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        } else {
            System.out.println("ERROR: TTS is null!");
        }
    }

    @Override
    public void onBeginningOfSpeech() {
        Log.i(LOG_TAG, "onBeginningOfSpeech");

    }

    @Override
    public void onBufferReceived(byte[] buffer) {
        Log.i(LOG_TAG, "onBufferReceived: " + buffer);
    }

    @Override
    public void onEndOfSpeech() {
        Log.i(LOG_TAG, "onEndOfSpeech");
        sugiliteVoiceInterface.listeningEnded();
    }

    @Override
    public void onError(int errorCode) {
        String errorMessage = getErrorText(errorCode);
        Log.d(LOG_TAG, "FAILED " + errorMessage);
        sugiliteVoiceInterface.listeningEnded();
        stopListening();

    }

    @Override
    public void onEvent(int arg0, Bundle arg1) {
        Log.i(LOG_TAG, "onEvent");
    }

    @Override
    public void onPartialResults(Bundle arg0) {
        Log.i(LOG_TAG, "onPartialResults");
    }

    @Override
    public void onReadyForSpeech(Bundle arg0) {
        Log.i(LOG_TAG, "onReadyForSpeech");
    }

    @Override
    public void onResults(Bundle results) {
        Log.i(LOG_TAG, "onResults");
        ArrayList<String> matches = results
                .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        sugiliteVoiceInterface.resultAvailable(matches);
        stopListening();
    }

    @Override
    public void onRmsChanged(float rmsdB) {
        Log.i(LOG_TAG, "onRmsChanged: " + rmsdB);
        long currentTime = Calendar.getInstance().getTimeInMillis();
        if(lastStartListening > -1 && (currentTime - lastStartListening > 10000)){
            Log.i(LOG_TAG, "Speech recognition timeout!");
            stopListening();
        }
    }

    public static String getErrorText(int errorCode) {
        String message;
        switch (errorCode) {
            case SpeechRecognizer.ERROR_AUDIO:
                message = "Audio recording error";
                break;
            case SpeechRecognizer.ERROR_CLIENT:
                message = "Client side error";
                break;
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                message = "Insufficient permissions";
                break;
            case SpeechRecognizer.ERROR_NETWORK:
                message = "Network error";
                break;
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                message = "Network timeout";
                break;
            case SpeechRecognizer.ERROR_NO_MATCH:
                message = "No match";
                break;
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                message = "RecognitionService busy";
                break;
            case SpeechRecognizer.ERROR_SERVER:
                message = "error from server";
                break;
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                message = "No speech input";
                break;
            default:
                message = "Didn't understand, please try again.";
                break;
        }
        return message;
    }

    public static boolean checkStringContains(String originalString, String... stringsToCheck){
        for(String string : stringsToCheck){
            if(originalString.contains(string)){
                return true;
            }
        }
        return false;
    }


}
