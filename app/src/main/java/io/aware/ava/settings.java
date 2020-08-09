package io.aware.ava;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class settings extends AppCompatActivity {

    public EditText deepl;
    public EditText owm;
    public EditText wolframalpha;
    public EditText scaleserp;
    public EditText serverurl;
    public EditText caldav_server_url;
    public EditText caldav_name;
    public EditText caldav_pw;
    public EditText phonenumber;
    public EditText ibmasrurl;
    public EditText ibmasrkey;
    public EditText ibmttsurl;
    public EditText ibmttskey;
    public Button save;
    public Button defaultButton;
    public Button logs;
    public SharedPreferences pref;
    public Spinner spinwakeword;
    public Spinner webviewloading;
    public Spinner languageSpinner;
    public Spinner asr;
    public Spinner lcscr;
    public Spinner hlpfeature;

    public String wakeword;
    public String[] wakewords;
    public int webload = 0;
    public int asrID = 0;
    public int lcscrID = 0;
    public String language;
    public String[] languages;
    public int helpfeature;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);
        deepl = findViewById(R.id.deepl);
        owm = findViewById(R.id.owm);
        wolframalpha = findViewById(R.id.wolframalpha);
        scaleserp = findViewById(R.id.scaleserp);
        save = findViewById(R.id.save);
        serverurl = findViewById(R.id.server_url);
        phonenumber = findViewById(R.id.phonenumber);
        defaultButton = findViewById(R.id.setdefault);
        logs = findViewById(R.id.logs);
        caldav_server_url = findViewById(R.id.caldav_server_url);
        caldav_name = findViewById(R.id.caldavname);
        caldav_pw = findViewById(R.id.caldavpw);
        spinwakeword= (Spinner) findViewById(R.id.wakewordspin);//fetch the spinner from layout file
        webviewloading = (Spinner) findViewById(R.id.webviewload);
        languageSpinner = (Spinner) findViewById(R.id.language);
        asr = (Spinner) findViewById(R.id.asr);
        lcscr = (Spinner) findViewById(R.id.lckdscreen);
        hlpfeature = (Spinner) findViewById(R.id.helpfeature);
        ibmasrurl = findViewById(R.id.ibmasrurl);
        ibmasrkey = findViewById(R.id.ibmasrkey);
        ibmttsurl = findViewById(R.id.ibmttsurl);
        ibmttskey = findViewById(R.id.ibmttskey);

        wakewords = getResources().getStringArray(R.array.wakewordlist);
        languages = getResources().getStringArray(R.array.languages);


        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, getResources()
                .getStringArray(R.array.wakewordlist));//setting the country_array to spinner
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinwakeword.setAdapter(adapter);
//if you want to set any action you can do in this listener
        spinwakeword.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long id) {
                wakeword = wakewords[position];
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });

        ArrayAdapter<String> adapter2 = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, getResources()
                .getStringArray(R.array.webviewloading));//setting the country_array to spinner
        adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        webviewloading.setAdapter(adapter2);
//if you want to set any action you can do in this listener
        webviewloading.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long id) {
                webload = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });

        ArrayAdapter<String> adapter3 = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, getResources()
                .getStringArray(R.array.languages));//setting the country_array to spinner
        adapter3.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        languageSpinner.setAdapter(adapter3);
//if you want to set any action you can do in this listener
        languageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long id) {
                language = languages[position];
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });


        ArrayAdapter<String> adapter4 = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, getResources()
                .getStringArray(R.array.ASR));//setting the country_array to spinner
        adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        asr.setAdapter(adapter4);
//if you want to set any action you can do in this listener
        asr.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long id) {
                asrID = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });


        ArrayAdapter<String> adapter5 = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, getResources()
                .getStringArray(R.array.lckscr));//setting the country_array to spinner
        adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        lcscr.setAdapter(adapter5);
//if you want to set any action you can do in this listener
        lcscr.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long id) {
                lcscrID = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });



        ArrayAdapter<String> adapter6 = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, getResources()
                .getStringArray(R.array.hlpf));//setting the country_array to spinner
        adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        hlpfeature.setAdapter(adapter6);
//if you want to set any action you can do in this listener
        hlpfeature.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long id) {
                helpfeature = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });



        pref = getApplicationContext().getSharedPreferences("settings", 0); // 0 - for private mode
        deepl.setText(pref.getString("deepl","Key"));
        owm.setText(pref.getString("owm","Key"));
        wolframalpha.setText(pref.getString("wolframalpha","Key"));
        scaleserp.setText(pref.getString("scaleserp","Key"));
        serverurl.setText(pref.getString("serverurl","https://dashboard.skillserver.de/api/public/skills"));
        caldav_server_url.setText(pref.getString("caldav_serverurl","https://caldav.url"));
        caldav_name.setText(pref.getString("caldav_name","Name"));
        caldav_pw.setText(pref.getString("caldav_pw","Password"));
        phonenumber.setText(pref.getString("phonenumber",""));
        ibmasrurl.setText(pref.getString("ibmasrurl",""));
        ibmasrkey.setText(pref.getString("ibmasrkey",""));
        ibmttsurl.setText(pref.getString("ibmttsurl",""));
        ibmttskey.setText(pref.getString("ibmttskey",""));


        for(int i=0;i<wakewords.length; i++){
            if(wakewords[i].equals(pref.getString("wakeword","heyava"))){
                spinwakeword.setSelection(i);
            }
        }

        for(int i=0;i<languages.length; i++){
            if(languages[i].equals(pref.getString("language","auto"))){
                languageSpinner.setSelection(i);
            }
        }

        webviewloading.setSelection(pref.getInt("webviewload",0));
        asr.setSelection(pref.getInt("ASR",1));
        lcscr.setSelection(pref.getInt("lckscr",0));
        hlpfeature.setSelection(pref.getInt("hlp", 0));


        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pref = getApplicationContext().getSharedPreferences("settings", 0); // 0 - for private mode
                SharedPreferences.Editor editor = pref.edit();
                editor.putString("deepl",deepl.getText().toString());
                editor.putString("owm",owm.getText().toString());
                editor.putString("wolframalpha",wolframalpha.getText().toString());
                editor.putString("scaleserp",scaleserp.getText().toString());
                editor.putString("serverurl",serverurl.getText().toString());
                editor.putString("caldav_serverurl",caldav_server_url.getText().toString());
                editor.putString("caldav_name",caldav_name.getText().toString());
                editor.putString("caldav_pw",caldav_pw.getText().toString());
                editor.putString("ibmasrurl",ibmasrurl.getText().toString());
                editor.putString("ibmasrkey",ibmasrkey.getText().toString());
                editor.putString("ibmttsurl",ibmttsurl.getText().toString());
                editor.putString("ibmttskey",ibmttskey.getText().toString());
                editor.putString("wakeword",wakeword);
                editor.putString("language",language);
                editor.putString("phonenumber",phonenumber.getText().toString());
                editor.putInt("webviewload",webload);
                editor.putInt("ASR",asrID);
                editor.putInt("lckscr",lcscrID);
                editor.putInt("hlp",helpfeature);
                editor.commit();

                finish();
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent);
            }
        });

        defaultButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deepl.setText("Key");
                owm.setText("Key");
                wolframalpha.setText("Key");
                scaleserp.setText("Key");
                serverurl.setText("https://dashboard.skillserver.de/api/public/skills");
            }
        });

        logs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPreferences errors = getApplicationContext().getSharedPreferences("ErrorLog", Context.MODE_MULTI_PROCESS);
                String error = errors.getString("ErrorLogs","Log:\n");
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, error);
                sendIntent.setType("text/plain");

                Intent shareIntent = Intent.createChooser(sendIntent, null);
                startActivity(shareIntent);
            }
        });

    }

    public void stopService() {
        Intent serviceIntent = new Intent(this, wakeword.class);
        stopService(serviceIntent);
    }


    @Override
    public void onBackPressed() {
        save.performClick();
    }

}
