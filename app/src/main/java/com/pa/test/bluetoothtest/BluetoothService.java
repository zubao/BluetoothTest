package com.pa.test.bluetoothtest;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;

public class BluetoothService extends Thread {
    private final BluetoothServerSocket mmServerSocket;
    private final BluetoothAdapter bluetoothAdapter;
    private final String TAG = "BluetoothTest";
    private final Handler mHandler;
    private ConnectedThread mConnectedThread;

    public BluetoothService(Handler handler) {
        BluetoothServerSocket tmp = null;
        bluetoothAdapter    = BluetoothAdapter.getDefaultAdapter();
        try {
            tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord("bluetooth_test", UUID.fromString(Config.UUID));
        } catch (IOException e) {
            Log.e(TAG, "Socket's listen() method failed", e);
        }
        mmServerSocket = tmp;
        mHandler    = handler;
    }

    public void run() {
        BluetoothSocket socket = null;
        // Keep listening until exception occurs or a socket is returned.
        while (true) {

            Log.i(TAG, "service start wait connect...");

            Message writtenMsg = mHandler.obtainMessage(Config.MESSAGE_STATE, -1, -1, "服务端等待连接...");
            writtenMsg.sendToTarget();
            try {
                socket = mmServerSocket.accept();
            } catch (IOException e) {
                Log.e(TAG, "Socket's accept() method failed", e);
                writtenMsg = mHandler.obtainMessage(Config.MESSAGE_STATE, -1, -1, "服务端异常！");
                writtenMsg.sendToTarget();
                Log.i(TAG, "service connect failed");

                break;
            }

            if (socket != null) {
                // A connection was accepted. Perform work associated with
                // the connection in a separate thread.
                Log.i(TAG, "connect success ");

                writtenMsg = mHandler.obtainMessage(Config.MESSAGE_STATE, -1, -1, "服务端连接成功！");
                writtenMsg.sendToTarget();
                manageMyConnectedSocket(socket);

                try {
                    mmServerSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            }
        }
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

    // Closes the connect socket and causes the thread to finish.
    public void cancel() {
        try {
            mmServerSocket.close();
        } catch (IOException e) {
            Log.e(TAG, "Could not close the connect socket", e);
        }
    }
}
