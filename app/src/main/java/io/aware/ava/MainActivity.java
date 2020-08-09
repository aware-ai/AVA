package io.aware.ava;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.javiersantos.appupdater.AppUpdater;
import com.github.javiersantos.appupdater.enums.UpdateFrom;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


public class MainActivity extends AppCompatActivity {

    private int activeTimes = 0;
    private final int DEMO_PERMISSION_CODE = 111;
    public WindowManager windowManager;
    public LayoutInflater li;
    public LinearLayout mTopView;
    public Button settingsButton;
    public Button stopButton;
    public Button skillsButton;
    public Button assistant;
    public TextView wakewordhint;
    public static Context mainContext;
    public WebView browser;
    public Button agree;
    public Button disagree;
    public LinearLayout disclaim;
    public boolean accepted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mainContext = this.getApplicationContext();
        stopService();
        SharedPreferences prefer = mainContext.getSharedPreferences("stt", Context.MODE_MULTI_PROCESS); // 0 - for private mode
        SharedPreferences.Editor editorer = prefer.edit();
        editorer.putBoolean("iscalling",false);
        editorer.commit();

        initialize();

        SharedPreferences pref = getApplicationContext().getSharedPreferences("stt", 0); // 0 - for private mode
        SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean("isListening",false);
        editor.commit();

        SharedPreferences settings = getSharedPreferences("firstrun1", Context.MODE_MULTI_PROCESS);
        boolean firstrun = settings.getBoolean("accepted",false);
        if(!firstrun){
            browser.loadUrl("https://ava.a-ware.io/disclaimer-ava-app/");
            disclaim.setVisibility(View.VISIBLE);
        }

        AppUpdater appUpdater = new AppUpdater(this)
                .setUpdateFrom(UpdateFrom.JSON)
                .setCancelable(false)
                .setButtonDoNotShowAgain(null)
                .setButtonDismiss(null)
                .setUpdateJSON("https://skillserver.de/ava.json");
        appUpdater.start();

        startService();
    }




    public void initialize() {
        getRuntimePermissions();
        askPermission();

        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            // only for gingerbread and newer versions
            wakewordhint = findViewById(R.id.wakewordhint);
            wakewordhint.setVisibility(View.VISIBLE);
        }

        browser = findViewById(R.id.browser);
        disclaim = findViewById(R.id.disclaim);
        agree = findViewById(R.id.agree);
        agree.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(accepted){
                    SharedPreferences settings = getSharedPreferences("firstrun1", Context.MODE_MULTI_PROCESS);
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putBoolean("accepted", true);
                    // Commit the edits!
                    editor.commit();

                    disclaim.setVisibility(View.GONE);
                } else {
                    accepted = true;
                    browser.loadUrl("https://ava.a-ware.io/privacy-policy-ava-app/");
                }

            }
        });

        disagree = findViewById(R.id.disagree);
        disagree.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                System.exit(0);
            }
        });


        settingsButton = findViewById(R.id.settingsButton);

        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), settings.class);
                startActivity(intent);

            }
        });

        stopButton = findViewById(R.id.stopbutton);

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopService();
                int pid = android.os.Process.myPid();
                android.os.Process.killProcess(pid);
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_HOME);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        });

        assistant = findViewById(R.id.assistant);

        assistant.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), assistant.class);
                startActivity(intent);
            }
        });

        skillsButton = findViewById(R.id.skillsbutton);
        skillsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), skillslist.class);
                startActivity(intent);
            }
        });

    }



    private final static int REQUEST_CODE = 10101;





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
                    new String[]{Manifest.permission.RECORD_AUDIO}, DEMO_PERMISSION_CODE);
        } else {

        }
    }

    private void getRuntimePermissions() {
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

    private String[] getRequiredPermissions() {
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

    private static boolean isPermissionGranted(Context context, String permission) {
        if (ContextCompat.checkSelfPermission(context, permission)
                == PackageManager.PERMISSION_GRANTED) {
            Log.i("Tag","Permission granted: " + permission);

            return true;
        }
        Log.i("Tag", "Permission NOT granted: " + permission);
        return false;
    }


    private boolean allPermissionsGranted() {
        for (String permission : getRequiredPermissions()) {
            if (!isPermissionGranted(this, permission)) {
                return false;
            }
        }
        return true;
    }

    public void startService() {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // only for gingerbread and newer versions
            SharedPreferences errors = mainContext.getSharedPreferences("Screenstate", mainContext.MODE_MULTI_PROCESS);
            SharedPreferences.Editor editore = errors.edit();
            editore.putBoolean("state", true);
            editore.commit();

            SharedPreferences pref = getApplicationContext().getSharedPreferences("settings", 0);
            if(pref.getInt("lckscr",0) <= 1){
                Intent i = new Intent(mainContext, BackgroundService.class);
                i.putExtra("screen_state", true);
                mainContext.startService(i);
            }

            Intent serviceIntent = new Intent(this, wakeword.class);
            ContextCompat.startForegroundService(this, serviceIntent);
        }
    }

    public void stopService() {
        Intent serviceIntent = new Intent(this, wakeword.class);
        stopService(serviceIntent);
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, @NonNull int[] grantResults) {
        Log.i("Tag", "Permission granted!");
        if (allPermissionsGranted()) {
            Toast.makeText(this,"All permission granted",Toast.LENGTH_SHORT).show();
        } else {
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onDestroy() {

        super.onDestroy();
    }

}
