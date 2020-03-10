package edu.cmu.hcii.sugilite.verbal_instruction_demo.speech;

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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;

import edu.cmu.hcii.sugilite.accessibility_service.SugiliteAccessibilityService;

/**
 * @author toby
 * @date 11/22/17
 * @time 4:49 PM
 */
public interface SugiliteVoiceRecognitionListener {
    void setSugiliteVoiceInterface(SugiliteVoiceInterface sugiliteVoiceInterface);
    void startListening();
    void stopListening();
    void speak(String content, String utteranceId, Runnable onDone);
    void stopTTS();
    void stopAllAndEndASRService();
    void setContextPhrases(String... contextPhrases);
    TextToSpeech getTTS();
}
