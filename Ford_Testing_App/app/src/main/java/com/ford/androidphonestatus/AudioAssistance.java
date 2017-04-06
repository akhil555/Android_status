package com.ford.androidphonestatus;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import android.content.Context;

import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Environment;
import android.widget.Button;


public class AudioAssistance {

	private static AudioAssistance INSTANCE;
	@SuppressWarnings("unused")
	private static Context mContext;
	MediaPlayer mp = new MediaPlayer();
	Button play,repeatc,shuffle,stop,repeatn,repeata,playl,next,volmax,volmin;
	 final String MEDIA_PATH = new String(Environment.getExternalStorageDirectory()+"/"); 
	 ArrayList<HashMap<String, String>> songsList = new ArrayList<HashMap<String, String>>();
	 private boolean isRepeat = true;
	 private int currentIndex;
	 private boolean isShuffle = false;
	 private boolean playall = false;
	private AudioAssistance(Context c){
		mContext = c;

	}
	public static AudioAssistance getInstance(Context c){
		if(INSTANCE == null)

			INSTANCE = new AudioAssistance(c);
		return INSTANCE;
	}

	public ArrayList<HashMap<String, String>> getPlayList(){
		 File home = new File(MEDIA_PATH);
		 System.out.println("no of mp3 files is"+home.listFiles(new FileExtensionFilter()).length);
		 if(home.listFiles(new FileExtensionFilter()).length > 0){
			 for(File file :home.listFiles(new FileExtensionFilter())){
				 HashMap<String, String> song = new HashMap<String, String>(); 
				 song.put("songTitle", file.getName().substring(0, (file.getName().length() - 4))); 
				 song.put("songPath", file.getPath()); 
				 songsList.add(song); 
			 }
			 
		 }else{
			 PhoneStatusService.sendMessage("Please place some songs on sdcard");
		 }
		return songsList;
		
	}
	public class FileExtensionFilter implements FilenameFilter {

		@Override
		public boolean accept(File dir, String name) {
			// TODO Auto-generated method stub
			return (name.endsWith(".mp3") || name.endsWith(".MP3")); 
		}

	}
	
	
	public void playList(){
		
		//System.out.println("currentindex is"+currentIndex);
		playSong(currentIndex);
		mp.setOnCompletionListener(new OnCompletionListener() {
			
			@Override
			public void onCompletion(MediaPlayer mp) {
				//System.out.println("completed");
			//	playall = true;
				nextSong();				
			}

			
		});
		
	}
	public void nextSong() {
	    if (currentIndex <(songsList.size()-1)) {
	    	playSong(currentIndex+1);
	    	
	    	currentIndex = currentIndex +1;
	  //  	Toast.makeText(this, "current index is"+currentIndex, Toast.LENGTH_LONG).show();
	} else {
	  
	        playSong(0);
	        currentIndex = 0;
	 }
	}
	
	
	public void playSong(int songIndex){
	
		mp.reset();
		try {
			System.out.println("file path is"+songsList.get(0).get("songPath"));
			mp.setDataSource(songsList.get(songIndex).get("songPath"));
			mp.prepare();
			mp.start();
			
			System.out.println("playing current song");
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public void repeatCurrentSong(){
		
		
		mp.setOnCompletionListener(new OnCompletionListener() {
			
			@Override
			public void onCompletion(MediaPlayer arg0) {
				mp.setLooping(true);
				playSong(currentIndex);		
				System.out.println("playing surrent song");
			}
		});
		
	}
	
	public void repeatPlayList(){
	//	Toast.makeText(this, "current index for repeat play list is"+currentIndex, Toast.LENGTH_LONG).show();
		
		mp.setOnCompletionListener(new OnCompletionListener() {
			
			@Override
			public void onCompletion(MediaPlayer mp) {
				if(currentIndex == (songsList.size()-1)){
				if(currentIndex > 0)
				{
					
					System.out.println("entered repeat all");
//					for(int i=songsList.size();i<songsList.size();i--){
//						playSong(songsList.size() - 1);
//		                currentIndex = songsList.size() - 1;
//					}
					System.out.println("the songlist size is"+songsList.size());
					currentIndex = 0;
					System.out.println("the current index  is"+currentIndex);
					playList();
//for(int i=0;i<songsList.size();i++){

//	playSong(i);
//	mp.setOnCompletionListener(new OnCompletionListener() {
//		
//		@Override
//		public void onCompletion(MediaPlayer mp) {
//			
//			
//		}
//	});

//	i++;
//	System.out.println("the repeat index after is"+i);
//}
			  
					
				}
				else{
					playSong(currentIndex - (songsList.size()-1));
					currentIndex = currentIndex -(songsList.size()-1);
					nextSong();
			
				}
				
				}	else{
					System.out.println("repeating next song");
					nextSong();
					}
				}
		});
		
		
	}
	
	public void repeatNone(){
		mp.setLooping(false);
	}
	
	public void shuffleSong(){
		if(isShuffle){
		Random random = new Random();
		currentIndex = random.nextInt((songsList.size()-1) - 0 +1) + 0;
		System.out.println("currentIndex is"+currentIndex);
		playSong(currentIndex);}else{
			if(currentIndex < (songsList.size() - 1)){
			      playSong(currentIndex + 1); 
			      currentIndex = currentIndex + 1; 
			      }else{ // play first song //
			      playSong(0); currentIndex = 0; } }
		}
		
		
	
	public void stopSong(){
		mp.stop();
	}
	
	
}
