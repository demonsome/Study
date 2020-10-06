package com.example.luolab.measureppg;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class Biofeedback extends AppCompatActivity {

    private View Biofeedback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.biofeedback);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Biofeedback = inflater.inflate(R.layout.biofeedback, container, false);
        return Biofeedback;
    }
}
