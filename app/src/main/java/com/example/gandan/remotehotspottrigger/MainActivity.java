package com.example.gandan.remotehotspottrigger;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private Set<BluetoothDevice> pairedDevices;
    private AcceptThread mAcceptThread;
    private ConnectedThread mConnectedThread;
    private final String NAME = "ESLAB";
    private final UUID MY_UUID = UUID.fromString("c6bd7060-295d-11e6-bdf4-0800200c9a66");
    Button b1;
    private int check;

    boolean wasAPEnabled = false;
    static WifiAP wifiAp;
    private WifiManager wifi;
    static Button btnWifiToggle;
    private static final String TAG = "Tizen-RHT";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnWifiToggle = (Button) findViewById(R.id.button);
        b1 = (Button) findViewById(R.id.button2);




        wifiAp = new WifiAP();
        wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        btnWifiToggle.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                wifiAp.toggleWiFiAP(wifi, MainActivity.this);



            }
        });

        updateStatusDisplay();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD|WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON|WindowManager.LayoutParams.FLAG_DIM_BEHIND);


    }

    public void turnOn(){
        wifiAp.toggleWiFiAP(wifi, MainActivity.this);
    }


    public void connect(View v){
        Toast.makeText(getApplicationContext(), "Making accepting socket", Toast.LENGTH_LONG).show();

        Log.d(TAG, "Connect button clicked");
        mAcceptThread = new AcceptThread();
        mAcceptThread.run();

        Log.d(TAG, "aaaaa");
        turnOn();




    }

    @Override
    public void onResume() {
        super.onResume();
        /*
        if (wasAPEnabled) {
            if (wifiAp.getWifiAPState()!=wifiAp.WIFI_AP_STATE_ENABLED && wifiAp.getWifiAPState()!=wifiAp.WIFI_AP_STATE_ENABLING){
                wifiAp.toggleWiFiAP(wifi, MainActivity.this);
            }
        }
        updateStatusDisplay();
        */
    }

    @Override
    public void onPause() {
        super.onPause();
        /*
        boolean wifiApIsOn = wifiAp.getWifiAPState()==wifiAp.WIFI_AP_STATE_ENABLED || wifiAp.getWifiAPState()==wifiAp.WIFI_AP_STATE_ENABLING;
        if (wifiApIsOn) {
            wasAPEnabled = true;
            wifiAp.toggleWiFiAP(wifi, MainActivity.this);
        } else {
            wasAPEnabled = false;
        }
        updateStatusDisplay();
        */
    }

    public static void updateStatusDisplay() {

        if (wifiAp.getWifiAPState()==wifiAp.WIFI_AP_STATE_ENABLED || wifiAp.getWifiAPState()==wifiAp.WIFI_AP_STATE_ENABLING) {
            btnWifiToggle.setText("ON");
            //findViewById(R.id.bg).setBackgroundResource(R.drawable.bg_wifi_on);
        } else {
            btnWifiToggle.setText("OFF");
            //findViewById(R.id.bg).setBackgroundResource(R.drawable.bg_wifi_off);
        }

    }


    private class AcceptThread extends Thread {
        private BluetoothServerSocket mmServerSocket =null;

        public AcceptThread(){

            Log.d(TAG, "Accept thread is being created");
            // Use a temporary object that is later assigned to mmServerSocket.
            // because mmServerSocket is final
            BluetoothServerSocket tmp = null;

            try{
                // MY_UUID is the app's UUID string. also used by the client <code>
                tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
            } catch (IOException e){
                Log.d(TAG, "exception occured: " + e);
            }
            try {
                mmServerSocket = tmp;
            } catch(Exception e){
                Log.d(TAG, "Exception!: " + e);
            }
            Log.d(TAG, "Accept thread is created");
        }

        public void run(){
            Log.d(TAG, "Start to acceptThread");

            //Toast.makeText(getApplicationContext(), "Accept starts", Toast.LENGTH_SHORT).show();
            BluetoothSocket socket = null;
            // Keep listening until exception occurs or a socket is returned
            while(true){
                try{
                    socket = mmServerSocket.accept();
                    Log.d(TAG, "Socket Accepted!");
                } catch (IOException e) {
                    break;
                }

                // If a connection was accepted
                if(socket != null) {
                    // Do work to manage the connection (in a separate thread)
                    mConnectedThread = new ConnectedThread(socket);
                    mConnectedThread.run();
                    String str = new String("AndroidAP");
                    byte[] write_byte = new byte[100];

                    Log.d(TAG, "Let's write to Socket!");

                    write_byte = str.getBytes();
                    mConnectedThread.write(write_byte);


                    try{
                        mmServerSocket.close();
                    } catch (IOException closeException) { }


                    Log.d(TAG, "Socket is closed");
                    //break;
                }

            }

        }

        // Will cancel the listening socket. and cause the thread to finish

        public void cancel(){
            try{
                mmServerSocket.close();
            } catch (IOException e) { }
        }

    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        WifiAP mWifiAP;
        private WifiManager wifi;

        public ConnectedThread(BluetoothSocket socket) {
            Log.d(TAG, "Connect Thread is created");
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;


            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            Log.d(TAG, "Connect Thread starts");
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                    try {
                        // Read from the InputStream
                        bytes = mmInStream.read(buffer);

                        Log.d(TAG, "Reading Socket: " + bytes + "readed");
                        Log.d(TAG, "Content: " + buffer);

                        // Send the obtained bytes to the UI activity

                    /*
                    Intent toMainActivity = new Intent("com.exmaple.gandan.bluetooth_service.TurnOnAP");
                    sendBroadcast(toMainActivity);
                    */
                    /*
                    mWifiAP = new WifiAP();
                    wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);

                    mWifiAP.toggleWiFiAP(wifi, MainActivity.mContext);
                    */

                        // hahahahahaahahhahah





                    /*
                    mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
                            .sendToTarget();

                    */
                    break;

                } catch (IOException e) {
                    break;
                }
            }
            wifiAp.toggleWiFiAP(wifi, MainActivity.this);
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) { }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }


}
