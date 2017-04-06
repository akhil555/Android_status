package com.ford.androidphonestatus;

import java.io.File;
import java.io.IOException;


import android.app.Dialog;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.media.ToneGenerator;
import android.os.Environment;
import android.os.IBinder;
import android.os.ServiceManager;
import android.view.IWindowManager;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.widget.Toast;

public class EmergencyAssistance {

	private static Context mContext;
	private static EmergencyAssistance INSTANCE;
	MediaRecorder callrecorder = new MediaRecorder();
	File audiofile;
	private EmergencyAssistance(Context c){
		mContext =c;
	}

	public static EmergencyAssistance getInstance(Context c){
		if(INSTANCE ==null)
			INSTANCE = new EmergencyAssistance(c);
		return INSTANCE;

	}
	
	/**
	 * Starts recording the call conversation
	 * 
	 */

	public void startRecord() {

		try {
			audiofile = File.createTempFile("amrtmp", ".amr",Environment.getExternalStorageDirectory());
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		callrecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
		callrecorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
		callrecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
		callrecorder.setAudioEncodingBitRate(4750);
		callrecorder.setAudioSamplingRate(8000);
		callrecorder.setOutputFile(audiofile.getAbsolutePath());
		try {
			System.out.println("PREPARE");
			callrecorder.prepare();
			System.out.println("START");
			callrecorder.start();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {

			e.printStackTrace();
		}

	}

	/**
	 * Stops recording the call conversation
	 * 
	 */
	public void stopRecord() {

		callrecorder.stop();
		System.out.println("STOP");
		callrecorder.release();	
		System.out.println("RELEASE");
	}
	
	public void startDTMF0(){
		
		
		ToneGenerator toneGenerator = new
				ToneGenerator(AudioManager.STREAM_VOICE_CALL,
				ToneGenerator.MIN_VOLUME>>1);
						toneGenerator.startTone(ToneGenerator.TONE_DTMF_0);
						System.out.println("DTMF0 sent");
						toneGenerator.stopTone();

	}
	
	public void startDTMF1(){
		ToneGenerator toneGenerator = new
				ToneGenerator(AudioManager.STREAM_VOICE_CALL,
				ToneGenerator.MIN_VOLUME>>1);
						toneGenerator.startTone(ToneGenerator.TONE_DTMF_1);
						System.out.println("DTMF1 sent");
						toneGenerator.stopTone();

	}
	
	public void powerOff() throws IOException{
		Runtime.getRuntime().exec("adb shell sendevent /dev/input/event0 1 116 0");
		Runtime.getRuntime().exec("adb shell sendevent /dev/input/event0 1 116 1");
		
	}

	
}
