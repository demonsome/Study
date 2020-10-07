/*
	
	此類別為量測 PPG 畫面，主要包含連接手機 serial port 以及接收 PPG 模組的資料

*/

package com.example.luolab.measureppg;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.StrictMode;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Stack;
import java.util.zip.Inflater;


public class GuanView extends Fragment {

    private final int SerialDataSize = 30020;

    private View GuanView;
    private View dialogView;
    private View menu_dialogView;

    private DoubleTwoDimQueue dataQ;

    private int startPointer;
    private int endPointer;
    private int fftPoints;
    private int image_processed;
    private int state_fft;
    private int FPS;

    private long BPM;

    private Stack<Long> timestampQ;

    private TextView imgProcessed;

    private boolean first_fft_run;
    private boolean start_fft;
    private boolean keep_thread_running;

    private boolean serialopen;

    private Handler fileHandler;
    private Handler mHandler;

    private Spinner mySpinner;

    private String SpinnerSelected;

    private Button start_btn;
    private Button setUiInfo_btn;
    private Button menu_btn;
    private Button Preview_Btn;

    private GraphView G_Graph;
    private LineGraphSeries<DataPoint> G_Series;

    private int[] TempSize = new int[2];
    private int SizeIndex = 0;

    private byte[] SerialData_Queue = new byte[SerialDataSize];
    private int Queue_Index_Rear = 0;
    private int Queue_Index_Front = 0;

    private int mXPoint;
    private int PPGTime = 1;
    private int Scale = 150;
    private int Time_GET = 0;
    private int Min_Time_GET = 0;
    private int Min_Time_Flag = 0;

    private Thread myFFTThread;

    private Calendar c;
    private SimpleDateFormat dateformat;

    private FileWriter fileWriter;
    private BufferedWriter bw;

    private File f;
    private String FilePath = null;

    private AlertDialog.Builder UsrInfoDialog_Builder;
    private AlertDialog UsrInfoDialog;

    private AlertDialog.Builder MenuDialog_Builder;
    private AlertDialog MenuDialog;

    private ArrayList<String> usrInfo_Array;
    private ArrayAdapter<String> usrInfo_Adapter;

    private LayoutInflater LInflater;

    private boolean SerialFlag = false;
    private boolean Stop_Flag = false;
    private boolean Preview_Flag = false;

    private TextView[] UsrInfo = new TextView[6];

    private TextView time_tv;
    private TextView Minute_tv;

    public int arg;

    private UiDataBundle appData;

    private String Get_Uri = "https://lens.csie.ncku.edu.tw/~Platform/getDataFromDB.php";
    private String Insert_Uri = "https://lens.csie.ncku.edu.tw/~Platform/insertDataToDB.php";
    private String Get_Query_Command = "SELECT * FROM PPG";
    private String Get_Query_Command_GSR = "SELECT * FROM gsr";
    private String Get_Query_Command_Guan = "SELECT * FROM guan";
    private String Insert_Query_Command = "INSERT INTO PPG (name,age,birthday,height,weight,doctor)VALUES";
    private String Insert_Query_Command_GSR = "INSERT INTO GSR (name,age,birthday,height,weight,doctor)VALUES";
    private String Insert_Query_Command_Guan = "INSERT INTO guan (name,age,birthday,height,weight,doctor)VALUES";
    private String Update_Command = "UPDATE PPG SET ";
    private String Update_Command_GSR = "UPDATE GSR SET ";
    private String Update_Command_Guan = "UPDATE guan SET ";

    public final String ACTION_USB_PERMISSION = "com.hariharan.arduinousb.USB_PERMISSION";
    UsbManager usbManager;
    UsbDevice device;
    UsbSerialDevice serialPort;
    UsbDeviceConnection connection;







    // Serial port 有資料傳進來手機時就會被觸發此事件，可進行讀取資料
    UsbSerialInterface.UsbReadCallback mCallback = new UsbSerialInterface.UsbReadCallback() { //Defining a Callback which triggers whenever data is read.
        @Override
        public void onReceivedData(byte[] arg0) {
            SetChoice(arg0,arg0.length,LInflater);
        }
    };


	
	// 進行 Serial port 設定
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        //Broadcast Receiver to automatically start and stop the Serial connection.
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ACTION_USB_PERMISSION)) {
                boolean granted = intent.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
                if (granted) {
                    connection = usbManager.openDevice(device);
                    serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection);
                    if (serialPort != null) {
                        if (serialPort.open()) { //Set Serial Connection Parameters.
                            serialPort.setBaudRate(9600);
                            serialPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
                            serialPort.setStopBits(UsbSerialInterface.STOP_BITS_1);
                            serialPort.setParity(UsbSerialInterface.PARITY_NONE);
                            serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                            serialPort.read(mCallback);
                            SerialFlag = true;
                        } else {
                            Log.d("SERIAL", "PORT NOT OPEN");
                        }
                    } else {
                        Log.d("SERIAL", "PORT IS NULL");
                    }
                } else {
                    Log.d("SERIAL", "PERM NOT GRANTED");
                }
            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
                onClickStart();
            }
        };
    };




	// 與 Arduino beetle 進行連線
    private void onClickStart(){
        HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
        if (!usbDevices.isEmpty()) {
            boolean keep = true;
            for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
                device = entry.getValue();
                int deviceVID = device.getVendorId();
                if (deviceVID == 0x2341)//Arduino Vendor ID
                {
                    PendingIntent pi = PendingIntent.getBroadcast(LInflater.getContext(), 0, new Intent(ACTION_USB_PERMISSION), 0);
                    usbManager.requestPermission(device, pi);
                    keep = false;
                } else {
                    connection = null;
                    device = null;
                }

                if (!keep)
                    break;
            }
        }
    }
	
	// 更新 UI 畫面
    private void UpdateBPMUi() {
        mHandler = new Handler(Looper.getMainLooper()){
            @Override
            public void handleMessage(Message inputMessage){
                UiDataBundle incoming = (UiDataBundle) inputMessage.obj;

                if(BPM > 0) {
                    if(fftPoints < 1024){
                        imgProcessed.setTextColor(Color.rgb(100,100,200));
                    }
                    else{
                        imgProcessed.setTextColor(Color.rgb(100,200,100));
                    }
                    imgProcessed.setText("" + BPM);
                }

                Minute_tv.setText(Integer.toString(Min_Time_GET));
                time_tv.setText(Integer.toString(Time_GET));

            }
        };
    }
    public View onCreateView(final LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState){


        GuanView = inflater.inflate(R.layout.guan, container, false);

        LInflater = inflater;

        fileHandler = new Handler();

        fileWriter = null;
        bw = null;

        dateformat  = new SimpleDateFormat("yyyyMMddHHmmss");

        FilePath = String.valueOf(inflater.getContext().getExternalFilesDir(null)) + "/Guan";
        f = new File(String.valueOf(FilePath));
        f.mkdir();

        Preview_Btn = GuanView.findViewById(R.id.Preview_btn);

        usbManager = (UsbManager) inflater.getContext().getSystemService(inflater.getContext().USB_SERVICE);
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        inflater.getContext().registerReceiver(broadcastReceiver, filter);


        appData =new UiDataBundle();

        G_Graph = GuanView.findViewById(R.id.data_chart);

        imgProcessed = GuanView.findViewById(R.id.AvgBPM_tv);

        time_tv = GuanView.findViewById(R.id.time_tv);
        Minute_tv = GuanView.findViewById(R.id.Minute_tv);

        setUiInfo_btn = GuanView.findViewById(R.id.SetUsrInfo_btn);
        setUiInfo_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateDB();
                UsrInfoDialog.show();
            }
        });

        dataQ = new DoubleTwoDimQueue();
        startPointer = 0;
        endPointer = 0;
        fftPoints = 1024;
        image_processed = 0;
        first_fft_run = true;
        keep_thread_running = false;
        FPS = 25;
        BPM = 0;
        state_fft = 0;
        timestampQ = new Stack<Long>();
		
		
		// 設定 Start 按鈕事件
        start_btn = GuanView.findViewById(R.id.Start_btn);
        start_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(SerialFlag == true) {
                    VarReset();
                    ResetGraph();
                    StartBtn_Click(inflater);
                    keep_thread_running = true;
                    fftTHREAD();
                    myFFTThread.start();
                    OutputFile();
                }
            }
        });

        Preview_Btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(SerialFlag == true) {
                    Preview_Flag = true;
                    ResetGraph();
                    StartBtn_Click(inflater);
                    keep_thread_running = true;
                    fftTHREAD();
                    myFFTThread.start();
                }
            }
        });
		
		// 設定 Option 按鈕事件
        menu_btn = GuanView.findViewById(R.id.Optional_btn);
        menu_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MenuDialog.show();
            }
        });

        usrInfo_Array = new ArrayList<String>();

        dialogView = View.inflate(inflater.getContext(),R.layout.user_info,null);
        setUsrInfo();
        menuDialog();

        setUi(0);
        updateDB();
        onClickStart();
        UpdateBPMUi();

        return GuanView;
    }



	// 設定 FFT 點數
    private void handleInputData() {
        //Triggering for FFT
        if(first_fft_run){
            if(image_processed >= 1024) {
                fftPoints = 1024;
                startPointer = 10;
                endPointer = image_processed - 1;
                start_fft = true;
                first_fft_run = false;
                image_processed = 0;
            }
            else if((image_processed >= 768) && (image_processed < 512) && (state_fft == 2)){
                state_fft++;
                fftPoints = 512;
                endPointer = image_processed - 1;
                start_fft = true;
            } else if((image_processed >= 512) && (image_processed < 1024) && (state_fft == 1)){
                state_fft++;
                fftPoints = 512;
                endPointer = image_processed - 1;
                start_fft = true;
            } else if((image_processed >= 256) && (image_processed < 512) &&(state_fft == 0)){
                state_fft++;
                fftPoints = 256;
                endPointer = image_processed - 1;
                start_fft = true;
            }
        } else {
            if(image_processed >= 128){
                startPointer = startPointer  + image_processed;
                endPointer = endPointer + image_processed;
                start_fft = true;
                image_processed = 0;
            }
        }
    }
	// 透過 FFT 計算 BPM
    private void fftTHREAD()
    {
        myFFTThread = new Thread(){
            @Override
            public void run(){
                while(keep_thread_running){
                    if (start_fft == false){

                        //Sleeping part may lead to timing problems
                        try {
                            Thread.sleep(100);
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                    else {
                        start_fft = false;

                        double[][] sample_arr = new double[fftPoints][2];
                        double[]   input_arr = new double[fftPoints];
                        double[] freq_arr = new double[fftPoints];
                        fftLib f = new fftLib();

                        sample_arr = dataQ.toArray(startPointer, endPointer);
                        //input_arr = dataQ.toArray(startPointer, endPointer, 0);

                        long timeStart  = timestampQ.get(startPointer);
                        long timeEnd    = timestampQ.get(endPointer);


                        FPS =  (fftPoints * 1000)/ (int)(timeEnd - timeStart) ;





                    }
                }
            }
        };
    }
	
	// 設定 傳送至 PPG 模組的指令
    private void StartBtn_Click(LayoutInflater inflater){
        byte[] Data = new byte[1];

        if(Preview_Flag == true)
            Data[0] = 0x30;
        else {
            if (PPGTime == 1)
                Data[0] = 0x31;
            else if (PPGTime == 2)
                Data[0] = 0x32;
            else if (PPGTime == 3)
                Data[0] = 0x33;
            else if (PPGTime == 4)
                Data[0] = 0x34;
            else if (PPGTime == 5)
                Data[0] = 0x35;
        }

        serialPort.write(Data);
    }
	// 將收到的 Serial 資料存入 Queue
    private void PushSerialData(byte[] data,int size)
    {
        for(int i = 0 ; i < size ; i++) {
            if(Queue_Index_Front == (SerialDataSize - 1))
                Queue_Index_Front = 0;
            SerialData_Queue[Queue_Index_Rear++] = data[i];
        }
    }


	// 將 Queue 裡的資料 pop出來
    private byte PopSerialData()
    {
        if(Queue_Index_Front == (SerialDataSize - 1))
            Queue_Index_Front = 0;
        return SerialData_Queue[Queue_Index_Front++];
    }


	// 看是預先查看訊號還是直接開始量測
    private void SetChoice(byte[] buf,int size,LayoutInflater inflater){
        if(Preview_Flag == true) {
            PushSerialData(buf, size);
            UpdateGraph_2(size, inflater);
        }else{
            PushSerialData(buf,size);
            UpdateGraph(size,inflater);
        }
    }

    // 處裡每個 2Bytes 資料的合併

    private void AppedSeriesData(int size,LayoutInflater inflater)
    {
        double queueData[][] = new double[1][2];

        if(size % 2 != 0){
            TempSize[SizeIndex] = size;
        }
        if(SizeIndex > 0 && (TempSize[0] + size) % 2 != 0){
            for(int i = 0; i < (size + TempSize[0] - 1) / 2; i++) {
                int data = (int)(PopSerialData() << 8);
                int data2 =  (int)(PopSerialData());

                if(data2 < 0)
                    data2 += 256;

                G_Series.appendData(new DataPoint(mXPoint++,(data + data2)-450), true, 400);
                queueData[0][0] = (data + data2) - 450;
                queueData[0][1] = 0.0;
                dataQ.Qpush(queueData);
                image_processed++;
            }
            SizeIndex = 0;
            TempSize[0] = 1;
            TempSize[1] = 0;
        }
        else if(SizeIndex > 0 && (TempSize[0] + size) % 2 == 0){
            for(int i = 0; i < (size + TempSize[0]) / 2; i++) {
                int data = (int)(PopSerialData() << 8);
                int data2 =  (int)(PopSerialData());

                if(data2 < 0)
                    data2 += 256;

                G_Series.appendData(new DataPoint(mXPoint++,(data + data2)-450), true, 400);
                queueData[0][0] = (data + data2) - 450;
                queueData[0][1] = 0.0;
                dataQ.Qpush(queueData);
                image_processed++;
            }
            SizeIndex = -1;
            TempSize[0] = 0;
            TempSize[1] = 0;
        }
        else if(size % 2 == 0){
            for(int i = 0; i < size / 2; i++) {
                int data = (int)(PopSerialData() << 8);
                int data2 =  (int)(PopSerialData());

                if(data2 < 0)
                    data2 += 256;

                G_Series.appendData(new DataPoint(mXPoint++,(data + data2)-450), true, 400);
                queueData[0][0] = (data + data2) - 450;
                queueData[0][1] = 0.0;
                dataQ.Qpush(queueData);
                image_processed++;
            }
        }
        handleInputData();
        timestampQ.push((Long) System.currentTimeMillis());
        /*
        if(mXPoint == Integer.parseInt(selectSampleRate)) {
            Stop_Flag = true;
        }
        */

        SizeIndex++;
    }
	
	// 將收到的 Serial 資料 進行畫圖呈現
    private void UpdateGraph(final int size, final LayoutInflater inflater){
        G_Graph.post(new Runnable() {
            @Override
            public void run() {
                AppedSeriesData(size, LInflater);
                G_Graph.getViewport().setMaxX(mXPoint);
                //G_Graph.getViewport().setMinX(0);
                G_Graph.getViewport().setMinX(mXPoint - Scale);
                //mXPoint += 1;

                Time_GET = mXPoint / 25 - Min_Time_Flag;

                if(mXPoint >= 25) {
                    if (mXPoint == 1500) {
                        Min_Time_GET = 1;
                        Min_Time_Flag = 60;
                    } else if (mXPoint == 3000) {
                        Min_Time_GET = 2;
                        Min_Time_Flag = 120;
                    } else if (mXPoint == 4500) {
                        Min_Time_GET = 3;
                        Min_Time_Flag = 180;
                    } else if (mXPoint == 6000) {
                        Min_Time_GET = 4;
                        Min_Time_Flag = 240;
                    } else if (mXPoint == 7500) {
                        Min_Time_GET = 5;
                        Min_Time_Flag = 300;
                    }
                }
                if(mXPoint >= (PPGTime * 1500)) {
                    Stop_Flag = true;
                }

                Message uiMessage = mHandler.obtainMessage(1, appData);
                uiMessage.sendToTarget();
            }
        });
    }
    private void AppedSeriesData_2(int size,LayoutInflater inflater)
    {
        double queueData[][] = new double[1][2];

        if(size % 2 != 0){
            TempSize[SizeIndex] = size;
        }
        if(SizeIndex > 0 && (TempSize[0] + size) % 2 != 0){
            for(int i = 0; i < (size + TempSize[0] - 1) / 2; i++) {
                int data = (int)(PopSerialData() << 8);
                int data2 =  (int)(PopSerialData());

                if(data2 < 0)
                    data2 += 256;


                G_Series.appendData(new DataPoint(mXPoint++,(data + data2)-450), true, 400);
                queueData[0][0] = (data + data2) - 450;
                queueData[0][1] = 0.0;
                dataQ.Qpush(queueData);
                image_processed++;
            }
            SizeIndex = 0;
            TempSize[0] = 1;
            TempSize[1] = 0;
        }
        else if(SizeIndex > 0 && (TempSize[0] + size) % 2 == 0){
            for(int i = 0; i < (size + TempSize[0]) / 2; i++) {
                int data = (int)(PopSerialData() << 8);
                int data2 =  (int)(PopSerialData());

                if(data2 < 0)
                    data2 += 256;

                G_Series.appendData(new DataPoint(mXPoint++,(data + data2)-450), true, 400);
                queueData[0][0] = (data + data2) - 450;
                queueData[0][1] = 0.0;
                dataQ.Qpush(queueData);
                image_processed++;
            }
            SizeIndex = -1;
            TempSize[0] = 0;
            TempSize[1] = 0;
        }
        else if(size % 2 == 0){
            for(int i = 0; i < size / 2; i++) {
                int data = (int)(PopSerialData() << 8);
                int data2 =  (int)(PopSerialData());

                if(data2 < 0)
                    data2 += 256;

                G_Series.appendData(new DataPoint(mXPoint++,(data + data2)-450), true, 400);
                queueData[0][0] = (data + data2) - 450;
                queueData[0][1] = 0.0;
                dataQ.Qpush(queueData);
                image_processed++;
            }
        }
        handleInputData();
        timestampQ.push((Long) System.currentTimeMillis());
        /*
        if(mXPoint == Integer.parseInt(selectSampleRate)) {
            Stop_Flag = true;
        }
        */

        SizeIndex++;
    }
	
    private void UpdateGraph_2(final int size, final LayoutInflater inflater){
        G_Graph.post(new Runnable() {
            @Override
            public void run() {
                AppedSeriesData_2(size, LInflater);
                G_Graph.getViewport().setMaxX(mXPoint);
                //G_Graph.getViewport().setMinX(0);
                G_Graph.getViewport().setMinX(mXPoint - Scale);
                //mXPoint += 1;


                Time_GET = mXPoint / 25 - Min_Time_Flag;

                if(mXPoint >= 750) {
                    //mXPoint = 0;
                    Toast.makeText(LInflater.getContext(),"量測前測試已完畢",Toast.LENGTH_SHORT).show();
                }

                Message uiMessage = mHandler.obtainMessage(1, appData);
                uiMessage.sendToTarget();
            }
        });
    }
	// 所有變數進行 初始化
    private void VarReset()
    {
        timestampQ = null;
        timestampQ = new Stack<Long>();
        dataQ = null;
        dataQ = new DoubleTwoDimQueue();

        startPointer = 0;
        endPointer = 0;
        fftPoints = 1024;
        image_processed = 0;
        first_fft_run = true;
        keep_thread_running = false;
        Preview_Flag = false;
        FPS = 25;
        BPM = 0;
        state_fft = 0;
        Stop_Flag = false;
    }
	// 將畫圖 初始化
    private void ResetGraph()
    {
        G_Graph.getViewport().setMaxX(5);
        //G_Graph.getViewport().setMaxY(255);
        G_Graph.getViewport().setMaxY(500);
        G_Graph.getViewport().setMinY(-100);
        //G_Graph.getViewport().setMinY(20);
        G_Graph.getViewport().setYAxisBoundsManual(true);

        G_Graph.getViewport().setMinX(0);
        G_Graph.getGridLabelRenderer().setHighlightZeroLines(false);
//        G_Graph.getGridLabelRenderer().setGridStyle(GridLabelRenderer.GridStyle.HORIZONTAL);
//        G_Graph.getGridLabelRenderer().setNumVerticalLabels(3);
//        G_Graph.getGridLabelRenderer().setPadding(15);
        G_Graph.getViewport().setXAxisBoundsManual(true);

        G_Graph.getGridLabelRenderer().reloadStyles();

        G_Graph.removeAllSeries();
        G_Series = new LineGraphSeries<DataPoint>();
        G_Graph.addSeries(G_Series);
        mXPoint = 0;
        Time_GET = 0;
        Min_Time_GET = 0;
        Min_Time_Flag = 0;

        Queue_Index_Rear = 0;
        Queue_Index_Front = 0;

        for(int i = 0 ; i < SerialData_Queue.length ; i++)
            SerialData_Queue[i] = 0;

        TempSize[0] = 0;
        TempSize[1] = 0;
        SizeIndex = 0;
    }



	// 將資料進行上傳以及存檔
    private void OutputFile(){
        fileHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(Stop_Flag && mXPoint != 0) {
                    try {
                        mXPoint = 0;
                        keep_thread_running = false;
                        time_tv.setText(Integer.toString(0));
                        new AlertDialog.Builder((Activity) LInflater.getContext()).setMessage("已量完畢，" + '\n' + '\n' + "如需量測別的受測者" + '\n' + "請按setUsrInfo更改")
                                .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {

                                    }
                                })
                                .create()
                                .show();

                        c = Calendar.getInstance();
                        fileWriter = new FileWriter(FilePath + "/" + dateformat.format(c.getTime()) + UsrInfo[0].getText() + ".txt", false);

/*
                        String result = GetDB(Get_Query_Command_Guan,Get_Uri);
                        JSONArray jsonArray = null;

                        int id = 0;

                        try {
                            jsonArray = new JSONArray(result);
                            for(int i = 0; i < jsonArray.length(); i++) {
                                JSONObject jsonData = jsonArray.getJSONObject(i);
                                if(UsrInfo[0].getText().toString().equals(jsonData.getString("name"))){
                                    id = Integer.parseInt(jsonData.getString("id"));
                                }
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        GetDB(Update_Command_Guan + "name='" + UsrInfo[0].getText().toString() + "',"
                                + "age='" + UsrInfo[1].getText().toString() + "',"
                                + "birthday='" + UsrInfo[2].getText().toString() + "',"
                                + "height='" + UsrInfo[3].getText().toString() + "',"
                                + "weight='" + UsrInfo[4].getText().toString() + "',"
                                + "mtime='" + PPGTime + "',"
                                + "time='" + dateformat.format(c.getTime()) + "',"
                                + "samplerate='25',"
                                + "Avg='" + BPM + "',"
                                + "value='" + Arrays.toString(dataQ.toArray(0, endPointer, 0)) + "',"
                                + "doctor='" + UsrInfo[5].getText().toString()
                                + "' WHERE id=" + id,Insert_Uri);
*/






/*
                        result = GetDB(Get_Query_Command_GSR,Get_Uri);
                        jsonArray = null;

                        id = 0;

                        try {
                            jsonArray = new JSONArray(result);
                            for(int i = 0; i < jsonArray.length(); i++) {
                                JSONObject jsonData = jsonArray.getJSONObject(i);
                                if(UsrInfo[0].getText().toString().equals(jsonData.getString("name"))){
                                    id = Integer.parseInt(jsonData.getString("id"));
                                }
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        GetDB(Update_Command_GSR + "name='" + UsrInfo[0].getText().toString() + "',"
                                + "age='" + UsrInfo[1].getText().toString() + "',"
                                + "birthday='" + UsrInfo[2].getText().toString() + "',"
                                + "height='" + UsrInfo[3].getText().toString() + "',"
                                + "weight='" + UsrInfo[4].getText().toString() + "',"
                                + "doctor='" + UsrInfo[5].getText().toString()
                                + "' WHERE id=" + id,Insert_Uri);


                        GetDB("UPDATE whichid SET which='" + id + "' WHERE id=1",Insert_Uri);

                        bw = new BufferedWriter(fileWriter);
                        SetFileHeader(bw);
                        bw.write(Arrays.toString(dataQ.toArray(0, endPointer, 0)));
                        bw.close();
*/
                        VarReset();

                    }catch(IOException e) {
                        e.printStackTrace();
                    }
                }
                fileHandler.postDelayed(this, 1000);
            }
        },1000);
    }

    public String PpgToString (double a[])
    {
        String ans=",";
        int l=a.length;
        for (int j = 0; j < l; j++) {
            ans = ans+a[j]+',';
        }
        ans=ans+']';
        return ans;
    }

	// 存檔檔案名稱的標頭檔
    private void SetFileHeader(BufferedWriter bw)
    {
        try {
            bw.write("時間 : " + dateformat.format(c.getTime()));
            bw.newLine();
            bw.newLine();
            bw.write("量測時間 : " + PPGTime + "分鐘");
            bw.newLine();
            bw.newLine();
            bw.write("年齡 : " + UsrInfo[1].getText());
            bw.newLine();
            bw.newLine();
            bw.write("生日 : " + UsrInfo[2].getText());
            bw.newLine();
            bw.newLine();
            bw.write("身高 : " + UsrInfo[3].getText());
            bw.newLine();
            bw.newLine();
            bw.write("體重 : " + UsrInfo[4].getText());
            bw.newLine();
            bw.newLine();
            bw.write("SampleRate = 25");
            bw.newLine();
            bw.newLine();
            bw.write("訊號 : ");
            bw.newLine();
            bw.newLine();
        }catch(IOException e){
            e.printStackTrace();
        }
    }
	// Option 對話窗格
    private void menuDialog()
    {
        menu_dialogView = View.inflate(LInflater.getContext(),R.layout.menu,null);

        MenuDialog_Builder = new AlertDialog.Builder((Activity)LInflater.getContext())
                .setTitle("Option")
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        TextView scale_tv = MenuDialog.findViewById(R.id.Scale_tv);
                        TextView ppgtime_tv = MenuDialog.findViewById(R.id.PPG_Time_tv);

                        if(!scale_tv.getText().toString().equals(""))
                            Scale = Integer.parseInt(scale_tv.getText().toString());
                        if(!ppgtime_tv.getText().toString().equals(""))
                            PPGTime = Integer.parseInt(ppgtime_tv.getText().toString());
                        if(scale_tv.getText().toString().equals("") && ppgtime_tv.getText().toString().equals("")){
                            Scale = 150;
                            PPGTime = 1;
                        }
                    }
                });
        MenuDialog = MenuDialog_Builder.create();
        MenuDialog.setView(menu_dialogView);
    }
	// 設定使用者資訊 視窗
    private void setUsrInfo()
    {
        UsrInfoDialog_Builder = new AlertDialog.Builder((Activity)LInflater.getContext())
                .setTitle("CreatUsrInfo")
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        UsrInfo[0] = UsrInfoDialog.findViewById(R.id.Name_tv);
                        UsrInfo[1] = UsrInfoDialog.findViewById(R.id.Age_tv);
                        UsrInfo[2] = UsrInfoDialog.findViewById(R.id.Brithday_tv);
                        UsrInfo[3] = UsrInfoDialog.findViewById(R.id.Height_tv);
                        UsrInfo[4] = UsrInfoDialog.findViewById(R.id.Weight_tv);
                        UsrInfo[5] = UsrInfoDialog.findViewById(R.id.doctor_Name_tv);

                        if(UsrInfo[0].getText().toString().equals("") || UsrInfo[1].getText().toString().equals("") || UsrInfo[2].getText().toString().equals("") ||
                                UsrInfo[3].getText().toString().equals("") || UsrInfo[4].getText().toString().equals("") || UsrInfo[5].getText().toString().equals(""))
                        {
                            Toast.makeText(LInflater.getContext(),"請勿空白，確實填寫",Toast.LENGTH_SHORT).show();
                        }
                        else {
                            boolean flag = false;
                            String result = GetDB(Get_Query_Command_Guan,Get_Uri);
                            JSONArray jsonArray = null;
                            try {
                                jsonArray = new JSONArray(result);
                                for(int i = 0; i < jsonArray.length(); i++) {
                                    JSONObject jsonData = jsonArray.getJSONObject(i);
                                    if(UsrInfo[0].getText().toString().equals(jsonData.getString("name"))){
                                        flag = true;
                                    }
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            if(flag == false) {
                                GetDB(Insert_Query_Command_GSR +
                                        "('" + UsrInfo[0].getText().toString() + "','"
                                        + UsrInfo[1].getText().toString() + "','"
                                        + UsrInfo[2].getText().toString() + "','"
                                        + UsrInfo[3].getText().toString() + "','"
                                        + UsrInfo[4].getText().toString() + "','"
                                        + UsrInfo[5].getText().toString() + "')", Insert_Uri);
                                GetDB(Insert_Query_Command_Guan +
                                        "('" + UsrInfo[0].getText().toString() + "','"
                                        + UsrInfo[1].getText().toString() + "','"
                                        + UsrInfo[2].getText().toString() + "','"
                                        + UsrInfo[3].getText().toString() + "','"
                                        + UsrInfo[4].getText().toString() + "','"
                                        + UsrInfo[5].getText().toString() + "')", Insert_Uri);
                                usrInfo_Array.add(UsrInfo[0].getText().toString());
                            }
                            Toast.makeText(LInflater.getContext(),"設定完成",Toast.LENGTH_SHORT).show();
                            setUi(1);
                        }
                    }
                });
        UsrInfoDialog = UsrInfoDialog_Builder.create();
        UsrInfoDialog.setView(dialogView);

        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork()
                .penaltyLog()
                .build());

        usrInfo_Array = new ArrayList<String>();

        String result = GetDB(Get_Query_Command_Guan,Get_Uri);

        JSONArray jsonArray = null;
        try {
            jsonArray = new JSONArray(result);
            for(int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonData = jsonArray.getJSONObject(i);
                usrInfo_Array.add(jsonData.getString("name"));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        usrInfo_Adapter = new ArrayAdapter<String>(LInflater.getContext(),R.layout.usr_spinner,R.id.spinner_tv,usrInfo_Array);

        mySpinner = dialogView.findViewById(R.id.usrSpinner);
        mySpinner.setAdapter(usrInfo_Adapter);
        mySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                SpinnerSelected = parent.getSelectedItem().toString();

                ArrayList<String> Name = new ArrayList<>();
                ArrayList<String> Age = new ArrayList<>();
                ArrayList<String> Birthday = new ArrayList<>();
                ArrayList<String> Height = new ArrayList<>();
                ArrayList<String> Weight = new ArrayList<>();
                ArrayList<String> Doctor = new ArrayList<>();

                String result = GetDB(Get_Query_Command_Guan,Get_Uri);
                JSONArray jsonArray = null;
                try {
                    jsonArray = new JSONArray(result);
                    for(int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonData = jsonArray.getJSONObject(i);
                        Name.add(jsonData.getString("name"));
                        Age.add(jsonData.getString("age"));
                        Birthday.add(jsonData.getString("birthday"));
                        Height.add(jsonData.getString("height"));
                        Weight.add(jsonData.getString("weight"));
                        Doctor.add(jsonData.getString("doctor"));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                UsrInfo[0] = UsrInfoDialog.findViewById(R.id.Name_tv);
                UsrInfo[1] = UsrInfoDialog.findViewById(R.id.Age_tv);
                UsrInfo[2] = UsrInfoDialog.findViewById(R.id.Brithday_tv);
                UsrInfo[3] = UsrInfoDialog.findViewById(R.id.Height_tv);
                UsrInfo[4] = UsrInfoDialog.findViewById(R.id.Weight_tv);
                UsrInfo[5] = UsrInfoDialog.findViewById(R.id.doctor_Name_tv);

                for(int i = 0 ; i < Name.size() ;i++){
                    if(Name.get(i).equals(SpinnerSelected)){
                        UsrInfo[0].setText(Name.get(i));
                        UsrInfo[1].setText(Age.get(i));
                        UsrInfo[2].setText(Birthday.get(i));
                        UsrInfo[3].setText(Height.get(i));
                        UsrInfo[4].setText(Weight.get(i));
                        UsrInfo[5].setText(Doctor.get(i));
                        break;
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }
	// 設定 UI Enable 以及 Disable
    private void setUi(int state){
        if(state == 0){
            start_btn.setEnabled(false);
            setUiInfo_btn.setEnabled(true);
            Preview_Btn.setEnabled(false);
        }else{
            start_btn.setEnabled(true);
            setUiInfo_btn.setEnabled(true);
            Preview_Btn.setEnabled(true);
        }
    }
	
	// 從資料庫撈取資料
    private String GetDB(String Query_Command,String uri)
    {
        String result = null;
        try {
            result = DBConnector.executeQuery(Query_Command,uri);
        } catch(Exception e) {
        }
        return result;
    }
	// 將資料庫最新的訊息撈出來
    private void updateDB()
    {
        usrInfo_Array.clear();

        String result = GetDB(Get_Query_Command_Guan,Get_Uri);
        JSONArray jsonArray = null;
        try {
            jsonArray = new JSONArray(result);
            for(int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonData = jsonArray.getJSONObject(i);
                usrInfo_Array.add(jsonData.getString("name"));
            }
        } catch (JSONException e) {
            e.printStackTrace();
            new AlertDialog.Builder(LInflater.getContext()).setMessage("此應用程式需要有網路，偵測您無開啟網路" + '\n' + "請確定開始此應用程式時，網路是有連線的狀態" + '\n' + "如未開啟網路並連線，請開啟連線後，關閉此程式再重新開啟此應用程式")
                    .setPositiveButton("ok", new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
                    .create()
                    .show();
        }
    }



}
