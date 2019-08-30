package com.pa.test.bluetoothtest;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;

public class BluetoothClient extends Thread{
    private final BluetoothSocket mmSocket;
    private final BluetoothDevice mmDevice;
    private final String TAG    = "BluetoothTest";
    private final BluetoothAdapter bluetoothAdapter;
    private final Handler mHandler;
    private ConnectedThread mConnectedThread;

    public BluetoothClient(BluetoothDevice device, Handler handler) {
        // Use a temporary object that is later assigned to mmSocket
        // because mmSocket is final.
        BluetoothSocket tmp = null;
        mmDevice = device;
        bluetoothAdapter    = BluetoothAdapter.getDefaultAdapter();

        try {
            // Get a BluetoothSocket to connect with the given BluetoothDevice.
            // MY_UUID is the app's UUID string, also used in the server code.
            tmp = device.createRfcommSocketToServiceRecord(UUID.fromString(Config.UUID));
        } catch (IOException e) {
            Log.e(TAG, "Socket's create() method failed", e);
        }
        mmSocket = tmp;
        mHandler    = handler;
    }

    public void run() {
        // Cancel discovery because it otherwise slows down the connection.
        Log.i(TAG, "start connect...");
        bluetoothAdapter.cancelDiscovery();
        Message writtenMsg = mHandler.obtainMessage(Config.MESSAGE_STATE, -1, -1, "客户端发起连接...");
        writtenMsg.sendToTarget();
        try {
            // Connect to the remote device through the socket. This call blocks
            // until it succeeds or throws an exception.
            mmSocket.connect();
        } catch (IOException connectException) {
            // Unable to connect; close the socket and return.
            try {
                mmSocket.close();
            } catch (IOException closeException) {
                Log.e(TAG, "Could not close the client socket", closeException);
            }
            writtenMsg = mHandler.obtainMessage(Config.MESSAGE_STATE, -1, -1, "连接失败！");
            writtenMsg.sendToTarget();
            Log.i(TAG, "connect failed");
            return;
        }

        Log.i(TAG, "connect success");

        writtenMsg = mHandler.obtainMessage(Config.MESSAGE_STATE, -1, -1, "连接成功！");
        writtenMsg.sendToTarget();

        // The connection attempt succeeded. Perform work associated with
        // the connection in a separate thread.
        manageMyConnectedSocket(mmSocket);
    }

    public void sendData(String data){
        if(mConnectedThread != null){
            mConnectedThread.write(data.getBytes());
        }
    }

    void manageMyConnectedSocket(BluetoothSocket socket){
        mConnectedThread    = new ConnectedThread(socket, mHandler);
        mConnectedThread.start();
    }

    // Closes the client socket and causes the thread to finish.
    public void cancel() {
        try {
            mmSocket.close();
        } catch (IOException e) {
            Log.e(TAG, "Could not close the client socket", e);
        }
    }
}
