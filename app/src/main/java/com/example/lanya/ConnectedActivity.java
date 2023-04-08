package com.example.lanya;

import android.Manifest;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class ConnectedActivity extends AppCompatActivity {
    private static final int SELECT_FILE_REQUEST = 1;
    private BluetoothSocket bluetoothSocket;
    private OutputStream outputStream;
    private InputStream inputStream;
    private BluetoothDevice device;
    private UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private ProgressDialog pd;
    private TextView tv_log;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connected);
        initView();

        pd = new ProgressDialog(this);
        pd.setCancelable(false);

        device = (BluetoothDevice) getIntent().getExtras().get("device");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) !=
                PackageManager.PERMISSION_GRANTED) {

            tv_log.append("没有连接权限");

            return;
        }

        try {
            bluetoothSocket = device.createInsecureRfcommSocketToServiceRecord(uuid);
            bluetoothSocket.connect();
            tv_log.append("连接成功");
        } catch (IOException e) {
            e.printStackTrace();
            tv_log.append("连接失败：" + e.getMessage());
        }

        Button sendFileButton = findViewById(R.id.send_file_button);
        sendFileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectFile();
            }
        });
    }

    private void selectFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        startActivityForResult(intent, SELECT_FILE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SELECT_FILE_REQUEST && resultCode == RESULT_OK) {
            Uri uri = data.getData();
            pd.show();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    sendFile(uri);
                }
            }).start();
        }
    }

    private void sendFile(Uri fileUri) {
        try {
            outputStream = bluetoothSocket.getOutputStream();
            InputStream fileInputStream = getContentResolver().openInputStream(fileUri);
            byte[] buffer = new byte[1024];
            int bytesRead;

            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
                outputStream.flush();
            }

            fileInputStream.close();

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    pd.dismiss();
                    tv_log.append("发送成功");
                    Toast.makeText(ConnectedActivity.this, "File sent", Toast.LENGTH_SHORT).show();
                }
            });

        } catch (IOException e) {
            e.printStackTrace();

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    pd.dismiss();
                    tv_log.append("发送失败：" + e.getMessage());
                    Toast.makeText(ConnectedActivity.this, "Failed to send file", Toast.LENGTH_SHORT).show();
                }
            });

        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bluetoothSocket != null) {
            try {
                bluetoothSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void initView() {
        tv_log = (TextView) findViewById(R.id.tv_log);
    }
}