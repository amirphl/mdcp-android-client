package utils;

import android.widget.TextView;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;


import java.io.File;
import java.security.SecureRandom;

import timber.log.Timber;
import utils.data.JobDBHelper;

import static com.nxtgizmo.androidmqttdemo.dashboard.DashBoardActivity.QOS;
import static com.nxtgizmo.androidmqttdemo.dashboard.DashBoardActivity.WEB_ADDRESS;
import static com.nxtgizmo.androidmqttdemo.dashboard.DashBoardActivity.UNREGISTRATION_TOPIC;
import static com.nxtgizmo.androidmqttdemo.dashboard.DashBoardActivity.REGISTRATION_TOPIC;

public class JobExecutionService {

    private final MqttAndroidClient client;
    private final File filesDir;
    private final File cacheDir;
    private final JobDBHelper dbHelper;
    private final TextView logTextView;
    private final SecureRandom numberGenerator = new SecureRandom();
    private String deviceId;

    // message format: relative_executable_address + space + relative_input_address + space + fraction + space + total_fractions + space + uuid
    // ex: /media/jobs/2021/01/01/executables/yy_Sg0go6G.jar /media/jobs/2021/01/01/input_files/xx_mEibqXd.csv 3 10 c98610fb7bfb8068cf2616e1c2c00a76
    private class MessageListener implements IMqttMessageListener {
        private boolean firstMessage = true;

        @Override
        public void messageArrived(String topic, MqttMessage message) throws MqttException {
            String m = null;
            if (firstMessage)
                firstMessage = false;
            else
                m = String.format("received another message in topic %s: %s \nignored it",
                        topic, message);

            if (message.isDuplicate())
                m = String.format("received duplicated message in topic %s: %s \nignored it",
                        topic, message);

            if (message.isRetained())
                m = String.format("received retained message in topic %s: %s \nignored it",
                        topic, message);

            if (m != null) {
                JobExecutionService.this.addLogInTextView(m);
                return;
            }

            m = String.format("received message in topic %s: %s", topic, message);
            JobExecutionService.this.addLogInTextView(m);
            String[] arr = message.toString().split(" ");
            String jobExecutableURL = getAbsoluteAddress(arr[0]);
            String jobInputURL = getAbsoluteAddress(arr[1]);
            int fraction = Integer.parseInt(arr[2]);
            int totalFractions = Integer.parseInt(arr[3]);
            String jobId = arr[4];
            String executableFileName = getLastPartOfStringBySlash(jobExecutableURL);
            String inputFileName = getLastPartOfStringBySlash(jobInputURL);
            Job job = new Job(filesDir, cacheDir, jobExecutableURL, jobInputURL,
                    executableFileName, inputFileName, fraction, totalFractions, jobId, topic,
                    dbHelper, logTextView);
            job.run();
            JobExecutionService.this.unregister(topic);
            JobExecutionService.this.register_and_listen();
        }
    }

    public JobExecutionService(MqttAndroidClient client, File filesDir, File cacheDir,
                               JobDBHelper dbHelper, TextView logTextView) throws MqttException {
        this.client = client;
        this.filesDir = filesDir;
        this.cacheDir = cacheDir;
        this.dbHelper = dbHelper;
        this.logTextView = logTextView;
        connect();
    }

    private void connect() throws MqttException {
        IMqttActionListener onConnect = new IMqttActionListener() {

            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
                addLogInTextView("connected to broker");
                register_and_listen();
            }

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                addLogInTextView(exception.getMessage());
            }
        };
        client.connect(null, onConnect);
    }

    private void disconnect() throws MqttException {
        if (client.isConnected()) {
            client.disconnect();
            addLogInTextView("disconnected from the broker");
        }
    }

    private String register() throws MqttException {
        String deviceId = unique();
        client.publish(REGISTRATION_TOPIC, deviceId.getBytes(), QOS, false);
        String m = String.format("device registered as %s", deviceId);
        addLogInTextView(m);
        return deviceId;
    }

    private void unregister(String deviceId) throws MqttException {
        client.publish(UNREGISTRATION_TOPIC, deviceId.getBytes(), QOS, false);
        String m = String.format("unregistered %s", deviceId);
        addLogInTextView(m);
    }

    private void listen(String topic) throws MqttException {
        MessageListener listener = new MessageListener();
        client.subscribe(topic, QOS, listener);
        String m = String.format("listening to %s", topic);
        addLogInTextView(m);
    }

    private void unsubscribe(String topic) throws MqttException {
        client.unsubscribe(topic);
        String m = String.format("unsubscribed %s", topic);
        addLogInTextView(m);
    }

    private void register_and_listen() {
        String topic;
        try {
            topic = register();
            deviceId = topic;
        } catch (MqttException e) {
            addLogInTextView(e.getMessage());
            try {
                disconnect();
            } catch (MqttException e2) {
                addLogInTextView(e2.getMessage());
            }
            return;
        }

        try {
            listen(topic);
        } catch (MqttException e) {
            addLogInTextView(e.getMessage());
            try {
                unregister(topic);
                disconnect();
            } catch (MqttException e2) {
                addLogInTextView(e2.getMessage());
            }
        }
    }

    private String unique() {
        long MSB = 0x8000000000000000L;
        return Long.toHexString(MSB | numberGenerator.nextLong()) +
                Long.toHexString(MSB | numberGenerator.nextLong());
    }

    private String getAbsoluteAddress(String relativeAddress) {
        return WEB_ADDRESS + relativeAddress.trim();
    }

    private String getLastPartOfStringBySlash(String input) {
        String[] arr = input.split("/");
        return arr[arr.length - 1];
    }

    public void terminate() throws MqttException {
        unsubscribe(deviceId);
        unregister(deviceId);
        disconnect();
        addLogInTextView("terminated");
    }

    private void addLogInTextView(String logMessage) {
        Timber.d("======================= %s", logMessage);
        logTextView.append(logMessage + "\n------------------\n");
    }
}
