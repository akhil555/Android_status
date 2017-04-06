package com.ford.androidphonestatus;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothHealth;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothProfile.ServiceListener;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;





@SuppressLint("NewApi")
public class BTManager {

	
	
	
	
	
	private static final String NAME = "BluetoothHF";
	//private static final UUID MY_UUID = UUID.fromString("0000111F-0000-1000-8000-00805F9B34FB");
	//PBAP_PSE_UUID    from samsung mobile management wesite
	//changed UUID for PSE devices(Phonebook server Equipment)
	private static final UUID MY_UUID = UUID.fromString("0000112F-0000-1000-8000-00805F9B34FB");
	private static BTManager INSTANCE;
	private static Context mContext;
	private BluetoothAdapter mBTAdapter;
	private Class<?> classBluetoothPan = null;
	private Class<?> classBluetoothPbap = null;
	private Class<?> classBluetoothMap = null;
	private Class<?> classBluetoothPairDlg = null;
	private Class<?> classRequestDialog = null;
	private Class<?> classCloseProfiles = null;
	private Constructor<?> BTPanCtor = null;
	private Object BTSrvInstance = null;
	private Class<?> noparams[] = {};
	private Method mIsBTTetheringOn;
	private Method mSetBTTethering;
	private Method closePbap;
	private Method closeMap;
	private Method acceptRequest;
	private Method denyRequest;	
	private Method closeProfiles;
	private ServiceListener mProfileListner;
	private BluetoothA2dp mBluetoothAudio;
	private BluetoothHeadset mBluetoothHeadset;

	private BluetoothHealth mBluetoothHealth;
	private BluetoothGatt mBluetoothGatt;
	private BluetoothDevice mmDevice; 
	BluetoothSocket socket = null; 
	String TAG = "FORD";
	public OutputStream outStream = null;
    public InputStream inStream=null;
    
    
	protected Handler sHandler;

	
	
	
	private BTManager(Context c){
		mContext = c;
		mBTAdapter = getBTAdapter();
	}
	
	public static BTManager getInstance(Context c){
		if(INSTANCE == null)
			INSTANCE = new BTManager(c);
		return INSTANCE;
	}

	public BluetoothAdapter getBTAdapter() {
	    if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1)
	        return BluetoothAdapter.getDefaultAdapter();
	    else {
	        BluetoothManager bm = (BluetoothManager) mContext.getSystemService(mContext.BLUETOOTH_SERVICE);
	        return bm.getAdapter();
	    }
	
	}

	/* Set Bluetooth Tethering ON/OFF
	 *  Parameter - <True/False> */
	public void setBtTethering(boolean value) {
		mBTAdapter = getBTAdapter();
	    try {
	        classBluetoothPan = Class.forName("android.bluetooth.BluetoothPan");
	        mSetBTTethering = classBluetoothPan.getDeclaredMethod("setBluetoothTethering", boolean.class);
	        mSetBTTethering.setAccessible(true);
	        BTPanCtor = classBluetoothPan.getDeclaredConstructor(Context.class, ServiceListener.class);
	        BTPanCtor.setAccessible(true);
	        BTSrvInstance = BTPanCtor.newInstance(mContext.getApplicationContext(), new BTPanServiceListener(mContext.getApplicationContext()));
	    
	        if(mBTAdapter != null && mSetBTTethering != null) {
	            mSetBTTethering.invoke(BTSrvInstance, new Boolean(value));
	           PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage(String.format("Bluetooth Tethring: %s", value)));
	        }
	    } catch (ClassNotFoundException e) {
	    	PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage(String.format("Failed to set bluetooth tethering")));
			PhoneStatusService.sendException(e, false);
	    } catch (Exception e) {
	    	PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage((String.format("Failed to set bluetooth tethering"))));
			PhoneStatusService.sendException(e, false);
	    }
	}
	


	/* Check the Bluetooth Tethering status
	 * Parameter - <None> */
	public boolean IsBluetoothTetherEnabled() {
		mBTAdapter = getBTAdapter();
	    try {
	        classBluetoothPan = Class.forName("android.bluetooth.BluetoothPan");
	        mIsBTTetheringOn = classBluetoothPan.getDeclaredMethod("isTetheringOn", noparams);
	        BTPanCtor = classBluetoothPan.getDeclaredConstructor(Context.class, ServiceListener.class);
	        BTPanCtor.setAccessible(true);
	        BTSrvInstance = BTPanCtor.newInstance(mContext.getApplicationContext(), new BTPanServiceListener(mContext.getApplicationContext()));
	    
	        if(mBTAdapter != null) {
	            boolean val = (Boolean)mIsBTTetheringOn.invoke(BTSrvInstance, (Object []) noparams);
	            PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage (String.format("Bluetooth Tethring status: %s", val)));
	            return val;
	        }
	    } catch (ClassNotFoundException e) {
	    	PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage((String.format("Failed to fetch bluetooth tethring status"))));
			PhoneStatusService.sendException(e, false);
	    } catch (Exception e) {
	    	PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage((String.format("Failed to fetch bluetooth tethring status"))));
			PhoneStatusService.sendException(e, false);
	    }
	    return false;
	}

	public class BTPanServiceListener implements ServiceListener {
	    private final Context context;
	    public BTPanServiceListener(final Context context) {
	        this.context = context;
	    }

	    @Override
	    public void onServiceConnected(final int profile,
	                                   final BluetoothProfile proxy) {
	        //Some code must be here or the compiler will optimize away this callback.
	        Log.i("MyApp", "BTPan proxy toggled");
	        
	    }

	    @Override
	    public void onServiceDisconnected(final int profile) {
	    }
	}
	
	public void acceptRequest() throws NoSuchMethodException{

	try {
		classRequestDialog  = Class.forName("com.android.settings.bluetooth.BluetoothPermissionActivity");
		System.out.println("class name"+classRequestDialog);
		acceptRequest = classRequestDialog.getDeclaredMethod("onPositive");
		acceptRequest.setAccessible(true);
		 PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage (String.format("Accept Request")));
	} catch (ClassNotFoundException e) {
		
		e.printStackTrace();
	}
	}
	
	public void denyRequest() throws NoSuchMethodException{
		mBTAdapter = getBTAdapter();
		try {
			classRequestDialog  = Class.forName("com.android.settings.bluetooth.BluetoothPermissionActivity");
			denyRequest = classRequestDialog.getDeclaredMethod("onNegative");
			denyRequest.setAccessible(true);
			PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage(String.format("Deny Request")));
		} catch (ClassNotFoundException e) {
			
			e.printStackTrace();
		}
	}
	
	
	
	public void closeProfiles() throws NoSuchMethodException, ClassNotFoundException{
	
		mBTAdapter.closeProfileProxy(BluetoothProfile.HEADSET, mBluetoothHeadset);
		mBTAdapter.closeProfileProxy(BluetoothProfile.A2DP, mBluetoothAudio);
		mBTAdapter.closeProfileProxy(BluetoothProfile.GATT, mBluetoothGatt);
		mBTAdapter.closeProfileProxy(BluetoothProfile.HEALTH, mBluetoothHealth);

		System.out.println("closing all profiles");
				
		
	}
	
	public void closeProfilePbap(){
		mBTAdapter = getBTAdapter();
		try {
			classBluetoothPbap = Class.forName("android.bluetooth.BluetoothPbap");
			closePbap = classBluetoothPbap.getDeclaredMethod("close");
			
			closePbap.setAccessible(true);
			
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void closeProfileMap(){
		mBTAdapter = getBTAdapter();
		try {
			classBluetoothMap = Class.forName("android.bluetooth.BluetoothMap");
			closeMap = classBluetoothMap.getDeclaredMethod("close");
			closeMap.setAccessible(true);
			System.out.println("MAP CLOSED");
			
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void closeProfileHfp(){
		mBTAdapter.closeProfileProxy(BluetoothProfile.HEADSET, mBluetoothHeadset);
	}
	
	public void closeProfileAudio(){
		mBTAdapter.closeProfileProxy(BluetoothProfile.A2DP, mBluetoothAudio);
	}
	
	public void openRFComSocket()  {
	

		Runnable runnable = new Runnable() {
			
			@Override
			public void run() {
				BluetoothSocket socket = null; 
				mmDevice = mBTAdapter.getRemoteDevice("D0:39:72:50:58:B3");
				  PhoneStatusService.showMessage("started RF");
				  System.out.println("STARTRED RF");
				try {
					PhoneStatusService.showMessage("invoked RF");
					Method m = mmDevice.getClass().getMethod("createRfcommSocket", new Class[] { int.class });

					socket = (BluetoothSocket) m.invoke(mmDevice, 1); 
					  Log.i(TAG, "RFCOMM socket created.");
					  System.out.println("RFCOMM socket created.");
		        } catch (NoSuchMethodException e) {
		            Log.i(TAG, "Could not invoke createRfcommSocket.");
		            e.printStackTrace();
		        } catch (IllegalArgumentException e) {
		            Log.i(TAG, "Bad argument with createRfcommSocket.");
		            e.printStackTrace();
		        } catch (IllegalAccessException e) {
		            Log.i(TAG, "Illegal access with createRfcommSocket.");
		            e.printStackTrace();
		        } catch (InvocationTargetException e) {
		            Log.i(TAG, "Invocation target exception: createRfcommSocket.");
		            e.printStackTrace();
		        }
		        Log.i(TAG, "Got socket for device "+socket.getRemoteDevice()); 
		        System.out.println("Got socket for device "+socket.getRemoteDevice());
		        mBTAdapter.cancelDiscovery(); 
		        
		        Log.i(TAG, "Connecting socket...");
		        try {
		            socket.connect(); 
		            Log.i(TAG, "Socket connected.");
		        } catch (IOException e) {
		            try {
		                Log.e(TAG, "Failed to connect socket. ", e);
		                socket.close();
		                Log.e(TAG, "Socket closed because of an error. ", e);
		            } catch (IOException eb) {
		                Log.e(TAG, "Also failed to close socket. ", eb);
		            }
		            return;
		        }
		        try {
		            outStream = socket.getOutputStream(); 
		            Log.i(TAG, "Output stream open.");
		            inStream = socket.getInputStream();
		            Log.i(TAG, "Input stream open.");
		        } catch (IOException e) {
		            Log.e(TAG, "Failed to create output stream.", e);  
		        }
		       
		        try {
					outStream.write("AT+BRSF=20\r".getBytes());
					 byte[] inBuffer = new byte[1024];
				       
					inStream.read(inBuffer);
					 String command = new String(inBuffer).trim();
					 Log.i(TAG, "command is"+command);
					socket.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
			}
	        Log.d(TAG, "command sent");
 
		}
		};
		
		  System.out.println("Got socket for device ");
		
        
	}


	/* Unpair already paired device by name 
	 *  Parameter - <Device Name>: Name of the mobile devices which need to be unpaired */
	public void unpairDeviceByName(String devicename) {
		BluetoothDevice btdevice = null;
		mBTAdapter = getBTAdapter();
		try {

			Set<BluetoothDevice> pairedDevices = mBTAdapter.getBondedDevices();
			Iterator entries = pairedDevices.iterator();
			while (entries.hasNext()) {
				BluetoothDevice temp = (BluetoothDevice) entries.next();
				if (temp.getName().equalsIgnoreCase(devicename)) {
					btdevice = temp;
					if (btdevice != null) {

						Method m = btdevice.getClass().getMethod("removeBond",(Class[]) null);
						m.invoke(btdevice, (Object[]) null);
						PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage (String.format("Unpaired the device: %s", devicename)));

					}else
					{
						PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage((String.format("Device not paired: %s", devicename) )));
					}
					//break;
				}
			}

		}catch (Exception e) {
			PhoneStatusService.sendMessage(String.format("Failed to unpair the device"));
			PhoneStatusService.sendException(e, false);
		}
	}

	/* Set bluetooth discoverable to ON */
	public void DiscoverableON(){
		Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
		discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
		discoverableIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		mContext.startActivity(discoverableIntent);
		 //PhoneStatusService.parse_cmd_result =  PhoneStatusService.sendMessage (String.format("Switching ON discovery Mode"));
		PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage ("Switching ON discovery Mode"));
	
	}
	
	
	public boolean isEnabled(){
		return mBTAdapter.isEnabled();
		
	}
	public boolean isDiscovering(){;
		return mBTAdapter.isDiscovering();
	}

	public void enable(){
		mBTAdapter.enable();
	}

	public void disable(){
		mBTAdapter.disable();
		
	}

	public Set<BluetoothDevice> getBondedDevices(){
		return mBTAdapter.getBondedDevices();
	}

	public BluetoothDevice getRemoteDevice(String addr){
		return mBTAdapter.getRemoteDevice(addr);
	}
	
	public void acceptPair(){
		Intent intent = new Intent(BluetoothDevice.ACTION_PAIRING_REQUEST);

		BluetoothDevice device = intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
		try {
			device.getClass().getMethod("setPairingConfirmation", boolean.class).invoke(device, true);
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		
//		try {
//			
//			 classBluetoothPairDlg.forName("android.settings.bluetooth.BluetoothPairingDialog");
//			 Method cpair = classBluetoothPairDlg.getDeclaredMethod("onCancel");
//			 cpair.setAccessible(true);
//			System.out.println("CANCELED PAIR");
//		} catch (NoSuchMethodException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (ClassNotFoundException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		
//catch (IllegalArgumentException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} 
	}
	
	/**
	 * 
	 * */
	

	public void DiscoveryON(){
		Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
		discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0);
		discoverableIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		mContext.startActivity(discoverableIntent);
//		PhoneStatusService.sendMessage (String.format("Switching ON discovery Mode"));
	}
	
	
	/**
	 * 
	 * */
	
	public void cancelPair(){
		Intent intent = new Intent(BluetoothDevice.ACTION_PAIRING_REQUEST);

		BluetoothDevice device = intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
		try {
			device.getClass().getMethod("cancelPairingUserInput");
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}  catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void checkPairing(String name){
	/*Set<BluetoothDevice> devices = mBTAdapter.getBondedDevices();
		for (BluetoothDevice device :devices){
			if(device.getAddress().equalsIgnoreCase(addr)){
				PhoneStatusService.sendMessage("Device is already paired");
			}else{
				PhoneStatusService.sendMessage("Device is not paired");
			}
		}*/
		
		Set<BluetoothDevice> devices = mBTAdapter.getBondedDevices();
	       System.out.println("the devices size is"+devices.size());
	       if(devices.size() > 0){
	              for (BluetoothDevice device :devices){
	                     if(device.getName().equalsIgnoreCase(name)){
	                           System.out.println("device name is"+device.getName());
	                         PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage("Device is already paired"));
	                           
	                     }
	                     else{
	                    	PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage("Device is not paired"));
	                           
	                     }
	                     
	              }
	       }else{
	    	 PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage("No paired devices available"));
	             
	       }
		

		
	}
	
	public void getProfiles(){
		mBTAdapter.getProfileProxy(mContext, mProfileListner, BluetoothProfile.A2DP);
		mBTAdapter.getProfileProxy(mContext, mProfileListner, BluetoothProfile.HEADSET);
		mBTAdapter.getProfileProxy(mContext, mProfileListner, BluetoothProfile.HEALTH);
		
		
	
		
		
		mProfileListner = new ServiceListener() {
			public void onServiceConnected(int profile, BluetoothProfile proxy) {
				System.out.println("Check for servive connection");
			    if (profile == BluetoothProfile.HEADSET) {
			        mBluetoothHeadset = (BluetoothHeadset) proxy;
			       PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage("Connected device has Headset Profile"));
			    }
			    if(profile == BluetoothProfile.A2DP){
			    	mBluetoothAudio = (BluetoothA2dp) proxy;
			    	PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage("Connected device has Audio Profile"));
			    }
			    if(profile == BluetoothProfile.HEALTH){
			    	mBluetoothHealth = (BluetoothHealth) proxy;
			    	PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage("Connected device has Health Profile"));
			    }
			    
			    
			    if (mBTAdapter.isEnabled()) {
			    	mBTAdapter.cancelDiscovery();

				    Set<BluetoothDevice> devices = mBTAdapter.getBondedDevices();

			       BluetoothDevice device = (BluetoothDevice) devices.toArray()[1];
		   
			      
			    
			    
			}
		}
			public void onServiceDisconnected(int profile) {
				System.out.println("Check for no servive connection");
			    if (profile == BluetoothProfile.HEADSET) {
			        mBluetoothHeadset = null;
			       PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage("Connected device has no Headset Profile"));
			    }
			    if(profile == BluetoothProfile.A2DP){
			    	mBluetoothAudio = null;
			    	PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage("Connected device has no Audio Profile"));
			    }
			    if(profile == BluetoothProfile.HEALTH){
			    	mBluetoothHealth = null;
			    	PhoneStatusService.parse_cmd_result.add(PhoneStatusService.sendMessage("Connected device has no Health Profile"));
			    }
			}
			};
			
			

	}
	
}