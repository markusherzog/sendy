package com.markusherzog.sendy;

import java.io.IOException;
import java.util.Map;

import com.markusherzog.utils.HttpClient;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

public class Settings extends Activity {
	private static final String TAG = "sendy";
	private EditText mUsername;
    private EditText mPassword;
    private CheckBox mUseHttps;
    
    private SettingsDbAdapter mDbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mDbHelper = new SettingsDbAdapter(this);
        mDbHelper.open();
        
        setContentView(R.layout.account_edit);
       
        mUsername = (EditText) findViewById(R.id.username);
        mPassword = (EditText) findViewById(R.id.password);
        mUseHttps = (CheckBox) findViewById(R.id.cb_usehttps);
		
		populateFields();
		
		final Button confirmButton = (Button) findViewById(R.id.btn_confirm);
        confirmButton.setOnClickListener(new View.OnClickListener() {

        	public void onClick(View view) {
        		saveState();
        	    setResult(RESULT_OK);
        	    finish();
        	}
          
        });
        
        final Button testButton = (Button) findViewById(R.id.btn_test_login);
        testButton.setOnClickListener(new View.OnClickListener() {

        	public void onClick(View view) {
        		
        		String username = mUsername.getText().toString();
                String password = mPassword.getText().toString();
                boolean usehttps = mUseHttps.isChecked();
        		String url = (usehttps?"https":"http") + "://posterous.com/api/getsites";

                HttpClient httpClient = new HttpClient(url, HttpClient.GET, username, password);
                 
                String response = httpClient.get();
                int statusCode = httpClient.getResponseCode();
                
				Log.d(TAG, "statusCode: " + statusCode);
				switch(statusCode) {
					case(200):
						Toast.makeText(getBaseContext(), "Successfully logged in. Login correct.", Toast.LENGTH_LONG).show();
						break;
					case(401):
						Toast.makeText(getBaseContext(), "Access denied. Login incorrect.", Toast.LENGTH_LONG).show();
						break;
					default:
						Toast.makeText(getBaseContext(), statusCode + (response.length()==0?"":" - " + response), Toast.LENGTH_LONG).show();
				}
        	}
          
        });
    }
    
    private void populateFields() {
        Cursor account = mDbHelper.fetchAccount(1);
        startManagingCursor(account);
        mUsername.setText(account.getString(
        		account.getColumnIndexOrThrow(SettingsDbAdapter.KEY_USERNAME)));
        mPassword.setText(account.getString(
        		account.getColumnIndexOrThrow(SettingsDbAdapter.KEY_PASSWORD)));
        if(account.getInt(
        		account.getColumnIndexOrThrow(SettingsDbAdapter.KEY_USEHTTPS)) == 1) {
        	mUseHttps.setChecked(true);
        } else {
        	mUseHttps.setChecked(false);
        }
    }
    
    private void saveState() {
        String username = mUsername.getText().toString();
        String password = mPassword.getText().toString();
        boolean usehttps = mUseHttps.isChecked();
        mDbHelper.updateAccount(1, username, password, usehttps);
    }
    
}
