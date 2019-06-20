package com.example.jinha.myapplication;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;


/**
 * A simple {@link Fragment} subclass.
 */
public class BluetoothFragment extends AppCompatActivity {
    // Layout
    private TextView textBluetoothState, textBluetoothDevice, textBluetoothAddress, textWifiSSID, textInfoWlan;
    private EditText editWifiPassword;
    private Button buttonBluetoothConnect, buttonBluetoothDisconnect, buttonWifiSearch, buttonWifiConnect, btnBluetoothList;
    private ArrayAdapter<String> WifiList;
    // Bluetooth Basic Component
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket mSocket;
    private BluetoothDevice mDevice;
    private boolean workFlag = false;
    private final UUID mUUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    // Bluetooth Connect Dialog Related
    private ArrayAdapter<String> arrayAdapterDevice;
    private List<BluetoothDevice> listDevice;
    // Bluetooth Client Input and Output Stream Related
    private ConnectThread connectThread;
    private ConnectedThread workThread;
    private OutputStream mOutputStream;
    private InputStream mInputStream;
    private boolean commandFlag = true;
    private String lastSend = "";

    public BluetoothFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_bluetooth);

        // Bluetooth Basic Setting
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(bluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "This device is not capable to Bluetooth!", Toast.LENGTH_SHORT).show();
            finish();
        }
        if(bluetoothAdapter.isEnabled()) {
            if (bluetoothAdapter.isDiscovering()) {
                Toast.makeText(getApplicationContext(), "Bluetooth is currently in device discovery process.", Toast.LENGTH_SHORT).show();
            }
        } else {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
        }

        // Bluetooth Dialog Related
        arrayAdapterDevice = new ArrayAdapter<String>(this, android.R.layout.select_dialog_singlechoice);
        listDevice = new ArrayList<>();
        WifiList = new ArrayAdapter<String>(this, android.R.layout.select_dialog_singlechoice);

        // Layout Setting
        textBluetoothState = (TextView)findViewById(R.id.txtBtState);
        textBluetoothDevice = (TextView)findViewById(R.id.txtBtDevice);
        textBluetoothAddress = (TextView)findViewById(R.id.txtBtAddress);
        textWifiSSID = (TextView)findViewById(R.id.txtWifiID);
        textInfoWlan = (TextView)findViewById(R.id.txtInfoWlan);
        editWifiPassword = (EditText)findViewById(R.id.editWifiPw);
        buttonBluetoothConnect = (Button)findViewById(R.id.btnBtConnect);
        buttonBluetoothDisconnect = (Button)findViewById(R.id.btnBtDisconnect);
        buttonWifiSearch = (Button)findViewById(R.id.btnWifiSearch);
        buttonWifiConnect = (Button)findViewById(R.id.btnWifiConnect);
        btnBluetoothList = (Button)findViewById(R.id.btnBtList);

        // Register Intent, Broadcast Receiver
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothDevice.ACTION_UUID);
        registerReceiver(BluetoothStateReceiver, filter);

        // Button Click Event
        buttonBluetoothConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mDevice != null) {
                    PairAndConnectThread pairAndConnectThread = new PairAndConnectThread(mDevice);
                    pairAndConnectThread.start();
                } else {
                    ToastMsg("Select paired device!");
                }
            }
        });
        buttonBluetoothDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InitConnect();
            }
        });
        buttonWifiSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!workFlag) {
                    ToastMsg("Connection required!");
                } else {
                    //SendCommand("L~E~E");

                    byte[] bytes = "L~E~E".getBytes();
                    try {
                        mOutputStream.write(bytes);
                        mOutputStream.flush();
                    } catch(Exception e) {
                    }


                    WifiList.clear();
                    WifiList.notifyDataSetChanged();

                    CreateWifiListDialog();
                }
            }
        });
        buttonWifiConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String strSSID = textWifiSSID.getText().toString();
                String strPassword = editWifiPassword.getText().toString();
                if(strSSID.equals("")) {
                    ToastMsg("Choose Wi-Fi First!");
                    return;
                }
                if(strPassword.equals("")) {
                    String comSSID = "S~" + strSSID + "~E";
                    String comPassword = "P~~E";
                    String comConnect = "C~E~E";
                    SendCommand(comSSID);
                    SendCommand(comPassword);
                    SendCommand(comConnect);
                }
                else {
                    String comSSID = "S~" + strSSID + "~E";
                    String comPassword = "P~" + strPassword + "~E";
                    String comConnect = "C~E~E";
                    SendCommand(comSSID);
                    SendCommand(comPassword);
                    SendCommand(comConnect);
                }
            }
        });
        btnBluetoothList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InitConnect(); // Bluetooth Connection Initialization
                CreateListDialog();
            }
        });
    }

    public class PairAndConnectThread extends Thread {
        private BluetoothDevice pairingDevice;

        public PairAndConnectThread(BluetoothDevice item) {
            pairingDevice = item;
        }

        @Override
        public void run() {
            long infoReqStart = System.currentTimeMillis(), infoReqEnd, infoReqPeriod;
            int timeout = 0;

            while(timeout < 10) {
                infoReqEnd = System.currentTimeMillis();
                infoReqPeriod = infoReqEnd - infoReqStart;
                if(infoReqPeriod > 1000) {
                    timeout++;
                    infoReqStart = System.currentTimeMillis();

                    Set<BluetoothDevice> devices = bluetoothAdapter.getBondedDevices();
                    int devicesCount = devices.size();
                    boolean pairedFlag = false;
                    if(devicesCount > 0) {
                        for(BluetoothDevice item : devices) {
                            if(pairingDevice.getAddress().equals(item.getAddress())) {
                                pairedFlag = true;
                            }
                        }
                    }

                    if(pairedFlag) {
                        connectThread = new ConnectThread();
                        connectThread.start();
                        break;
                    } else {
                        pairingDevice.createBond();
                    }
                }
            }
        }
    }

    public boolean SendCommand(String command) {
        if(workFlag) {
            lastSend = command;
            byte[] bytes = command.getBytes();
            try {
                mOutputStream.write(bytes);
                mOutputStream.flush();
                commandFlag = false;
                long startTime = System.currentTimeMillis();
                int timeout = 0;
                while(!commandFlag) {
                    long endTime = System.currentTimeMillis();
                    long spendTime = endTime - startTime;
                    if(spendTime > 1000) {
                        timeout++;
                        startTime = System.currentTimeMillis();
                        bytes = lastSend.getBytes();
                        mOutputStream.write(bytes);
                        mOutputStream.flush();
                        if(timeout >= 5.0) break;
                    }
                }
                return true;
            } catch(Exception e) {
                ToastMsg("Error sending data!");
            }
        }
        return false;
    }

    public void InitConnect() {
        mDevice = null;
        textBluetoothState.setText("Not Connected");
        textBluetoothDevice.setText("");
        textBluetoothAddress.setText("");
        textWifiSSID.setText("");
        textInfoWlan.setText("");
        editWifiPassword.setText("");
        WifiList.clear();
        WifiList.notifyDataSetChanged();
        if(mSocket != null) {
            if (mSocket.isConnected()) {
                try {
                    mSocket.close();
                    mInputStream.close();
                    mOutputStream.close();
                } catch (IOException e) {
                }
            }
        }
        try {
            if(workThread != null) {
                workFlag = false;
                workThread = null;
            }
        } catch(Exception e) {
        }
        listDevice.clear();
        arrayAdapterDevice.clear();
        arrayAdapterDevice.notifyDataSetChanged();
    }

    public boolean IsExistDevice(BluetoothDevice device) {
        for(BluetoothDevice item : listDevice) {
            if(item.equals(device)) {
                return true;
            }
        }
        return false;
    }

    public void CreateListDialog() {
        if(bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
        bluetoothAdapter.startDiscovery();

        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Bluetooth Devices");
        alert.setIcon(R.drawable.connect);

        /*
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                if(!IsExistDevice(device)) {
                    listDevice.add(device);
                    arrayAdapterDevice.add(device.getName() + "\n" + device.getAddress());
                    arrayAdapterDevice.notifyDataSetChanged();
                }
            }
        }
        */

        alert.setAdapter(arrayAdapterDevice, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Connect Dialog Click Listener
                mDevice = listDevice.get(which);
                textBluetoothDevice.setText(mDevice.getName());
                textBluetoothAddress.setText(mDevice.getAddress());
                bluetoothAdapter.cancelDiscovery();
            }
        });

        alert.show();
    }

    public void CreateWifiListDialog() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Accessible Wi-Fi");
        alert.setIcon(R.drawable.connect);

        alert.setAdapter(WifiList, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                textWifiSSID.setText(WifiList.getItem(which));
            }
        });
        alert.show();
    }

    public void ToastMsg(final String str) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), str, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).start();
    }

    class ConnectThread extends Thread {
        public ConnectThread() {
            BluetoothSocket tmp = null;
            try {
                tmp = mDevice.createRfcommSocketToServiceRecord(mUUID);
            } catch(IOException e) {
            }
            mSocket = tmp;
        }

        public void run() {
            bluetoothAdapter.cancelDiscovery();
            try {
                mSocket.connect();
                workThread = new ConnectedThread();
                workThread.start();
            } catch(IOException connectException) {
                try {
                    mSocket.close();
                } catch(IOException closeException) {
                }
                return;
            }
        }
    }

    class ConnectedThread extends Thread {
        private boolean IsExistSSID(String ssid) {
            String item;

            for (int i = 0; i < WifiList.getCount(); i++) {
                item = WifiList.getItem(i);

                if (item.equals(ssid))
                    return true;
            }

            return false;
        }

        @Override
        public void run() {
            try {
                mOutputStream = mSocket.getOutputStream();
                mInputStream = mSocket.getInputStream();
                workFlag = true;
                textBluetoothState.post(new Runnable() {
                    @Override
                    public void run() {
                        textBluetoothState.setText("Connected");
                    }
                });
                //readBufferPosition = 0;
                //readBuffer = new byte[1024];
            } catch (IOException e) {
                ToastMsg("Can't Connect to Server!");
                workFlag = false;
                InitConnect();
            }

            ToastMsg("Connceted!");
            long infoReqStart = System.currentTimeMillis();
            long infoReqEnd;
            long infoReqPeriod;

            while (workFlag) {
                try {
                    infoReqEnd = System.currentTimeMillis();
                    infoReqPeriod = infoReqEnd - infoReqStart;
                    if (infoReqPeriod > 1000) {
                        infoReqStart = System.currentTimeMillis();
                        byte[] bytes;
                        bytes = "I~E~E".getBytes();
                        //lastSend = "I~E~E";
                        mOutputStream.write(bytes);
                        mOutputStream.flush();
                    }

                    // Check there are available bytes, so this thread is none-blocked.
                    int bytesAvailable = mInputStream.available();
                    if (bytesAvailable > 0) {
                        final byte[] packetBytes = new byte[bytesAvailable];
                        mInputStream.read(packetBytes);
                        final String rcvStr = new String(packetBytes, "UTF-8");

                        String[] splits = rcvStr.split("~");
                        if (splits.length != 3) {
                            byte[] bytes = lastSend.getBytes();
                            mOutputStream.write(bytes);
                            mOutputStream.flush();
                            continue;
                        }
                        if (!splits[2].equals("e")) {
                            byte[] bytes = lastSend.getBytes();
                            mOutputStream.write(bytes);
                            mOutputStream.flush();
                            continue;
                        }

                        //ToastMsg(rcvStr);
                        switch (splits[0]) {
                            case "l":
                                if (splits.length == 3) {
                                    final String rcvSSID = splits[1];
                                    if(rcvSSID.equals("")) continue;
                                    new Thread(new Runnable() {
                                        @Override
                                        public void run() {
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    if (!IsExistSSID(rcvSSID)) {
                                                        WifiList.add(rcvSSID);
                                                        WifiList.notifyDataSetChanged();
                                                    }
                                                }
                                            });
                                        }
                                    }).start();
                                }
                                continue;
                            case "e":
                                byte[] bytes = lastSend.getBytes();
                                mOutputStream.write(bytes);
                                mOutputStream.flush();
                                continue;
                            case "o":
                                commandFlag = true;
                                continue;
                            case "i":
                                String infoWlan;
                                if (splits[1].equals("f")) {
                                    infoWlan = "OFF";
                                } else {
                                    infoWlan = "ON: "+splits[1];
                                }
                                final String finalInfoWlan = infoWlan;
                                textInfoWlan.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        textInfoWlan.setText(finalInfoWlan);
                                    }
                                });
                                continue;
                        }
                    }
                } catch (Exception e) {
                }
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        InitConnect();
        unregisterReceiver(BluetoothStateReceiver);
    }

    private final BroadcastReceiver BluetoothStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                if(mDevice != null) {
                    try {
                        mDevice.fetchUuidsWithSdp();
                    } catch(Exception ignored) {
                    }
                }
            } else if(BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if(!IsExistDevice(device)) {
                    listDevice.add(device);
                    arrayAdapterDevice.add(device.getName() + "\n" + device.getAddress());
                    arrayAdapterDevice.notifyDataSetChanged();
                }
            }
        }
    };
}