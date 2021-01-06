package com.nxtgizmo.androidmqttdemo.dashboard;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.nxtgizmo.androidmqttdemo.R;
import com.nxtgizmo.androidmqttdemo.mqtt_app.MqttApp;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.MqttException;

import javax.inject.Inject;

import timber.log.Timber;
import utils.JobExecutionService;


public class DashBoardActivity extends AppCompatActivity implements DashboardContract {

    @Inject
    MqttAndroidClient client;
    private TextView message;

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
        message = findViewById(R.id.message);

        APP_NAME = getString(R.string.app_name);
        REGISTRATION_TOPIC = getString(R.string.registration_topic);
        UNREGISTRATION_TOPIC = getString(R.string.unregistration_topic);
        QOS = Integer.parseInt(getString(R.string.qos));
        EXECUTABLE_JOB_CLASS = getString(R.string.executable_job_class);
        EXECUTABLE_START_METHOD_NAME = getString(R.string.executable_start_method_name);
        WEB_ADDRESS = getString(R.string.web_address);

        try {
            jobExecutionService = new JobExecutionService(client, getCacheDir(),
                    getCacheDir());
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onSuccess(String successMessage) {
        Timber.d(successMessage);
        message.setText(successMessage);
    }

    @Override
    public void onError(String errorMessage) {
        Timber.d(errorMessage);
        message.setText(errorMessage);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            jobExecutionService.terminate();
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
}
