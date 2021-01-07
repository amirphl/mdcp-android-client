package utils;

import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;

import dalvik.system.DexClassLoader;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import timber.log.Timber;

import static com.nxtgizmo.androidmqttdemo.dashboard.DashBoardActivity.APP_NAME;
import static com.nxtgizmo.androidmqttdemo.dashboard.DashBoardActivity.EXECUTABLE_JOB_CLASS;
import static com.nxtgizmo.androidmqttdemo.dashboard.DashBoardActivity.EXECUTABLE_START_METHOD_NAME;
import static com.nxtgizmo.androidmqttdemo.dashboard.DashBoardActivity.WEB_ADDRESS;

public class Job {

    private final String jobExecutableURL;
    private final String jobInputURL;
    private final String executableFileName;
    private final String inputFileName;
    private final int fraction;
    private final int totalFraction;
    private final String jobId;
    private final File filesDir;
    private final File cacheDir;
    private final String deviceId;
    private final TextView logTextView;

    public Job(File filesDir, File cacheDir,
               String jobExecutableURL, String jobInputURL, String executableFileName,
               String inputFileName, int fraction, int totalFractions, String jobId,
               String deviceId, TextView logTextView) {
        this.jobExecutableURL = jobExecutableURL;
        this.jobInputURL = jobInputURL;
        this.executableFileName = executableFileName;
        this.inputFileName = inputFileName;
        this.fraction = fraction;
        this.totalFraction = totalFractions;
        this.jobId = jobId;
        this.filesDir = filesDir;
        this.cacheDir = cacheDir;
        this.deviceId = deviceId;
        this.logTextView = logTextView;
    }

    public void run() {
        try {
            String executableFilePath = writeFileOnInternalStorage(executableFileName,
                    download(jobExecutableURL));
            String inputFilePath = writeFileOnInternalStorage(inputFileName,
                    download(jobInputURL));
            final DexClassLoader classLoader = new DexClassLoader(executableFilePath,
                    cacheDir.getAbsolutePath(), null, this.getClass().getClassLoader());

            final Class<Object> c = (Class<Object>) classLoader.loadClass(EXECUTABLE_JOB_CLASS);
            final Object executableJobInstance = c.newInstance();
            Method start = c.getMethod(EXECUTABLE_START_METHOD_NAME, String.class);

            String m = String.format("starting execution of %s with %s as input",
                    executableFileName, inputFileName);
            addLogInTextView(m);

            long s = System.currentTimeMillis();
            String outputFilePath = (String) start.invoke(executableJobInstance, inputFilePath); // TODO don't let write anywhere except
            long e = System.currentTimeMillis();

            m = String.format("took %s milliseconds to execute %s", (e - s), executableFileName);
            addLogInTextView(m);

            uploadOutput(outputFilePath);
        } catch (IOException | ClassNotFoundException | IllegalAccessException |
                InstantiationException | NoSuchMethodException | InvocationTargetException e) {
            addLogInTextView(e.getMessage());
            try {
                String errorsFilePath = writeFileOnInternalStorage("errors.csv",
                        e.getMessage().getBytes());
                uploadOutput(errorsFilePath);
            } catch (IOException e2) {
                addLogInTextView(e2.getMessage());
            }
        }
    }

    private byte[] download(String downloadUrl) throws IOException {
        long startTime = System.currentTimeMillis();
        InputStream is = new URL(downloadUrl).openConnection().getInputStream();
        BufferedInputStream bis = new BufferedInputStream(is);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] data = new byte[5000];
        int current;
        while ((current = bis.read(data, 0, data.length)) != -1)
            baos.write(data, 0, current);
        long endTime = System.currentTimeMillis();
        String m = String.format("DownloadManager: downloaded %s in %s milliseconds", downloadUrl,
                (endTime - startTime));
        addLogInTextView(m);
        return baos.toByteArray();
    }

    private String writeFileOnInternalStorage(String fileName, byte[] body)
            throws IOException {
        File dir = new File(filesDir, APP_NAME);
        if (!dir.exists()) {
            boolean t = dir.mkdir();
            String m = String.format("created folder %s ? %s", APP_NAME, t);
            addLogInTextView(m);
        }
        File f = new File(dir, fileName);
        String m = String.format("write file in %s", f.getAbsoluteFile());
        addLogInTextView(m);
        FileOutputStream fos = new FileOutputStream(f);
        fos.write(body);
        fos.flush();
        fos.close();
        return f.getAbsolutePath();
    }

    private void uploadOutput(String path) {
        File file = new File(path);
        MultipartBody multipartBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("index", String.valueOf(fraction))
                .addFormDataPart("device_id", deviceId)
                .addFormDataPart("file", "partial_result_file",
                        RequestBody.create(MediaType.parse("text/csv"), file)) // TODO media type may be unknown
                .build();
        Request request = new Request.Builder()
                .url(WEB_ADDRESS + "/jobs/" + jobId + "/partial-results/")
                .post(multipartBody)
                .build();
        try {
            Response response = new OkHttpClient().newCall(request).execute();
            int respCode = response.code();
            String respMsg = response.message();
            String respBody = response.body().string();
            String m = String.format("upload result:\nstatus code: %s\nresponse message: %s\nresponse body: %s",
                    respCode, respMsg, respBody);
            addLogInTextView(m);
        } catch (IOException e) {
            addLogInTextView(e.getMessage());
        }
    }

    private void addLogInTextView(String logMessage) {
        Timber.d("======================= %s", logMessage);
        logTextView.append(logMessage + "\n------------------\n");
    }
}