package io.aware.ava;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.webkit.WebView;

public class skillslist extends AppCompatActivity {

    public WebView browser;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_skillslist);
        browser = (WebView) findViewById(R.id.browser);
        browser.loadUrl("https://github.com/flozi00/AWare-Skill-Server/blob/master/skills.csv");
    }
}
