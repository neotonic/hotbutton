package com.example.hotbutton;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;

import android.content.Context;
import android.database.CursorJoiner.Result;
import android.os.AsyncTask;
import android.sax.TextElementListener;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import android.net.Uri;

import android.os.Bundle;

import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;



import android.view.Menu;




import android.app.Activity;

public class jsonClient extends AsyncTask<String, String, String> {

	/*
	static String url = "131.234.254.40";
	static Integer port = 31337;
	*/
	static String url = "131.234.254.41";
	static Integer port = 8080;
	
	public static Socket client = new Socket();
    
	private Boolean isLoginOkay = false;
	
	
	static PrintWriter out;
	static BufferedReader in;
	
	static Boolean isRegistered = false;
	
	public static ImageView imageViewHotButton;
	public static EditText editTextName;
	public static Button buttonRegister;
	public static TextView textViewName;
	public static Button buttonConnect;
	public static EditText editTextAddress;
	public static EditText editTextPort;
	
	@Override
	protected String doInBackground(String... params) {
		
		
		
		
		// connect to server if not connected
		if(params[0].equals("connect") && params[1] != null) {
			
			String address = params[1];
			Integer port = Integer.parseInt(params[2]);
			
			
			connect(address, port);
		}
		
		if(params[0].equals("play") && params[1] != null) {
					
			play(params[1]);
		}
		
		return null;
	}
	

	protected void onPostExecute (String arg) {
		

	}
	
	
	/*
	 * connect - connecting to server
	 */
	
	private void connect(String address, Integer port) {
		
		do {
			
			try {
				
				Log.d("client", "Connect to: " + address + ":" + port);
				
				client = new Socket();
				
				client.connect(new InetSocketAddress(address, port));
				
				
				out = new PrintWriter(client.getOutputStream(),true);
				in = new BufferedReader(new InputStreamReader(client.getInputStream()));
	        
				// say hi to server
				out.println("hi");
				
				publishProgress("connected");
				
//
			} catch(UnknownHostException e) {
         		Log.d("Exception", "Unknown host: " + url);

         	} catch(IOException e) {
         		Log.d("Exception", "No I/O : " + e.toString());
         	}
			
			
			if(!client.isConnected()) {
				
				try {
					
					Thread.sleep(1000);
					
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
		
		}while(!client.isConnected());
		
	}
	
	
	/*
	 * login - try login with typed username
	 */
	
	private void play(String name) {
		
		//Log.d("play(String name)", "name: " + name);
		
		if(!client.isConnected() || name.equals(""))	
			return;
	
		
		Log.d("CLIENT", "Try login as: " + name);
		
		// send loginname to server
		out.println("login-" + name);
		
		while(true)
		{
			
			try {
				String line = in.readLine();
				
				// debug
				Log.d("SERVER SAYS", line);
				
				publishProgress(line);
			
				
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
			}
		}
	}
	
	
    protected void onProgressUpdate(String... status) {
        
    	
    	if(status[0].equals("connected"))
    	{	
            editTextName.setEnabled(true);
            buttonRegister.setEnabled(true);
            
            return;
    	}
    	
    	if(status[0].equals("login-fail") || status[0].equals("kick"))
    	{
    		textViewName.setText("Name: ");
    		
    		editTextName.setAlpha(255);
    		buttonRegister.setAlpha(255);
    		
    		editTextName.setEnabled(true);
    		buttonRegister.setEnabled(true);
    		
            imageViewHotButton.setEnabled(false);
            imageViewHotButton.setAlpha(50);
    		
    		isLoginOkay = false;
    		
    		return;
    	}
    	
    	if(status[0].equals("login-okay-"))
    	{
    		textViewName.setText("Eingelogt als " + editTextName.getText() + ".");
    		
    		editTextName.setAlpha(0);
    		buttonRegister.setAlpha(0);
    		
    		editTextName.setEnabled(false);
    		buttonRegister.setEnabled(false);
    		
    		isLoginOkay = true;
    		
    		return;
    	}
    	
    	if(status[0].equals("lock") && isLoginOkay)
    	{
            imageViewHotButton.setEnabled(false);
            imageViewHotButton.setAlpha(50);	
            
            return;
    	}
    	
    	if(status[0].equals("unlock") && isLoginOkay)
    	{
            imageViewHotButton.setEnabled(true);
            imageViewHotButton.setAlpha(255);
            
            return;
    	}
    }
}
