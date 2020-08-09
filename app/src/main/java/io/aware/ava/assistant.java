package io.aware.ava;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.AlarmClock;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.telephony.SmsManager;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.github.zagum.speechrecognitionview.RecognitionProgressView;

import com.ibm.cloud.sdk.core.http.HttpMediaType;
import com.ibm.cloud.sdk.core.security.Authenticator;
import com.ibm.cloud.sdk.core.security.IamAuthenticator;
import com.ibm.watson.developer_cloud.android.library.audio.MicrophoneHelper;
import com.ibm.watson.developer_cloud.android.library.audio.MicrophoneInputStream;
import com.ibm.watson.developer_cloud.android.library.audio.StreamPlayer;
import com.ibm.watson.developer_cloud.android.library.audio.utils.ContentType;
import com.ibm.watson.speech_to_text.v1.SpeechToText;
import com.ibm.watson.speech_to_text.v1.model.RecognizeOptions;
import com.ibm.watson.speech_to_text.v1.model.SpeechRecognitionResults;
import com.ibm.watson.speech_to_text.v1.websocket.BaseRecognizeCallback;
import com.ibm.watson.speech_to_text.v1.websocket.RecognizeCallback;

import com.ibm.watson.text_to_speech.v1.model.SynthesizeOptions;
import com.mozilla.speechlibrary.ISpeechRecognitionListener;
import com.mozilla.speechlibrary.MozillaSpeechService;
import com.mozilla.speechlibrary.STTResult;

import org.adblockplus.libadblockplus.android.AdblockEngine;
import org.apache.commons.lang3.LocaleUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


import org.adblockplus.libadblockplus.android.webview.AdblockWebView;



public class assistant extends Activity {


    public Button button1;
    public AdblockWebView browser;
    public String spokentext;
    public String skillcategory;
    public String langcode;
    public String langucode;
    public ImageView imageView1;
    public ImageView imageView2;
    public LinearLayout ratings;
    public String openedUrl;
    public Boolean iscalling = false;
    public TextToSpeech tts;
    public SpeechRecognizer stt;
    public Thread ttsThread;
    public String answerType = "answer";
    public String datakey = "";
    public String additionalData;
    public RecognitionProgressView recognitionProgressView;
    public Context assistContext;

    public ISpeechRecognitionListener mVoiceSearchListener;
    public MozillaSpeechService mMozillaSpeechService;

    public MicrophoneInputStream myOggStream;
    public MicrophoneHelper microphoneHelper;
    public boolean isrecording = false;
    public Authenticator authenticator;
    public SpeechToText speechservice;
    public String ibmtext;

    public Authenticator authenticator2;
    public com.ibm.watson.text_to_speech.v1.TextToSpeech textService;
    public StreamPlayer player = new StreamPlayer();

    public Locale loclang;

    @Override
    protected void onCreate(Bundle _savedInstanceState) {
        super.onCreate(_savedInstanceState);
        setContentView(R.layout.assistant);
        initialize(_savedInstanceState);

        if(MainActivity.mainContext != null){
            assistContext = MainActivity.mainContext;
            Log.i("assistContext","Main");
        } else if(wakeword.wakeContext != null){
            assistContext = wakeword.wakeContext;
            Log.i("assistContext","wakeword");
        } else {
            assistContext = this.getApplicationContext();
            Log.i("assistContext","assistant");
        }

        askPermission();
        getRuntimePermissions();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        microphoneHelper = new MicrophoneHelper(this);

        SharedPreferences pref = assistContext.getSharedPreferences("settings", 0); // 0 - for private mode
        String lang = pref.getString("language","auto");
        if(lang.contains("auto")){
            loclang = Locale.getDefault();
        } else {
            loclang = LocaleUtils.toLocale(langtocode(lang).replace("-","_"));
        }

        try {
            authenticator = new IamAuthenticator(pref.getString("ibmasrkey","Key"));
            speechservice = new SpeechToText(authenticator);

            authenticator2 = new IamAuthenticator(pref.getString("ibmttskey","Key"));
            textService = new com.ibm.watson.text_to_speech.v1.TextToSpeech(authenticator2);

            speechservice.setServiceUrl(pref.getString("ibmasrurl","Key"));
            textService.setServiceUrl(pref.getString("ibmttsurl","Keyip"));

        } catch (Exception e) {
            int asr = pref.getInt("ASR",0);
            if(asr == 2){
                Toast.makeText(assistContext,"Error while login to IBM",Toast.LENGTH_SHORT).show();
            }
        }


        tts = new TextToSpeech(assistContext, new TextToSpeech.OnInitListener(){
            @Override
            public void onInit(int arg0) {
                if (arg0 == TextToSpeech.SUCCESS){
                    Log.i("TTSENGINE","Ready");
                }
            }
            });

        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String s) {
                Log.i("TTSENGINE","starting");
            }

            @Override
            public void onDone(String s) {
                Log.e("TTSENGINE","done");
            }

            @Override
            public void onError(String s) {
                Log.e("TTSENGINE","Error:" + s);
            }
        });

        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_DENIED) {
                requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 1000);
            } else {
                initializeLogic();
            }
        } else {
            initializeLogic();
        }


        mVoiceSearchListener = new ISpeechRecognitionListener() {
            public void onSpeechStatusChanged(final MozillaSpeechService.SpeechState aState, final Object aPayload){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.i("DeepSpeech",aState.toString());
                        switch (aState) {
                            case DECODING:
                                // Handle when the speech object changes to decoding state
                                break;
                            case MIC_ACTIVITY:
                                // Captures the activity from the microphone
                                double db = (double)aPayload * -1;
                                break;
                            case STT_RESULT:
                                // When the api finished processing and returned a hypothesis
                                SharedPreferences pref = assistContext.getSharedPreferences("stt", 0); // 0 - for private mode
                                SharedPreferences.Editor editor = pref.edit();
                                editor.putBoolean("isListening",false);
                                editor.commit();
                                String transcription = ((STTResult)aPayload).mTranscription;
                                ToneGenerator toneGen1 = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
                                toneGen1.startTone(ToneGenerator.TONE_CDMA_PIP,150);
                                mMozillaSpeechService.removeListener(mVoiceSearchListener);
                                Toast.makeText(assistContext, transcription, Toast.LENGTH_SHORT).show();
                                callAPI(transcription);
                                float confidence = ((STTResult)aPayload).mConfidence;
                                break;
                            case START_LISTEN:
                                // Handle when the api successfully opened the microphone and started listening
                                break;
                            case NO_VOICE:
                                mMozillaSpeechService.removeListener(mVoiceSearchListener);
                                Toast.makeText(assistContext, "No Voice", Toast.LENGTH_SHORT).show();
                                stopAll();
                                // Handle when the api didn't detect any voice
                                break;
                            case CANCELED:
                                mMozillaSpeechService.removeListener(mVoiceSearchListener);
                                // Handle when a cancelation was fully executed
                                break;
                            case ERROR:
                                mMozillaSpeechService.removeListener(mVoiceSearchListener);
                                Toast.makeText(assistContext, "Error", Toast.LENGTH_SHORT).show();
                                SharedPreferences errors = assistContext.getSharedPreferences("ErrorLog",Context.MODE_MULTI_PROCESS);
                                String error = errors.getString("ErrorLogs","Log:\n");
                                SharedPreferences.Editor editore = errors.edit();
                                editore.putString("ErrorLogs", error + "Mozilla ASR Error\n");
                                editore.commit();
                                stopAll();
                                // Handle when any error occurred
                                break;
                            default:
                                break;
                        }
                    }
                });
            }
        };

        try{
            startListen();
        } catch (Exception e) {
            finishAndRemoveTask();
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1000) {
            initializeLogic();
        }
    }

    public void ttsspeak(final String texttospeak){

        SharedPreferences prefer = getApplicationContext().getSharedPreferences("settings", Context.MODE_MULTI_PROCESS);
        int asr = prefer.getInt("ASR",0);

        if(asr == 2){
            new SynthesisTask().execute(texttospeak);
        } else {

            Bundle bundle = new Bundle();
            bundle.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC);
            if(!texttospeak.toLowerCase().equals("none")){
                try{
                    tts.setLanguage(LocaleUtils.toLocale(langtocode(langucode).replace("-","_")));
                } catch (Exception e) {
                    e.printStackTrace();
                    tts.setLanguage(Locale.getDefault());
                }
                tts.speak(texttospeak, TextToSpeech.QUEUE_ADD, bundle, null);
            }
            Log.i("TTSENGINE","Speaking");
            ttsThread =
                    new Thread(
                            new Runnable() {
                                @Override
                                public void run() {

                                    while(tts.isSpeaking()){
                                        try {
                                            Thread.sleep(250);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                    }

                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            button1.setText(R.string.speak);
                                        }
                                    });

                                }
                            });
            ttsThread.start();

        }


        button1.setText(R.string.speak);

    }

    public void ttsstop(){
        try{
            tts.stop();
        } catch (Exception e){
        }

        try{
            player.interrupt();
        } catch (Exception e){
        }
    }


    public void initialize(Bundle _savedInstanceState) {

        ratings = (LinearLayout) findViewById(R.id.ratings);
        imageView1 = (ImageView) findViewById(R.id.imageView1);
        imageView2 = (ImageView) findViewById(R.id.imageView2);
        button1 = (Button) findViewById(R.id.button);
        browser = (AdblockWebView) findViewById(R.id.browser);
        browser.getSettings().setJavaScriptEnabled(true);
        browser.getSettings().setPluginState(WebSettings.PluginState.ON);
        browser.getSettings().setAllowFileAccess(true);
        browser.getSettings().setMediaPlaybackRequiresUserGesture(false);
        browser.setWebViewClient(new WebViewClient() {
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                openedUrl = url;

                ratings.setVisibility(View.VISIBLE);

                if (url.contains("youtube.com")) {
                    emulateClick(view);
                }
            }

        });

        stt = SpeechRecognizer.createSpeechRecognizer(this);

        recognitionProgressView = (RecognitionProgressView) findViewById(R.id.recognition_view);
        recognitionProgressView.setSpeechRecognizer(stt);

        int[] colors = {
                ContextCompat.getColor(this, R.color.colorPrimary),
                ContextCompat.getColor(this, R.color.colorPrimary),
                ContextCompat.getColor(this, R.color.colorPrimary),
                ContextCompat.getColor(this, R.color.colorPrimary),
                ContextCompat.getColor(this, R.color.colorPrimary)
        };
        recognitionProgressView.setColors(colors);

        recognitionProgressView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopAll();
            }
        });

        imageView1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View _view) {
                ratings.setVisibility(View.GONE);
                delFromCache(spokentext, skillcategory, "upvote", langcode);
            }
        });

        imageView2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View _view) {
                ratings.setVisibility(View.GONE);
                delFromCache(spokentext, skillcategory, "downvote", langcode);
            }
        });

        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View _view) {
                if(isrecording){
                    microphoneHelper.closeInputStream();
                    isrecording = false;

                    SharedPreferences pref = assistContext.getSharedPreferences("stt", 0); // 0 - for private mode
                    SharedPreferences.Editor editor = pref.edit();
                    editor.putBoolean("isListening",false);
                    editor.commit();

                    ToneGenerator toneGen1 = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
                    toneGen1.startTone(ToneGenerator.TONE_CDMA_PIP,250);

                    recognitionProgressView.stop();
                    recognitionProgressView.setVisibility(View.GONE);
                    browser.setVisibility(View.VISIBLE);
                    button1.setVisibility(View.VISIBLE);
                    Toast.makeText(assistContext, ibmtext, Toast.LENGTH_SHORT).show();
                    callAPI(ibmtext);

                    button1.setText("Stop");

                } else if (tts.isSpeaking()) {
                    stopAll();
                    button1.setText(R.string.speak);
                } else {
                    Intent intent = new Intent(assistContext, assistant.class);
                    startActivity(intent);
                }

            }
        });


        recognitionProgressView.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle _param1) {
            }

            @Override
            public void onBeginningOfSpeech() {
            }

            @Override
            public void onRmsChanged(float _param1) {
            }

            @Override
            public void onBufferReceived(byte[] _param1) {
            }

            @Override
            public void onEndOfSpeech() {
            }

            @Override
            public void onPartialResults(Bundle _param1) {
            }

            @Override
            public void onEvent(int _param1, Bundle _param2) {
            }

            @Override
            public void onResults(Bundle _param1) {
                try {
                    stt.stopListening();
                    stt.cancel();
                } catch (Exception e) {

                }
                SharedPreferences pref = assistContext.getSharedPreferences("stt", 0); // 0 - for private mode
                SharedPreferences.Editor editor = pref.edit();
                editor.putBoolean("isListening",false);
                editor.commit();
                final ArrayList<String> _results = _param1.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                final String _result = _results.get(0);
                Toast.makeText(assistContext, _result, Toast.LENGTH_SHORT).show();
                callAPI(_result);
            }

            @Override
            public void onError(int _param1) {
                final String _errorMessage;

                SharedPreferences errors = assistContext.getSharedPreferences("ErrorLog",Context.MODE_MULTI_PROCESS);
                String error = errors.getString("ErrorLogs","Log:\n");
                SharedPreferences.Editor editore = errors.edit();
                editore.putString("ErrorLogs", error + "Google ASR Error\n");
                editore.commit();


                //promptSpeechInput();

                SharedPreferences pref = assistContext.getSharedPreferences("stt", 0); // 0 - for private mode
                SharedPreferences.Editor editor = pref.edit();
                editor.putBoolean("isListening",false);
                editor.commit();

                stopAll();
            }
        });

    }

    public void initializeLogic() {
    }

    public void stopAll() {
        Log.i("DeepSpeech","Stopp All");
        ttsstop();

        try{
            stt.cancel();
            stt.stopListening();
        } catch (Exception e) {

        }

        try{
            mMozillaSpeechService.cancel();
            mMozillaSpeechService.removeListener(mVoiceSearchListener);
        } catch (Exception e) {

        }
        recognitionProgressView.stop();
        recognitionProgressView.setVisibility(View.GONE);
        browser.setVisibility(View.VISIBLE);
        button1.setVisibility(View.VISIBLE);
        browser.stopLoading();
    }


    @Deprecated
    public void showMessage(String _s) {
        Toast.makeText(assistContext, _s, Toast.LENGTH_SHORT).show();
    }

    @Deprecated
    public int getLocationX(View _v) {
        int _location[] = new int[2];
        _v.getLocationInWindow(_location);
        return _location[0];
    }

    @Deprecated
    public int getLocationY(View _v) {
        int _location[] = new int[2];
        _v.getLocationInWindow(_location);
        return _location[1];
    }

    @Deprecated
    public int getRandom(int _min, int _max) {
        Random random = new Random();
        return random.nextInt(_max - _min + 1) + _min;
    }

    @Deprecated
    public ArrayList<Double> getCheckedItemPositionsToArray(ListView _list) {
        ArrayList<Double> _result = new ArrayList<Double>();
        SparseBooleanArray _arr = _list.getCheckedItemPositions();
        for (int _iIdx = 0; _iIdx < _arr.size(); _iIdx++) {
            if (_arr.valueAt(_iIdx))
                _result.add((double) _arr.keyAt(_iIdx));
        }
        return _result;
    }

    @Deprecated
    public float getDip(int _input) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, _input, getResources().getDisplayMetrics());
    }

    @Deprecated
    public int getDisplayWidthPixels() {
        return getResources().getDisplayMetrics().widthPixels;
    }

    @Deprecated
    public int getDisplayHeightPixels() {
        return getResources().getDisplayMetrics().heightPixels;
    }


    public void emulateClick(final WebView webview) {
        long delta = 100;
        long downTime = SystemClock.uptimeMillis();
        float x = webview.getLeft() + webview.getWidth() / 2; //in the middle of the webview
        float y = webview.getTop() + webview.getHeight() / 8;

        final MotionEvent downEvent = MotionEvent.obtain(downTime, downTime + delta, MotionEvent.ACTION_DOWN, x, y, 0);
        // change the position of touch event, otherwise, it'll show the menu.
        final MotionEvent upEvent = MotionEvent.obtain(downTime, downTime + delta, MotionEvent.ACTION_UP, x + 10, y + 10, 0);

        webview.post(new Runnable() {
            @Override
            public void run() {
                if (webview != null) {
                    webview.dispatchTouchEvent(downEvent);
                    webview.dispatchTouchEvent(upEvent);
                }
            }
        });
    }


    void delFromCache(String url, String category, String rating, String lang) {

        final String browserURL = url;
        url = "https://dashboard.skillserver.de/api/public/skills/" + rating + "?text=" + URLEncoder.encode(url) + "&category=" + URLEncoder.encode(category) + "&lang=" + URLEncoder.encode(lang);
        Log.i("Votingurl",url);
        StringRequest request = new StringRequest(url, new Response.Listener<String>() {
            @Override
            public void onResponse(String jsonString) {
                Toast.makeText(assistContext, "Voted",
                        Toast.LENGTH_SHORT).show();

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {

            }
        });

        RequestQueue rQueue = Volley.newRequestQueue(assistant.this);
        int socketTimeout = 15000;//30 seconds - change to what you want
        RetryPolicy policy = new DefaultRetryPolicy(socketTimeout, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        request.setRetryPolicy(policy);
        rQueue.add(request);
    }

    public String generateURL() {
        String deepl;
        String owm;
        String wolframalpha;
        String scaleserp;
        String endURL = "";
        String lang;

        SharedPreferences pref = assistContext.getSharedPreferences("settings", 0); // 0 - for private mode

        deepl = pref.getString("deepl", "Key");
        owm = pref.getString("owm", "Key");
        wolframalpha = pref.getString("wolframalpha", "Key");
        scaleserp = pref.getString("scaleserp", "Key");
        lang = pref.getString("language","auto");

        endURL += "&calendar_url=" + pref.getString("caldav_serverurl","https://caldav.url");
        endURL += "&calendar_username=" + pref.getString("caldav_name","Name");
        endURL += "&calendar_password=" + pref.getString("caldav_pw","Password");

        if (!deepl.contains("Key") && deepl != "") {
            endURL += "&deepl=" + URLEncoder.encode(deepl);
        }

        if (!scaleserp.contains("Key") && scaleserp != "") {
            endURL += "&serpstack_api=" + URLEncoder.encode(scaleserp);
        }

        if (!wolframalpha.contains("Key") && wolframalpha != "") {
            endURL += "&wolframalpha_api=" + URLEncoder.encode(wolframalpha);
        }

        if (!owm.contains("Key") && owm != "") {
            endURL += "&owmapi=" + URLEncoder.encode(owm);
        }

        if(!lang.contains("auto")){
            endURL += "&lang=" + URLEncoder.encode(lang);
        } else {
            lang = Locale.getDefault().toString().split("_")[0];
            endURL += "&lang=" + URLEncoder.encode(lang);
        }


        return endURL;
    }

    void callAPI(String url) {
        final boolean[] scriptrunning = {false};
        final String orgininal_request = url;
        SharedPreferences prefer = assistContext.getSharedPreferences("stt", Context.MODE_MULTI_PROCESS); // 0 - for private mode
        iscalling = prefer.getBoolean("iscalling",false);
        if(iscalling){
            return;
        }
        SharedPreferences.Editor editor = prefer.edit();
        editor.putBoolean("iscalling",true);
        editor.commit();

        String keys = generateURL();
        if(!answerType.contains("answer")){
            datakey = datakey + "=" + URLEncoder.encode(url) + "&";
            keys = keys + datakey + URLEncoder.encode(additionalData);
            url = spokentext;
        } else {
            datakey = "";
            spokentext = url;
        }
        SharedPreferences pref = assistContext.getSharedPreferences("settings", 0); // 0 - for private mode
        String serverurl = pref.getString("serverurl","https://dashboard.skillserver.de/api/public/skills");
        if(serverurl.contains("http://") || serverurl.contains("https://")){
            url = serverurl + "?text=" + URLEncoder.encode(url) + keys + "&secret=g8iEyrkek0ZuiH1QLolgepOKNaU1Gi3Y0Nf6JHC8";
        } else {
            url = "https://dashboard.skillserver.de/api/public/skills?text=" + URLEncoder.encode(url) + keys + "&secret=g8iEyrkek0ZuiH1QLolgepOKNaU1Gi3Y0Nf6JHC8";
        }

        Log.i("APICALLURL", url);
        Log.i("APICALL", "Call");
        StringRequest request = new StringRequest(url, new Response.Listener<String>() {
            @Override
            public void onResponse(String jsonString) {
                Log.i("APICALL", "Speak");
                iscalling = false;
                SharedPreferences preferer = assistContext.getSharedPreferences("stt", Context.MODE_MULTI_PROCESS); // 0 - for private mode
                SharedPreferences.Editor editorer = preferer.edit();
                editorer.putBoolean("iscalling",false);
                editorer.commit();

                try {
                    JSONObject object = new JSONObject(jsonString);
                    Iterator<?> keys = object.keys();

                    try {
                        String error = object.getString("error");
                        if(error.contains("script running")){
                            TimeUnit.SECONDS.sleep(2);
                            scriptrunning[0] = true;
                        }
                    } catch (Exception e) {

                    }

                    String speakIt = object.getString("speak");
                    String inputtext;
                    for (int i = 0; i < 10; i++) {
                        speakIt = speakIt.replace(Integer.toString(i) + ". ", Integer.toString(i) + ".");
                    }

                    recognitionProgressView.stop();
                    recognitionProgressView.setVisibility(View.GONE);
                    browser.setVisibility(View.VISIBLE);
                    button1.setVisibility(View.VISIBLE);

                    Toast.makeText(assistContext, speakIt, Toast.LENGTH_LONG).show();

                    button1.setText("Stop");


                    answerType = object.getString("answer_type");
                    langcode = object.getString("lang");
                    inputtext = object.getString("input");

                    try{
                        langucode = object.getString("langucode");
                    } catch (Exception e){
                        langucode = langcode;
                    }

                    ttsspeak(speakIt);

                    Log.i("APICALL","Language Code "+langcode);

                    if(!answerType.contains("answer")){
                        datakey = datakey + "&" + answerType;
                        while( keys.hasNext() ) {
                            String key = (String) keys.next();
                            additionalData = "";
                            if(!key.equals("input") && !key.equals("intent") && !key.equals("slots") && !key.equals("lang") && !key.equals("skill_category") && !key.equals("speak") && !key.equals("answer_url") && !key.equals("answer_type")){
                                additionalData = additionalData + "&" + key + "=" + object.get(key);
                            }
                            Log.i("APICALLData","Key: " + key);
                            Log.i("APICALLData","Value: " + object.get(key));
                            while(tts.isSpeaking()){
                            }
                            startListen();
                        }
                    } else {

                        try{
                            stt.stopListening();
                            stt.destroy();
                        } catch (Exception e){

                        }

                        try {
                            mMozillaSpeechService.removeListener(mVoiceSearchListener);
                        } catch (Exception e){

                        }

                        String skill_category = object.getString("skill_category");
                        skillcategory = skill_category;

                        if(skillcategory.contains("helpme")){
                            helpme();
                        } else if (skill_category.contains("appcontrol")) {
                            try {
                                openApps(object.getString("appnames"));
                            } catch (Exception e) {

                            }
                        } else if (skill_category.contains("phonecall")) {
                            try {
                                callContact(object.getString("name"));
                            } catch (Exception e) {

                            }
                        } else if(skill_category.contains("closeava")){
                            Intent startMain = new Intent(Intent.ACTION_MAIN);
                            startMain.addCategory(Intent.CATEGORY_HOME);
                            startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(startMain);
                        } else if(skill_category.contains("clock")){
                            double hours = 0;
                            double minutes = 0;
                            double seconds = 0;

                            try {
                                hours = object.getDouble("hours");
                            } catch (Exception e) {

                            }

                            try {
                                minutes = object.getDouble("minutes");
                            } catch (Exception e) {

                            }

                            try {
                                seconds = object.getDouble("seconds");
                            } catch (Exception e) {

                            }

                            if(object.getJSONObject("intent").getString("intentName").contains("timer")){
                                int timer = (int) hours * 60 * 60 + (int) minutes * 60 + (int) seconds;
                                startTimer("AVA's Timer",timer);
                            } else {
                                createAlarm("Ava's Alarm",(int) hours,(int) minutes);
                            }
                        }else if(skill_category.contains("navigation")){
                            String intention = object.getJSONObject("intent").getString("intentName");
                            String target = URLEncoder.encode(object.getString("target"));
                            String mapsurl;

                            if(intention.contains("search")){
                                mapsurl = "https://www.google.com/maps/search/?api=1&query=" + target;
                            } else {
                                mapsurl = "https://www.google.com/maps/dir/?api=1&destination=" + target;
                            }

                            Intent intent = new Intent(android.content.Intent.ACTION_VIEW,
                                    Uri.parse(mapsurl));
                            startActivity(intent);

                        }else if(skill_category.contains("notes")){
                            String notes = object.getString("sentences");

                            Intent sendIntent = new Intent();
                            sendIntent.setAction(Intent.ACTION_SEND);
                            sendIntent.putExtra(Intent.EXTRA_TEXT, notes);
                            sendIntent.setType("text/plain");

                            Intent shareIntent = Intent.createChooser(sendIntent, null);
                            startActivity(shareIntent);

                        } else {
                            try {
                                final String answer_url = object.getString("answer_url");
                                SharedPreferences pref = assistContext.getSharedPreferences("settings", 0); // 0 - for private mode
                                int loadWeb = pref.getInt("webviewload",0);
                                if(loadWeb == 0 || loadWeb == 1){
                                    if(answer_url.contains("ask_search_engine")){
                                        getbrowserUrl(langcode, inputtext);
                                    }
                                    browser.loadUrl(answer_url);
                                } else if(loadWeb == 1){

                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {

                                            if (!isFinishing()){
                                                new AlertDialog.Builder(assistant.this)
                                                        .setTitle(R.string.loadWeb)
                                                        .setMessage(answer_url)
                                                        .setCancelable(false)
                                                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                                            @Override
                                                            public void onClick(DialogInterface dialog, int which) {
                                                                browser.loadUrl(answer_url);
                                                            }
                                                        }). setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which) {

                                                    }
                                                }).show();
                                            }
                                        }
                                    });
                                }
                            } catch (JSONException e2) {
                                Log.e("openapp", e2.toString());

                            }
                        }

                    }

                } catch (JSONException e) {
                    Log.e("APICALL", "exception", e);
                    if(!scriptrunning[0]){
                        Toast.makeText(assistContext, "Some error occurred!!", Toast.LENGTH_SHORT).show();
                        stopAll();
                    } else {
                        callagain(orgininal_request);
                    }
                }

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                Toast.makeText(assistContext, "Some error occurred!!", Toast.LENGTH_SHORT).show();
                stopAll();
            }
        });

        RequestQueue rQueue = Volley.newRequestQueue(assistant.this);
        int socketTimeout = 30000;//30 seconds - change to what you want
        RetryPolicy policy = new DefaultRetryPolicy(socketTimeout, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        request.setRetryPolicy(policy);
        rQueue.add(request);
    }

    public void callagain(String orgininal_request) {
        SharedPreferences prefer = assistContext.getSharedPreferences("stt", Context.MODE_MULTI_PROCESS); // 0 - for private mode
        SharedPreferences.Editor editor = prefer.edit();
        editor.putBoolean("iscalling",false);
        editor.commit();
        callAPI(orgininal_request);
    }


    void getbrowserUrl(String lang, String text) {
        String url;
        SharedPreferences pref = assistContext.getSharedPreferences("settings", 0); // 0 - for private mode
        String serverurl = pref.getString("serverurl","https://dashboard.skillserver.de/api/public/skills");
        if(serverurl.contains("http://") || serverurl.contains("https://")){
            url = serverurl + "/url?text=" + URLEncoder.encode(text) + "&lang=" + URLEncoder.encode(lang);
        } else {
            url = "https://dashboard.skillserver.de/api/public/skills/url?text=" + URLEncoder.encode(text) + "&lang=" + URLEncoder.encode(lang);
        }

        Log.i("APICALLURL", url);
        Log.i("APICALL", "Call");
        StringRequest request = new StringRequest(url, new Response.Listener<String>() {
            @Override
            public void onResponse(String jsonString) {
                Log.i("APICALL", "Speak");

                try {
                    JSONObject object = new JSONObject(jsonString);
                    Iterator<?> keys = object.keys();

                    try {
                        final String answer_url = object.getString("url");
                        SharedPreferences pref = assistContext.getSharedPreferences("settings", 0); // 0 - for private mode
                        int loadWeb = pref.getInt("webviewload",0);
                        if(loadWeb == 0){
                            if(answer_url.contains("ask_search_engine")){

                            }
                            browser.loadUrl(answer_url);
                        } else if(loadWeb == 1){

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {

                                    if (!isFinishing()){
                                        new AlertDialog.Builder(assistant.this)
                                                .setTitle(R.string.loadWeb)
                                                .setMessage(answer_url)
                                                .setCancelable(false)
                                                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which) {
                                                        browser.loadUrl(answer_url);
                                                    }
                                                }). setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {

                                            }
                                        }).show();
                                    }
                                }
                            });
                        }
                    } catch (JSONException e2) {
                        Log.e("openapp", e2.toString());

                    }

                } catch (JSONException e) {
                    Log.e("APICALL", "exception", e);
                    Toast.makeText(assistContext, "Some error occurred!!", Toast.LENGTH_SHORT).show();
                    stopAll();
                }

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                Toast.makeText(assistContext, "Some error occurred!!", Toast.LENGTH_SHORT).show();
                stopAll();
            }
        });

        RequestQueue rQueue = Volley.newRequestQueue(assistant.this);
        int socketTimeout = 30000;//30 seconds - change to what you want
        RetryPolicy policy = new DefaultRetryPolicy(socketTimeout, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        request.setRetryPolicy(policy);
        rQueue.add(request);
    }


    public void startListen() {
        SharedPreferences pref = assistContext.getSharedPreferences("stt", Context.MODE_MULTI_PROCESS); // 0 - for private mode
        if(pref.getBoolean("isListening",false)){
            return;
        } else {
            SharedPreferences.Editor editor = pref.edit();
            editor.putBoolean("isListening",true);
            editor.commit();
            Log.e("APICALL", "StartListening");

            SharedPreferences prefer = getApplicationContext().getSharedPreferences("settings", Context.MODE_MULTI_PROCESS);
            int asr = prefer.getInt("ASR",0);

            recognitionProgressView.setVisibility(View.VISIBLE);
            browser.setVisibility(View.GONE);
            button1.setVisibility(View.GONE);
            recognitionProgressView.play();

            if(asr == 2){
                myOggStream = microphoneHelper.getInputStream(true);
                button1.setVisibility(View.VISIBLE);
                recognitionProgressView.setVisibility(View.GONE);
                isrecording = true;
                ToneGenerator toneGen1 = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
                toneGen1.startTone(ToneGenerator.TONE_CDMA_PIP,250);

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            speechservice.recognizeUsingWebSocket(getRecognizeOptions(myOggStream),
                                    new MicrophoneRecognizeDelegate());
                        } catch (Exception e) {
                        }
                    }
                }).start();

            } else if(asr == 1){

                try{
                    stt.cancel();
                } catch(Exception e) {

                }

                Intent _intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                _intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName());
                _intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                _intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, loclang);
                stt.startListening(_intent);
            } else if (asr == 0){
                ToneGenerator toneGen1 = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
                toneGen1.startTone(ToneGenerator.TONE_CDMA_PIP,250);
                try{
                    mMozillaSpeechService.cancel();
                    mMozillaSpeechService.removeListener(mVoiceSearchListener);
                } catch(Exception e) {

                }
                mMozillaSpeechService = MozillaSpeechService.getInstance();
                mMozillaSpeechService.setLanguage(loclang.toLanguageTag());
                mMozillaSpeechService.addListener(mVoiceSearchListener);
                mMozillaSpeechService.start(getApplicationContext());
            }

        }

    }

    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
        Log.e("assistantTask", "onPause");

    }

    @Override
    protected void onStop() {
        // TODO Auto-generated method stub
        super.onStop();
        stopAll();
        SharedPreferences pref = assistContext.getSharedPreferences("stt", Context.MODE_MULTI_PROCESS); // 0 - for private mode
        SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean("isListening",false);
        editor.commit();
        Log.e("assistantTask", "onStop");

    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }



    public void callContact(String contactName) {
        ContentResolver cr = getContentResolver();
        Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI,
                null, null, null, null);

        if ((cur != null ? cur.getCount() : 0) > 0) {
            while (cur != null && cur.moveToNext()) {
                String id = cur.getString(
                        cur.getColumnIndex(ContactsContract.Contacts._ID));
                String name = cur.getString(cur.getColumnIndex(
                        ContactsContract.Contacts.DISPLAY_NAME));

                if (cur.getInt(cur.getColumnIndex(
                        ContactsContract.Contacts.HAS_PHONE_NUMBER)) > 0) {
                    Cursor pCur = cr.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                            new String[]{id}, null);
                    while (pCur.moveToNext()) {
                        String phoneNo = pCur.getString(pCur.getColumnIndex(
                                ContactsContract.CommonDataKinds.Phone.NUMBER));

                        if (name.toLowerCase().contains(contactName.toLowerCase())) {
                            String uri = "tel:" + phoneNo;
                            Log.i("PhoneCall", "Name: " + name);
                            Intent intent = new Intent(Intent.ACTION_CALL);
                            intent.setData(Uri.parse(uri));
                            startActivity(intent);
                            break;
                        }
                    }
                    pCur.close();
                }
            }
        }
        if(cur!=null){
            cur.close();
        }


    }




    public void openApps(String app) {
        Context c = assistContext;
        //This is where we build our list of app details, using the app
        //object we created to store the label, package name and icon

        PackageManager pm = c.getPackageManager();

        Intent i = new Intent(Intent.ACTION_MAIN, null);
        i.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> allApps = pm.queryIntentActivities(i, 0);
        for(ResolveInfo ri:allApps) {
            if(app.toLowerCase().contains(ri.loadLabel(pm).toString().toLowerCase())){
                Intent launchIntent = getPackageManager().getLaunchIntentForPackage(ri.activityInfo.packageName);
                if (launchIntent != null) {
                    startActivity(launchIntent);//null pointer check in case package name was not found
                } else {

                }
            }
        }

        for(ResolveInfo ri:allApps) {
            if(ri.loadLabel(pm).toString().toLowerCase().contains(app.toLowerCase())){
                Intent launchIntent = getPackageManager().getLaunchIntentForPackage(ri.activityInfo.packageName);
                if (launchIntent != null) {
                    startActivity(launchIntent);//null pointer check in case package name was not found
                } else {

                }
            }
        }

    }



    public void startTimer(String message, int seconds) {
        Intent intent = new Intent(AlarmClock.ACTION_SET_TIMER)
                .putExtra(AlarmClock.EXTRA_MESSAGE, message)
                .putExtra(AlarmClock.EXTRA_LENGTH, seconds)
                .putExtra(AlarmClock.EXTRA_SKIP_UI, true);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        }
    }

    public void createAlarm(String message, int hour, int minutes) {
        Intent intent = new Intent(AlarmClock.ACTION_SET_ALARM)
                .putExtra(AlarmClock.EXTRA_MESSAGE, message)
                .putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                .putExtra(AlarmClock.EXTRA_HOUR, hour)
                .putExtra(AlarmClock.EXTRA_MINUTES, minutes);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        }
    }



    public void helpme(){

        SharedPreferences pref = getApplicationContext().getSharedPreferences("settings", Context.MODE_MULTI_PROCESS); // 0 - for private mode

        String phonenumber = pref.getString("phonenumber","");
        int helpfeature = pref.getInt("hlp", 0);

        if(helpfeature == 4){
            try{
                GPSTracker gpsTracker;
                gpsTracker = new GPSTracker(assistContext);
                if (gpsTracker.getLocation() != null) {
                    if (gpsTracker.getLatitude() != 0 && gpsTracker.getLongitude() != 0) {
                        String lat =  Double.toString(gpsTracker.getLatitude());
                        String longi =  Double.toString(gpsTracker.getLongitude());
                        String message = "Help me, this is my location: https://www.google.com/maps/search/?api=1&query=" + lat + "," + longi;
                        SmsManager smsManager = SmsManager.getDefault();
                        String[] phonenumbers = phonenumber.split(",");
                        for(String p : phonenumbers){
                            smsManager.sendTextMessage(p, null, message, null, null);
                        }
                        Toast.makeText(assistContext, "Send SMS", Toast.LENGTH_SHORT).show();
                    }
                }
            } catch (Exception e){

            }

            try {
                MediaPlayer mediaPlayer = new MediaPlayer();

                AssetFileDescriptor descriptor = assistContext.getAssets().openFd("alarm.mp3");
                mediaPlayer.setDataSource(descriptor.getFileDescriptor(), descriptor.getStartOffset(), descriptor.getLength());
                descriptor.close();

                AudioManager mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
                mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0);


                mediaPlayer.prepare();
                mediaPlayer.setVolume(1f, 1f);
                mediaPlayer.setLooping(false);
                mediaPlayer.start();

                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener(){
                    int maxCount = 1;
                    int count = 0; // initialise outside listener to prevent looping

                    @Override
                    public void onCompletion(MediaPlayer mediaPlayer) {
                        if(count < maxCount) {
                            count = count + 1;
                            mediaPlayer.seekTo(0);
                            mediaPlayer.start();
                        }
                    }});

            } catch (Exception e) {
                e.printStackTrace();
            }

        } else if(helpfeature == 3){
            String[] phonenumbers = phonenumber.split(",");
            for(String p : phonenumbers){

            }

            try{
                Intent intent = new Intent(Intent.ACTION_CALL);
                intent.setData(Uri.parse("tel:" + phonenumbers[0]));
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
            } catch (Exception e){

            }

        } else if(helpfeature == 2){
            try{
                GPSTracker gpsTracker;
                gpsTracker = new GPSTracker(assistContext);
                if (gpsTracker.getLocation() != null) {
                    if (gpsTracker.getLatitude() != 0 && gpsTracker.getLongitude() != 0) {
                        String lat =  Double.toString(gpsTracker.getLatitude());
                        String longi =  Double.toString(gpsTracker.getLongitude());
                        String message = "Help me, this is my location: https://www.google.com/maps/search/?api=1&query=" + lat + "," + longi;
                        SmsManager smsManager = SmsManager.getDefault();
                        String[] phonenumbers = phonenumber.split(",");
                        for(String p : phonenumbers){
                            smsManager.sendTextMessage(p, null, message, null, null);
                        }
                        Toast.makeText(assistContext, "Send SMS", Toast.LENGTH_SHORT).show();
                    }
                }
            } catch (Exception e){

            }
        } else if(helpfeature == 1){
            try {
                MediaPlayer mediaPlayer = new MediaPlayer();

                AssetFileDescriptor descriptor = assistContext.getAssets().openFd("alarm.mp3");
                mediaPlayer.setDataSource(descriptor.getFileDescriptor(), descriptor.getStartOffset(), descriptor.getLength());
                descriptor.close();

                AudioManager mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
                mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0);


                mediaPlayer.prepare();
                mediaPlayer.setVolume(1f, 1f);
                mediaPlayer.setLooping(false);
                mediaPlayer.start();

                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener(){
                    int maxCount = 3;
                    int count = 0; // initialise outside listener to prevent looping

                    @Override
                    public void onCompletion(MediaPlayer mediaPlayer) {
                        if(count < maxCount) {
                            count = count + 1;
                            mediaPlayer.seekTo(0);
                            mediaPlayer.start();
                        }
                    }});

            } catch (Exception e) {
                e.printStackTrace();
            }
        }



    }






    public void askPermission(){
        Intent intent = new Intent();
        String packageName = getPackageName();
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + packageName));
            startActivity(intent);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent myintent = new Intent();
                myintent.setAction(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                myintent.setData(Uri.parse("package:" + packageName));
                startActivity(myintent);
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {

            // Grant permission(s) dialog
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO}, 111);
        } else {

        }
    }

    public void getRuntimePermissions() {
        List<String> allNeededPermissions = new ArrayList<>();
        for (String permission : getRequiredPermissions()) {
            if (!isPermissionGranted(this, permission)) {
                allNeededPermissions.add(permission);
            }
        }

        if (!allNeededPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(
                    this, allNeededPermissions.toArray(new String[0]), 1);
        }
    }

    public String[] getRequiredPermissions() {
        try {
            PackageInfo info =
                    this.getPackageManager()
                            .getPackageInfo(this.getPackageName(), PackageManager.GET_PERMISSIONS);
            String[] ps = info.requestedPermissions;
            if (ps != null && ps.length > 0) {
                return ps;
            } else {
                return new String[0];
            }
        } catch (Exception e) {
            return new String[0];
        }
    }

    public static boolean isPermissionGranted(Context context, String permission) {
        if (ContextCompat.checkSelfPermission(context, permission)
                == PackageManager.PERMISSION_GRANTED) {
            Log.i("Tag","Permission granted: " + permission);
            return true;
        }
        Log.i("Tag", "Permission NOT granted: " + permission);
        return false;
    }


    public boolean allPermissionsGranted() {
        for (String permission : getRequiredPermissions()) {
            if (!isPermissionGranted(this, permission)) {
                return false;
            }
        }
        return true;
    }

    private class MicrophoneRecognizeDelegate extends BaseRecognizeCallback implements RecognizeCallback {
        @Override
        public void onTranscription(SpeechRecognitionResults speechResults) {
            System.out.println(speechResults);
            if (speechResults.getResults() != null && !speechResults.getResults().isEmpty()) {
                String text = speechResults.getResults().get(0).getAlternatives().get(0).getTranscript();
                //Toast.makeText(assistContext, text, Toast.LENGTH_SHORT).show();
                ibmtext = text;
            }
        }

        @Override
        public void onError(Exception e) {
            try {
                // This is critical to avoid hangs
                // (see https://github.com/watson-developer-cloud/android-sdk/issues/59)
                e.printStackTrace();
                myOggStream.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }

        }

        @Override
        public void onDisconnected() {
            recognitionProgressView.stop();
            recognitionProgressView.setVisibility(View.GONE);
            browser.setVisibility(View.VISIBLE);
            button1.setVisibility(View.VISIBLE);
            Toast.makeText(assistContext, ibmtext, Toast.LENGTH_SHORT).show();
        }
    }

    private RecognizeOptions getRecognizeOptions(InputStream captureStream) {
        return new RecognizeOptions.Builder()
                .audio(captureStream)
                .contentType(ContentType.OPUS.toString())
                .model(loclang.toLanguageTag() + "_BroadbandModel")
                .interimResults(true)
                .inactivityTimeout(2000)
                .build();
    }


    private class SynthesisTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            String voice;
            if (langucode == null){
                langucode = Locale.getDefault().toLanguageTag();
            }
            langucode = langucode.split("-")[0];
            if(langucode.contains("ar")){
                voice = "ar-AR_OmarVoice";
            } else if(langucode.contains("pt")){
                voice = "pt-BR_IsabelaV3Voice";
            } else if(langucode.contains("zh")){
                voice = "zh-CN_LiNaVoice";
            } else if(langucode.contains("nl")){
                voice = "nl-NL_EmmaVoice";
            } else if(langucode.contains("en")){
                voice = "en-US_LisaV3Voice";
            } else if(langucode.contains("fr")){
                voice = "fr-FR_ReneeV3Voice";
            } else if(langucode.contains("de")){
                voice = "de-DE_BirgitV3Voice";
            } else if(langucode.contains("it")){
                voice = "it-IT_FrancescaV3Voice";
            } else if(langucode.contains("ja")){
                voice = "ja-JP_EmiV3Voice";
            } else if(langucode.contains("ko")){
                voice = "ko-KR_YoungmiVoice";
            } else if(langucode.contains("es")){
                voice = "es-LA_SofiaV3Voice";
            } else {
                voice = "en-US_LisaV3Voice";
            }

            SynthesizeOptions synthesizeOptions = new SynthesizeOptions.Builder()
                    .text(params[0])
                    .voice(voice)
                    .accept(HttpMediaType.AUDIO_WAV)
                    .build();
            player.playStream(textService.synthesize(synthesizeOptions).execute().getResult());
            return "Did synthesize";
        }
    }

    public String langtocode(String lang){
        if(lang.contains("en")){
            return "en-US";
        } else if(lang.contains("ja")){
            return "ja-JP";
        } else if(lang.contains("ko")){
            return "ko-KR";
        } else if(lang.contains("pt")){
            return "pt-BR";
        } else if(lang.contains("zh")){
            return "zh-CN";
        } else {
            return lang.toLowerCase() + "-" + lang.toUpperCase();
        }
    }

}
