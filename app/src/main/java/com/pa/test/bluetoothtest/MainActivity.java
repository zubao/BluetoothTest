package com.pa.test.bluetoothtest;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.tx_pairs)
    TextView mTxPairs;
    @BindView(R.id.tx_scan_devices)
    TextView mTxScanDevices;

    @BindView(R.id.btnClientRequest)
    Button btnClient;

    @BindView(R.id.btnServiceAccept)
    Button btnService;

    @BindView(R.id.tx_logs)
    TextView txLogs;

    @BindView(R.id.et_bluetooth_no)
    EditText etNo;

    @BindView(R.id.et_data)
    EditText etData;

    private ArrayList<BluetoothDevice> mBuletoothDeviceList;
    private Map<String, String> mHashMap    = new HashMap<>();

    StringBuilder sb    = new StringBuilder();


    Handler.Callback mCallback  = new Handler.Callback() {
        @Override
        public boolean handleMessage(Message message) {
            switch (message.what){
                case Config.MESSAGE_CONNECT_FAIL:
                    break;
                case Config.MESSAGE_STATE:
                    txLogs.setText(txLogs.getText().toString()+"\n" + message.obj.toString());
                    break;
                case Config.MESSAGE_READ:
                    txLogs.setText(txLogs.getText().toString()+"\n" + message.obj.toString());
                    break;
                case Config.MESSAGE_TOAST:
                    break;
                case Config.MESSAGE_WRITE:
                    txLogs.setText(txLogs.getText().toString()+"\n" + message.obj.toString());
                    break;
            }
            return true;
        }
    };

    Handler mHandler;
    private BluetoothClient mClient;
    private BluetoothService mService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);

        //注册一个搜索结束时的广播
        IntentFilter filter2=new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        //注册一个搜索结束时的广播
        IntentFilter filter3=new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        registerReceiver(receiver,filter3);
        registerReceiver(receiver,filter2);
        registerReceiver(receiver, filter);
        mBuletoothDeviceList    = new ArrayList<BluetoothDevice>();

        mHandler    = new Handler(mCallback);

        getBluetoothInfo();

        if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION
            }, 100);
        }

        Log.i("TAG", Config.UUID);
    }

    private void getBluetoothInfo(){
        BluetoothAdapter adapter    = BluetoothAdapter.getDefaultAdapter();
        String bluetoothName    = adapter.getName();
        String address          = adapter.getAddress();
        String state            = adapter.isEnabled() ? "Enabled" : "Disabled";

        txLogs.setText(bluetoothName + "\n"
            + address + "\n"
                + state
        );
    }


    @OnClick({R.id.btnCheck, R.id.btnPairs, R.id.btnScan, R.id.btnSend, R.id.btnClientRequest, R.id.btnServiceAccept})
    public void onClick(View view){
        switch (view.getId()){
            case R.id.btnCheck:
                checkBluetooth();
                break;
            case R.id.btnPairs:
                checkPairs();
                break;
            case R.id.btnScan:
                startDiscovery();
                break;
            case R.id.btnClientRequest:
                clientRequestLink();
                break;
            case R.id.btnServiceAccept:
                serviceAccept();
                break;
            case R.id.btnSend:
                sendData();
                break;
        }
    }

    private void sendData(){
        String data     = etData.getText().toString();
        if(data.isEmpty()){
            Toast.makeText(this, "内容不能为空", Toast.LENGTH_SHORT).show();
            return;
        }

        if(mClient != null){
            mClient.sendData(data);
        }

        if(mService != null){
            mService.sendData(data);
        }

    }

    private void clientRequestLink(){
        btnService.setEnabled(false);

        String no   = etNo.getText().toString().trim();
        if(TextUtils.isEmpty(no)){
            Toast.makeText(this, "请输入需要连接的蓝牙索引", Toast.LENGTH_SHORT).show();
            return;
        }
        int index   = Integer.parseInt(no) - 1;
        if(index < mBuletoothDeviceList.size()){
            BluetoothDevice device  = mBuletoothDeviceList.get(index);
            mClient  = new BluetoothClient(device, mHandler);
            mClient.start();
        }
    }

    private void serviceAccept(){
        btnClient.setEnabled(false);
        mService    = new BluetoothService(mHandler);
        mService.start();
    }


    private void checkBluetooth(){
        BluetoothAdapter adapter    =  BluetoothAdapter.getDefaultAdapter();
        if(adapter == null || adapter.isEnabled() == false){
            Toast.makeText(this, "蓝牙无法正常启动，请去设置页启动蓝牙", Toast.LENGTH_SHORT).show();
            return;
        }
    }

    private void checkPairs(){
        BluetoothAdapter bluetoothAdapter       = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices      = bluetoothAdapter.getBondedDevices();
        StringBuilder sb    = new StringBuilder();
        if (pairedDevices.size() > 0) {
            // There are paired devices. Get the name and address of each paired device.
            for (BluetoothDevice device : pairedDevices) {
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                sb.append(deviceHardwareAddress)
                        .append("--")
                        .append(deviceName)
                        .append("\n");
            }
        }
        mTxPairs.setText(sb.toString());
    }

    private void startDiscovery(){
        BluetoothAdapter adapter    =  BluetoothAdapter.getDefaultAdapter();
        adapter.startDiscovery();
        mBuletoothDeviceList.clear();
        mHashMap.clear();
        mTxScanDevices.setText("");
        sb.delete(0, sb.length());

//        final StringBuilder sb    = new StringBuilder();
//        adapter.startLeScan(new BluetoothAdapter.LeScanCallback() {
//            @Override
//            public void onLeScan(BluetoothDevice bluetoothDevice, int i, byte[] bytes) {
//                BluetoothDevice device          = bluetoothDevice;
//                String deviceName               = device.getName();
//                String deviceHardwareAddress    = device.getAddress(); // MAC address
//
//                mBuletoothDeviceList.add(device);
//                sb.append(""+mBuletoothDeviceList.size()).append(deviceHardwareAddress)
//                        .append("--")
//                        .append(deviceName)
//                        .append("--")
//                        .append(device.getType())
//                        .append("--")
//                        .append(device.getBondState())
//                        .append("--")
//                        .append(i)
//                        .append("\n");
//                mTxScanDevices.setText(mTxScanDevices.getText().toString() + sb.toString());
//            }
//        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Don't forget to unregister the ACTION_FOUND receiver.
        unregisterReceiver(receiver);
    }

    // Create a BroadcastReceiver for ACTION_FOUND.
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            Log.d("TAG", "onReceive");
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device          = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName               = device.getName();
                String deviceHardwareAddress    = device.getAddress(); // MAC address

                if(TextUtils.isEmpty(deviceName) == false && !mHashMap.containsKey(deviceHardwareAddress)){
                    mBuletoothDeviceList.add(device);
                    mHashMap.put(deviceHardwareAddress, deviceName);
                    sb.append(mBuletoothDeviceList.size()+"、")
                            .append(deviceHardwareAddress)
                            .append("--")
                            .append(deviceName)
                            .append("--")
                            .append(device.getType())
                            .append("\n");

                    mTxScanDevices.setText(sb.toString());
                }
            }else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){
                Log.d("TAG", "onReceive finish");

            }else if(BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)){
                Log.d("TAG", "onReceive start");

            }

        }
    };
}
