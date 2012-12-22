package com.example.hotbutton;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;


import com.example.hotbutton.R;
import com.example.hotbutton.jsonClient;
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
	
	
	
	Socket client;
	
	
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

       
        
        final clientLoginAndPlay listener = new clientLoginAndPlay();
        
        // disable all widgets
        //imageViewHotButton.setEnabled(false);
        //imageViewHotButton.setAlpha(50);
        
        editTextName.setEnabled(false);
        buttonRegister.setEnabled(false);
        
        
        // connect to server
        buttonConnect.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				new clientConnect().execute(editTextAddress.getText().toString(),
											editTextPort.getText().toString());
				
				/*
				new jsonClient().execute("connect", 
										editTextAddress.getText().toString(),
										editTextPort.getText().toString());
				*/
			}
		});
        
        // login on server
        
        buttonRegister.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				listener.execute(editTextName.getText().toString());
				//new jsonClient().execute("play", editTextName.getText().toString());
				
			}
		});
        
        imageViewHotButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
            	
            	Toast.makeText(v.getContext(), "asd", Toast.LENGTH_SHORT).show();
            	
            	listener.listening = false;
            	listener.stop();
            	try {
					listener.wait(100000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            	
            	new clientBuzz().execute();
            	      
            }
        });
    }
    
    public class clientConnect extends AsyncTask<String, String, String> {
    	
    	
		@Override
		protected String doInBackground(String... params) {
			
			String address = params[0];
			Integer port = Integer.parseInt(params[1]);
			
			do {
				
				try {
					
					Log.d("client", "Connect to: " + address + ":" + port);
					
					client = new Socket();
					
					client.connect(new InetSocketAddress(address, port));
					
					
					PrintWriter out = new PrintWriter(client.getOutputStream(),true);
					
					// say hi to server
					out.println("hi");
					
					publishProgress("connected");
					
				} catch(UnknownHostException e) {
	         		Log.d("Exception", "Unknown host: " + address);

	         	} catch(IOException e) {
	         		Log.d("Exception", "No I/O : " + e.toString());
	         	}
				
				// sleep if connection failed
				if(!client.isConnected()) {
					
					try {
						
						Thread.sleep(1000);
						
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
				}
			
			}while(!client.isConnected());
			
			return null;
		}
		

		protected void onPostExecute (String arg) {
			
			EditText editTextName =  (EditText) findViewById(R.id.editTextName);
            editTextName.setEnabled(true);
            
            Button buttonRegister = (Button) findViewById(R.id.buttonRegister);
            buttonRegister.setEnabled(true);
            
            Button buttonConnect = (Button) findViewById(R.id.buttonConnect);
            buttonConnect.setEnabled(false);
            
			EditText editTextAddress = (EditText) findViewById(R.id.editTextAddress);
			editTextAddress.setEnabled(false);
			
			EditText editTextPort = (EditText) findViewById(R.id.editTextPort);
			editTextPort.setEnabled(false);
            
		}
    	
    	
    }
    
    
    public class clientLoginAndPlay extends AsyncTask<String, String, String> {
    	
    	PrintWriter out;
    	BufferedReader in;
    	
    	Boolean loginOkay = false;
    	public Boolean listening = false;
    	
    	protected String doInBackground(String... params) {
			
    		String name = params[0];
    		
    		Log.d("CLIENT", "Try login as: " + name);
    		
    		listening = true;
    		
			try {
				
				
				out = new PrintWriter(client.getOutputStream(),true);
				in = new BufferedReader(new InputStreamReader(client.getInputStream()));
	    		
				
				if(!loginOkay) {
					// send loginname to server
					out.println("login-" + name);
				}
				
				
				
				while(listening)
				{
					String line = in.readLine();
					
					// debug
					Log.d("SERVER SAYS", line);
					
					publishProgress(line);
					
				}
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
			}
    		
					
			return null;
		}
    	
    	public void stop() {
			
    		listening = false;
		}

		protected void onProgressUpdate(String... status) {
    		
			ImageView imageViewHotButton = (ImageView) findViewById(R.id.imageViewHotButton);
			     
			TextView textViewName = (TextView) findViewById(R.id.textViewName);
			EditText editTextName = (EditText) findViewById(R.id.editTextName);
			    
			Button buttonRegister = (Button) findViewById(R.id.buttonRegister);
			Button buttonConnect = (Button) findViewById(R.id.buttonConnect);
			    
			EditText editTextAddress = (EditText) findViewById(R.id.editTextAddress);
			   
			EditText editTextPort = (EditText) findViewById(R.id.editTextPort);
          
        	
        	if(status[0].equals("login-fail") || status[0].equals("kick"))
        	{
        		textViewName.setText("Name: ");
        		
        		//editTextName.setAlpha(255);
        		//buttonRegister.setAlpha(255);
        		
        		editTextName.setEnabled(true);
        		buttonRegister.setEnabled(true);
        		
                imageViewHotButton.setEnabled(false);
                imageViewHotButton.setAlpha(50);
        		
                loginOkay = false;
        		
        		return;
        	}
        	
        	if(status[0].equals("login-okay"))
        	{
        		textViewName.setText("Eingelogt als " + editTextName.getText() + ".");
        		
        		//editTextName.setAlpha(0);
        		//buttonRegister.setAlpha(0);
        		
        		editTextName.setEnabled(false);
        		buttonRegister.setEnabled(false);
        		
        		loginOkay = true;
        		
        		return;
        	}
        	
        	if(status[0].equals("lock") && loginOkay)
        	{
                imageViewHotButton.setEnabled(false);
                imageViewHotButton.setAlpha(50);	
                
                return;
        	}
        	
        	if(status[0].equals("unlock") && loginOkay)
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
    	
    	
    }
}