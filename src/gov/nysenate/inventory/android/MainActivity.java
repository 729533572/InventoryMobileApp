package gov.nysenate.inventory.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.DialogInterface;
import android.content.IntentFilter;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.Html;
import android.text.method.KeyListener;
import android.util.Log;

import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import gov.nysenate.inventory.android.UpgradeActivity.MyWebReceiver;

import java.io.IOException;
import java.io.InputStream;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;
//   WIFI Code Added Below
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.speech.RecognizerIntent;

public class MainActivity extends Activity {

	// WIFI Code Added Below
	WifiManager mainWifi;
	// WifiReceiver receiverWifi;
	List<ScanResult> wifiList;
	ScanResult currentWifiResult;
	int senateWifiFound = -1;
	int senateVisitorWifiFound = -1;
	int connectedTo = -1;
	String senateSSID = "";
	String senateSSIDpwd = "";
	String senateVisitorSSID = "";
	String senateVisitorSSIDpwd = "";
	String wifiMessage = "" /*"Horrible News!!! Currently no Wifi Networks found!!! You need a Wifi network (Preferrably a NY Senate one) in order to use this app."*/;
	String currentSSID = "";
    private ListView mList;
	public static String nauser = null;
    Resources resources = null;
	ClearableEditText user_name;
    ClearableEditText password;
	String URL = "";
	public static Properties properties; // Since we want to refer to this in other activities
    static AssetManager assetManager;
	Button buttonLogin;
	ProgressBar progressBarLogin;
	public final static String u_name_intent = "gov.nysenate.inventory.android.u_name";
	public final static String pwd_intent = "gov.nysenate.inventory.android.pwd";

	private static final String LOG_TAG = "AppUpgrade";
	private MyWebReceiver receiver;
	private int versionCode = 0;
	String appURI = "";
	static String latestVersionName;
	static int latestVersion;
	 
	private DownloadManager downloadManager;
	private long downloadReference;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		resources = this.getResources();
        user_name = (ClearableEditText) findViewById(R.id.user_name);
        password = (ClearableEditText) findViewById(R.id.password);

		// Read from the /assets directory for properties of the project
		// we can modify this file and the URL will be changed
		//Resources resources = this.getResources();
		progressBarLogin = (ProgressBar)findViewById(R.id.progressBarLogin);
		buttonLogin = (Button)findViewById(R.id.buttonLogin);

		AssetManager assetManager = resources.getAssets();
		try {
			InputStream inputStream = assetManager.open("invApp.properties");
			properties = new Properties();
			properties.load(inputStream); // we load the properties here and we
											// use same object elsewhere in
											// project
			URL = properties.get("WEBAPP_BASE_URL").toString();
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			// WIFI Code Added Below
			senateWifiFound = -1;
			senateVisitorWifiFound = -1;
			connectedTo = -1;
			boolean enablingWifi = false;
			long startTime = System.currentTimeMillis();
			mainWifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);

			while (!mainWifi.isWifiEnabled()
					|| System.currentTimeMillis() - startTime > 3000) {
				if (!enablingWifi) {
					mainWifi.setWifiEnabled(true);
					enablingWifi = true;
					Thread.sleep(1000);
				}
			}
			if (mainWifi.isWifiEnabled()) {
				Context context = getApplicationContext();
				int duration = Toast.LENGTH_SHORT;

				if (enablingWifi) {
					Toast toast = Toast.makeText(context,
							"Wifi has been enabled.", duration);
					toast.setGravity(Gravity.CENTER, 0, 0);
					toast.show();
					mainWifi = (WifiManager) getSystemService(Context.WIFI_SERVICE); // done
																						// Again
																						// BH
				}
			} else {
				Context context = getApplicationContext();
				int duration = Toast.LENGTH_SHORT;

				Toast toast = Toast
						.makeText(
								context,
								"Unable to Enable Wifi Connection necessary to login to this app. Please fix before continuing or contact STS/BAC.",
								3000);
				toast.setGravity(Gravity.CENTER, 0, 0);
				toast.show();
			}

			mainWifi.startScan();
			// get list of the results in object format ( like an array )
			wifiList = mainWifi.getScanResults();
			WifiInfo connectionInfo = mainWifi.getConnectionInfo();

			if (connectionInfo != null) {
				currentSSID = connectionInfo.getSSID().trim()
						.replaceAll("\"", "");
			} else {
				currentSSID = "";
			}
			for (int x = 0; x < wifiList.size(); x++) {
				currentWifiResult = wifiList.get(x);
				if (currentSSID.length() > 0) {
					if (currentSSID.equals(currentWifiResult.SSID)) {
						connectedTo = x;
					}
				}

				if (currentWifiResult.SSID.equalsIgnoreCase(senateSSID)) {
					senateWifiFound = x;
				}
				if (currentWifiResult.SSID.equalsIgnoreCase(senateVisitorSSID)) {
					senateVisitorWifiFound = x;
				}
			}


			if (connectedTo == -1) {
				// wifiMessage = wifiMessage +
				// " <font color='RED'>AAAA Currently not connected to a network ("+senateWifiFound+")</font>";
				Context context = getApplicationContext();
				int duration = Toast.LENGTH_SHORT;

				Toast toast = Toast.makeText(context,
						"NO WIFI NETWORK CONNECTION!!!", duration);
				toast.setGravity(Gravity.CENTER, 0, 0);
				toast.show();
				if (senateWifiFound > -1) { // A MUCH better option would be to
											// connect to Senate Net Instead
					// 1. Instantiate an AlertDialog.Builder with its
					// constructor
					AlertDialog.Builder builder = new AlertDialog.Builder(this);

					// 2. Chain together various setter methods to set the
					// dialog characteristics
					builder.setMessage(
							"Would you like to connect NY Senate Network instead? (You are connected to NY Senate Visitor Network)")
							.setTitle("Connect to NY Senate Network");
					// Add the buttons
					builder.setPositiveButton("Yes",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									WifiConfiguration conf = new WifiConfiguration();
									conf.SSID = "\"" + senateSSID + "\""; // Please
																			// note
																			// the
																			// quotes.
																			// String
																			// should
																			// contain
																			// ssid
																			// in
																			// quotes
									conf.preSharedKey = "\"" + senateSSIDpwd
											+ "\"";
									mainWifi.addNetwork(conf);
									List<WifiConfiguration> list = mainWifi
											.getConfiguredNetworks();
									for (WifiConfiguration i : list) {
										if (i.SSID != null
												&& i.SSID.equals("\""
														+ senateSSID + "\"")) {
											mainWifi.disconnect();
											mainWifi.enableNetwork(i.networkId,
													true);
											mainWifi.reconnect();

											break;
										}
									}

								}
							});
					builder.setNegativeButton("No",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									// User cancelled the dialog
								}
							});

					// 3. Get the AlertDialog from create()
					AlertDialog dialog = builder.create();
					dialog.show();
				}

			} else {
				if (connectedTo == senateWifiFound) { // GREAT!!! We are
														// connected to Senate
														// Net
					Context context = getApplicationContext();
					int duration = Toast.LENGTH_SHORT;

					Toast toast = Toast.makeText(context,
							"Connected to Senate Network.", duration);
					toast.setGravity(Gravity.CENTER, 0, 0);
					toast.show();
				} else if (connectedTo == senateVisitorWifiFound) { // NOT BAD,
																	// We are
																	// connected
																	// to Senate
																	// Visitor
																	// Net
					Context context = getApplicationContext();
					int duration = Toast.LENGTH_SHORT;

					Toast toast = Toast.makeText(context,
							"Connected to Senate Visitor Network.", duration);
					toast.setGravity(Gravity.CENTER, 0, 0);
					toast.show();
					if (senateWifiFound > -1) { // A MUCH better option would be
												// to connect to Senate Net
												// Instead
						// 1. Instantiate an AlertDialog.Builder with its
						// constructor
						AlertDialog.Builder builder = new AlertDialog.Builder(
								this);

						// 2. Chain together various setter methods to set the
						// dialog characteristics
						builder.setMessage(
								"Would you like to connect NY Senate Network instead? (You are connected to NY Senate Visitor Network)")
								.setTitle("Connect to NY Senate Network");
						// Add the buttons
						builder.setPositiveButton("Yes",
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int id) {
										WifiConfiguration conf = new WifiConfiguration();
										conf.SSID = "\"" + senateSSID + "\""; // Please
																				// note
																				// the
																				// quotes.
																				// String
																				// should
																				// contain
																				// ssid
																				// in
																				// quotes
										conf.preSharedKey = "\""
												+ senateSSIDpwd + "\"";
										mainWifi.addNetwork(conf);
										List<WifiConfiguration> list = mainWifi
												.getConfiguredNetworks();
										for (WifiConfiguration i : list) {
											if (i.SSID != null
													&& i.SSID
															.equals("\""
																	+ senateSSID
																	+ "\"")) {
												mainWifi.disconnect();
												mainWifi.enableNetwork(
														i.networkId, true);
												mainWifi.reconnect();

												break;
											}
										}

									}
								});
						builder.setNegativeButton("No",
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int id) {
										// User cancelled the dialog
									}
								});

						// 3. Get the AlertDialog from create()
						AlertDialog dialog = builder.create();
						dialog.show();
					}
				} else {
					Context context = getApplicationContext();
					int duration = Toast.LENGTH_SHORT;

					Toast toast = Toast.makeText(context, "Connected to "
							+ currentSSID + ".", duration);
					toast.setGravity(Gravity.CENTER, 0, 0);
					toast.show();
				}
			}

			// EditText text = (EditText) findViewById(R.id.wifiMessage);
			TextView t = (TextView) findViewById(R.id.textView1);
			t.setTextColor(Color.BLACK);
			if (connectionInfo != null
					&& connectionInfo.getSSID().trim().length() > 0) {
				t.setText(Html.fromHtml("<h1>" + wifiMessage + "</h1>"));

			} else {
				t.setText(Html.fromHtml("<h1> (No Current Connection Info) "
						+ wifiMessage + "</h1>"));
			}

		} catch (Exception e) {
			TextView t = (TextView) findViewById(R.id.textView1);
			t.setText(e.getMessage());
			StackTraceElement[] trace = e.getStackTrace();
			for (int x = 0; x < trace.length; x++) {
				t.setText(t.getText() + trace[x].toString());
			}
			t.setText(Html.fromHtml("<h1>" + t.getText() + "</h1>"));
			t.setTextColor(Color.RED);

		}
		 //Overall information about the contents of a package 
		  //This corresponds to all of the information collected from AndroidManifest.xml.
		  PackageInfo pInfo = null;
		  try {
		   pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
		  } 
		  catch (NameNotFoundException e) {
		   e.printStackTrace();
		  }
		  //get the app version Name for display
		  String version = pInfo.versionName;
		  //get the app version Code for checking
		  versionCode = pInfo.versionCode;
		  Log.i("onCreate VERSION CODE", "versionCode:"+versionCode);
		  //display the current version in a TextView
		 
		  //Broadcast receiver for our Web Request 
		  IntentFilter filter = new IntentFilter(MyWebReceiver.PROCESS_RESPONSE);
		  filter.addCategory(Intent.CATEGORY_DEFAULT);
		  receiver = new MyWebReceiver();
		  registerReceiver(receiver, filter);
		 
		  //Broadcast receiver for the download manager
		  filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
		  registerReceiver(downloadReceiver, filter);
		 
		  //check of internet is available before making a web service request
		  if(isNetworkAvailable(this)){
		   Intent msgIntent = new Intent(this, InvWebService.class);
			String URL = MainActivity.properties.get("WEBAPP_BASE_URL").toString();

		   msgIntent.putExtra(InvWebService.REQUEST_STRING, URL+"/CheckAppVersion?appName=InventoryMobileApp.apk");
		   startService(msgIntent);
		  }
		
		

	}

	 @Override
	 public void onDestroy() {
	  //unregister your receivers
	  this.unregisterReceiver(receiver);
	  this.unregisterReceiver(downloadReceiver);
	  super.onDestroy();
	 }
	 	
	 
	 //check for internet connection
	 private boolean isNetworkAvailable(Context context) {
	  ConnectivityManager connectivity = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
	  if (connectivity != null) {
	   NetworkInfo[] info = connectivity.getAllNetworkInfo();
	   if (info != null) {
	    for (int i = 0; i < info.length; i++) {
	     Log.v(LOG_TAG,String.valueOf(i));
	     if (info[i].getState() == NetworkInfo.State.CONNECTED) {
	      Log.v(LOG_TAG, "connected!");
	      return true;
	     }
	    }
	   }
	  }
	  return false;
	 }
	
	public Properties getProperties() {
	    if (properties==null) {
            InputStream inputStream = null;
            try {
                inputStream = assetManager.open("invApp.properties");
            } catch (IOException e1) {
                // TODO Auto-generated catch block
                MsgAlert msgAlert = new MsgAlert(getApplicationContext(), "Could not Open Properties File", "!!ERROR: The Inventory Mobile App could not open the Properties File. Please contact STS/BAC.");
                
                e1.printStackTrace();
            }
            properties = new Properties();
            try {
                properties.load(inputStream);
            } catch (IOException e) {
                MsgAlert msgAlert = new MsgAlert(getApplicationContext(), "Could not Load Properties File", "!!ERROR: The Inventory Mobile App could not open the Properties File. Please contact STS/BAC.");
                
            } // we load the properties here and we
                                            // use same object elsewhere in
                                            // project
	        
	    }
        return properties; 
	    
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	// our code begins

	public void validate(View view) {
		if (view.getId()==R.id.buttonLogin) {
			if (buttonLogin.getText().toString().equalsIgnoreCase("Close")) {
				finish();
			}
			else {
				buttonLogin.getBackground().setAlpha(70);
				progressBarLogin.setVisibility(ProgressBar.VISIBLE);
				Intent intent = new Intent(this, DisplayMessageActivity.class);
				// Intent intent = new Intent(this, MenuActivity.class);
				String u_name = user_name.getText().toString();
				String pwd = password.getText().toString();
				intent.putExtra(u_name_intent, u_name);
				intent.putExtra(pwd_intent, pwd);
				startActivity(intent);
				overridePendingTransition(R.anim.in_right, R.anim.out_left);
			}
		}
	}
	
	public void startUpdate(View View) {
		Intent intent = new Intent(this, UpgradeActivity.class);
		startActivity(intent);
		overridePendingTransition(R.anim.in_down, R.anim.out_down);
		
	}
	

	 //broadcast receiver to get notification when the web request finishes
	 public class MyWebReceiver extends BroadcastReceiver{
	 
	  public static final String PROCESS_RESPONSE = "gov.nysenate.inventory.android.intent.action.PROCESS_RESPONSE";
	 
	  @Override
	  public void onReceive(Context context, Intent intent) {
	 
	   String reponseMessage = intent.getStringExtra(InvWebService.RESPONSE_MESSAGE);
	   Log.v(LOG_TAG, reponseMessage);
	 
	   //parse the JSON response
	   JSONObject responseObj;
	   try {
	    responseObj = new JSONObject(reponseMessage);
	    boolean success = responseObj.getBoolean("success");
	    //if the reponse was successful check further
	    if(success){
	     //get the latest version from the JSON string
	     latestVersion = responseObj.getInt("latestVersion");
	     //get the lastest application URI from the JSON string
	     appURI = responseObj.getString("appURI");
	     latestVersionName = responseObj.getString("latestVersionName");
	     Log.i(LOG_TAG, "latestVersion:"+latestVersion+" > versionCode:"+versionCode);
	     //check if we need to upgrade?
	     if(latestVersion > versionCode){
	      user_name.setVisibility(View.INVISIBLE);
	      password.setVisibility(View.INVISIBLE);
	      buttonLogin.setText("Close");
	      progressBarLogin.setVisibility(ProgressBar.VISIBLE);
	      
	      //oh yeah we do need an upgrade, let the user know send an alert message
	      AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
	      builder.setMessage("There is newer version ("+latestVersionName+":"+latestVersion+") of this application available. In order to use this app, you MUST upgrade. Click OK to upgrade now?")
	      .setPositiveButton("OK", new DialogInterface.OnClickListener() {
	       //if the user agrees to upgrade
	       public void onClick(DialogInterface dialog, int id) {
	        //start downloading the file using the download manager
	        downloadManager = (DownloadManager)getSystemService(DOWNLOAD_SERVICE);
	        Uri Download_Uri = Uri.parse(appURI);
	        DownloadManager.Request request = new DownloadManager.Request(Download_Uri);
	        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI);
	        request.setAllowedOverRoaming(false);
	        request.setTitle("My Andorid App Download");
	        request.setDestinationInExternalFilesDir(MainActivity.this,Environment.DIRECTORY_DOWNLOADS,"InventoryMobileApp.apk");
	        downloadReference = downloadManager.enqueue(request);
	       }
	      })
	      .setNegativeButton("Close App", new DialogInterface.OnClickListener() {
	       public void onClick(DialogInterface dialog, int id) {
	        // User cancelled the dialog
	    	   //finish();
	       }
	      });
	      //show the alert message
	      builder.create().show();
	     }
	 
	    }
	   } catch (JSONException e) {
	    e.printStackTrace();
	   }
	 
	  }
	 
	 }
	 
	 //broadcast receiver to get notification about ongoing downloads
	 private BroadcastReceiver downloadReceiver = new BroadcastReceiver() {
	 
	  @Override
	  public void onReceive(Context context, Intent intent) {
	 
	   //check if the broadcast message is for our Enqueued download
	   long referenceId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
	   if(downloadReference == referenceId){
	 
	    Log.v(LOG_TAG, "Downloading of the new app version complete");
	    //start the installation of the latest version
	    Intent installIntent = new Intent(Intent.ACTION_VIEW);
	    installIntent.setDataAndType(downloadManager.getUriForDownloadedFile(downloadReference), 
	        "application/vnd.android.package-archive");
	    installIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	    startActivity(installIntent); 
  	    TextView t = (TextView) findViewById(R.id.textView1);
    	t.setText("There is newer version ("+latestVersionName+":"+latestVersion+") of this application available. In order to use this app, you MUST upgrade!!!! Next time click OK then INSTALL!!");
	     
	   }
	  }
	 }; 	
	// our code ends

}