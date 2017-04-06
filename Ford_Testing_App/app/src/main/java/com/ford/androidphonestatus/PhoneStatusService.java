package com.ford.androidphonestatus;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.ServiceManager;
import android.provider.Telephony;
import android.support.v4.app.ActivityCompat;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.IWindowManager;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;


/*
 * MainActivity initiates this service
 * this runs in background.
 * This service listens to the User input Command as well as 
 * listens to the Incoming Broadcast messages ,Like Incoming call
 * incoming messages ,Bluetooth States and Pairing request Intent etc.
 * 
 */

public class PhoneStatusService extends Service {

	// initialisations

	private static LinkedBlockingQueue<String> mMessages;
	private MyBroadcastReceiver mBroadcastReceiver;
	private OutgoingSmsContentObserver mOutgoingSmsObserver;
	private String mServerIpAddress;
	protected PowerManager.WakeLock mWakeLock;
	private Thread mClientThread;
	private Thread mServerTHread;
	private Class<?> callManagerClass = null;
	private static Context mContext;
	private TelephonyManager mTelephonyManager;
	private ContentResolver mContentResolver;
	MediaRecorder callrecorder = new MediaRecorder();
	final IWindowManager windowManager = IWindowManager.Stub.asInterface(ServiceManager.getService("window"));
	int signalStrength;
	int SignalStrength ;
	MyListener pslistener;
	String netmessage;
	String batmessage;
	Intent intent = new Intent();
	AudioManager audioManager;

	Bitmap mBitmap;   //Akhil-Add-contacts

	File root = new File(Environment.getExternalStorageDirectory(), "PhoneLog");

	static File logfile = new File(Environment.getExternalStorageDirectory() + "/PhoneLog/Phonelog.log.txt");

	public static List<String> parse_cmd_result = new ArrayList<String>();
	public BroadcastReceiver receiver;
	public static ArrayList discovered_devices = new ArrayList();

	/***
	 * OnCreate for the service listener registered and checking for the directory and the file on sdcard
	 */
	@SuppressLint("Wakelock")
	@SuppressWarnings("deprecation")
	@Override
	public void onCreate() {

		try {

			pslistener = new MyListener();
			mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
			mTelephonyManager.listen(pslistener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);

		} catch (Exception ex) {

			ex.printStackTrace();

		}
		this.registerReceiver(this.mBatInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
		if (!root.exists()) {
			root.mkdirs();
		}
		if (logfile.exists()) {
			logfile.delete();
		}


		//To keep the screen always awake
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		this.mWakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, "PhoneStatus");
		this.mWakeLock.acquire();


	}


	public class LocalBinder extends Binder {
		PhoneStatusService getService() {
			return PhoneStatusService.this;
		}
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	/**
	 * This method will be called
	 * when this app gets destroyed from phone memory
	 * so ,unregistering All broadcastreceivers
	 */


	@Override
	public void onDestroy() {

		if (receiver != null) {
			unregisterReceiver(receiver);
			receiver = null;
		}
		parse_cmd_result.add(sendMessage(" Service onDestroy() called"));
		super.onDestroy();
		try {
			mContext.unregisterReceiver(mBroadcastReceiver);
			if (mContentResolver != null)
				mContentResolver.unregisterContentObserver(mOutgoingSmsObserver);
		} catch (Exception e) {
			e.printStackTrace();
		}
		mWakeLock.release();
		if (logfile.exists()) {
			logfile.delete();
		}
	}


	/**
	 * onStart of the service where the client and server socket thread starts
	 */


	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {


		mContext = this;
		mContentResolver = getContentResolver();
		if (intent != null) {
			Bundle extras = intent.getExtras();
			mMessages = new LinkedBlockingQueue<String>();


			// Start a thread for the socket communication
			mClientThread = new Thread(new ClientSocketThread());
			mServerTHread = new Thread(new ServerSocketThread());
			mClientThread.start();
			mServerTHread.start();


			// Register listener for incoming calls
			mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
			if (null != mTelephonyManager)
				mTelephonyManager.listen(new MyListener(), PhoneStateListener.LISTEN_CALL_STATE);

			// BroadcastReceiver for outgoing calls and incoming texts
			mBroadcastReceiver = new MyBroadcastReceiver();
			IntentFilter filter = new IntentFilter();
			filter.addAction(Intent.ACTION_NEW_OUTGOING_CALL);
			filter.addAction("android.provider.Telephony.SMS_RECEIVED");
			filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
			filter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);
			filter.addAction("android.bluetooth.device.action.PAIRING_REQUEST");
			filter.addAction("com.android.ussd.IExtendedNetworkService");
			filter.addAction("android.provider.Telephony.SMS_DELIVER");
			filter.addAction("android.provider.Telephony.WAP_PUSH_DELIVER");
			filter.addAction("android.intent.action.RESPOND_VIA_MESSAGE");


			//filter.addAction(BluetoothDevice.ACTION_FOUND); //Akhil
			filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
			mContext.registerReceiver(mBroadcastReceiver, filter);

			// content observer for outgoing SMS
			mOutgoingSmsObserver = new OutgoingSmsContentObserver(new Handler());

			// contentResolver.registerContentObserver(Uri.parse("content://sms/out"),true, outgoingSms);
			if (mContentResolver != null)
				mContentResolver.registerContentObserver(Uri.parse("content://sms"), true, mOutgoingSmsObserver);
			//} //Commented by Nandita for auto connection


		}
		return START_REDELIVER_INTENT;
	}


	/**
	 * Phonelistener starts
	 */
	class MyListener extends PhoneStateListener {
		public void onCallStateChanged(int state, String incomingNumber) {
			super.onCallStateChanged(state, incomingNumber);
			String message;
			switch (state) {
				case TelephonyManager.CALL_STATE_RINGING:
					message = "Incoming call from " + incomingNumber;
					parse_cmd_result.add(message);

					break;
				case TelephonyManager.CALL_STATE_OFFHOOK:
					message = "Call active ";

					break;
				case TelephonyManager.CALL_STATE_IDLE:
					message = "Call state idle ";

					break;
				default:
					message = "Unknown call state " + state;
					break;
			}
			showAndSendMessage(message);

		}

		@Override
		public void onSignalStrengthsChanged(SignalStrength signalStrength) {

			super.onSignalStrengthsChanged(signalStrength);
			SignalStrength = signalStrength.getGsmSignalStrength();
			netmessage = "signal Strength is" + SignalStrength;

		}
	}

	/**
	 * Broadcast receiver for receiving the message
	 */
	private BroadcastReceiver mBatInfoReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context ctxt, Intent intent) {
			int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
			batmessage = "The battery level is" + level;

		}
	};


	class MyBroadcastReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			String message;
			if (intent != null) {
				String action = intent.getAction();

				if (action != null) {

//					if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
//						// Retrieve the bond state and the device involved
//
//						BluetoothDevice dev = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
//						final int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
//						final int prevState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR);
//						if (state == BluetoothDevice.BOND_BONDED && prevState == BluetoothDevice.BOND_BONDING) {
//							parse_cmd_result.add(sendMessage(" just paired to :" + dev.getName() + " with MAC ADD:" + dev.getAddress()));
//						}
//
//						if (state == BluetoothDevice.BOND_BONDED && prevState != BluetoothDevice.BOND_BONDING) {
//							parse_cmd_result.add(sendMessage("already paired to :" + dev.getName() + " with MAC ADD:" + dev.getAddress()));
//						}
//						if (state == BluetoothDevice.BOND_NONE && prevState == BluetoothDevice.BOND_BONDED) {
//							parse_cmd_result.add(sendMessage("Was paired already ,now unpaired :dev name:" + dev.getName() + " with MAC ADD:" + dev.getAddress()));
//						}
//						if (prevState == BluetoothDevice.BOND_NONE && state == BluetoothDevice.BOND_BONDING) {
//							parse_cmd_result.add(sendMessage("Sent pairing Request to " + dev.getName() + " with MAC ADD:" + dev.getAddress()));
//						}
//						if (prevState == BluetoothDevice.BOND_BONDING && state == BluetoothDevice.BOND_NONE) {
//							parse_cmd_result.add(sendMessage(dev.getName() + " with MAC ADD:" + dev.getAddress() + " did not respond to Pairing request"));
//						}
//						if (state == BluetoothDevice.BOND_BONDING) {
//							parse_cmd_result.add(sendMessage("pairing to :" + dev.getName() + " with MAC ADD:" + dev.getAddress()));
//
//						}
//
//					}
//					if(BluetoothDevice.ACTION_PAIRING_REQUEST.equals(action)){
//						try {
//							BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);int pairing_key = intent.getIntExtra("android.bluetooth.device.extra.PAIRING_KEY", 0);
//							parse_cmd_result.add(sendMessage(String.format("Received pairing request from :%s ", device.getName())));
//							parse_cmd_result.add(sendMessage(String.format("MAC_ADDRESS : %s", device.getAddress())));
//							parse_cmd_result.add(sendMessage(String.format("PAIRING_KEY : %d", pairing_key)));
//						} catch (Exception e) {
//							e.printStackTrace();
//						}
//					}
//					if (action.equals("android.bluetooth.device.action.PAIRING_REQUEST")) {
//						try {
//							BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
//							//int pin = intent.getIntExtra("android.bluetooth.device.extra.PAIRING_KEY", 0);
//							//the pin in case you need to accept for an specific pin
//							Log.d("PIN", " " + intent.getIntExtra("android.bluetooth.device.extra.PAIRING_KEY", 0));
//							int pairing_key = intent.getIntExtra("android.bluetooth.device.extra.PAIRING_KEY", 0);
//							parse_cmd_result.add(sendMessage(String.format("Received pairing request from :%s ", device.getName())));
//							parse_cmd_result.add(sendMessage(String.format("MAC_ADDRESS : %s", device.getAddress())));
//							parse_cmd_result.add(sendMessage(String.format("PAIRING_KEY : %d", pairing_key)));
//							//maybe you look for a name or address
//	//				                    Log.d("Bonded", device.getName());
//	//				                    byte[] pinBytes;
//	//				                    pinBytes = (""+pin).getBytes("UTF-8");
//	//				                    device.setPin(pinBytes);
//	//				                    //setPairing confirmation if neeeded
//	//				                    device.setPairingConfirmation(true);
//
//						} catch (Exception e) {
//							e.printStackTrace();
//						}
//
//					/*if(device != null){
//							String macAddress = device.getAddress();
//							parse_cmd_result.add(sendMessage(String.format("Received pairing request from %s [%s]", device.getName(), macAddress));
//							parse_cmd_result.add(sendMessage(String.format("Received pairing request from %s ", device.getName()));
//							if (macAddress != null){
//								byte[] pinBytes = { 1, 2, 3, 4 };
//								try{
//									Method m = device.getClass().getMethod("setPin", byte[].class);
//									m.invoke(device, pinBytes);
//									device.getClass().getMethod("setPairingConfirmation", boolean.class).invoke(device, true);
//									device.getClass().getMethod("cancelPairingUserInput").invoke(device);
//								}
//								catch (Exception e){
//									sendException(e, false);
//								}
//							}*/
//				}

					if (action.equals(Intent.ACTION_NEW_OUTGOING_CALL)) {
						message = "Outgoing call to " + intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
						showAndSendMessage(message);
						//parse_cmd_result.add(sendMessage("call-"+intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER)+" ,"+ message));
						parse_cmd_result.add(sendMessage("call-" + intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER)));
						parse_cmd_result.add(sendMessage(message));
					}
					if (action.equals("android.provider.Telephony.SMS_RECEIVED")) {
						Bundle bundle = intent.getExtras();
						if (bundle != null) {
							Object[] pdus = (Object[]) bundle.get("pdus");
							if (pdus != null & pdus.length > 0) {
								SmsMessage text = SmsMessage.createFromPdu((byte[]) pdus[0]);
								if (text != null) {
									showAndSendMessage(String.format("Received SMS text message from %s: \"%s\"", text.getOriginatingAddress(), text.getMessageBody()));
									parse_cmd_result.add(sendMessage(String.format("Received SMS text message from %s: \"%s\"", text.getOriginatingAddress(), text.getMessageBody())));


								}
							}
						}
					}
					if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
						message = "Bluetooth state changed: ";
						int newState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
						switch (newState) {
							case BluetoothAdapter.STATE_OFF:
								message += " off";
								break;
							case BluetoothAdapter.STATE_ON:
								message += " on";
								break;
							case BluetoothAdapter.STATE_TURNING_OFF:
								message += " turning off";
								break;
							case BluetoothAdapter.STATE_TURNING_ON:
								message += " turning on";
								break;
							default:
								message += " unknown state (" + newState + ")";
						}
						parse_cmd_result.add(sendMessage(message));
					}
					if (action.equals(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)) {
						message = "Bluetooth connection state changed: ";
						int state = intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE, BluetoothAdapter.STATE_DISCONNECTED);
						BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
						String deviceInfo = "";
						if (device != null)
							deviceInfo = String.format("%s [%s]", device.getName(), device.getAddress());

						switch (state) {
							case BluetoothAdapter.STATE_CONNECTED:
								message += " connected to" + device.getName();
								break;
							case BluetoothAdapter.STATE_CONNECTING:
								message += " connecting to" + device.getName();
								break;
							case BluetoothAdapter.STATE_DISCONNECTED:
								message += " disconnected from" + device.getName();
								break;
							case BluetoothAdapter.STATE_DISCONNECTING:
								message += " disconnecting from" + device.getName();
								break;

							default:
								message += " unknown connection state (" + state + "):";
						}
						showMessage(message + " " + deviceInfo);
						parse_cmd_result.add(sendMessage(message + " " + deviceInfo));
					}

					if (action.equals("com.android.ussd.IExtendedNetworkService")) {
						System.out.println("PBAP request recived");
						parse_cmd_result.add(sendMessage("PBAP request recived"));
						showAndSendMessage("USSD service received");
					}

				}
			}
		}
	}


	/**
	 * Server socket thread starts
	 * It Listens to Squish Request over TCP connection and
	 * updates result in a global array list parse_cmd_result
	 *
	 * @author SH297910
	 */

	class ServerSocketThread implements Runnable {
		private static final int sourcePort = 4445;
		private static final String app_version = "1.2.0_Beta";
		public boolean isRunning = true;
		ServerSocket serverSocket = null;

		@Override
		public void run() {
			try {
				serverSocket = new ServerSocket(sourcePort);
				serverSocket.setReuseAddress(true);
				//serverSocket.bind(new InetSocketAddress(sourcePort));  //added by Akhil : due to java.bind exception
				while (isRunning) {

					Socket clientSocket = serverSocket.accept();


					BufferedReader in = new BufferedReader(
							new InputStreamReader(clientSocket.getInputStream()));
					BufferedOutputStream bos = new BufferedOutputStream(clientSocket.getOutputStream());

					PrintWriter socketOut = new PrintWriter(clientSocket.getOutputStream(), true);


					final String inputLine;
					//parse_cmd_result.clear();
					int size = parse_cmd_result.size();
					inputLine = in.readLine();
					mMessages.add("Parsing [" + inputLine + "]");
					parseCommand(inputLine);
					if (parse_cmd_result.isEmpty()) {
						try {
							System.out.println("Waiting...");
							Thread.sleep(8000);

						} catch (Exception e) {
						}
					}

					System.out.println("parse_cmd_result:" + parse_cmd_result);
					System.out.println("parse_cmd_result size :" + parse_cmd_result.size());
					if (parse_cmd_result.size() >= size) {
						socketOut.println(parse_cmd_result);
						parse_cmd_result.clear();
					}


					socketOut.flush();
				}
				serverSocket.close();
			} catch (IOException e) {
				sendException(e, false);
				parse_cmd_result.add(sendMessage("Failed to connect to Server.Exception : " + e.toString() + " " + e.getMessage()));
			} finally {
				try {
					if (serverSocket != null)
						serverSocket.close();
				} catch (IOException e) {
					sendException(e, false);
				}
			}
		}


		/* Receiving input commands and then calling the respective function */

		@TargetApi(Build.VERSION_CODES.KITKAT)
		private void parseCommand(String input) throws BindException {

			try {
				if (input != null && input.length() > 0) {
					final String[] tokens = input.split("-");
					if (tokens != null && tokens.length > 0) {
						String command = tokens[0].toLowerCase();
						if (command != null) {

							if (command.equalsIgnoreCase("call")) {
								if (tokens.length > 1) {

									Intent intent = new Intent(Intent.ACTION_CALL);
									intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
									intent.setData(Uri.parse(String.format("tel:%s", tokens[1])));
									if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
										// TODO: Consider calling
										//    ActivityCompat#requestPermissions
										// here to request the missing permissions, and then overriding
										//   public void onRequestPermissionsResult(int requestCode, String[] permissions,
										//                                          int[] grantResults)
										// to handle the case where the user grants the permission. See the documentation
										// for ActivityCompat#requestPermissions for more details.
										return;
									}
									startActivity(intent);

									//parse_cmd_result.add(sendMessage("call-"+tokens[1]));

								} else {

									parse_cmd_result.add(sendMessage("Invaild Parameter"));

								}
							} else if (command.equalsIgnoreCase("request_result")) {

								System.out.println("replied to request");


							} else if (command.equalsIgnoreCase("start_dialer")) {


								System.out.println("Launching Dialer");
								Intent intent = new Intent(Intent.ACTION_DIAL);
								intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
								//intent.setData(Uri.parse("tel:0123456789"));
								startActivity(intent);


							}

//							else if(command.equalsIgnoreCase("turnoff_HFP")){
//								
//								
//								 BluetoothA2dp   mBluetoothA2DP = null;
//								// Get the default adapter
//								BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
//								
//								
//								
//
//								BluetoothProfile.ServiceListener mProfileListener = new BluetoothProfile.ServiceListener() {
//								    public void onServiceConnected(int profile, BluetoothProfile proxy) {
//								        if (profile == BluetoothProfile.A2DP) {
//								        	mBluetoothA2DP = (BluetoothA2dp) proxy;
//								        }
//								    }
//								    public void onServiceDisconnected(int profile) {
//								        if (profile == BluetoothProfile.HEADSET) {
//								        	mBluetoothA2DP = null;
//								        }
//								    }
//								};
//								// Establish connection to the proxy.
//								mBluetoothAdapter.getProfileProxy(mContext, mProfileListener, BluetoothProfile.A2DP);
//
//								// ... call functions on mBluetoothA2Dp
//								
//
//								
//								// Close proxy connection after use.
//								mBluetoothAdapter.closeProfileProxy(BluetoothProfile.A2DP , mBluetoothA2DP);
//								
//								
//								
//							}

							else if (command.equalsIgnoreCase("request_app_version")) {

								String deviceMan = android.os.Build.MANUFACTURER;
								String deviceModel = android.os.Build.MODEL;
								parse_cmd_result.add(sendMessage("App Version:" + app_version + " Device ManuFacturer :" + deviceMan + " Device Model :" + deviceModel));

							} else if (command.equalsIgnoreCase("request_own_number")) {


								TelephonyManager tMgr = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
								try {
									tMgr = (TelephonyManager)getSystemService(TELEPHONY_SERVICE);
									String sim = tMgr.getSimSerialNumber();


									String mPhoneNumber = null;
									mPhoneNumber = tMgr.getLine1Number();
									System.out.println("phone number :" + sim);
									//dailNumber("282") ;
									parse_cmd_result.add(sendMessage("IMSI_Number :" + sim));
								} catch (SecurityException e) {
									parse_cmd_result.add(sendMessage("IMSI_Number :"));
									e.printStackTrace();
									System.out.println("Security Exception raised while Fetching IMSI_Number");
								}


							} else if (command.equalsIgnoreCase("stop_app")) {

								System.exit(0);
							}

							else if(command.equalsIgnoreCase("stop_discovery")){
								parse_cmd_result.clear();
								discovered_devices.clear();
								final BluetoothAdapter mBTAdapter = BluetoothAdapter.getDefaultAdapter();
								if (mBTAdapter.isDiscovering()) {
									mBTAdapter.cancelDiscovery();
									showMessage("Device Discovery Stopped");
									parse_cmd_result.add(sendMessage("Device Discovery Stopped"));
								}else{
									parse_cmd_result.add(sendMessage("Bluetooth Not Discovering"));
								}
							}
							else if(command.equalsIgnoreCase("start_discovery")){
								parse_cmd_result.clear();
								discovered_devices.clear();
								final BluetoothAdapter mBTAdapter = BluetoothAdapter.getDefaultAdapter();
								if (mBTAdapter.isDiscovering()) {
									mBTAdapter.cancelDiscovery();
								}

								IntentFilter filter = new IntentFilter();
								filter.addAction(BluetoothDevice.ACTION_FOUND);
								filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
								filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
								receiver = new BroadcastReceiver() {
									@Override
									public void onReceive(Context context, Intent intent) {
										//do something based on the intent's action
										String action = intent.getAction();
										if (action.equals(BluetoothDevice.ACTION_FOUND)) {
											BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
											// Create a new device item
											discovered_devices.add(device.getAddress());
											//discovered_devices.add(device.getName());
											//discovered_devices.add(device.getUuids());
											System.out.println(discovered_devices);

										}
										if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
											//discovery starts, we can show progress dialog or perform other tasks

											showMessage("Discovery Started");
										}
										if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
											//discovery finishes, dismis progress dialog
											showMessage("Discovery Finished");
											parse_cmd_result.add(sendMessage(discovered_devices.toString()));

										}


									}


								};
								registerReceiver(receiver, filter);
								mBTAdapter.startDiscovery();
								parse_cmd_result.add(sendMessage("Discovery Started"));

							}
//
							else if (command.equalsIgnoreCase("bluetooth") || command.equalsIgnoreCase("bt")) {
								parse_cmd_result.clear();
								if (tokens.length > 1) {
									if (tokens[1].equalsIgnoreCase("on")) {
										BTManager.getInstance(mContext).enable();
										parse_cmd_result.add(sendMessage("Bluetooth state changed:  on"));
									} else if (tokens[1].equalsIgnoreCase("off")) {
										BTManager.getInstance(mContext).disable();
										parse_cmd_result.add(sendMessage("Bluetooth state changed:  off"));


			//							else if (command.equalsIgnoreCase("check_connected")) {
//								parse_cmd_result.clear();
////								IntentFilter filter = new IntentFilter();
////								filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
////								filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
////								filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
////								BroadcastReceiver bt_receiver;
////								bt_receiver = new BroadcastReceiver() {
////									@Override
////									public void onReceive(Context context, Intent intent) {
////										//do something based on the intent's action
////										String action = intent.getAction();
////										BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
//////										if (BluetoothDevice.ACTION_FOUND.equals(action)) {
//////											... //Device found
//////										}
////										 if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
////											//Device is now connected
////											 showMessage("Device is Connected "+BluetoothDevice.EXTRA_NAME);
////											parse_cmd_result.add(sendMessage("Device is Connected "+BluetoothDevice.EXTRA_NAME));
////										}
//////										else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
//////											 //Done searching
//////										}
////										else if (BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED.equals(action)) {
////											 //Device is about to disconnect
////
////										}
////										else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {//Device has disconnected
////											 showMessage("Device is Disconnected "+BluetoothDevice.EXTRA_NAME);
////											 parse_cmd_result.add(sendMessage("Device is Disconnected "+BluetoothDevice.EXTRA_NAME));
////										}
////
////									}
////
////
////								};
////								registerReceiver(bt_receiver, filter);
//								final BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
//								BluetoothProfile.ServiceListener mProfileListener = new BluetoothProfile.ServiceListener() {
//									public void onServiceConnected(int profile, BluetoothProfile proxy) {
//										if (profile == BluetoothProfile.A2DP) {
//											boolean deviceConnected = false;
//											BluetoothA2dp btA2dp = (BluetoothA2dp) proxy;
//
//											List<BluetoothDevice> a2dpConnectedDevices = btA2dp.getConnectedDevices();
//											if (a2dpConnectedDevices.size() != 0) {
//												for (BluetoothDevice device : a2dpConnectedDevices) {
//													if (device.getName().contains("DEVICE_NAME")) {
//														deviceConnected = true;
//													}
//												}
//											}
//											if (!deviceConnected) {
//												Toast.makeText(getBaseContext(), "DEVICE NOT CONNECTED", Toast.LENGTH_SHORT).show();
//											}
//											mBluetoothAdapter.closeProfileProxy(BluetoothProfile.A2DP, btA2dp);
//										}
//									}
//
//									public void onServiceDisconnected(int profile) {
//										// TODO
//									}
//								};
//								mBluetoothAdapter.getProfileProxy(getApplicationContext(), mProfileListener, BluetoothProfile.A2DP);
//
//
//
//							}
										} else if (tokens[1].equalsIgnoreCase("pair")) {
										String mSyncMacAddress = tokens[2];
										parse_cmd_result.clear();

										//BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
										BluetoothAdapter mBTAdapter = BluetoothAdapter.getDefaultAdapter();
										if (mBTAdapter.isDiscovering()) {
											discovered_devices.clear();
											mBTAdapter.cancelDiscovery();
										}
										discovered_devices.clear();
										mBTAdapter.getRemoteDevice(mSyncMacAddress);

										//BluetoothDevice sync = BTManager.getInstance(mContext).getRemoteDevice(mSyncMacAddress);
										BluetoothDevice device = mBTAdapter.getRemoteDevice(mSyncMacAddress);
										Boolean reqStatus = device.createBond();


										//parse_cmd_result.add(sendMessage("started Bonding"));


//										try {
//
//											Method method = sync.getClass().getMethod("createBond", (Class[]) null);
//											method.invoke(sync, (Object[]) null);
//										} catch (Exception e) {
//											parse_cmd_result.add(sendMessage("Raised Exception while sending pairing request"));
//											e.printStackTrace();
//										}

										//System.out.println("bt_pair_response:  " + sync.getClass().getMethod("createBond", (Class[]) null).invoke(sync, (Object[]) null));
										//Toast.makeText(getApplicationContext(),"Pair-response :"+result,Toast.LENGTH_LONG);
										IntentFilter filter = new IntentFilter();
										filter.addAction("android.bluetooth.device.action.PAIRING_REQUEST");
										filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);

										receiver = new BroadcastReceiver() {
											@Override
											public void onReceive(Context context, Intent intent) {
												//do something based on the intent's action
												String action = intent.getAction();
												if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
													// Retrieve the bond state and the device involved

													BluetoothDevice dev = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
													final int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
													final int prevState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR);
													if (state == BluetoothDevice.BOND_BONDED && prevState == BluetoothDevice.BOND_BONDING) {
														parse_cmd_result.add(sendMessage(" just paired to :" + dev.getName() + " with MAC ADD:" + dev.getAddress()));
													} else if (state == BluetoothDevice.BOND_BONDING) {
														parse_cmd_result.add(sendMessage("pairing to :" + dev.getName() + " with MAC ADD:" + dev.getAddress()));

													} else if (state == BluetoothDevice.BOND_BONDED && prevState != BluetoothDevice.BOND_BONDING) {
														parse_cmd_result.add(sendMessage("already paired to :" + dev.getName() + " with MAC ADD:" + dev.getAddress()));
													} else if (state == BluetoothDevice.BOND_NONE && prevState == BluetoothDevice.BOND_BONDED) {
														parse_cmd_result.add(sendMessage("Was paired already ,now unpaired :dev name:" + dev.getName() + " with MAC ADD:" + dev.getAddress()));
													} else if (prevState == BluetoothDevice.BOND_NONE && state == BluetoothDevice.BOND_BONDING) {
														parse_cmd_result.add(sendMessage("Sent pairing Request to " + dev.getName() + " with MAC ADD:" + dev.getAddress()));
													} else if (prevState == BluetoothDevice.BOND_BONDING && state == BluetoothDevice.BOND_NONE) {
														parse_cmd_result.add(sendMessage(dev.getName() + " with MAC ADD:" + dev.getAddress() + "did not respond to Pairing requset"));
													}

												}
												if (BluetoothDevice.ACTION_PAIRING_REQUEST.equals(action)) {
													try {
														BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
														int pairing_key = intent.getIntExtra("android.bluetooth.device.extra.PAIRING_KEY", 0);
														parse_cmd_result.add(sendMessage(String.format("Received pairing request from :%s ", device.getName())));
														parse_cmd_result.add(sendMessage(String.format("MAC_ADDRESS : %s", device.getAddress())));
														parse_cmd_result.add(sendMessage(String.format("PAIRING_KEY : %d", pairing_key)));
													} catch (Exception e) {
														e.printStackTrace();
													}
												}

											}


										};
										registerReceiver(receiver, filter);


									}
								} else {
									parse_cmd_result.add(sendMessage("Invaild parameter"));
								}
							} else if (command.equalsIgnoreCase("check_pair")) {

								if (tokens.length > 1) {
									parse_cmd_result.clear();
									String name = tokens[1];
									BTManager.getInstance(mContext).checkPairing(name);

								}

							}
							else if (command.equalsIgnoreCase("nopair")) {
								parse_cmd_result.clear();
								BTManager.getInstance(getApplicationContext()).cancelPair();

							} else if (command.equalsIgnoreCase("show") || command.equalsIgnoreCase("check")) {
								parse_cmd_result.clear();
								if (tokens.length > 1) {
									if (tokens[1].equalsIgnoreCase("bluetooth") || tokens[1].equalsIgnoreCase("bt")) {
										if (BTManager.getInstance(mContext).isEnabled()) {
											parse_cmd_result.add(sendMessage("Bluetooth Enabled"));

											if (BTManager.getInstance(mContext).isDiscovering()) {
												parse_cmd_result.add(sendMessage("Bluetooth enabled, discovering"));

											} else {
												parse_cmd_result.add(sendMessage("Bluetooth enabled, not discovering"));

											}
										} else {


											parse_cmd_result.add(sendMessage("Bluetooth disabled"));

										}
									} else {
										parse_cmd_result.add(sendMessage("Invaild parameter"));

									}
								} else {
									parse_cmd_result.add(sendMessage("Invaild parameter"));

								}
							} else if (command.equalsIgnoreCase("send")
									|| command.equalsIgnoreCase("text")
									|| command.equalsIgnoreCase("sendtext")) {
								if (tokens.length > 2) {
									String number = tokens[1];
									String text_data = tokens[2];
									MessagingManager.getInstance(getApplicationContext()).sendSMS(text_data, number);
									//Added by Akhil Jumade : save sent messages.

									ContentValues values = new ContentValues();
									values.put("address", number);
									values.put("body", text_data);
									getContentResolver().insert(Uri.parse("content://sms/sent"), values);
								} else {
									parse_cmd_result.add(sendMessage("Invaild parameter"));

								}
							} else if (command.equalsIgnoreCase("create_messages")) {
								if (tokens.length > 2) {
									String number = tokens[1];
									String text_data = tokens[2];
									//MessagingManager.getInstance(getApplicationContext()).sendSMS(text_data, number);
									//Added by Akhil Jumade : save sent messages.

//									ContentValues values = new ContentValues();
//									values.put("address", number);
//									values.put("body", text_data);
//									 getContentResolver().insert(Uri.parse("content://sms/inbox"), values);

									try {
										ContentValues values = new ContentValues();
										values.put("address", number);
										values.put("body", text_data);
										values.put("read", 0);
										values.put("date", "25/08/2016");
										getContentResolver().insert(Uri.parse("content://sms/inbox"), values);
										//Uri.parse("content://sms/inbox", values));

									} catch (Exception ex) {

									}


								} else
									parse_cmd_result.add(sendMessage("Invaild parameter"));
							} else if (command.equalsIgnoreCase("delete_sms_by_body")) {
								if (tokens.length > 0) {
									String text_body = tokens[1];
									MessagingManager.getInstance(getApplicationContext()).deleteMessageByBody(text_body);
								} else {
									parse_cmd_result.add(sendMessage("Invaild parameter"));

								}
							} else if (command.equalsIgnoreCase("delete_sms_by_num")) {
								if (tokens.length > 0) {
									String num = tokens[1];
									MessagingManager.getInstance(getApplicationContext()).deleteMessageByNumber(num);
								} else {
									parse_cmd_result.add(sendMessage("Invaild parameter"));


								}
							} else if (command.equalsIgnoreCase("delete_all_sms")) {
								parse_cmd_result.clear();
								final String myPackageName = getPackageName();
								if (!Telephony.Sms.getDefaultSmsPackage(getApplicationContext()).equals(myPackageName)) {

									Intent intent = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
									intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, myPackageName);
									intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
									getApplication().startActivity(intent);

									deleteAllMessages();
								} else {

									deleteAllMessages();
								}

							} else if (command.equalsIgnoreCase("make_TestApp_default")) {
								final String myPackageName = getPackageName();
								if (!Telephony.Sms.getDefaultSmsPackage(getApplicationContext()).equals(myPackageName)) {

									//Intent intent = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
									Intent intent = new Intent("android.provider.Telephony.ACTION_CHANGE_DEFAULT");
									intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, getPackageName());
									intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
									getApplication().startActivity(intent);

									parse_cmd_result.add(sendMessage("AndroidPhonestatus is been made as default Messaging App."));

								} else {

									parse_cmd_result.add(sendMessage("AndroidPhoneStatus is Already default app for Messaging."));
								}

							}


//								else {
//										
//										System.out.println("NOT IN APP");
//										Uri inboxUri = Uri.parse("content://sms/conversations");
//										
//										Cursor c = getApplicationContext().getContentResolver().query(inboxUri , null, null, null, null);
//										
//										while (c.moveToNext()) {
//										    try {
//										        // Delete the SMS
//										        String pid = c.getString(0); // Get id;
//										        System.out.println("pid is"+pid);
//										        String uri = "content://sms/";
//										        getApplicationContext().getContentResolver().delete(Uri.parse(uri),
//										                null, null);
//										        System.out.println("Deleted");
//										    } catch (Exception e) {
//										    }
//										}			
//											} 


							else if (command.equalsIgnoreCase("home")) {
								Intent startMain = new Intent(Intent.ACTION_MAIN);
								startMain.addCategory(Intent.CATEGORY_HOME);
								startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
								getApplication().startActivity(startMain);
							} else if (command.equalsIgnoreCase("unread_sms_count")) {
								int UnreadSmsCount = MessagingManager.getInstance(getApplicationContext()).getUnreadSMSCount();
								parse_cmd_result.add(sendMessage("Number of unread sms count :" + UnreadSmsCount));


							} else if (command.equalsIgnoreCase("read_sms_count")) {
								int readSmsCount = MessagingManager.getInstance(getApplicationContext()).getReadSMSCount();
								parse_cmd_result.add(sendMessage("Number of read sms count :" + readSmsCount));

							} else if (command.equalsIgnoreCase("sms_total_count")) {
								MessagingManager.getInstance(getApplicationContext()).getTotalSMSCount();
							} else if (command.equalsIgnoreCase("open_sms_by_body")) {
								if (tokens.length > 1) {
									String text_body = tokens[1];
									MessagingManager.getInstance(getApplicationContext()).openMessageByBodyText(text_body);

								} else {
									parse_cmd_result.add(sendMessage("Invaild parameter"));

								}
							} else if (command.equalsIgnoreCase("open_sms_by_number")) {
								if (tokens.length > 1) {
									String number = tokens[1];
									MessagingManager.getInstance(getApplicationContext()).readMessage(number);

								} else {
									parse_cmd_result.add(sendMessage("Invaild parameter"));

								}


							} else if(command.equalsIgnoreCase("add_contact")){
								if (tokens.length > 1) {
									String name = tokens[1];
									String phone = tokens[2];
									String home = tokens[3];
									String work = tokens[4];
									String email = tokens[5];
									String comp = tokens[6];
									String job = tokens[7];
									PhonebookManager.getInstance(getApplicationContext()).addContact(name, phone, home, work, email, comp, job);
								}else {
									parse_cmd_result.add(sendMessage("Invaild parameter"));
								}
							}
							else if (command.equalsIgnoreCase("add_contact_new")) {

								if (tokens.length > 1) {
									String name = tokens[1];
									String phone = tokens[2];
									String home = tokens[3];
									String work = tokens[4];
									String email = tokens[5];
									String comp = tokens[6];
									String job = tokens[7];
									String add_image = tokens[8] ;
									String image_name = tokens[9];
									if (add_image.isEmpty() && image_name.isEmpty() ){
										mBitmap = null;
									}else{
										InputStream imageStream = null;
										try {

											// Getting InputStream of the selected image
											imageStream = new BufferedInputStream(new FileInputStream("/sdcard/PhoneLog/"+image_name+".png"));
											// Creating bitmap of the selected image from its inputstream
											mBitmap = BitmapFactory.decodeStream(imageStream);
											//imageStream = getContentResolver().openInputStream(Uri.parse(new File("/sdcard/PhoneLog/contact.png").toString()));
										} catch (FileNotFoundException e) {
											parse_cmd_result.add(sendMessage("/sdcard/PhoneLog/"+image_name+".png"+" is not found"));
											e.printStackTrace();
										}
									}
									PhonebookManager.getInstance(getApplicationContext()).addContact_new(name, phone, home, work, email, comp, job,mBitmap);
								} else {
									parse_cmd_result.add(sendMessage("Invaild parameter"));
								}

							} else if (command.equalsIgnoreCase("add_homecontact")) {
								if (tokens.length > 1) {
									String name = tokens[1];
									String phone = tokens[2];
									String homeone = tokens[3];
									String hometwo = tokens[4];
									String work = tokens[5];
									String email = tokens[6];
									String comp = tokens[7];
									String job = tokens[8];
									PhonebookManager.getInstance(getApplicationContext()).addHomeContact(name, phone, homeone, hometwo, work, email, comp, job);
								} else {
									parse_cmd_result.add(sendMessage("Invaild parameter"));

								}
							} else if (command.equalsIgnoreCase("add_workcontact")) {
								if (tokens.length > 1) {
									String name = tokens[1];
									String phone = tokens[2];
									String home = tokens[3];
									String workone = tokens[4];
									String worktwo = tokens[5];
									String email = tokens[6];
									String comp = tokens[7];
									String job = tokens[8];
									PhonebookManager.getInstance(getApplicationContext()).addWorkContact(name, phone, home, workone, worktwo, email, comp, job);
								} else {
									parse_cmd_result.add(sendMessage("Invaild parameter"));

								}
							} else if (command.equalsIgnoreCase("add_mobilecontact")) {
								if (tokens.length > 1) {
									String name = tokens[1];
									String phoneone = tokens[2];
									String phonetwo = tokens[3];
									String home = tokens[4];
									String work = tokens[5];
									String email = tokens[6];
									String comp = tokens[7];
									String job = tokens[8];
									PhonebookManager.getInstance(getApplicationContext()).addMobileContact(name, phoneone, phonetwo, home, work, email, comp, job);
								} else {
									parse_cmd_result.add(sendMessage("Invaild parameter"));

								}
							} else if (command.equalsIgnoreCase("add_othercontacttwo")) {
								if (tokens.length > 1) {
									String name = tokens[1];
									String phone = tokens[2];
									String home = tokens[3];
									String otherone = tokens[4];
									String othertwo = tokens[5];
									String work = tokens[6];
									String email = tokens[7];
									String comp = tokens[8];
									String job = tokens[9];
									PhonebookManager.getInstance(getApplicationContext()).addOtherContacttwo(name, phone, home, otherone, othertwo, work, email, comp, job);
								} else {
									parse_cmd_result.add(sendMessage("Invaild parameter"));

								}
							} else if (command.equalsIgnoreCase("add_othercontact")) {
								if (tokens.length > 1) {
									String name = tokens[1];
									String phone = tokens[2];
									String home = tokens[3];
									String other = tokens[4];
									String work = tokens[5];
									String email = tokens[6];
									String comp = tokens[7];
									String job = tokens[8];
									PhonebookManager.getInstance(getApplicationContext()).addOtherContact(name, phone, home, other, work, email, comp, job);
								} else {
									parse_cmd_result.add(sendMessage("Invaild parameter"));

								}
							} else if (command.equalsIgnoreCase("add_otherlistcontact")) {
								if (tokens.length > 1) {
									String name = tokens[1];
									String phone = tokens[2];
									String home = tokens[3];
									String other = tokens[4];
									String work = tokens[5];
									String email = tokens[6];
									String comp = tokens[7];
									String job = tokens[8];
									String address = tokens[9];
									PhonebookManager.getInstance(getApplicationContext()).addOtherListContacts(name, phone, home, other, work, email, comp, job, address);
								} else {
									parse_cmd_result.add(sendMessage("Invaild parameter"));

								}
							} else if (command.equalsIgnoreCase("add_addresscontact")) {
								if (tokens.length > 1) {
									String name = tokens[1];
									String phone = tokens[2];
									String home = tokens[3];
									String work = tokens[4];
									String email = tokens[5];
									String comp = tokens[6];
									String job = tokens[7];
									String address = tokens[8];
									PhonebookManager.getInstance(getApplicationContext()).addaddressContact(name, phone, home, work, email, comp, job, address);
								} else {
									parse_cmd_result.add(sendMessage("Invaild parameter"));

								}
							} else if (command.equalsIgnoreCase("dial_call_phonebook")) {
								if (tokens.length > 1) {
									String name = tokens[1];
									CallManager.getInstance(getApplicationContext()).dialCallPhonebook(name);
								} else {
									parse_cmd_result.add(sendMessage("Invaild parameter"));

								}
							} else if (command.equalsIgnoreCase("fetch_last_incoming")) {
								CallManager.getInstance(getApplicationContext()).fetchLastIncomingCall();
							} else if (command.equalsIgnoreCase("fetch_last_outgoing")) {
								CallManager.getInstance(getApplicationContext()).fetchLastOutgoingCall();
							} else if (command.equalsIgnoreCase("redial_last_outgoing")) {
								CallManager.getInstance(getApplicationContext()).redialLastCall();
							} else if (command.equalsIgnoreCase("delete_recent_calls")) {
								CallManager.getInstance(getApplicationContext()).deleteRecentCalls();
								parse_cmd_result.add(sendMessage("Deleted All call Logs"));

							} else if (command.equalsIgnoreCase("dial_from_call_history")) {
								if (tokens.length > 1) {
									String dialer_name = tokens[1];
									CallManager.getInstance(getApplicationContext()).redialFromCallHistory(dialer_name);
								} else {
									parse_cmd_result.add(sendMessage("Invaild parameter"));

								}
							} else if (command.equalsIgnoreCase("mobile_data")) {
								if (tokens.length > 1) {
									if ((tokens[1].equalsIgnoreCase("True")) || (tokens[1].equalsIgnoreCase("False"))) {
										String value = tokens[1];
										boolean data_value = Boolean.valueOf(value);
										CallManager.getInstance(getApplicationContext()).setMobileDataEnabled(data_value);
									} else {
										parse_cmd_result.add(sendMessage("Invaild parameter"));

									}
								} else {
									parse_cmd_result.add(sendMessage("Invaild parameter"));

								}
							} else if (command.equalsIgnoreCase("bt_tethring")) {
								if (tokens.length > 1) {
									if ((tokens[1].equalsIgnoreCase("True")) || (tokens[1].equalsIgnoreCase("False"))) {
										String bt_tethring_param = tokens[1];
										boolean bt_tethring_value = Boolean.valueOf(bt_tethring_param);
										BTManager.getInstance(getApplicationContext()).setBtTethering(bt_tethring_value);
									} else {
										parse_cmd_result.add(sendMessage("Invaild parameter"));

									}
								} else {


									parse_cmd_result.add(sendMessage("Invaild parameter"));

								}
							} else if (command.equalsIgnoreCase("bt_tethring_status")) {
								BTManager.getInstance(getApplicationContext()).IsBluetoothTetherEnabled();
							} else if (command.equalsIgnoreCase("bt_unpair")) {
								parse_cmd_result.clear();
								if (tokens.length > 1) {
									String bt_unpair_param = tokens[1];
									BTManager.getInstance(getApplicationContext()).unpairDeviceByName(bt_unpair_param);

								}
							} else if (command.equalsIgnoreCase("delete_allcontacts")) {
								PhonebookManager.getInstance(getApplicationContext()).deleteAllContacts();
								parse_cmd_result.add(sendMessage("All contacts are deleted"));

							} else if (command.equalsIgnoreCase("read_smscount")) {
								MessagingManager.getInstance(getApplicationContext()).getReadSMSNumber();

							} else if (command.equalsIgnoreCase("total_smscount")) {
								MessagingManager.getInstance(getApplicationContext()).getTotalSMSNumber();
							} else if (command.equalsIgnoreCase("unread_smscount")) {
								MessagingManager.getInstance(getApplicationContext()).getUnreadSMSNumber();
							} else if (command.equalsIgnoreCase("read_msg")) {
								if (tokens.length > 1) {
									String phoneno = tokens[1];
									//MessagingManager.getInstance(getApplicationContext()).openMessageByNumber(phoneno);
									MessagingManager.getInstance(getApplicationContext()).markMessageRead(mContext, phoneno);
								}
							} else if (command.equalsIgnoreCase("read_all_msg")) {

								//MessagingManager.getInstance(getApplicationContext()).openMessageByNumber(phoneno);
								MessagingManager.getInstance(getApplicationContext()).markAllMessageRead(mContext);
							} else if (command.equalsIgnoreCase("msg_date")) {
								if (tokens.length > 1) {
									String phoneno = tokens[1];
									MessagingManager.getInstance(getApplicationContext()).getMessageDate(phoneno);
								}
							} else if (command.equalsIgnoreCase("call_stamp")) {
								if (tokens.length > 1) {
									String phoneno = tokens[1];
									CallManager.getInstance(getApplicationContext()).getCallTimeStamp(phoneno);
								}
							} else if (command.equalsIgnoreCase("bt_vis")) {
								BTManager.getInstance(getApplicationContext()).DiscoveryON();
							} else if (command.equalsIgnoreCase("net_signal")) {
								parse_cmd_result.add(sendMessage("The network Signal strength is" + signalStrength));
								parse_cmd_result.add(sendMessage("The Signal Strength is" + SignalStrength));

							} else if (command.equalsIgnoreCase("net_signal")) {
								parse_cmd_result.add(sendMessage(netmessage));

							} else if (command.equalsIgnoreCase("bat_level")) {
								parse_cmd_result.add(sendMessage(batmessage));


							} else if (command.equalsIgnoreCase("clear")) {
								BTManager.getInstance(getApplicationContext()).closeProfiles();
								Log.i("PROFILES", "profiles cleared");
							} else if (command.equalsIgnoreCase("closemap")) {
								BTManager.getInstance(getApplicationContext()).closeProfileMap();
								Log.i("MAPPROFILES", "map profiles cleared");
							} else if (command.equalsIgnoreCase("closepbp")) {
								BTManager.getInstance(getApplicationContext()).closeProfilePbap();
								Log.i("MAPPROFILES", "map profiles cleared");
							} else if (command.equalsIgnoreCase("closehfp")) {
								BTManager.getInstance(getApplicationContext()).closeProfileHfp();
								Log.i("MAPPROFILES", "map profiles cleared");
							} else if (command.equalsIgnoreCase("closea2dp")) {
								BTManager.getInstance(getApplicationContext()).closeProfileAudio();
								Log.i("MAPPROFILES", "map profiles cleared");
							} else if (command.equalsIgnoreCase("manufacturer")) {
								String deviceMan = android.os.Build.MANUFACTURER;
								parse_cmd_result.add(sendMessage("The Manufacturer name is" + deviceMan));


							} else if (command.equalsIgnoreCase("carrier")) {
								String deviceCarrier = mTelephonyManager.getNetworkOperatorName();
								parse_cmd_result.add(sendMessage("The Carrier name is" + deviceCarrier));

							} else if (command.equalsIgnoreCase("phoneno")) {
								String phoneNumber = mTelephonyManager.getLine1Number();
								parse_cmd_result.add(sendMessage("The phoneNumber is" + phoneNumber));

							} else if (command.equalsIgnoreCase("phonesw")) {
								String phonesw = mTelephonyManager.getDeviceSoftwareVersion();
								parse_cmd_result.add(sendMessage("The phone software version is" + phonesw));

							}

//							else if(command.equalsIgnoreCase("profiles")){
//
//							}


							else if (command.equalsIgnoreCase("play")) {
								AudioAssistance.getInstance(getApplicationContext()).getPlayList();
								AudioAssistance.getInstance(getApplicationContext()).playList();

							} else if (command.equalsIgnoreCase("next")) {
								AudioAssistance.getInstance(getApplicationContext()).nextSong();
							} else if (command.equalsIgnoreCase("repeatc")) {
								AudioAssistance.getInstance(getApplicationContext()).repeatCurrentSong();

							} else if (command.equalsIgnoreCase("repeata")) {
								AudioAssistance.getInstance(getApplicationContext()).repeatPlayList();
							} else if (command.equalsIgnoreCase("repeata")) {
								AudioAssistance.getInstance(getApplicationContext()).repeatNone();
							} else if (command.equalsIgnoreCase("shuffle")) {
								//	AudioAssistance.getInstance(getApplicationContext()).getPlayList();
								AudioAssistance.getInstance(getApplicationContext()).shuffleSong();

							} else if (command.equalsIgnoreCase("stop")) {

								AudioAssistance.getInstance(getApplicationContext()).stopSong();

							} else if (command.equalsIgnoreCase("volmax")) {
								audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

								audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0);
							} else if (command.equalsIgnoreCase("volmin")) {
								audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

								audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, audioManager.getRingerMode(), 0);
							} else if (command.equalsIgnoreCase("startrecord")) {
								EmergencyAssistance.getInstance(getApplicationContext()).startRecord();
								parse_cmd_result.add(sendMessage("started recording"));


							} else if (command.equalsIgnoreCase("stoprecord")) {
								EmergencyAssistance.getInstance(getApplicationContext()).stopRecord();
							} else if (command.equalsIgnoreCase("openrf")) {
								BTManager.getInstance(getApplicationContext()).openRFComSocket();
								Log.i("RFCOMM", "rfcomm opened");
							} else if (command.equalsIgnoreCase("discoverable_on")) {
								parse_cmd_result.clear();
								BTManager.getInstance(getApplicationContext()).DiscoverableON();
							} else if (command.equalsIgnoreCase("poweroff")) {
								//	AudioAssistance.getInstance(getApplicationContext()).getPlayList();
								EmergencyAssistance.getInstance(getApplicationContext()).powerOff();

							} else if (command.equalsIgnoreCase("device_info")) {
								String info = Util.detailedDeviceInfo();
								parse_cmd_result.add(sendMessage(info));

							} else if (command.equalsIgnoreCase("delete_contact")) {
								if (tokens.length > 1) {
									String contact_name = tokens[1];
									PhonebookManager.getInstance(getApplicationContext()).deleteContact(contact_name);
								} else {
									parse_cmd_result.add(sendMessage("Invaild parameter"));

								}
							} else if (command.equalsIgnoreCase("contact_total_count")) {
								PhonebookManager.getInstance(getApplicationContext()).getTotalContactCount();
							} else if (command.equalsIgnoreCase("send_vCard_SMS")) {
								if (tokens.length > 2) {
									String name = tokens[1];
									String number = tokens[2];
									PhonebookManager.getInstance(getApplicationContext()).sendVCardSMS(name, number);
								} else {
									parse_cmd_result.add(sendMessage("Invaild parameter"));

								}
							} else if (command.equalsIgnoreCase("view_all_sms")) {
								if (tokens.length > 1) {
									String folder = tokens[1];
									MessagingManager.getInstance(getApplicationContext()).getAllSms(folder);
								}

							}


//							else if(command.equalsIgnoreCase("add_contact")){
//								if (tokens.length > 1) {
//									String name = tokens[1];
//									String[] number = {null,null,null,null,null,null};
//									int i=1,j = tokens.length;	
//									while(i < j){
//										if(tokens[i].contains("mobilenumber=")){
//											number[1] = tokens[i].substring(13);
//										}
//										else if(tokens[i].contains("homenumber=")){
//											number[2] = tokens[i].substring(11);
//										}
//										else if(tokens[i].contains("worknumber=")){
//											number[3] = tokens[i].substring(11);
//										}
//										else if(tokens[i].contains("emailid=")){
//											number[4] = tokens[i].substring(8);
//										}
//										else if(tokens[i].contains("company=")){
//											number[5] = tokens[i].substring(8);
//										}
//										else if([i].contains("jobtitle=")){
//											number[6] = tokens[i].substring(9);
//										}
//										i++;
//									}
//									PhonebookManager.getInstance(getApplicationContext()).addContact(name, number);;
//								}else {
//									parse_cmd_result.add(sendMessage("Invaild parameter"));
//								}
//							}


							//After inclusion of the client thread starting on start of command we need not send the ipaddr
							//							else if(command.equalsIgnoreCase("ipaddr"))
							//							{
							//								
							//								mServerIpAddress = tokens[1];
							//								Log.d("TAG", "It is here ipaddr"+mServerIpAddress);
							//								//new Thread(new ClientSocketThread()).start();
							//							//	mClientThread.start();
							//							}
							else {
								parse_cmd_result.add(sendMessage("Invaild command and parameter"));

							}
						} else {
							parse_cmd_result.add(sendMessage("Invaild command and parameter"));

						}
					} else {
						parse_cmd_result.add(sendMessage("Invaild command and parameter"));

					}
				} else {
					parse_cmd_result.add(sendMessage("Invaild command and parameter"));

				}
			} catch (Exception e) {
				sendException(e, false);
			}


		}


		private void elseif(boolean b) {
			// TODO Auto-generated method stub

		}
	}

	/***
	 * Client socket thread starts
	 * listens to any new incoming messages in Queue ,updated by ServerSocketThread
	 * and write logs to Phone_logs.txt
	 *
	 * @author SH297910
	 */

	class ClientSocketThread implements Runnable {
		private static final int destPort = 4444;
		public boolean isRunning;

		//PrintWriter out;
		//Socket socket;
		@Override
		public void run() {
			isRunning = true;
			try {
				while (isRunning) {
					Log.d("TAG", "Inside the client thread");
					String message = mMessages.take(); // blocks until a new message arrives
					Log.d("TAG", "The message is" + message);


					//Logging the data on the sdcard of the file
					if (!logfile.exists()) {
						try {
							logfile.createNewFile();
						} catch (IOException e) {

							e.printStackTrace();
						}

					}

					Log.d("TAG", "path of file is" + logfile);
					BufferedWriter buf;
					try {
						buf = new BufferedWriter(new FileWriter(logfile, true));
						buf.append(message + "\n");
						buf.newLine();
						buf.close();
					} catch (IOException e) {

						e.printStackTrace();
					}

				}
			} catch (InterruptedException e) {
				sendException(e, false);
			} finally {

			}
		}
	}

	class OutgoingSmsContentObserver extends ContentObserver {
		public OutgoingSmsContentObserver(Handler handler) {
			super(handler);
		}

		@Override
		public void onChange(boolean selfChange) {
			super.onChange(selfChange);
			try {
				Uri uri = Uri.parse("content://sms");
				Cursor cur = getContentResolver().query(uri, null, null, null, null);
				if (cur != null
						&& cur.moveToFirst()
						&& cur.getInt(cur.getColumnIndex("type")) == 2) {
					showAndSendMessage(
							String.format("Sent SMS text message to %s: \"%s\"", cur.getString(cur.getColumnIndex("address")), cur.getString(cur.getColumnIndex("body"))));
					parse_cmd_result.add(String.format("Sent SMS text message to %s: \"%s\"", cur.getString(cur.getColumnIndex("address")), cur.getString(cur.getColumnIndex("body"))));
				}
			} catch (Exception e) {
				sendException(e, false);
			}
		}

	}


	/*Utility Methods */
	public static void showMessage(String message) {
		Toast.makeText(mContext, message, Toast.LENGTH_LONG).show();
	}

	public void deleteAllMessages() {

		Uri inboxUri = Uri.parse("content://sms/conversations");
		Cursor c = getApplicationContext().getContentResolver().query(inboxUri, null, null, null, null);
		int count;
		count = c.getCount();
	       /*
		while (c.moveToNext()) {
		    try {
		        // Delete the SMS
		        String pid = c.getString(0); // Get id;
		        System.out.println("pid is"+pid);
		        String uri = "content://sms/";
		        getApplicationContext().getContentResolver().delete(Uri.parse(uri),
		              null, null);
		        System.out.println("Deleted");
		    } catch (Exception e) {
		    }
		}*/
		parse_cmd_result.add(sendMessage("Number of msg In Inbox: " + count));
		System.out.println("Number of msg In Inbox:" + count);
		if (count != 0) {
			while (c.moveToNext()) {
				try {
					// Delete the SMS
					String pid = c.getString(0); // Get id;
					System.out.println("pid is" + pid);
					String uri = "content://sms/";
					getApplicationContext().getContentResolver().delete(Uri.parse(uri),
							null, null);
					System.out.println("Deleted");
				} catch (Exception e) {
					parse_cmd_result.add(sendMessage("Failed To delete SMS from phone"));
				}
			}
			parse_cmd_result.add(sendMessage("All messages Cleared: True"));

		} else {
			parse_cmd_result.add(sendMessage("No Messages to Delete"));

		}
		c.close();


	}


	/* Send log message to server */
	public static String sendMessage(String message) {
		message = Util.generateDeviceInfo() + message;
		boolean fileDelete = false;

		try {
			if (mMessages != null) {
				mMessages.put(message);

			}

			Log.d("AndroidPhoneStatus : ", message);

		} catch (InterruptedException e) {
			sendException(e, false);
		}
		return message;


	}

	public String[] parsePhoneNumbers(String number) {
		String[] phone_numbers = number.split(",");

		for (int i = 0; i < phone_numbers.length; i++) {
			if (phone_numbers[i] == "[]")
				phone_numbers[i] = phone_numbers[i].replace("[", "");
			phone_numbers[i] = phone_numbers[i].replace("]", "");
			phone_numbers[i] = phone_numbers[i].replace("'", "");
		}

		return phone_numbers;
	}

	public static boolean isAppRunning(Context context) {

		//Author :Akhil Jumade 314314
		// check with the first task(task in the foreground)
		// in the returned list of tasks
		ActivityManager activityManager = (ActivityManager) context
				.getSystemService(Context.ACTIVITY_SERVICE);
		List<RunningTaskInfo> services = activityManager
				.getRunningTasks(Integer.MAX_VALUE);
		if (services.get(0).topActivity.getPackageName().toString()
				.equalsIgnoreCase(context.getPackageName().toString())) {
			// your application is running in the background
			return true;
		}
		return false;
	}


	public void showAndSendMessage(String message) {
		showMessage(message);
		sendMessage(message);
	}

	public static void sendException(Exception e, boolean showToast) {
		sendMessage(" Exception : " + e.toString() + " " + e.getMessage());
	}

	// Get vadafone balance(*111*2for vadafone)
	private void dailNumber(String code) {
		String ussdCode = "*" + code + Uri.encode("#");
		startActivity(new Intent("android.intent.action.CALL", Uri.parse("tel:" + ussdCode)));
	}

}


	

