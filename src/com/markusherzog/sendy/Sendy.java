package com.markusherzog.sendy;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.xml.sax.SAXException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.sax.Element;
import android.sax.EndElementListener;
import android.sax.EndTextElementListener;
import android.sax.RootElement;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Xml;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.markusherzog.sendy.api.Site;
import com.markusherzog.utils.HttpClient;

/**
 * TODO
 * - remove media from post
 * - encrypt password
 * - facebook connect
 * - lite/pro
 * - i18n
 * - widget
 * - voice recognition
 * 1) Settings back up to Data Backup
 *    - http://developer.android.com/guide/topics/data/backup.html
 *    - Adding this feature would allow a users settings to be backed up to the Google cloud - Meaning that if a user moved between phones or replaced their phone with a new model / formatted / rebuilt their phone that the settings would come back
 *	  => i don't want to store username/password in the cloud
 * 2) Local Drafts
 *    - The ability to have a drafts option would be greatly appreciated
 *    - I often write posterous posts in a number of separate stints with your client, its only possible to post there and then.
 *    - I would envisage saving attachments and  text to the SD Card
 * 3) Ability to time delay posts
 *    - I might want to write a post at midnight but not have posterous post it until the middle of the next day
 *    - The WebUI of posterous has this ability write and post which will not be made public until a certain date
 *    => not supported by API
 * 
 * @author markus
 *
 */
public class Sendy extends Activity {
	
	private static final String TAG = "sendy";
	private static final String DEFAULT_ACCOUNT = "default";
	private static final String APP_FILES_FOLDERNAME = Environment.getExternalStorageDirectory() + "/data/com.markusherzog.sendy/files";
	private static final String TMP_FILENAME = APP_FILES_FOLDERNAME + "/tmp.jpg";
	// private static final String DEBUG_FILENAME = APP_FILES_FOLDERNAME + "/debug.txt";
	
	private static final int ACTIVITY_CREATE_ACCOUNT = 0;
	private static final int ACTIVITY_EDIT_ACCOUNT = 1;
	private static final int ACTIVITY_ADD_PICTURE_FROM_CAMERA = 2;
	private static final int ACTIVITY_ADD_PICTURE_FROM_CARD = 3;
	private static final int MENU_SETTINGS = Menu.FIRST;
	private static final int MENU_ABOUT = Menu.FIRST + 1;
	
	private static final int COMMAND_UPDATE_SITE = 1;
	private static final int COMMAND_CLEAR_POST = 2;
	private static final int COMMAND_SHOW_TOAST = 3;
	private final DateFormat df = new SimpleDateFormat("yyyyMMddHHmmssSSS");
    private SharedPreferences mPrefs;
	private SettingsDbAdapter mDbHelper;
	private String mUsername;
	private String mPassword;
	private String mCurrentSiteId;
	private boolean mUseHttps;
	private PostData mPostData;
	private Site[] mSites;
	//private boolean mDebug=false;
	// PrintWriter mDebugPw;
	
	boolean mExternalStorageAvailable = false;
	boolean mExternalStorageWriteable = false;
	
	/**
	 * Handles commands sent from threads back to main app.
	 * receives a Message, containing a "command" (int), and optional "text" (String) and "time" (int) for Showing up in a Toast.
	 */
	final private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message m) {
			if(m.getData().getString("text") != null &&  m.getData().getInt("time") > 0) {
				Log.d(TAG, "text: " + m.getData().getString("text") + ", time: " + m.getData().getInt("time"));
				Toast.makeText(getBaseContext(), m.getData().getString("text"), m.getData().getInt("time")).show();
			}
			switch(m.getData().getInt("command")) {
				case COMMAND_SHOW_TOAST:
					break;
				case COMMAND_CLEAR_POST:
					// gets called after a successful post from post-thread
					discard();
					break;
				case COMMAND_UPDATE_SITE:
					// this gets called from fetchAndUpdateSites-thread
					Log.d(TAG, "update site");
					if(mCurrentSiteId == null || mCurrentSiteId.length() == 0) {
						useDefaultSite(); // sets primary site (default) as the site that's used
					}
					break;
				default:
			}	
		}
	};
	
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	Log.i(TAG, "onCreate"); 
        super.onCreate(savedInstanceState);
        
        this.checkSdCard();
        
        mPrefs = getSharedPreferences("sendy.prefs", Context.MODE_PRIVATE);
//        Log.d(TAG, "mDebug: " + mDebug + ", mExternalStorageWriteable: " + mExternalStorageWriteable);
//        if(mDebug && mExternalStorageWriteable) {
//        	try {
//        		mDebugPw = new PrintWriter( new FileWriter(DEBUG_FILENAME, true));
//        	} catch(IOException e) {
//        		mDebug = false;
//        		Log.d(TAG, e.getMessage());
//        		e.printStackTrace();
//        	}
//        } else {
//        	mDebug = false;
//        }

//        log("start debugging");
//        log("mExternalStorageAvailable: " + mExternalStorageAvailable);
//        log("mExternalStorageWriteable: " + mExternalStorageWriteable);
        
        
        if(mExternalStorageWriteable) {
        	File dir = new File(APP_FILES_FOLDERNAME);
	        if(!dir.exists()) {
	        	dir.mkdirs();
	        }
        }
        
        mPostData = new PostData();
        
        setContentView(R.layout.main);
        
        final EditText title = (EditText) findViewById(R.id.title);
        title.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}
			@Override
			public void afterTextChanged(Editable s) {
				mPostData.setTitle(s.toString());
			}
		});
        
        final EditText body = (EditText) findViewById(R.id.body);
        body.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}
			@Override
			public void afterTextChanged(Editable s) {
				mPostData.setBody(s.toString());
			}
		});
        
        final SharedPreferences.Editor editor = mPrefs.edit();
        final CheckBox cbPrivate = (CheckBox)findViewById(R.id.cb_private);
        cbPrivate.setChecked(mPrefs.getBoolean("private", false));
        final CheckBox cbAutopost = (CheckBox)findViewById(R.id.cb_autopost);
        cbAutopost.setChecked(mPrefs.getBoolean("autopost", true));
        cbPrivate.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isPrivateChecked) {
				cbAutopost.setEnabled(!isPrivateChecked); // if private == true, then autopost == false
				cbAutopost.setChecked(false);
				editor.putBoolean("private", isPrivateChecked);
				editor.putBoolean("autopost", false);
				editor.commit();
				
			}
		});
        cbAutopost.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isAutopostChecked) {
				editor.putBoolean("autopost", isAutopostChecked);
				editor.commit();
				
			}
		});     
        
        final Button btnAddPictureFromCamera = (Button)findViewById(R.id.btn_add_picture_from_camera);
        if(mExternalStorageWriteable) {
        	btnAddPictureFromCamera.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					Intent imageCaptureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
					imageCaptureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(TMP_FILENAME)));
					startActivityForResult(imageCaptureIntent, ACTIVITY_ADD_PICTURE_FROM_CAMERA);
				}
	        });
        } else {
        	btnAddPictureFromCamera.setEnabled(false);
        }
        
        final Button btnAddPictureFromCard = (Button)findViewById(R.id.btn_add_picture_from_card);
        if(mExternalStorageAvailable) {
        	btnAddPictureFromCard.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					Intent getImageFromCardIntent = new Intent(Intent.ACTION_GET_CONTENT);
					getImageFromCardIntent.setType("image/*");
					startActivityForResult(getImageFromCardIntent, ACTIVITY_ADD_PICTURE_FROM_CARD);
					
				}
	        });
        } else {
        	btnAddPictureFromCard.setEnabled(false);
        }
        
        final Button btnDiscard = (Button)findViewById(R.id.btn_discard);
        btnDiscard.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				discard();
			}
		});
        
        final Button btnPost = (Button)findViewById(R.id.btn_post);
        btnPost.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if(!mPostData.hasData()) {
					Toast.makeText(Sendy.this, "Nothing to post :-(", Toast.LENGTH_SHORT).show();
					return;
				}

				// need to disable display rotation, otherwise app crashes on closing waitingDialog
				setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
				final ProgressDialog waitingDialog = ProgressDialog.show(Sendy.this, "", "Posting ...", true);
				new Thread() {
                    public void run() {
                    	submit();
                    	// Dismiss the Dialog
                    	waitingDialog.dismiss();
                    	setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
                    }
				}.start();
			}
		});
        
        final View site = findViewById(R.id.site);
        site.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if((mSites == null || mSites.length == 0) && isOnline()) {
					try {
						mSites = fetchSites(mUseHttps, mUsername, mPassword);
					} catch(Exception e) {
						Toast.makeText(getBaseContext(), "Could not get your Sites.", Toast.LENGTH_LONG).show();
					}
				}
				if(mSites != null && mSites.length > 0) {
					AlertDialog.Builder ad = new AlertDialog.Builder(Sendy.this);
					ad.setTitle(getText(R.string.text_choose_site));
		        	ListAdapter adapter = new SitesAdapter(Sendy.this, mSites);
		        	ad.setSingleChoiceItems( adapter, -1, new DialogInterface.OnClickListener() {
		    			@Override
		    			public void onClick(DialogInterface dialog, int which) {
		    				// a choice has been made!
		    				setSite(mSites[which]);
		    				dialog.dismiss();
		    				saveCurrentSite(mSites[which]);
		    			}
		    		});
	        	ad.show();
				}
			}
        });

        mDbHelper = new SettingsDbAdapter(this);
        mDbHelper.open();
        
        fetchAccount(); // from DB
        useLastSite(); //
        if(isOnline()) {
        	fetchAndUpdateSites(); // from Web
        }
    }
    
    @Override
    protected void onResume() {
    	super.onResume();
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(0, MENU_SETTINGS, 0, R.string.menu_settings);
		menu.add(0, MENU_ABOUT, 0, R.string.menu_about);
		return true;
	}
	
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
		case MENU_SETTINGS:
			startActivityForResult(new Intent(this, Settings.class), ACTIVITY_EDIT_ACCOUNT);
			return true;
		case MENU_ABOUT:
			startActivity(new Intent(this, About.class));
			return true;
		}

		return super.onMenuItemSelected(featureId, item);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);
		switch(requestCode) {
		case ACTIVITY_EDIT_ACCOUNT:
			fetchAccount();
			break;
		case ACTIVITY_ADD_PICTURE_FROM_CAMERA:
			if (resultCode == RESULT_OK) {
				File f = new File(TMP_FILENAME);
				try { 
					FileInputStream fileInputStream = new FileInputStream(f);
			        byte[] data = new byte[(int) f.length()];
			        fileInputStream.read(data);
			        fileInputStream.close();
					f.delete();
					String filename = "sendy_" + df.format(new Date()) + ".jpg";
					this.addBinaryData(filename, data);
				} catch (FileNotFoundException e) {
				    e.printStackTrace();
				} catch(IOException e) {
					e.printStackTrace();
				}
			}
			break;
		case ACTIVITY_ADD_PICTURE_FROM_CARD:
			if (resultCode == RESULT_OK) {
				try {
					AssetFileDescriptor assetFileDescriptor = getContentResolver().openAssetFileDescriptor(intent.getData(), "r");
					InputStream inputStream = assetFileDescriptor.createInputStream();
			        byte[] data = new byte[(int) assetFileDescriptor.getLength()];
			        inputStream.read(data);
			        inputStream.close();
			        this.addBinaryData(assetFileDescriptor.toString(), data);
				} catch (FileNotFoundException e) {
				    e.printStackTrace();
				} catch(IOException e) {
					e.printStackTrace();
				}
			}
			break;
		}
	}
	
	private void checkSdCard() {
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state)) {
		    // We can read and write the media
		    this.mExternalStorageAvailable = true;
		    this.mExternalStorageWriteable = true;
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
		    // We can only read the media
			this.mExternalStorageAvailable = true;
			this.mExternalStorageWriteable = false;
		} else {
		    // "else" is wrong. It may be one of many other states, but all we need to know is we can neither read nor write
			this.mExternalStorageAvailable = false;
			this.mExternalStorageWriteable = false;
		}
	}
	
	/**
	 * Reads Username, Password from database and stores them in variables.
	 * If no account exists, show dialog for entering account data.
	 */
	private void fetchAccount() {
		// Get all of the rows from the database and create the item list
		Cursor accountsCursor = mDbHelper.fetchAccount(1);
        if(accountsCursor.getCount() == 0) {
//        	log("create account entry in database");
        	mDbHelper.createAccount(DEFAULT_ACCOUNT, "", "", true);
//        	log("opening setting dialog");
        	startActivityForResult(new Intent(this, Settings.class), ACTIVITY_CREATE_ACCOUNT);
        } else {
//        	log("found account entry in database");
            mUsername = accountsCursor.getString(accountsCursor.getColumnIndexOrThrow(SettingsDbAdapter.KEY_USERNAME));
            mPassword = accountsCursor.getString(accountsCursor.getColumnIndexOrThrow(SettingsDbAdapter.KEY_PASSWORD));
            if(accountsCursor.getInt(accountsCursor.getColumnIndexOrThrow(SettingsDbAdapter.KEY_USEHTTPS)) == 1) {
            	mUseHttps = true;
            } else {
            	mUseHttps = false;
            }
//            if(mDebug) {
//	            log("account username: " + mUsername);
//	            String stars = "";
//	            for(int i = 0; i < mPassword.length(); i ++) {
//	            	stars = stars + "*";
//	            }
//	            log("account password: " + stars);
//	            log("using https: " + mUseHttps);
//            }
        }
        accountsCursor.close();
	}
	
	
	/**
	 * XXX
	 * Fetches ID and names of the users posterous-sites
	 */
	private void fetchAndUpdateSites() {
		new Thread() {
            public void run() {
                try{
                    Sendy.this.mSites = fetchSites(mUseHttps, mUsername, mPassword);
                    
                    Message msg = mHandler.obtainMessage();
                    Bundle b = new Bundle();
                    
                    b.putInt("command", COMMAND_UPDATE_SITE);
                    	
                    msg.setData(b);
                    mHandler.sendMessage(msg);
                    
                } catch (Exception e) {
                	e.printStackTrace();
                }
            }
		}.start();
	}
	
	private static Site[] fetchSites(boolean useHttps, String username, String password) throws Exception {
    	String url = (useHttps?"https":"http") + "://posterous.com/api/getsites";
        HttpClient httpClient = new HttpClient(url, HttpClient.GET, username, password);
	
        String sitesXml = httpClient.get();
        return parseSites(sitesXml);
	}
	
	
	private boolean isOnline() {
		 ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		 return cm.getActiveNetworkInfo().isConnectedOrConnecting();
	}

	private void useLastSite() {
		String siteId = mPrefs.getString("site_id", "");
		String siteName = mPrefs.getString("site_name", "");
		
		if(siteId != null && siteId.length() > 0 && siteName!= null && siteName.length() > 0) {
			Site site = new Site();
			site.setId(siteId);
			site.setName(siteName);
			setSite(site);
		}
	}

	private void useDefaultSite() {
		if(mSites != null && mSites.length > 0) {
			for (Site site : mSites) {
				if(site.isPrimary()) {
					setSite(site);
					break;
				}
			}
		} else {
			// assume default site. do we want to put "default" in the title bar?
			// 
		}
	}
	
	private void setSite(Site site) {
		mCurrentSiteId = Integer.toString(site.getId());
		TextView siteText = (TextView)findViewById(R.id.site);
		siteText.setText(site.getName());
	}
	
	private void saveCurrentSite(Site site) {
		final SharedPreferences.Editor editor = mPrefs.edit();
		editor.putString("site_id", String.valueOf(site.getId()));
		editor.putString("site_name", String.valueOf(site.getName()));
		editor.commit();		
	}
	
	private static Site[] parseSites(String xml) throws Exception {
		final Site currentSite = new Site();
        RootElement root = new RootElement("rsp");
        final List<Site> sites = new ArrayList<Site>();
        Element site = root.getChild("site");
        site.setEndElementListener(new EndElementListener(){
            public void end() {
            	sites.add(currentSite.copy());
            }
        });
        site.getChild("id").setEndTextElementListener(new EndTextElementListener(){
            public void end(String id) {
                currentSite.setId(id);
            }
        });
        site.getChild("name").setEndTextElementListener(new EndTextElementListener(){
            public void end(String name) {
                currentSite.setName(name);
            }
        });
        
        site.getChild("primary").setEndTextElementListener(new EndTextElementListener(){
            public void end(String primary) {
                currentSite.setPrimary(primary);
            }
        });
        
        // Throws SAXException
        try {
        	Xml.parse(xml, root.getContentHandler());
        } catch(SAXException e) {
        	Log.e(TAG, "Error parsing xml: " + e.getMessage());
        	Log.d(TAG, "xml: " + xml);
        	throw new Exception(e.getMessage(), e);
        }
        return sites.toArray(new Site[0]);
    }

	private void addBinaryData(String name, byte[] data) {
		mPostData.addBinaryData(name, data);
		final TextView attachmentCount = (TextView) findViewById(R.id.attachment_count);
		int attachmentsCountN = mPostData.getBinaryData().size();
		if(attachmentsCountN == 0) {
			attachmentCount.setText("Your post has no Attachments.");
		} else if(attachmentsCountN == 1) {
			attachmentCount.setText("Your post has 1 Attachment.");
		} else {
			attachmentCount.setText("Your post has " + attachmentsCountN + " Attachments.");
		}
	}
	
	private void discard() {
		((EditText)findViewById(R.id.title)).setText("");
		//((EditText)findViewById(R.id.title)).se
		((EditText)findViewById(R.id.body)).setText("");
		((TextView)findViewById(R.id.attachment_count)).setText(R.string.text_attachment_count);
		mPostData.reset();
	}


	static class SitesAdapter extends ArrayAdapter<Site> {
	
		private static final int RESOURCE = R.layout.site_row;
		private LayoutInflater inflater;
	
	    static class ViewHolder {
	        TextView nameTextView;
	    }
	    
		public SitesAdapter(Context context, Site[] sites) {
			super(context, RESOURCE, sites);
			inflater = LayoutInflater.from(context);
		}
	
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;
	
			if (convertView == null) {
				// inflate a new view and setup the view holder for future use
				convertView = inflater.inflate(RESOURCE, null);
	
				holder = new ViewHolder();
				holder.nameTextView = (TextView) convertView.findViewById(R.id.site_row);
				convertView.setTag(holder);
			}  else {
				// view already defined, retrieve view holder
				holder = (ViewHolder)convertView.getTag();
			}
	
			Site site = getItem(position);
			if (site == null) {
				Log.e(TAG, "Invalid site for position: " + position);
			}
			holder.nameTextView.setText(site.getName()); // + (site.isPrimary()?" (primary)":"")
			return convertView;
		}
	}
	
	private void submit() {
		submit(false);
	}
	
	private void submit(boolean isRetry) {
		try {
        	String url = (mUseHttps?"https":"http") + "://posterous.com/api/newpost";
//        	log("start post to url: " + url);
//        	log("username = " + mUsername);
            HttpClient httpClient = new HttpClient(url, HttpClient.POST, mUsername, mPassword);
            httpClient.addParameter("source", "sendy");
            httpClient.addParameter("sourceLink", "http://www.markusherzog.com/sendy");
		    
		    if(mPostData.getTitle() != null && mPostData.getTitle().trim().length() > 0) {
            	httpClient.addParameter("title", mPostData.getTitle());
		    }
		    if(mPostData.getBody() != null && mPostData.getBody().trim().length() > 0) {
            	httpClient.addParameter("body", mPostData.getBody());
		    }
            
            if(mPostData.getBinaryData() != null) {
            	for (Map.Entry<String, PostData.BinaryData> binaryData: mPostData.getBinaryData().entrySet()) {
            		Log.d(TAG, "adding " + binaryData.getValue().getBytes() + " bytes as " + binaryData.getKey() + " to httpclient.");
            		httpClient.addParameter("media[]", binaryData.getKey(), binaryData.getValue().getBytes());
		    }
					}
            
            if(mCurrentSiteId != null) {
            	httpClient.addParameter("site_id", mCurrentSiteId);
//            	log("site_id = " + mCurrentSiteId);
		    	}
            httpClient.addParameter("private", mPrefs.getBoolean("private", false)?"1":"0");
            httpClient.addParameter("autopost", mPrefs.getBoolean("autopost", true)?"1":"0");

//            log("all set up. now doing actual post");
            int statusCode = -1;
            try {
            	statusCode = httpClient.post();
//                if(mDebug) {
//                    log("posting done. response code: " + statusCode);
//                    log("posting done. response text: " + httpClient.getResponse());
//                }
            } catch(IOException e) {
            	// Broken pipe? Try again one more time.
            	if(!isRetry) {
            		submit(true);
            	}
//            	if(mDebug) {
//                    log("exception while posting: " + e.getMessage());
//                    e.printStackTrace(mDebugPw);
//            	}
            }
            
            
            Message msg = mHandler.obtainMessage();
            Bundle b = new Bundle();
            switch(statusCode) {
            case HttpURLConnection.HTTP_OK:
            	b.putString("text", "Great, you've posted it!");
            	b.putInt("command", COMMAND_CLEAR_POST);
            	break;
            case HttpURLConnection.HTTP_UNAUTHORIZED:
            	b.putString("text", "Incorrect username/password");
            	b.putInt("command", COMMAND_SHOW_TOAST);
            	break;
            default:
            	b.putString("text", "Something went wrong :-( Maybe try posting again?");
            	b.putInt("command", COMMAND_SHOW_TOAST);
            }
            b.putInt("time", Toast.LENGTH_LONG);
            msg.setData(b);
            mHandler.sendMessage(msg);
        } catch (Exception e) {e.printStackTrace();}

	}
	
//	public void log(String string) {
//    	Log.d(TAG, string);
//    	if(mDebug) {
//			mDebugPw.println(df.format(new Date()) + ": " + string);
//			mDebugPw.flush();
//    	}
//	}
}