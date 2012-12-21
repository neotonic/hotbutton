package com.example.hotbutton;


import java.net.Socket;


import com.example.hotbutton.R;
import com.example.hotbutton.jsonClient;
import android.app.Activity;
import android.os.Bundle;


import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends Activity {
	

	Socket client;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        final ImageView imageViewHotButton = (ImageView) findViewById(R.id.imageViewHotButton);
        jsonClient.imageViewHotButton = imageViewHotButton;
        
        final TextView textViewName = (TextView) findViewById(R.id.textViewName);
        jsonClient.textViewName = textViewName;
        
        final EditText editTextName = (EditText) findViewById(R.id.editTextName);
        jsonClient.editTextName = editTextName;
        
        final Button buttonRegister = (Button) findViewById(R.id.buttonRegister);
        jsonClient.buttonRegister = buttonRegister;
        
        final Button buttonConnect = (Button) findViewById(R.id.buttonConnect);
        jsonClient.buttonConnect = buttonConnect;
        
        final EditText editTextAddress = (EditText) findViewById(R.id.editTextAddress);
        jsonClient.editTextAddress = editTextAddress;
       
        final EditText editTextPort = (EditText) findViewById(R.id.editTextPort);
        jsonClient.editTextPort = editTextPort;
       
        
        
        // disable all widgets
        imageViewHotButton.setEnabled(false);
        imageViewHotButton.setAlpha(50);
        
        editTextName.setEnabled(false);
        buttonRegister.setEnabled(false);
        
        
        // connect to server
        buttonConnect.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				new jsonClient().execute("connect", 
										editTextAddress.getText().toString(),
										editTextPort.getText().toString());
				
			}
		});
        
        
        
        
        
        // login on server
        
        buttonRegister.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				new jsonClient().execute("play", editTextName.getText().toString());
				
			}
		});
        
        
        imageViewHotButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
            	new jsonClient().execute("buzz");
            	
            	
        
            	
                
                 // Toast.makeText(v.getContext(),
                   //     "YEAH BABY",
                     //   Toast.LENGTH_LONG).show();
                    
            }
        });
    }
}