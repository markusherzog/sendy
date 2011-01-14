package com.markusherzog.sendy;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;

public class About extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {   
    	super.onCreate(savedInstanceState);
        setContentView(R.layout.about);
        WebView wv = (WebView)findViewById(R.id.about);
        wv.loadData(getString(R.string.about), "text/html", "UTF-8");
    }

}
