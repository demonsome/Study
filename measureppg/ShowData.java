/*

	更新所量到的數據，顯示於手機第三個畫面
	
	主要動作為連到資料庫將所要的資料撈出來並顯示

*/

package com.example.luolab.measureppg;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Debug;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.StrictMode;
import android.os.Vibrator;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;
import com.physicaloid.lib.Physicaloid;
import com.physicaloid.lib.usb.driver.uart.ReadLisener;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.zip.Inflater;


public class ShowData extends Fragment /*implements Update*/{
    private String Get_Uri = "https://lens.csie.ncku.edu.tw/~Platform/getDataFromDB.php";
    private String Find_ID = "SELECT * from whichid where id = 1";
    private View ShowDataView;

    private String[] GSRValue;
    private String[] PPGValue;
    private TextView[] Value_Textview;

    private Handler Update;
    private Intent it;
    private Uri uri;

    Button bt;
    Button bt2;

    private View dialogView;
    private AlertDialog Dialog;
    private AlertDialog.Builder Dialog_Builder;

    private View dialogView2;
    private AlertDialog Dialog2;
    private AlertDialog.Builder Dialog_Builder2;

    private LayoutInflater LInflater;

    private int ID = 0;

    private Handler mHandler;

    @SuppressLint("SetTextI18n")
    public View onCreateView(final LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState){

        ShowDataView = inflater.inflate(R.layout.showdata, container, false);

        LInflater = inflater;

        dialogView2 = View.inflate(inflater.getContext(),R.layout.ppg_feature_information,null);

        Dialog_Builder2 = new AlertDialog.Builder((Activity)inflater.getContext())
                .setTitle("PPG_Feature_Information")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });

        Dialog2 = Dialog_Builder2.create();
        Dialog2.setView(dialogView2);

        PPGValue = new String[18];

        Value_Textview = new TextView[18];
        Update = new Handler();

        bt2 = ShowDataView.findViewById(R.id.button2);

        int counter = 0;
        int counter2 = 0;

        Value_Textview[counter++] = ShowDataView.findViewById(R.id.feature1);
        Value_Textview[counter++] = ShowDataView.findViewById(R.id.feature2);
        Value_Textview[counter++] = ShowDataView.findViewById(R.id.feature3);
        Value_Textview[counter++] = ShowDataView.findViewById(R.id.feature4);
        Value_Textview[counter++] = ShowDataView.findViewById(R.id.feature5);
        Value_Textview[counter++] = ShowDataView.findViewById(R.id.feature6);
        Value_Textview[counter++] = ShowDataView.findViewById(R.id.feature7);
        Value_Textview[counter++] = ShowDataView.findViewById(R.id.feature8);
        Value_Textview[counter++] = ShowDataView.findViewById(R.id.feature9);
        Value_Textview[counter++] = ShowDataView.findViewById(R.id.feature10);
        Value_Textview[counter++] = ShowDataView.findViewById(R.id.feature11);
        Value_Textview[counter++] = ShowDataView.findViewById(R.id.feature12);
        Value_Textview[counter++] = ShowDataView.findViewById(R.id.feature13);
        Value_Textview[counter++] = ShowDataView.findViewById(R.id.feature14);
        Value_Textview[counter++] = ShowDataView.findViewById(R.id.feature15);
        Value_Textview[counter++] = ShowDataView.findViewById(R.id.feature32);
        Value_Textview[counter++] = ShowDataView.findViewById(R.id.feature34);


        String result = GetDB(Find_ID,Get_Uri);
        JSONArray jsonArray = null;
        try {
            jsonArray = new JSONArray(result);
            for(int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonData = jsonArray.getJSONObject(i);
                ID = Integer.parseInt(jsonData.getString("which"));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        result = GetDB("SELECT * FROM guan where id = " + ID,Get_Uri);
        jsonArray = null;
        try {
            jsonArray = new JSONArray(result);
            for(int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonData = jsonArray.getJSONObject(i);
                PPGValue[counter2++] = jsonData.getString("PeakTwiceAmp");
                PPGValue[counter2++] = jsonData.getString("TwiceDownAmp");
                PPGValue[counter2++] = jsonData.getString("Angle");
                PPGValue[counter2++] = jsonData.getString("PeakAmp");
                PPGValue[counter2++] = jsonData.getString("Systolic_Dis");
                PPGValue[counter2++] = jsonData.getString("Diastolic_Dis");
                PPGValue[counter2++] = jsonData.getString("PPT");
                PPGValue[counter2++] = jsonData.getString("IBI");
                PPGValue[counter2++] = jsonData.getString("C1");
                PPGValue[counter2++] = jsonData.getString("C2");
                PPGValue[counter2++] = jsonData.getString("C3");
                PPGValue[counter2++] = jsonData.getString("C4");
                PPGValue[counter2++] = jsonData.getString("C5");
                PPGValue[counter2++] = jsonData.getString("C6");
                PPGValue[counter2++] = jsonData.getString("C7");
                PPGValue[counter2++] = jsonData.getString("HRV");
                PPGValue[counter2++] = jsonData.getString("LF");
                PPGValue[counter2++] = jsonData.getString("HF");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
//        char[] LF = new char[10];
//        char[] HF = new char[10];
//        char[] HRV = new char[10];
        String LF = "";
        String HF = "";
        String HRV = "";
        if(PPGValue[15] != null) {
            if (PPGValue[15].equals("") != true) {
//                PPGValue[15].getChars(0, 5, HRV, 0);
//                PPGValue[16].getChars(0, 5, LF, 0);
//                PPGValue[17].getChars(0, 5, HF, 0);
                HRV = PPGValue[15];
                LF = PPGValue[16];
                HF = PPGValue[17];
                DecimalFormat df = new DecimalFormat("######0.00");
//                Double LF_HF = Double.parseDouble(String.valueOf(LF)) / Double.parseDouble(String.valueOf(HF));
                double LF_HF = 0;
                try {
                    LF_HF = Double.parseDouble(LF) / Double.parseDouble(HF);
                }catch (Exception e){
                    LF_HF = 0;
                }
                for (int i = 0; i < 15; i++) {
                    Value_Textview[i].setText(PPGValue[i]);
                }
//                Value_Textview[15].setText(String.valueOf(HRV));
//                Value_Textview[16].setText(String.valueOf(LF) + " / " + String.valueOf(HF) + " = " + df.format(LF_HF));
                Value_Textview[15].setText(HRV);
                Value_Textview[16].setText(LF + " / " + HF + " = " + df.format(LF_HF));
            }
        }

        bt2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Dialog2.show();
            }
        });

        //Update_Feature();

        return ShowDataView;
    }


    /*
    public void updateView() {
        Toast.makeText(LInflater.getContext(),"success",Toast.LENGTH_SHORT).show();
    }
	*/


    private String GetDB(String Query_Command,String uri)
    {
        String result = null;
        try {
            result = DBConnector.executeQuery(Query_Command,uri);
                /*
                    SQL 結果有多筆資料時使用JSONArray
                    只有一筆資料時直接建立JSONObject物件
                    JSONObject jsonData = new JSONObject(result);
                */

//            JSONArray jsonArray = new JSONArray(result);
//            for(int i = 0; i < jsonArray.length(); i++) {
//                JSONObject jsonData = jsonArray.getJSONObject(i);
//
//                usrInfo_Array.add(jsonData.getString("name"));
//            }
        } catch(Exception e) {
        }
        return result;
    }
	/*
    private void Update_Feature()
    {
        int counter2 = 0;

        String result = GetDB(Find_ID,Get_Uri);
        JSONArray jsonArray = null;
        try {
            jsonArray = new JSONArray(result);
            for(int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonData = jsonArray.getJSONObject(i);
                ID = Integer.parseInt(jsonData.getString("which"));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        result = GetDB("SELECT * FROM guan where id = " + ID,Get_Uri);
        jsonArray = null;
        try {
            jsonArray = new JSONArray(result);
            for(int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonData = jsonArray.getJSONObject(i);
                PPGValue[counter2++] = jsonData.getString("PeakTwiceAmp");
                PPGValue[counter2++] = jsonData.getString("TwiceDownAmp");
                PPGValue[counter2++] = jsonData.getString("Angle");
                PPGValue[counter2++] = jsonData.getString("PeakAmp");
                PPGValue[counter2++] = jsonData.getString("Systolic_Dis");
                PPGValue[counter2++] = jsonData.getString("Diastolic_Dis");
                PPGValue[counter2++] = jsonData.getString("PPT");
                PPGValue[counter2++] = jsonData.getString("IBI");
                PPGValue[counter2++] = jsonData.getString("C1");
                PPGValue[counter2++] = jsonData.getString("C2");
                PPGValue[counter2++] = jsonData.getString("C3");
                PPGValue[counter2++] = jsonData.getString("C4");
                PPGValue[counter2++] = jsonData.getString("C5");
                PPGValue[counter2++] = jsonData.getString("C6");
                PPGValue[counter2++] = jsonData.getString("C7");
                PPGValue[counter2++] = jsonData.getString("HRV");
                PPGValue[counter2++] = jsonData.getString("LF");
                PPGValue[counter2++] = jsonData.getString("HF");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        char[] LF = new char[10];
        char[] HF = new char[10];
        char[] HRV = new char[10];

        if(PPGValue[15] != null) {
            if (PPGValue[15].equals("") != true) {
                PPGValue[15].getChars(0, 5, HRV, 0);
                PPGValue[16].getChars(0, 5, LF, 0);
                PPGValue[17].getChars(0, 5, HF, 0);

                DecimalFormat df = new DecimalFormat("######0.00");
                Double LF_HF = Double.parseDouble(String.valueOf(LF)) / Double.parseDouble(String.valueOf(HF));

                for (int i = 0; i < 15; i++) {
                    Value_Textview[i].setText(PPGValue[i]);
                }
                Value_Textview[15].setText(String.valueOf(HRV));
                Value_Textview[16].setText(String.valueOf(LF) + " / " + String.valueOf(HF) + " = " + df.format(LF_HF));
            }
        }
    }*/
}
