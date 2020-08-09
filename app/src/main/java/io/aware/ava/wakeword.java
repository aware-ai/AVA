package io.aware.ava;

import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;

import androidx.core.app.NotificationCompat;

public class wakeword extends Service {
    public wakeword() {
    }

    private static final int SAMPLE_RATE = 16000;
    private static final int SAMPLE_DURATION_MS = 1000;
    private static final int RECORDING_LENGTH = (int) (SAMPLE_RATE * SAMPLE_DURATION_MS / 1000);
    private static final long AVERAGE_WINDOW_DURATION_MS = 500;
    private static final float DETECTION_THRESHOLD = 0.80f;
    private static final int SUPPRESSION_MS = 1500;
    private static final int MINIMUM_COUNT = 1;
    private static final long MINIMUM_TIME_BETWEEN_SAMPLES_MS = 30;
    private static final String LABEL_FILENAME = "file:///android_asset/labels.txt";
    private static final String MODEL_FILENAME = "file:///android_asset/model.pb";
    private static final String INPUT_DATA_NAME = "decoded_sample_data:0";
    private static final String SAMPLE_RATE_NAME = "decoded_sample_data:1";
    private static final String OUTPUT_SCORES_NAME = "labels_softmax";

    // UI elements.
    private static final int REQUEST_RECORD_AUDIO = 13;
    private static final String LOG_TAG = "wakewordlistener";

    // Working variables.
    short[] recordingBuffer = new short[RECORDING_LENGTH];
    int recordingOffset = 0;
    boolean shouldContinue = true;
    private Thread recordingThread;
    boolean shouldContinueRecognition = true;
    private Thread recognitionThread;
    private final ReentrantLock recordingBufferLock = new ReentrantLock();
    private TensorFlowInferenceInterface inferenceInterface;
    private List<String> labels = new ArrayList<String>();
    private List<String> displayedLabels = new ArrayList<>();
    private RecognizeCommands recognizeCommands = null;
    public static Context wakeContext;




    private static final String CHANNEL_ID = "wakewordchannel";

    public boolean recording = false;
    public WindowManager windowManager;
    public LayoutInflater li;
    public LinearLayout mTopView;
    public String selectedwakeword;
    public int helpfeature;
    public String phonenumber;
    public long timems;
    public int helpcount = 0;
    public String lastcommand = "";

    public static boolean screenOn = true;



    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "wakewordchannel",
                    NotificationManager.IMPORTANCE_LOW);

            NotificationManager manager = getSystemService(NotificationManager.class);
            assert manager != null;
            manager.deleteNotificationChannel(notificationChannel.getId());
            manager.createNotificationChannel(notificationChannel);
        }
    }

    public void stopitself(){
        stopForeground(true);
        stopSelf();
    }


    @Override
    public void onCreate() {
        super.onCreate();
        if(wakeContext == null){
            wakeContext = this.getApplicationContext();
        }
        windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        timems = System.currentTimeMillis();
        createNotificationChannel();

        SharedPreferences pref = getApplicationContext().getSharedPreferences("settings", Context.MODE_MULTI_PROCESS); // 0 - for private mode
        selectedwakeword = pref.getString("wakeword", "heyava");
        phonenumber = pref.getString("phonenumber","");
        helpfeature = pref.getInt("hlp", 0);

        String actualLabelFilename = LABEL_FILENAME.split("file:///android_asset/", -1)[1];
        Log.i(LOG_TAG, "Reading labels from: " + actualLabelFilename);
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(getAssets().open(actualLabelFilename)));
            String line;
            if(labels.size() == 0) {
                while ((line = br.readLine()) != null) {
                    labels.add(line);
                    if (line.charAt(0) != '_') {
                        displayedLabels.add(line.substring(0, 1).toUpperCase() + line.substring(1));
                    }
                }
            }

            br.close();
        } catch (IOException e) {
            throw new RuntimeException("Problem reading label file!", e);
        }

        // Set up an object to smooth recognition results to increase accuracy.
        recognizeCommands =
                new RecognizeCommands(
                        labels,
                        AVERAGE_WINDOW_DURATION_MS,
                        DETECTION_THRESHOLD,
                        SUPPRESSION_MS,
                        MINIMUM_COUNT,
                        MINIMUM_TIME_BETWEEN_SAMPLES_MS);

        String actualModelFilename = MODEL_FILENAME.split("file:///android_asset/", -1)[1];
        try {
            inferenceInterface = new TensorFlowInferenceInterface(getAssets(), MODEL_FILENAME);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Start the recording and recognition threads.
        display();
        startRecording();
        startRecognition();


        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                new Intent(this, MainActivity.class),
                0);


        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Wakeword")
                .setContentText("running")
                .setSmallIcon(R.drawable.avalogo)
                .setContentIntent(pendingIntent)
                .build();

        startForeground(1234, notification);
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        return super.onStartCommand(intent, flags, startId);
    }


    public void display(){

        int layoutParamsType;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutParamsType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        }
        else {
            layoutParamsType = WindowManager.LayoutParams.TYPE_PHONE;
        }
        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutParamsType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);


        li = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        mTopView = (LinearLayout) li.inflate(R.layout.running, null);

        windowManager.addView(mTopView, params);


    }


    public void startRecognition() {
        //recognize();
        if (recognitionThread != null) {
            return;
        }
        shouldContinueRecognition = true;
        recognitionThread =
                new Thread(
                        new Runnable() {
                            @Override
                            public void run() {
                                recognize();
                            }
                        });
        recognitionThread.start();
    }


    public void startRecording() {
        //record();
        if (recordingThread != null) {
            return;
        }
        shouldContinue = true;
        recordingThread =
                new Thread(
                        new Runnable() {
                            @Override
                            public void run() {
                                record();
                            }
                        });
        recordingThread.start();
    }

    public void stopRecording() {
        if (recordingThread == null) {
            return;
        }
        shouldContinue = false;
        shouldContinueRecognition = false;
        recordingThread = null;
        recognitionThread = null;
    }


    private void record() {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);


        // Estimate the buffer size we'll need for this device.
        int bufferSize =
                AudioRecord.getMinBufferSize(
                        SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            bufferSize = SAMPLE_RATE * 2;
        }
        short[] audioBuffer = new short[bufferSize / 2];

        AudioRecord record =
                new AudioRecord(
                        MediaRecorder.AudioSource.DEFAULT,
                        SAMPLE_RATE,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        bufferSize);

        if (record.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.e(LOG_TAG, "Audio Record can't initialize!");
            return;
        }

        record.startRecording();

        Log.v(LOG_TAG, "Start recording");

        // Loop, gathering audio data and copying it to a round-robin buffer.
        while (shouldContinue) {

            while(!screenOn) {
                SharedPreferences prefs = getApplicationContext().getSharedPreferences("settings", 0);
                if(prefs.getInt("lckscr",0) == 1){
                    stopitself();
                }
                try {
                    Thread.sleep(1500);
                    Log.i("WakewordScreen","screen off");
                } catch (InterruptedException e) {
                    Log.i("Wakeworddetection","Sleeping");
                }
            }


            int numberRead = record.read(audioBuffer, 0, audioBuffer.length);
            int maxLength = recordingBuffer.length;
            int newRecordingOffset = recordingOffset + numberRead;
            int secondCopyLength = Math.max(0, newRecordingOffset - maxLength);
            int firstCopyLength = numberRead - secondCopyLength;
            // We store off all the data for the recognition thread to access. The ML
            // thread will copy out of this buffer into its own, while holding the
            // lock, so this should be thread safe.
            recordingBufferLock.lock();
            try {
                System.arraycopy(audioBuffer, 0, recordingBuffer, recordingOffset, firstCopyLength);
                System.arraycopy(audioBuffer, firstCopyLength, recordingBuffer, 0, secondCopyLength);
                recordingOffset = newRecordingOffset % maxLength;
            } finally {
                recordingBufferLock.unlock();
            }





        }

        record.stop();
        record.release();
    }



    private void recognize() {

        Log.v(LOG_TAG, "Start recognition");

        short[] inputBuffer = new short[RECORDING_LENGTH];
        float[] floatInputBuffer = new float[RECORDING_LENGTH];
        float[] outputScores = new float[labels.size()];
        String[] outputScoresNames = new String[] {OUTPUT_SCORES_NAME};
        int[] sampleRateList = new int[] {SAMPLE_RATE};

        while (shouldContinueRecognition) {

            while(!screenOn) {
                try {
                    Thread.sleep(1500);

                } catch (InterruptedException e) {
                    Log.i("Wakeworddetection","Sleeping");
                }
            }

            // The recording thread places data in this round-robin buffer, so lock to
            // make sure there's no writing happening and then copy it to our own
            // local version.
            recordingBufferLock.lock();
            try {
                int maxLength = recordingBuffer.length;
                int firstCopyLength = maxLength - recordingOffset;
                int secondCopyLength = recordingOffset;
                System.arraycopy(recordingBuffer, recordingOffset, inputBuffer, 0, firstCopyLength);
                System.arraycopy(recordingBuffer, 0, inputBuffer, firstCopyLength, secondCopyLength);
            } finally {
                recordingBufferLock.unlock();
            }

            // We need to feed in float values between -1.0f and 1.0f, so divide the
            // signed 16-bit inputs.
            for (int i = 0; i < RECORDING_LENGTH; ++i) {
                floatInputBuffer[i] = inputBuffer[i] / 32767.0f;
            }

            // Run the model.
            inferenceInterface.feed(SAMPLE_RATE_NAME, sampleRateList);
            inferenceInterface.feed(INPUT_DATA_NAME, floatInputBuffer, RECORDING_LENGTH, 1);
            inferenceInterface.run(outputScoresNames,true);
            try {
                inferenceInterface.fetch(OUTPUT_SCORES_NAME, outputScores);
            } catch (Exception e) {

            }


            // Use the smoother to figure out if we've had a real recognition event.
            long currentTime = System.currentTimeMillis();
            final RecognizeCommands.RecognitionResult result = recognizeCommands.processLatestResults(outputScores, currentTime);

            Log.i("WakewordDetection", Float.toString(result.score) + ": " + result.foundCommand);
            if(result.score == 0.0f){
                SharedPreferences errors = wakeContext.getSharedPreferences("ErrorLog",Context.MODE_MULTI_PROCESS);
                String error = errors.getString("ErrorLogs","Log:\n");
                SharedPreferences.Editor editore = errors.edit();
                editore.putString("ErrorLogs", error + "To few results from microphone for wakeword-detection\n");
                editore.commit();
            }

            if((result.foundCommand.contains(selectedwakeword) || result.foundCommand.contains("hilfe")) && result.score > DETECTION_THRESHOLD){
                SharedPreferences pref = getApplicationContext().getSharedPreferences("stt", Context.MODE_MULTI_PROCESS);
                boolean listening = pref.getBoolean("isListening",false);
                boolean ww;
                boolean help = false;
                if(result.foundCommand.contains(selectedwakeword)){
                    ww = true;
                } else {
                    ww = false;
                    if(currentTime - timems < 3000){
                        helpcount += 1;
                        if(helpcount >= 1){
                            help = true;
                        }
                    } else {
                        timems = currentTime;
                        helpcount = 0;
                        help = false;
                    }
                }

                if(!listening){
                    Log.v("APICALL", "recognitioned");

                    if(helpfeature == 4 && ww == false && help){
                        try{
                            GPSTracker gpsTracker;
                            gpsTracker = new GPSTracker(wakeContext);
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
                                    Toast.makeText(wakeContext, "Send SMS", Toast.LENGTH_SHORT).show();
                                }
                            }
                        } catch (Exception e){

                        }

                        try {
                            MediaPlayer mediaPlayer = new MediaPlayer();

                            AssetFileDescriptor descriptor = wakeContext.getAssets().openFd("alarm.mp3");
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

                    } else if(helpfeature == 3 && ww == false && help){
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

                    } else if(helpfeature == 2 && ww == false && help){
                        try{
                            GPSTracker gpsTracker;
                            gpsTracker = new GPSTracker(wakeContext);
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
                                    Toast.makeText(wakeContext, "Send SMS", Toast.LENGTH_SHORT).show();
                                }
                            }
                        } catch (Exception e){

                        }
                    } else if(helpfeature == 1 && ww == false && help){
                        try {
                            MediaPlayer mediaPlayer = new MediaPlayer();

                            AssetFileDescriptor descriptor = wakeContext.getAssets().openFd("alarm.mp3");
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
                    } else if(ww == true && lastcommand.contains("hilfe") == false){

                        lastcommand = "heyava";
                        SharedPreferences errors = wakeContext.getSharedPreferences("ErrorLog",Context.MODE_MULTI_PROCESS);
                        String error = errors.getString("ErrorLogs","Log:\n");
                        SharedPreferences.Editor editore = errors.edit();
                        editore.putString("ErrorLogs", error + result.foundCommand + ": " + String.valueOf(result.score) + "\n");
                        editore.commit();

                        Intent intent = new Intent(this, assistant.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                        startActivity(intent);


                        try {
                            Random r = new Random();
                            int i1 = r.nextInt(2500 - 1800) + 1800;
                            Thread.sleep(i1);
                        } catch (InterruptedException e) {
                            Log.i("Wakeworddetection","Sleeping");
                        }



                    }

                }

                lastcommand = result.foundCommand;
            }

            try {
                Random r = new Random();
                int i1 = r.nextInt(400 - 200) + 200;
                Thread.sleep(i1);
            } catch (InterruptedException e) {

            }



        }

        Log.v(LOG_TAG, "End recognition");
    }





    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
