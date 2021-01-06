package utils;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;


import java.io.File;
import java.security.SecureRandom;

import timber.log.Timber;

import static com.nxtgizmo.androidmqttdemo.dashboard.DashBoardActivity.QOS;
import static com.nxtgizmo.androidmqttdemo.dashboard.DashBoardActivity.WEB_ADDRESS;
import static com.nxtgizmo.androidmqttdemo.dashboard.DashBoardActivity.UNREGISTRATION_TOPIC;
import static com.nxtgizmo.androidmqttdemo.dashboard.DashBoardActivity.REGISTRATION_TOPIC;

public class JobExecutionService {

    private final MqttAndroidClient client;
    private SecureRandom numberGenerator;
    private String deviceId;
    private final File filesDir;
    private final File cacheDir;

    // message format: relative_executable_address + space + relative_input_address + space + fraction + space + total_fractions + space + uuid
    // ex: /media/jobs/2021/01/01/executables/yy_Sg0go6G.jar /media/jobs/2021/01/01/input_files/xx_mEibqXd.csv 3 10 c98610fb7bfb8068cf2616e1c2c00a76
    private class MessageListener implements IMqttMessageListener {
        @Override
        public void messageArrived(String topic, MqttMessage message) throws MqttException {
//            if (message.isDuplicate())
//                return;
//            Timber.d("------ %s", message.getQos()); TODO
//            Timber.d("------ %s", message.isRetained());  TODO
            // TODO unsubscribe
            Timber.d(message.toString());
            String[] arr = message.toString().split(" ");
            String jobExecutableURL = getAbsoluteAddress(arr[0]);
            String jobInputURL = getAbsoluteAddress(arr[1]);
            int fraction = Integer.parseInt(arr[2]);
            int totalFractions = Integer.parseInt(arr[3]);
            String jobId = arr[4];
            String executableFileName = getLastPartOfStringBySlash(jobExecutableURL);
            String inputFileName = getLastPartOfStringBySlash(jobInputURL);
            Job job = new Job(filesDir, cacheDir, jobExecutableURL, jobInputURL,
                    executableFileName, inputFileName, fraction, totalFractions, jobId, deviceId);
            job.run();
            JobExecutionService.this.unregister();
//                JobExecutionService.this.unsubscribe(); TODO ???
            JobExecutionService.this.register_and_listen();
        }
    }

    ;

    public JobExecutionService(MqttAndroidClient client, File filesDir, File cacheDir) throws MqttException {
        this.client = client;
        this.filesDir = filesDir;
        this.cacheDir = cacheDir;
//        MqttConnectOptions connectionOptions = new MqttConnectOptions();
//        connectionOptions.setCleanSession(false); // TODO ???
        connect();
    }

    private void connect() throws MqttException {
        IMqttActionListener onConnect = new IMqttActionListener() {

            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
                Timber.d("======================= connected to broker");
                register_and_listen();
            }

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                exception.printStackTrace(); // TODO exit program
            }
        };
        client.connect(null, onConnect);
    }

    private void disconnect() throws MqttException {
        if (client.isConnected()) {
            client.disconnect();
            Timber.d("======================= disconnected from the broker");
        }
    }

    private String register() throws MqttException {
        String deviceId = unique();
        client.publish(REGISTRATION_TOPIC, deviceId.getBytes(), QOS, false); // TODO retain, QOS, check really published
        Timber.d("======================= registered as %s", deviceId);
        return deviceId;
    }

    private void unregister() throws MqttException {
        client.publish(UNREGISTRATION_TOPIC, deviceId.getBytes(), QOS, false); // TODO retain, QOS
        Timber.d("======================= unregistered %s", deviceId);
    }

    private void listen(String topic) throws MqttException {
        MessageListener listener = new MessageListener();
        client.subscribe(topic, QOS, listener);
        Timber.d("======================= listening to %s", topic);
    }

    private void unsubscribe() throws MqttException {
        client.unsubscribe(deviceId);
        Timber.d("======================= unsubscribed %s", deviceId);
    }

    private void register_and_listen() {
        try {
            deviceId = register();
        } catch (MqttException e) {
            e.printStackTrace();
            // TODO exit program
            return;
        }
        try {
            listen(deviceId);
        } catch (MqttException e) {
            e.printStackTrace();
            try {
                unregister();
            } catch (MqttException e2) {
                e2.printStackTrace();
            }
            return;
            // TODO exit program
        }
    }

    public String unique() {
        SecureRandom ng = numberGenerator;
        if (ng == null) {
            numberGenerator = ng = new SecureRandom();
        }
        long MSB = 0x8000000000000000L;
        return Long.toHexString(MSB | ng.nextLong()) + Long.toHexString(MSB | ng.nextLong());
    }

    private String getAbsoluteAddress(String relativeAddress) {
        return WEB_ADDRESS + relativeAddress.trim();
    }

    private String getLastPartOfStringBySlash(String input) {
        String[] arr = input.split("/");
        return arr[arr.length - 1];
    }

    public void terminate() throws MqttException {
        unsubscribe();
        unregister();
        disconnect();
        Timber.d("======================= terminated");
    }
}
