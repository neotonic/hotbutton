package com.example.hotbutton;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;


import com.example.hotbutton.R;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;


import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	
	
	Boolean loggedIn = false;
	//Socket client;
	clientLogin listener;
	public static Socket socket;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	 
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        
        // disable widgets
        final EditText editTextName = (EditText) findViewById(R.id.editTextName);
        Button buttonRegister = (Button) findViewById(R.id.buttonRegister);
        Button buttonConnect = (Button) findViewById(R.id.buttonConnect);
        
        final EditText editTextAddress = (EditText) findViewById(R.id.editTextAddress);
        final EditText editTextPort = (EditText) findViewById(R.id.editTextPort);

      
        editTextName.setEnabled(false);
        buttonRegister.setEnabled(false);
        
        
        // connect to server
        buttonConnect.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				new clientConnect().execute(editTextAddress.getText().toString(),
											editTextPort.getText().toString());
			}
		});
        
        // login on server
        buttonRegister.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				listener = new clientLogin();
				listener.execute(editTextName.getText().toString());
			}
		});
        
        
        // exit
        Button buttonExit = (Button) findViewById(R.id.buttonExit);
        buttonExit.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				android.os.Process.killProcess(android.os.Process.myPid());
			}
		});
    }
    
    public class clientConnect extends AsyncTask<String, String, String> {
    	
    	@Override
    	protected void onPreExecute() {
    		
    		// disable connection widgets
    		
            Button buttonConnect = (Button) findViewById(R.id.buttonConnect);
            buttonConnect.setEnabled(false);
            
			EditText editTextAddress = (EditText) findViewById(R.id.editTextAddress);
			editTextAddress.setEnabled(false);
			
			EditText editTextPort = (EditText) findViewById(R.id.editTextPort);
			editTextPort.setEnabled(false);
    	}
    	
		@Override
		protected String doInBackground(String... params) {
			
			String address = params[0];
			Integer port = Integer.parseInt(params[1]);
			
			
			try {
				
				Log.d("SOCKET", "Connect to: " + address + ":" + port);
				
				MainActivity.socket = new Socket();
				MainActivity.socket.connect(new InetSocketAddress(address, port), 1000);

				MainActivity.socket.setSoTimeout(1000);
				
								
			} catch(UnknownHostException e) {
         		Log.d("Exception", "Unknown host: " + address);

         	} catch(IOException e) {
         		Log.d("Exception", "No I/O : " + e.toString());
         	}
			
			
			return null;
		}
		

		protected void onPostExecute (String arg) {
			
			Log.d("clientConnect:onPostExecute", "client.isConnected() == " + MainActivity.socket.isConnected());
			
			if(MainActivity.socket.isConnected())
			{
				// enable login widgets
	    		
				EditText editTextName =  (EditText) findViewById(R.id.editTextName);
	            editTextName.setEnabled(true);
	            
	            Button buttonRegister = (Button) findViewById(R.id.buttonRegister);
	            buttonRegister.setEnabled(true);
	            
			}
			else
			{
				// enable connection widgets
				
	            Button buttonConnect = (Button) findViewById(R.id.buttonConnect);
	            buttonConnect.setEnabled(true);
	             
				EditText editTextAddress = (EditText) findViewById(R.id.editTextAddress);
				editTextAddress.setEnabled(true);
				
				EditText editTextPort = (EditText) findViewById(R.id.editTextPort);
				editTextPort.setEnabled(true);
				
				Toast.makeText(MainActivity.this, "Verbindung fehlgeschlagen.", Toast.LENGTH_SHORT).show();
			}
		}
    }
    
    
    public class clientLogin extends AsyncTask<String, String, String> {
    	
    	PrintWriter out;
    	BufferedReader in;
    	
    	
    	public Boolean listen = false;
    	
    	protected String doInBackground(String... params) {
			
    		
    		listen = true;
    		
			try {
				
				
				out = new PrintWriter(MainActivity.socket.getOutputStream(),true);
				in = new BufferedReader(new InputStreamReader(MainActivity.socket.getInputStream()));
	    		
				
				if(!loggedIn) {
					
					// send loginname to server
					String name = params[0];
					
					Log.d("CLIENT", "Try login as: " + name);
					out.println("login-" + name);
				}

				
				
				// start listen server
				while(listen)
				{
					try {
					
						String line = in.readLine();
						
						Log.d("SERVER SAYS", line);
						
						publishProgress(line);
					
					} catch (IOException e) {
						// TODO Auto-generated catch block
						//e.printStackTrace();
					} catch (Exception e) {

						android.os.Process.killProcess(android.os.Process.myPid());
					}
				}
				
				Log.d("clientLoginAndPlay", "Stop listen");
				
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
			}
    		
					
			return null;
		}

		protected void onProgressUpdate(String... status) {

			     
			TextView textViewName = (TextView) findViewById(R.id.textViewName);
			EditText editTextName = (EditText) findViewById(R.id.editTextName);
			Button buttonRegister = (Button) findViewById(R.id.buttonRegister);
			
        	
        	if(status[0].startsWith("login-fail") || status[0].equals("kick"))
        	{
        		textViewName.setText("Name: ");
        		
        		editTextName.setEnabled(true);
        		buttonRegister.setEnabled(true);
        		
                loggedIn = false;
        		
        		return;
        	}
        	
        	if(status[0].startsWith("login-okay"))
        	{ 
        		textViewName.setText("Eingelogt als " + editTextName.getText() + ".");
        		
        		editTextName.setEnabled(false);
        		buttonRegister.setEnabled(false);
        		
        		listen = false;
        		loggedIn = true;
        		
        		startActivity(new Intent(MainActivity.this, ButtonActivity.class));
        		
        		return;
        	}
    	}
    }
    public class clientBuzz extends AsyncTask<String, String, String> {

		@Override
		protected String doInBackground(String... params) {
			

			Log.d("clientBuzz", "BUZZ");
			
			PrintWriter out;
			try {
				out = new PrintWriter(MainActivity.socket.getOutputStream(),true);
				
				out.println("buzz!");
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
			return null;
		}
		protected void onPostExecute (String arg) {
			
			listener = new clientLogin();
			listener.execute(((EditText)findViewById(R.id.editTextName)).getText().toString());
		}
    }
}