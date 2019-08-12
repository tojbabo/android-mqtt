package com.example.ojjj.mqtt_app;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.listener.ChartTouchListener;
import com.github.mikephil.charting.listener.OnChartGestureListener;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.ColorTemplate;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.StringTokenizer;

public class MainActivity extends AppCompatActivity implements OnChartValueSelectedListener {
    private MqttAndroidClient mqttAndroidClient;
    String SSLpasswd = "123123";
    LineData data;
    Button btn;
    EditText ipAdd, port;
    TextView doubleClick, longClick;
    ListView listview;
    ListView listview_topic;
    private LineChart mChart;
    private boolean moveToLastEntry = true;

    ArrayList<String> logTopic = new ArrayList<>();
    ArrayList<String> logTime = new ArrayList<>();

    ArrayList<String> pick_logList = new ArrayList<>();
    ArrayList<String> pick_logTopic = new ArrayList<>();
    ArrayList<String> pick_logTime = new ArrayList<>();

    ArrayList<String> topicList = new ArrayList<>();
    ArrayList<String> newtopic = new ArrayList<>();

    ArrayList<String> jsonID = new ArrayList<>();
    ArrayList<String> jsonMSG = new ArrayList<>();

    ArrayList<String> connect_state = new ArrayList<>();
    ArrayList<String> connect_id = new ArrayList<>();
    ArrayList<String> connect_time = new ArrayList<>();

    ArrayList<String> sub_time = new ArrayList<>();
    ArrayList<String> sub_id = new ArrayList<>();
    ArrayList<String> sub_topic = new ArrayList<>();

    Button send_btn;
    Intent in;
    topicAdapter topicAdapter;
    LogAdapter logAdapter;
    boolean isDoubleTap = false;
    String pick = "";
    String pre ="";
    int index;
    String ClientID = "Mobile_Monitor";
    boolean remoteon = false;
    Button remotebtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_background);
        // 이초동안 splash화면보이기
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                setContentView(R.layout.activity_main);
                doubleClick = findViewById(R.id.DoubleCLick);
                doubleClick.setText("<double tap>터치로 로그보기 : OFF");
                longClick = findViewById(R.id.LongCLick);
                longClick.setText("<long tap>고정 그래프 : OFF");

                // 간단한 메시지 보내는 버튼
                send_btn = findViewById(R.id.btn_publicsh);
                // IP를 입력할 수 있는 에디트 텍스트
                ipAdd = findViewById(R.id.setIp);
                // port를 입력할 수 있는 에디트 텍스트
                port = findViewById(R.id.setPort);
                // 그래프가 그려질 공간
                mChart = findViewById(R.id.chart);
                // 선택된 토픽에 대한 데이터가 표시될 리스트 뷰
                listview = findViewById(R.id.loglistview);
                // 수신된 모든 토픽들이 표시될 리스트 뷰
                listview_topic = findViewById(R.id.topiclistview);


                mChart.setOnChartValueSelectedListener(MainActivity.this);

                // listview에 사용될 어댑터 생성 및 설정
                logAdapter = new LogAdapter(MainActivity.this, pick_logList,pick_logTime,pick_logTopic);
                listview.setAdapter(logAdapter);

                // listview_topic에 사용될 어댑터 생성 및 설정
                topicAdapter = new topicAdapter(MainActivity.this, topicList,newtopic);
                listview_topic.setAdapter(topicAdapter);

                // 라돈 센서에 라이트를 끄고 키기 위한 간단한 리모컨 설정
                ////////////////////////////////////////////////
                remotebtn = findViewById(R.id.remote);
                remotebtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        LinearLayout lay  = findViewById(R.id.lay_remote);

                        if(remoteon == false) {
                            remotebtn.setText("REMOTE OFF");
                            lay.setVisibility(View.VISIBLE);
                            remoteon = true;
                        }
                        else{
                            remotebtn.setText("REMOTE ON");
                            lay.setVisibility(View.GONE);
                            remoteon = false;
                        }


                    }
                });
                /////////////////////////////////////////////////

                // listview_topic에서 특정 토픽을 선택하여 listview에 출력하기 위한 이벤트
                listview_topic.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                        LinearLayout lay;
                        newtopic.set(i,"");
                        topicAdapter.notifyDataSetChanged();
                        mChart.getLegend().setEnabled(false);
                        graph_clear();

                        // 선택된 토픽이 연결에 대한 시스템 토픽일 경우
                        if(topicList.get(i).equals("$connection")){
                            Toast.makeText(MainActivity.this, topicList.get(i).toString(), Toast.LENGTH_SHORT).show();
                            pick = "$connection";                                                   // 선택된 토픽은 connect
                            logAdapter.notifyDataChanged(connect_id,connect_time,connect_state);    // 어댑터에 연결된 ArrayList를 변경함
                            lay = findViewById(R.id.layout_text);                                   // 리스트 뷰 위의 text를 connect 토픽에 맞게 변경
                            lay.setVisibility(View.GONE);                                           //
                            lay = findViewById(R.id.layout_system);                                 //
                            lay.setVisibility(View.VISIBLE);                                        //
                        }
                        // 선택된 토픽이 구독에 관한 시스템 토픽일 경우
                        else if(topicList.get(i).equals("$subscribe")){
                            Toast.makeText(MainActivity.this, topicList.get(i).toString(), Toast.LENGTH_SHORT).show();
                            pick = "$subscribe";                                                    // 선택된 토픽 subscribe
                            logAdapter.notifyDataChanged(sub_id,sub_time,sub_topic);                // 어댑터 연결된 ArrayList 변경
                            lay = findViewById(R.id.layout_text);
                            lay.setVisibility(View.GONE);
                            lay = findViewById(R.id.layout_system);
                            lay.setVisibility(View.VISIBLE);
                        }
                        // 그 밖의 메시지일 경우
                        else {
                            Toast.makeText(MainActivity.this, topicList.get(i).toString(), Toast.LENGTH_SHORT).show();
                            pick_up_data(topicList.get(i).toString());                              // 그 외 토픽일 경우 함수 호출
                            lay = findViewById(R.id.layout_system);
                            lay.setVisibility(View.GONE);
                            lay = findViewById(R.id.layout_text);
                            lay.setVisibility(View.VISIBLE);
                        }
                    }
                });
                chartInit();
            }
        }, 2000);
    }
    // 연결을 시작하는 함수
    public void StartMqttConnect(View v) {
        // ip를 받아와서 client 객체 만들기
        btn = findViewById(R.id.btn_connect);                                                       // 연결 버튼 설정
        btn.setEnabled(false);                                                                      // 일단 눌려지면 비활성화
        if (mqttAndroidClient == null || !mqttAndroidClient.isConnected()) {                        // 연결이 안되어 있을때
            ipAdd.setEnabled(false);                                                                // Ip 입력칸 비활성화
            port.setEnabled(false);                                                                 // port 입력칸 비활성화
            MqttConnect();
        }
        else{                                                                                       // 연결이 되어있을때
            try {
                mqttAndroidClient.disconnect();                                                     // 연결 해제
            } catch (MqttException e) {
                e.printStackTrace();
            }
            btn.setText("connect");                                                                 // 버튼 텍스트 변경
            remoteon = false;                                                                       // 리모콘 해제
            remotebtn.setText("REMOTE ON");
            findViewById(R.id.lay_remote).setVisibility(View.GONE);
            remotebtn.setEnabled(false);
            port.setEnabled(true);                                                                  // port 입력칸 활성화
            btn.setEnabled(true);                                                                   // 연결버튼 활성화
            ipAdd.setEnabled(true);                                                                 // ip 입력칸 활성화
            send_btn.setEnabled(false);                                                             // send 버튼 비활성화
        }
    }
    // 연결을 시도하는 함수
    public void MqttConnect(){
        mqttAndroidClient = new MqttAndroidClient(this,  "ssl://"+ipAdd.getText().toString()+":"+port.getText().toString()+"", ClientID);   // Mqtt 객체 생성
        try {
            final IMqttToken token = mqttAndroidClient.connect(getMqttConnectionOption(this));    //mqtttoken 이라는것을 만들어 connect option을 달아줌
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    mqttAndroidClient.setBufferOpts(getDisconsnectedBufferOption());    //연결에 성공한경우
                    Toast.makeText(MainActivity.this, "연결성공", Toast.LENGTH_SHORT).show();
                    remotebtn.setEnabled(true);                                                     // 리모컨 버튼 활성화
                    btn.setEnabled(true);                                                           // 연결/해제버튼 활성화
                    ipAdd.setEnabled(false);                                                        // Ip 입력창 비활성화
                    port.setEnabled(false);                                                         // port 입력창 비활성화
                    send_btn.setEnabled(true);                                                      // send 버튼 활성화
                    btn.setText("disconnect");                                                      // 연결버튼 텍스트 변경
                    MqttAction();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {   //연결에 실패한경우
                    Toast.makeText(MainActivity.this, "실패", Toast.LENGTH_SHORT).show();
                    btn.setEnabled(true);                                                           // 연결/해제 버튼 활성화
                    remoteon = false;                                                               // 리모컨 비활성화
                    remotebtn.setEnabled(false);
                    ipAdd.setEnabled(true);                                                         // ip 입력창 활성화
                    port.setEnabled(true);                                                          // port 입력창 활성화
                    findViewById(R.id.lay_remote).setVisibility(View.GONE);                         // 리모컨 해제
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnrecoverableKeyException e) {
            e.printStackTrace();
        } catch (MqttException e) {
            e.printStackTrace();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }

    }

    private DisconnectedBufferOptions getDisconsnectedBufferOption() {
        DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
        disconnectedBufferOptions.setBufferEnabled(true);
        disconnectedBufferOptions.setBufferSize(100);
        disconnectedBufferOptions.setPersistBuffer(true);
        disconnectedBufferOptions.setDeleteOldestMessages(false);
        return disconnectedBufferOptions;
    }
    // Mqtt 서버에 연결하기 위한 옵션을 생성하는 함수
    private MqttConnectOptions getMqttConnectionOption(Context context)
            throws IOException, UnrecoverableKeyException, CertificateException,
            NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();                           // mqtt 옵션 객체 생성
        SocketFactory.SocketFactoryOptions socketFactoryOptions = new SocketFactory.SocketFactoryOptions(); // mqtt에 설정할 소켓 팩토리 옵션 객체 생성
        socketFactoryOptions.withCaInputStream(context.getResources().openRawResource(R.raw.ca));   // 소켓 팩토리 옵션 객체에 인증서 설정
        socketFactoryOptions.withClientP12Password(SSLpasswd);                                      // 소켓 팩토리 옵션 객체에 .p12파일 비밀번호 설정
        socketFactoryOptions.withClientP12InputStream(context.getResources().openRawResource(R.raw.android));   // 소켓 팩토리 옵션 객체에 .p12파일 설정
        mqttConnectOptions.setSocketFactory(new SocketFactory(socketFactoryOptions));               // mqtt 옵션에 소켓 팩토리 설정
        return mqttConnectOptions;
    }
    // 연결성공시 불려지는 함수 : 메세지가 도착할떄
    public void MqttAction() {
        //클라이언트의 콜백을 처리
        try {
            mqttAndroidClient.subscribe("#",0);
            mqttAndroidClient.subscribe("$SYS/broker/log/#",0);
        } catch (MqttException e) {
            e.printStackTrace();
        }
        mqttAndroidClient.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) { // 연결이 끊겼을때
                Toast.makeText(MainActivity.this, "연결 끊어짐", Toast.LENGTH_SHORT).show();
                findViewById(R.id.lay_remote).setVisibility(View.GONE);                             // 열려있을 리모컨 해제
                ipAdd.setEnabled(true);                                                             // ip 입력창 활성화
                port.setEnabled(true);                                                              // port 입력창 활성화
                remotebtn.setText("REMOTE ON");                                                     // 리모컨 재설정
                remoteon = false;
                btn.setText("connect");                                                             // 연결 버튼 재설정
                remotebtn.setEnabled(false);
                send_btn.setEnabled(false);
                mqttAndroidClient= null;                                                            // mqtt 객체 해제
            }
            @Override
            public void messageArrived(String topic, MqttMessage message) {                         // 모든 메시지가 도착하면 콜백

                long now = System.currentTimeMillis();                                              // 메세지 도착시간 받기
                Date date = new Date(now);                                                          // 도착 시간을 형태에 맞게 저장
                SimpleDateFormat sdfNow = new SimpleDateFormat("HH:mm:ss");
                String formatDate = sdfNow.format(date);

                String msg = new String(message.getPayload());                                      // 메세지 스트링형태로 받기
                topic = system_topic(topic);                                                        // 수신된 토픽이 시스템 메시지라면 시스템 형태로 재구성


                if(!topicList.contains(topic)){                                                     // 현재 수신된 토픽이 새로운 토픽일때
                    topicList.add(topic);                                                           // 토픽 리스트에 추가
                    Collections.sort(topicList);                                                    // 리스트를 이름순으로 정렬
                    newtopic.add(topicList.indexOf(topic),"new");                          // 도착 알림 설정
                }
                else{newtopic.set(topicList.indexOf(topic),"new");}                                 // 도착 알림만 설정

                topicAdapter.notifyDataSetChanged();                                                // 토픽 리스트가 변화된걸 알림
                if (topic.charAt(0) == '$')                                                         // 도착한 메시지가 시스템 메시지일 경우
                    system_msg(msg, formatDate);                                                    // 시스템 메시지를 형태에 맞게 변환
                else {                                                                              // 도착한 메시지가 일반 메시지일 경우
                    logTime.add(formatDate);                                                        // 도착 시간과 토픽 추가
                    logTopic.add(topic);
                    JSONObject jobject = null;
                    String id;
                    String data;
                    try {                                                                           // 도착한 메시지를 json 형태에 맞게 분리
                        jobject = new JSONObject(msg);
                        id = jobject.getString("id");
                        data = jobject.getString("message");
                    }catch (JSONException e) {
                        e.printStackTrace();
                        id = " - ";
                        data = msg;
                    }
                    jsonID.add(id);                                                                 // 형탵에 맞게 분리된 데이터 를 각 리스트에 추가
                    jsonMSG.add(data);
                    if(topic.equals(pick)) {                                                        // 도착된 메시지의 토픽이 선택된 토픽과 같을때
                        addEntry(Float.valueOf(data), topic);                                       // 그래프에 추가 그리기
                        pick_logTime.add(formatDate);                                               // 시간과 데이터 추가
                        pick_logList.add(data);
                        pick_logTopic.add(id);
                    }
                    logAdapter.notifyDataSetChanged();                                              // 리스트뷰 다시 그리기
                }
            }
            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
            }
        });
    }
    // 시스템 토픽을 수신했을 경우 토픽 이름을 정리함
    public String system_topic(String topic){
        if(topic.charAt(0) == '$') {                                                                // 맨 앞글자가 $(시스템 메시지) 일 경우
            if (topic.contains("subscribe"))                                                        // 토픽에 구독이 포함될 경우
                return "$subscribe";                                                                // 토픽 이름 변경
            else if (topic.contains("/N"))                                                          // 연결에 관련된 토픽일 경우
                return "$connection";                                                               // 토픽 이름 변경
        }
        return topic;

    }
    // 시스템 메시지를 수신했을 경우 메시지를 정리함
    public void system_msg(String msg,String time){
        // 연결에 관한 메시지가 도착 했을 경우 메시지를 가공하는 것으로
        // 메시지에서 아이디와 이벤트를 분리하여
        // 각 리스트에 추가한다.
        if(msg.contains("disconnected")){
            int start = msg.indexOf("Client")+7;
            int end = msg.indexOf("disconnected");
            connect_id.add(msg.substring(start,end));
            connect_state.add("disconnected");
            connect_time.add(time);
        } else if (msg.contains(" connected")) {
            int start = msg.indexOf("as")+3;
            int end =msg.indexOf("(");
            connect_id.add(msg.substring(start,end));
            connect_state.add("connected");
            connect_time.add(time);
        }
        else if(msg.contains("error")){
            int start = msg.indexOf("client")+7;
            int end = msg.indexOf("disconnecting");
            if(end == -1)
                end = msg.indexOf(",")-1;

            connect_id.add(msg.substring(start,end));
            connect_state.add("socket error");
            connect_time.add(time);
        }
        else{
            sub_time.add(time);
            StringTokenizer  st = new StringTokenizer(msg," ");
            st.nextToken();
            sub_id.add(st.nextToken());
            String qos = st.nextToken();
            String temp = st.nextToken();
            if(temp.contains("$SYS/"))
                temp = "system log";
            sub_topic.add(temp+" - "+qos);
        }
    }
    // 특정 토픽을 선택했을 경우 이벤트
    public void pick_up_data(String topic){
        listview.setAdapter(logAdapter);                                                            // 선택된 토픽의 메시지가 리스트뷰에 보일 수 있도록 어뎁터 연결
        pick_logTopic.clear();                                                                      // 기존의 리스트 비우기
        pick_logTime.clear();
        pick_logList.clear();

        for(int i = 0 ; i < jsonMSG.size();i++){                                                    // 도착한 모든 메시지 중
            if(logTopic.get(i).toString().equals(topic)) {                                          // 선택한 토픽과 동일한 토픽의 메시지를
                pick_logList.add(jsonMSG.get(i));                                                   // 출력할 리스트에 추가한다
                pick_logTime.add(logTime.get(i));
                pick_logTopic.add(jsonID.get(i));
                try {
                    float f = Float.valueOf(jsonMSG.get(i));                                        // 메시지에 실수 값이 있을경우
                    addEntry(f,topic);                                                              // 그래프에 그리기
                    mChart.getLegend().setEnabled(true);                                            // 그래프에 레전드 설정
                }catch(NumberFormatException e){}
            }
        }
        pick = topic;                                                                               // 선택된 토픽을 설정
        logAdapter.notifyDataChanged(pick_logList,pick_logTime,pick_logTopic);                      // 선택된 토픽의 리스트로 리스트 뷰 설정
    }
    // 차트 설정, 시작 함수
    public void chartInit() {
        mChart.setOnChartValueSelectedListener(this);                                               // 선택시 데이터받아올수있게 리스너 설정
        mChart.setDragEnabled(true);
        mChart.setScaleEnabled(true);
        mChart.setDrawGridBackground(false);
        mChart.setBackgroundColor(Color.parseColor("#14213d"));
        mChart.setDescription(null);                                                                // 차트에서 Description 설정 저는 따로 안했습니다.
        data = new LineData();                                                                      // LineDataSet을 담는 그릇. 여러개의 라인 데이터도 가능
        data.setValueTextColor(Color.WHITE);
        mChart.setData(data);
        Legend l = mChart.getLegend();
        l.setPosition(Legend.LegendPosition.ABOVE_CHART_LEFT);                                      //레전드 설정 (차트 위 왼쪽에)
        l.setForm(Legend.LegendForm.LINE);
        l.setTextColor(Color.WHITE);
        l.setTextSize(20);
        XAxis xl;
        xl = mChart.getXAxis();                                                                      // xl은 x축
        xl.setTextColor(Color.WHITE);
        xl.setDrawGridLines(false);                                                                  // x의 선 그리지 않게
        xl.setAvoidFirstLastClipping(true);
        xl.setEnabled(true);
        xl.setValueFormatter(new IntegerValueFormatter());                                           // x축 값 정수로 바꿈
        xl.setPosition(XAxis.XAxisPosition.BOTTOM);                                                  // x축이 그래프 밑쪽에 있게
        YAxis leftAxis = mChart.getAxisLeft();                                                       // leftAxis는 y축의 왼쪽설정
        leftAxis.setDrawGridLines(false);
        leftAxis.setTextColor(Color.WHITE);
        YAxis rightAxis = mChart.getAxisRight();                                                     // rightAxis는 아무것도 없게
        rightAxis.setEnabled(false);
        mChart.setOnChartGestureListener(new OnChartGestureListener() {
            @Override
            public void onChartGestureStart(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {
            }

            @Override
            public void onChartGestureEnd(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {
            }

            @Override
            public void onChartLongPressed(MotionEvent me) {
                if (moveToLastEntry) {
                    moveToLastEntry = false;
                    longClick.setText("<long tap>고정 그래프 : ON");
                } else {
                    moveToLastEntry = true;                                                             // 길게 누르면 추가되는게 보이게
                    longClick.setText("<long tap>고정 그래프 : OFF");
                }
            }

            @Override
            public void onChartDoubleTapped(MotionEvent me) {
                if (isDoubleTap) {
                    isDoubleTap = false;
                    doubleClick.setText("<double tap>터치로 로그보기 : OFF");
                } else {
                    isDoubleTap = true;
                    doubleClick.setText("<double tap>터치로 로그보기 : ON");
                }
            }

            @Override
            public void onChartSingleTapped(MotionEvent me) {
            }

            @Override
            public void onChartFling(MotionEvent me1, MotionEvent me2, float velocityX, float velocityY) {
            }

            @Override
            public void onChartScale(MotionEvent me, float scaleX, float scaleY) {
            }

            @Override
            public void onChartTranslate(MotionEvent me, float dX, float dY) {
            }
        });
    }
    //차트에 필요한 라인데이터셋 만드는 함수
    private LineDataSet createSet(String topic) {
        LineDataSet set = new LineDataSet(null, topic);
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setColor(ColorTemplate.getHoloBlue());
        set.setCircleColor(Color.WHITE);
        set.setLineWidth(2f);
        set.setCircleRadius(4f);
        set.setFillAlpha(65);
        set.setFillColor(ColorTemplate.getHoloBlue());
        set.setHighLightColor(Color.rgb(244, 117, 117));
        set.setValueTextColor(Color.WHITE);
        set.setValueTextSize(9f);
        set.setDrawValues(true);
        return set;
    }
    // 여러 버튼에 대한 이벤트
    public void select_topic_setting(View v) {
        if (v.getId() == R.id.btn_clear) {                                                          // 리스트 뷰 비우기 버튼
            logTopic.clear();                                                                       // 모든 ArrayList 비움
            logTime.clear();
            jsonMSG.clear();
            jsonID.clear();
            topicList.clear();
            pick_logList.clear();
            pick_logTime.clear();
            pick_logTopic.clear();
            logAdapter.notifyDataChanged(pick_logList,pick_logTime,pick_logTopic);                  // 선택 리스트 뷰 다시 그리기
            topicAdapter.notifyDataSetChanged();                                                    // 토픽 리스트 뷰 다시 그리기
            mChart.getLegend().setEnabled(false);                                                   // 그래프 또한 비우기
            pick = "";                                                                              // 선택한 토픽 없애기
            graph_clear();
        } else if (v.getId() == R.id.btn_publicsh) {                                                // 보내기 버튼 선택시
            final Dialog custom = new Dialog(this);                                         // 다이얼로그 생성
            custom.setContentView(R.layout.custom_layout);
            final EditText topic = custom.findViewById(R.id.send_topic);
            final EditText msg = custom.findViewById(R.id.send_message);

            Button ok = custom.findViewById(R.id.ok);
            Button clear = custom.findViewById(R.id.clear);
            Button cancel = custom.findViewById(R.id.cancel);
            if(!pre.equals("")){                                                                    // 이전에 보낸 토픽이 존재한다면
                topic.setText(pre);                                                                 // 동일한 토픽으로 설정
            }
            else if (!pick.equals("")){                                                             // 선택한 토픽이 있다면
                topic.setText(pick);                                                                // 선택한 토픽으로 설정
            }
            ok.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {                                                    // 확인 버튼 선택시
                    try {
                        String message = "{\"id\":\""+ClientID+"\",\"message\":\""+msg.getText().toString()+"\"}"; // 메시지 문자열화
                        String topiC = topic.getText().toString();                                  // 토픽 받아옴
                        mqttAndroidClient.publish(topiC, new MqttMessage(message.getBytes()));      // 토픽과 메시지로 발행
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                }
            });
            clear.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {                                                    // 클리어 선택시 모든 에디트 뷰 비우기
                    topic.setText("");
                    msg.setText("");
                }
            });
            cancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {                                                    // 취소 선택시 다이얼로그 종료
                    custom.dismiss();
                }
            });
            custom.show();
        }
        else if(v.getId() == R.id.btn_remove) {                                                     // 특정 토픽 지우기
            if(pick.equals("$connection")){                                                         // 컨넥트 토픽 선택시 관련 리스트 비우기
                connect_time.clear();
                connect_state.clear();
                connect_id.clear();
                topicList.remove("$connection");
                pick = "";
            }
            else if(pick.equals("$subscribe")){                                                     // 구독 토픽 선택시 관련 리스트 비우기
                sub_topic.clear();
                sub_id.clear();
                sub_topic.clear();
                topicList.remove("$subscribe");
                pick = "";
            }
            else if (!pick.equals("")) {                                                            // 일반 토픽 선택시
                topicList.remove(pick);                                                             // 관련 리스트 비움
                for (int i = 0; i < jsonMSG.size(); i++) {
                    if (logTopic.get(i).toString().equals(pick)) {
                        logTime.remove(i);
                        jsonMSG.remove(i);
                        jsonID.remove(i);
                        logTopic.remove(i);
                        i--;
                    }
                }
                pick_logList.clear();
                pick_logTime.clear();
                pick_logTopic.clear();
            }
            topicAdapter.notifyDataSetChanged();
            logAdapter.notifyDataSetChanged();
            graph_clear();
            pick = "";
        }
    }
    // 그래프에 데이터 추가 함수
    public void addEntry(float x,String topic){
        LineData data = mChart.getData();

        if (data != null) {
            ILineDataSet set = data.getDataSetByIndex(0);

            if (set == null) {
                set = createSet(topic);
                data.addDataSet(set);
            }

            data.addEntry(new Entry(set.getEntryCount(), x), 0);
            data.notifyDataChanged();
            mChart.notifyDataSetChanged();
            mChart.setVisibleXRangeMaximum(30);

            if (moveToLastEntry) {
                mChart.moveViewToX(data.getEntryCount());
            }
        }
    }
    // 그래프를 비우는 함수
    public void graph_clear(){
        data.clearValues();
        //mChart.clear();
        mChart.invalidate();
    }
    // 그래프에 선택되었을때 실행되는 함수
    @Override
    public void onValueSelected(Entry e, Highlight h) {
        index =0;
        for(int i= 0 ; i<(int)e.getX();i++){
            try {
                Float num = Float.valueOf(pick_logList.get(i));
            }catch(NumberFormatException ne){ try{
                int num = Integer.valueOf(pick_logList.get(i));
            }catch (NumberFormatException fd){index++;}
            }
        }
        index+=(int)e.getX();
        listview.post(new Runnable() {
            @Override
            public void run() {
                listview.requestFocusFromTouch();
                listview.setSelection(index);
            }
        });

        if(isDoubleTap) {
            Toast.makeText(this, "Time: " + pick_logTime.get((int)e.getX()) + "  Data: " + e.getY(), Toast.LENGTH_SHORT).show();
        }
    }
    @Override
    public void onNothingSelected() {
        Log.i("Nothing selected", "Nothing selected.");
    }
    // 그래프에서 X값을 처리하기 위한 함수
    class IntegerValueFormatter implements IAxisValueFormatter {
        public IntegerValueFormatter() {
        }

        @Override
        public String getFormattedValue(float v, AxisBase axisBase) {
            try {
                return pick_logTime.get((int) v);
            }catch(IndexOutOfBoundsException e) {
                return "";
            }
        }
    }
    // 리모컨에 대한 이벤트 처리
    public void remotecontroll (View v) throws MqttException{
        String topic = "RADON/CONTROL/LED";
        if(v.getId() == R.id.Aon){
            Button btn = findViewById(v.getId());
            mqttAndroidClient.publish(topic,
                    new MqttMessage(new String("{\"id\":\""+ClientID+"\",\"message\":\""+btn.getText().toString()+"\"}").getBytes()));
        }
        else if(v.getId() == R.id.Aoff){
            Button btn = findViewById(v.getId());
            mqttAndroidClient.publish(topic,
                    new MqttMessage(new String("{\"id\":\""+ClientID+"\",\"message\":\""+btn.getText().toString()+"\"}").getBytes()));
        }
        else if(v.getId() == R.id.Ron){
            Button btn = findViewById(v.getId());
            mqttAndroidClient.publish(topic,
                    new MqttMessage(new String("{\"id\":\""+ClientID+"\",\"message\":\""+btn.getText().toString()+"\"}").getBytes()));
        }
        else if(v.getId() == R.id.Roff){
            Button btn = findViewById(v.getId());
            mqttAndroidClient.publish(topic,
                    new MqttMessage(new String("{\"id\":\""+ClientID+"\",\"message\":\""+btn.getText().toString()+"\"}").getBytes()));
        }
        else if(v.getId() == R.id.Gon){
            Button btn = findViewById(v.getId());
            mqttAndroidClient.publish(topic,
                    new MqttMessage(new String("{\"id\":\""+ClientID+"\",\"message\":\""+btn.getText().toString()+"\"}").getBytes()));
        }
        else if(v.getId() == R.id.Goff){
            Button btn = findViewById(v.getId());
            mqttAndroidClient.publish(topic,
                    new MqttMessage(new String("{\"id\":\""+ClientID+"\",\"message\":\""+btn.getText().toString()+"\"}").getBytes()));
        }
        else if(v.getId() == R.id.G2on){
            Button btn = findViewById(v.getId());
            mqttAndroidClient.publish(topic,
                    new MqttMessage(new String("{\"id\":\""+ClientID+"\",\"message\":\""+btn.getText().toString()+"\"}").getBytes()));
        }
        else if(v.getId() == R.id.G2off){
            Button btn = findViewById(v.getId());
            mqttAndroidClient.publish(topic,
                    new MqttMessage(new String("{\"id\":\""+ClientID+"\",\"message\":\""+btn.getText().toString()+"\"}").getBytes()));
        }

    }
    public void Mqtt_setting(Context context) throws MqttException {
        MqttAndroidClient mqttAndroidClient;
        mqttAndroidClient = new MqttAndroidClient(context, "ssl:127.0.0.1:1883",ClientID);
        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        IMqttToken token = mqttAndroidClient.connect(mqttConnectOptions);
        token.setActionCallback(new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
                Toast.makeText(getBaseContext(), "연결성공", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                Toast.makeText(getBaseContext(), "실패", Toast.LENGTH_SHORT).show();
            }
        });
        mqttAndroidClient.subscribe("topic",0);
        mqttAndroidClient.publish("topic",
                new MqttMessage(new String("Message").getBytes()));
        mqttAndroidClient.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                Toast.makeText(getBaseContext(), "연결 끊어짐", Toast.LENGTH_SHORT).show();
            }
            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                String msg = new String(message.getPayload());
                Toast.makeText(getBaseContext(), "receive : "+msg, Toast.LENGTH_SHORT).show();
            }
            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });
    }
}