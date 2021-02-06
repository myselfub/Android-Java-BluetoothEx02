package com.example.bluetoothex02;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import androidx.annotation.Nullable;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.UUID;

public class ServerActivity extends AppCompatActivity {
    //블루투스 장치 식별 번호
    static final UUID BT_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    //    final static UUID BT_UUID = UUID.fromString("8CE255C0-200A-11E0-AC64-0800200C9A66");
    public static final int REQ_ENABLE = 10;
    public static final int REQ_DISCOVERYABLE = 20;

    TextView tv;
    EditText et;

    BluetoothAdapter bluetoothAdapter;
    BluetoothServerSocket serverSocket;
    BluetoothSocket socket;
    DataInputStream dis;
    //    InputStream dis;
    DataOutputStream dos;
    //    OutputStream dos;
    String me;

    //이너클래스 참조 멤버 변수
    ServerThread serverThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server);

        getSupportActionBar().setTitle("SERVER");
        tv = findViewById(R.id.serverTv);
        et = findViewById(R.id.serverEt);

        //블루투스 관리자 객체 소환하기
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "이 기기에는 블루투스가 없습니다.", Toast.LENGTH_SHORT).show();
            finish(); //이 순간 곧바로 종료되지 않음
            return;     //그래서 꼭 return 써주자.
        }
        //블루투스가 켜져있는지 확인
        if (bluetoothAdapter.isEnabled()) {
            //서버소켓 생성 작업 실행
            createServerSocket();

        } else {
            //블루투스장치 ON 선택 액티비티 보이기(단, 액티비티의 스타일이 다이얼로그로 되어 있음)
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, REQ_ENABLE);
        }
    }//onCreate()..

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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQ_ENABLE:
                if (resultCode == RESULT_CANCELED) {
                    //Enable을 시키지 않았으므로 프로그램 종료
                    Toast.makeText(this, "블루투스를 허용하지 않았습니다.\n앱을 종료합니다.", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    //서버 소켓 생성 및 실행
                    createServerSocket();
                }
                break;
            case REQ_DISCOVERYABLE:
                if (resultCode == RESULT_CANCELED) {
                    Toast.makeText(this, "블루투스 탐색을 허용하지 않았습니다.\n다른 장치에서 이 장치를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
                }
                break;
        }

    }// onActivityResult

    //서버소켓 생성 작업을 하는 메소드
    void createServerSocket() {
        // 통신을 하기 위한 스레드 객체 생성
        serverThread = new ServerThread();
        serverThread.start();

        //상대방이 내 디바이스의 블루투스를
        //탐색하는 것을 허용하기
        allowDiscovery();
    }

    //탐색 허용 작업 메소드
    void allowDiscovery() {
        //탐색 허용 여부를 보여주는 다이얼로그 스타일의 액티비티를 실행
        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        //기본으로 120초 동안 탐색을 허용하도록...
        //최대 300초까지 설정 가능함
        intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);// 300초
        startActivityForResult(intent, REQ_DISCOVERYABLE);

    }

    //inner class////
    class ServerThread extends Thread {
        @Override
        public void run() {
            init();
        }//run() ..

        private void init() {
            try {
                //서버소켓 생성
                serverSocket = bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord("SERVER", BT_UUID);
                showText("서버 소켓 생성했습니다.\n클라이언트의 접속을 기다립니다.\n");

                //클라이언트의 접속 기다리기..
                socket = serverSocket.accept();//클라이언트가 접속할 때까지 대기
                me = bluetoothAdapter.getName();
                showText(me + " 클라이언트가 접속했습니다.\n");

                //접속이 되었으니 둘사이에 데이터를 주고받을
                //무지개 로드 (Stream)
                dis = new DataInputStream(socket.getInputStream());
//                dis = socket.getInputStream();
                dos = new DataOutputStream(socket.getOutputStream());
//                dos = socket.getOutputStream();

                //dis & dos 를 이용해서 원하는 통신 작업 수행!
                String text;
                byte[] buffer = null;
                while (dis != null) {
//                    text = dis.readUTF();
                    int bytes = dis.available();
                    if (bytes != 0) {
                        Log.d("aaaaaaaa", String.valueOf(bytes));
                        buffer = new byte[bytes];
                        dis.read(buffer, 0, bytes);
                        Log.d("aaaaaaaa", buffer.toString());
                        text = new String(buffer, "UTF-8");
                        Log.d("aaaaaaaa", text);
                        showText(text);
                    }
                    sleep(500);
                }
            } catch (IOException | InterruptedException e) {
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
            if (serverSocket == null) {
                init();
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tv.append(msg); // 글씨 누적
                }
            });
        }
    }//ServerThread

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
}//ServerActivity class..