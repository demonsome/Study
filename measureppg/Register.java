/*
	為註冊畫面，將使用者所註冊的用戶資訊存入資料庫
*/


package com.example.luolab.measureppg;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class Register {

    private View ResigsterView;

    private TextView psd;
    private TextView account;
    private TextView Name;

    private Button register;

    private String Insert_Uri = "https://lens.csie.ncku.edu.tw/~Platform/insertDataToDB.php";
    private String Insert_Query_Command = "INSERT INTO doctor (name,account,password)VALUES";

    public View onCreateView(final LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState){

        ResigsterView = inflater.inflate(R.layout.resigster, container, false);
		
		// 初始化這些變數
        Name = ResigsterView.findViewById(R.id.Name_tv);
        account = ResigsterView.findViewById(R.id.account_tv);
        psd = ResigsterView.findViewById(R.id.password_tv);
        register = ResigsterView.findViewById(R.id.Register_btn);
		
		
		// 當註冊按鈕被按下後，檢查資料是否正確以及上傳至資料庫
        register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(Name.getText().toString().equals("") || account.getText().toString().equals("") || psd.getText().toString().equals("")){
                    Toast.makeText(inflater.getContext(),"請勿空白，確實填寫",Toast.LENGTH_SHORT).show();
                }
                else{
                    String result = GetDB(Insert_Query_Command +
                            "('" + Name.getText().toString() + "','"
                            + account.getText().toString() + "','"
                            + psd.getText().toString() + "')", Insert_Uri);
                    if(result.equals("")) {
                        new AlertDialog.Builder(inflater.getContext()).setMessage("此應用程式需要有網路，偵測您無開啟網路" + '\n' + "請確定開始此應用程式時，網路是有連線的狀態" + '\n' + "如未開啟網路並連線，請開啟連線後，關閉此程式再重新開啟此應用程式")
                                .setPositiveButton("ok", new DialogInterface.OnClickListener() {

                                    public void onClick(DialogInterface dialog, int which) {
                                    }
                                })
                                .create()
                                .show();
                    }
                    else {
                        new AlertDialog.Builder(inflater.getContext()).setMessage("註冊成功")
                                .setPositiveButton("ok", new DialogInterface.OnClickListener() {

                                    public void onClick(DialogInterface dialog, int which) {
                                    }
                                })
                                .create()
                                .show();
                    }
                }
            }
        });

        return ResigsterView;
    }
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
}
