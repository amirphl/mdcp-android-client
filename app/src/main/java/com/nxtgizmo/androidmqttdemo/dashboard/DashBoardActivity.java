package com.nxtgizmo.androidmqttdemo.dashboard;

import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.nxtgizmo.androidmqttdemo.R;
import com.nxtgizmo.androidmqttdemo.mqtt_app.MqttApp;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.MqttException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.regex.Pattern;

import javax.inject.Inject;

import timber.log.Timber;
import utils.JobExecutionService;
import utils.data.JobDBHelper;


public class DashBoardActivity extends AppCompatActivity implements DashboardContract {

    @Inject
    MqttAndroidClient client;
    private TextView logTextView;

    private JobExecutionService jobExecutionService;
    public static String APP_NAME;
    public static String REGISTRATION_TOPIC;
    public static String UNREGISTRATION_TOPIC;
    public static String EXECUTABLE_JOB_CLASS;
    public static String EXECUTABLE_START_METHOD_NAME;
    public static String WEB_ADDRESS;
    public static int QOS;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ((MqttApp) getApplication()).getMqttComponent().inject(this);
        logTextView = findViewById(R.id.message);
        logTextView.setMovementMethod(new ScrollingMovementMethod());
        JobDBHelper dbHelper = new JobDBHelper(getApplicationContext());

        APP_NAME = getString(R.string.app_name);
        REGISTRATION_TOPIC = getString(R.string.registration_topic);
        UNREGISTRATION_TOPIC = getString(R.string.unregistration_topic);
        QOS = Integer.parseInt(getString(R.string.qos));
        EXECUTABLE_JOB_CLASS = getString(R.string.executable_job_class);
        EXECUTABLE_START_METHOD_NAME = getString(R.string.executable_start_method_name);
        WEB_ADDRESS = getString(R.string.web_address);

        try {
            jobExecutionService = new JobExecutionService(client, getCacheDir(), getCacheDir(),
                    dbHelper, logTextView);
        } catch (MqttException e) {
            Timber.d("======================= %s", e.getMessage());
            logTextView.append(e.getMessage() + "\n------------------\n");
        }
    }

    @Override
    public void onSuccess(String successMessage) {
        logTextView.setText(successMessage);
    }

    @Override
    public void onError(String errorMessage) {
        logTextView.setText(errorMessage);
    }

    @Override
    protected void onDestroy() {
        try {
            jobExecutionService.terminate();
        } catch (MqttException e) {
            Timber.d("======================= %s", e.getMessage());
            logTextView.append(e.getMessage() + "\n------------------\n");
        }
        super.onDestroy();
    }
}
