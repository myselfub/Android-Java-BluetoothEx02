package com.example.bluetoothex02;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class ClientActivity extends AppCompatActivity {

    static final UUID BT_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    public static final int REQ_ENABLE = 10;
    public static final int REQ_DISCOVERY = 20;

    BluetoothAdapter bluetoothAdapter;
    BluetoothSocket socket;
    DataInputStream dis;
    //    InputStream dis;
    DataOutputStream dos;
    //    OutputStream dos;
    ClientThread clientThread;
    TextView tv;
    EditText et;
    String me;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);

        getSupportActionBar().setTitle("CLIENT");
        tv = findViewById(R.id.clientTv);
        et = findViewById(R.id.clientEt);

        //블루투스 관리자 객체 소환
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "이 기기에는 블루투스가 없습니다.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        if (bluetoothAdapter.isEnabled()) {
            //서버 블루투스 장치 탐색 및 리스트 보기를 해주는 액티비티 실행하기
            discoveryBluetoothDevices();
        } else {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, REQ_ENABLE);
        }
    }//onCreate() ..

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (dis != null) {
                dis.close();
            }
            if (dos != null) {
                dos.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //주변의 블루투스 장치들을 탐색하여
    //리스트로 보여주는 액티비티를 실행하는 메소드
    void discoveryBluetoothDevices() {
        Intent intent = new Intent(this, BTDevicesListActivity.class);
        startActivityForResult(intent, REQ_DISCOVERY);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQ_ENABLE:
                if (resultCode == RESULT_CANCELED) {
                    Toast.makeText(this, "사용 불가", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    //서버 블루투스 장치 탐색 및 리스트 보기
                    discoveryBluetoothDevices();
                }
                break;
            case REQ_DISCOVERY:
                //결과가 OK면.
                if (resultCode == RESULT_OK) {
                    //연결할 블루투스 장치의 mac주소를 얻어왔다.
                    String deviceAddress = data.getStringExtra("Address");

                    //이 주소를 통해 Socket연결 작업 실행
                    //하는 별도의 Thread객체 생성 및 실행
                    clientThread = new ClientThread(deviceAddress);
                    clientThread.start();

                }
                break;
        }
    }

    //inner class///////////
    class ClientThread extends Thread {

        String address;// 연결할 장치의 mac주소

        public ClientThread(String address) {
            this.address = address;
        }

        @Override
        public void run() {
            //소켓생성하기 위해 Blutooth 장치 객체 얻어오기
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);

            //디바이스를 통해 소켓연결
            try {
                socket = device.createInsecureRfcommSocketToServiceRecord(BT_UUID);
                socket.connect();//연결 시도
                me = bluetoothAdapter.getName();

                //만약 연결이 성공되었다고 메세지
                showText("클라이언트가 접속했습니다.\n");

                dis = new DataInputStream(socket.getInputStream());
//                dis = socket.getInputStream();
                dos = new DataOutputStream(socket.getOutputStream());
//                dos = socket.getOutputStream();

                //원하는 통신 작업 수행...
                String text;
                byte[] buffer = null;
                while (dis != null) {
//                    text = dis.readUTF();
                    int bytes = dis.available();
                    if (bytes != 0) {
                        buffer = new byte[bytes];
                        Log.d("aaaaaaaa", String.valueOf(bytes));
                        dis.read(buffer, 0, bytes);
                        text = new String(buffer, "UTF-8");
                        Log.d("aaaaaaaa", text);
                        showText(text);
                    }
                    sleep(500);
                }
            } catch (IOException | InterruptedException e) {
                tv.setText("블루투스 연결 중 오류가 발생했습니다.");
                try {
                    if (dis != null) {
                        dis.close();
                    }
                } catch (IOException ioe) {
                    e.printStackTrace();
                }
            }
        }

        void showText(final String msg) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tv.append(msg); // 글씨 누적
                }
            });
        }
    }//ClientThread clas..

    public void sendMsg(View view) {
        if (dos != null) {
            if (et.getText().toString().length() > 0) {
                try {
                    String text = me + " : " + et.getText().toString() + "\n";
//                    dos.writeUTF(text);
                    dos.write(text.getBytes("UTF-8"));
                    dos.flush();
                    tv.append(text);
                    et.setText("");
                } catch (IOException e) {
                    try {
                        if (dos != null) {
                            dos.close();
                        }
                    } catch (IOException ioe) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

}//ClientActivity class..