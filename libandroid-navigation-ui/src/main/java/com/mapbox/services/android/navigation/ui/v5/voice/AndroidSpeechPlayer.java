package com.mapbox.services.android.navigation.ui.v5.voice;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.text.TextUtils;

import java.util.HashMap;
import java.util.Locale;

import timber.log.Timber;

/**
 * Default player used to play voice instructions when a connection to Polly is unable to be established.
 * <p>
 * This instruction player uses {@link TextToSpeech} to play voice instructions.
 *
 * @since 0.6.0
 */
public class AndroidSpeechPlayer implements InstructionPlayer {

  private static final String DEFAULT_UTTERANCE_ID = "default_id";

  private TextToSpeech textToSpeech;
  private boolean isMuted;
  private boolean languageSupported = false;

  /**
   * Creates an instance of {@link AndroidSpeechPlayer}.
   *
   * @param context used to create an instance of {@link TextToSpeech}
   * @param language to initialize locale to set
   * @since 0.6.0
   */
  AndroidSpeechPlayer(Context context, final String language) {
    textToSpeech = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
      @Override
      public void onInit(int status) {
        if (status == TextToSpeech.ERROR || language == null) {
          Timber.w("There was an error initializing native TTS");
          textToSpeech = null;
          return;
        }

        Locale locale = new Locale(language);
        if (textToSpeech.isLanguageAvailable(locale) == TextToSpeech.LANG_AVAILABLE) {
          languageSupported = true;
          textToSpeech.setLanguage(locale);
        } else {
          Timber.w("The specified language is not supported by TTS");
          textToSpeech = null;
        }
      }
    });
  }

  /**
   * @param instruction voice instruction to be synthesized and played
   */
  @Override
  public void play(String instruction) {
    if (canPlay(instruction)) {
      HashMap<String, String> params = new HashMap<>(1);
      params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, DEFAULT_UTTERANCE_ID);
      textToSpeech.speak(instruction, TextToSpeech.QUEUE_ADD, params);
    }
  }

  /**
   * @return true if muted, false if not
   */
  @Override
  public boolean isMuted() {
    return isMuted;
  }

  /**
   * @param isMuted true if should be muted, false if should not
   */
  @Override
  public void setMuted(boolean isMuted) {
    this.isMuted = isMuted;
    if (isMuted) {
      muteTts();
    }
  }

  @Override
  public void onOffRoute() {
    muteTts();
  }

  @Override
  public void onDestroy() {
    if (textToSpeech != null) {
      textToSpeech.stop();
      textToSpeech.shutdown();
    }
  }

  /**
   * Called when setting muted mid-instruction.
   * Mutes TTS only if currently speaking.
   */
  private void muteTts() {
    if (textToSpeech.isSpeaking()) {
      textToSpeech.stop();
    }
  }

  private boolean canPlay(String instruction) {
    return languageSupported && !isMuted && !TextUtils.isEmpty(instruction);
  }

  void setInstructionListener(final InstructionListener instructionListener) {
    textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
      @Override
      public void onStart(String utteranceId) {
        if (instructionListener != null) {
          instructionListener.onStart();
        }
      }

      @Override
      public void onDone(String utteranceId) {
        if (instructionListener != null) {
          instructionListener.onDone();
        }
      }

      @Override
      public void onError(String utteranceId) {
        if (instructionListener != null) {
          instructionListener.onError(false);
        }
      }
    });
  }
}
