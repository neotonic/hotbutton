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
	Socket client;
	clientLoginAndPlay listener;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	
    	 
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        ImageView imageViewHotButton = (ImageView) findViewById(R.id.imageViewHotButton);
        TextView textViewName = (TextView) findViewById(R.id.textViewName);
        final EditText editTextName = (EditText) findViewById(R.id.editTextName);
        Button buttonRegister = (Button) findViewById(R.id.buttonRegister);
        Button buttonConnect = (Button) findViewById(R.id.buttonConnect);
        
        final EditText editTextAddress = (EditText) findViewById(R.id.editTextAddress);
        final EditText editTextPort = (EditText) findViewById(R.id.editTextPort);

        
        
        
        
        // disable all widgets
        imageViewHotButton.setEnabled(false);
        imageViewHotButton.setAlpha(50);
        
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
				
				listener = new clientLoginAndPlay();
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
        
        
        // buzz
        imageViewHotButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
            	
            	Toast.makeText(v.getContext(), "buzz", Toast.LENGTH_SHORT).show();
      
            	listener.listening = false;
            	new clientBuzz().execute();
            	
            	      
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
				
				client = new Socket();
				
				client.connect(new InetSocketAddress(address, port), 1000);

				client.setSoTimeout(1000);
				
				PrintWriter out = new PrintWriter(client.getOutputStream(),true);
				
				Log.d("SOCKET", "Maybe connected to: " + address + ":" + port);
				
				// say hi to server
				out.println("hi");
				
				
			} catch(UnknownHostException e) {
         		Log.d("Exception", "Unknown host: " + address);

         	} catch(IOException e) {
         		Log.d("Exception", "No I/O : " + e.toString());
         	}
			
			
			return null;
		}
		

		protected void onPostExecute (String arg) {
			
			Log.d("clientConnect:onPostExecute", "client.isConnected() == " + client.isConnected());
			
			if(client.isConnected())
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
				
			}
		}
    }
    
    
    public class clientLoginAndPlay extends AsyncTask<String, String, String> {
    	
    	PrintWriter out;
    	BufferedReader in;
    	
    	
    	public Boolean listening = false;
    	
    	protected String doInBackground(String... params) {
			
    		
    		listening = true;
    		
			try {
				
				
				out = new PrintWriter(client.getOutputStream(),true);
				in = new BufferedReader(new InputStreamReader(client.getInputStream()));
	    		
				
				if(!loggedIn) {
					// send loginname to server
					String name = params[0];
					
					Log.d("CLIENT", "Try login as: " + name);
					out.println("login-" + name);
				}
				
				
				
				while(listening)
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
				
				Log.d("clientLoginAndPlay", "Stop listening");
				
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
			}
    		
					
			return null;
		}

		protected void onProgressUpdate(String... status) {
    		
			ImageView imageViewHotButton = (ImageView) findViewById(R.id.imageViewHotButton);
			     
			TextView textViewName = (TextView) findViewById(R.id.textViewName);
			EditText editTextName = (EditText) findViewById(R.id.editTextName);
			Button buttonRegister = (Button) findViewById(R.id.buttonRegister);

          
        	
        	if(status[0].startsWith("login-fail") || status[0].equals("kick"))
        	{
        		textViewName.setText("Name: ");
        		
        		editTextName.setAlpha(255);
        		buttonRegister.setAlpha(255);
        		
        		editTextName.setEnabled(true);
        		buttonRegister.setEnabled(true);
        		
                imageViewHotButton.setEnabled(false);
                imageViewHotButton.setAlpha(50);
        		
                loggedIn = false;
        		
        		return;
        	}
        	
        	if(status[0].startsWith("login-okay"))
        	{ 
        		textViewName.setText("Eingelogt als " + editTextName.getText() + ".");
        		
        		editTextName.setAlpha(0);
        		buttonRegister.setAlpha(0);
        		
        		editTextName.setEnabled(false);
        		buttonRegister.setEnabled(false);
        		
        		loggedIn = true;
        		
        		return;
        	}
        	
        	if(status[0].startsWith("lock") && loggedIn)
        	{
                imageViewHotButton.setEnabled(false);
                imageViewHotButton.setAlpha(50);	
                
                return;
        	}
        	
        	if(status[0].startsWith("unlock") && loggedIn)
        	{
                imageViewHotButton.setEnabled(true);
                imageViewHotButton.setAlpha(255);
                
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
				out = new PrintWriter(client.getOutputStream(),true);
				
				out.println("buzz!");
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
			return null;
		}
		protected void onPostExecute (String arg) {
			
			listener = new clientLoginAndPlay();
			listener.execute(((EditText)findViewById(R.id.editTextName)).getText().toString());
		}
    }
}