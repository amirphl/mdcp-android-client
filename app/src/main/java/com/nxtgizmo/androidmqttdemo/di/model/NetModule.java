package com.nxtgizmo.androidmqttdemo.di.model;

import android.app.Application;

import com.nxtgizmo.androidmqttdemo.R;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.MqttClient;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class NetModule {

    public NetModule() {
    }

    @Provides
    @Singleton
    MqttAndroidClient provideMqttAndroidClient(Application application) {
        String clientId = MqttClient.generateClientId();
        String brokerAddress = application.getString(R.string.broker_address);
        return new MqttAndroidClient(application, brokerAddress, clientId);
    }
}