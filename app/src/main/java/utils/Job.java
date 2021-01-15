package utils;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.HashMap;

import dalvik.system.DexClassLoader;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import utils.data.JobContract;

import static com.nxtgizmo.androidmqttdemo.dashboard.DashBoardActivity.APP_NAME;
import static com.nxtgizmo.androidmqttdemo.dashboard.DashBoardActivity.EXECUTABLE_JOB_CLASS;
import static com.nxtgizmo.androidmqttdemo.dashboard.DashBoardActivity.EXECUTABLE_START_METHOD_NAME;
import static com.nxtgizmo.androidmqttdemo.dashboard.DashBoardActivity.WEB_ADDRESS;

public class Job {

    private final JobExecutionService jobExecutionService;
    private final String jobExecutableURL;
    private final String jobInputURL;
    private final String executableFileName;
    private final int fraction;
    private final int totalFractions;
    private final String jobId;
    private final String TIME = "TIME";
    private final String SIZE = "SIZE";
    private final String PATH = "PATH";
    private final String outputFilePath;
    private final File appDir;

    public Job(JobExecutionService jobExecutionService, String jobExecutableURL, String jobInputURL,
               String executableFileName, int fraction, int totalFractions, String jobId) {
        this.jobExecutionService = jobExecutionService;
        this.jobExecutableURL = jobExecutableURL;
        this.jobInputURL = jobInputURL;
        this.executableFileName = executableFileName;
        this.fraction = fraction;
        this.totalFractions = totalFractions;
        this.jobId = jobId;
        outputFilePath = jobExecutionService.getCacheDir().getAbsolutePath() + "/" + APP_NAME + "/output";
        appDir = new File(jobExecutionService.getCacheDir(), APP_NAME);
        createAppDir();
    }

    public void run() {
        try {
            HashMap<String, Object> h = downloadAndSave(jobExecutableURL, executableFileName);
            long timeSpentToDownloadExecutable = (long) h.get(TIME);
            String executableFilePath = (String) h.get(PATH);
            int size = (int) h.get(SIZE);

            final DexClassLoader classLoader = new DexClassLoader(executableFilePath,
                    jobExecutionService.getCacheDir().getAbsolutePath(), null,
                    this.getClass().getClassLoader());
            final Class<Object> c = (Class<Object>) classLoader.loadClass(EXECUTABLE_JOB_CLASS);
            final Object executableJobInstance = c.newInstance();
            Method start = c.getMethod(EXECUTABLE_START_METHOD_NAME,
                    String.class, String.class, int.class, int.class);

            String m = String.format("starting execution >\nexecutable:\n%s\ninput:\n%s", executableFileName, jobInputURL);
            jobExecutionService.onSuccess(m);

            long s = System.currentTimeMillis();
            start.invoke(executableJobInstance, jobInputURL, outputFilePath, fraction, totalFractions); // TODO don't let write anywhere except
            long e = System.currentTimeMillis();
            long consumedTime = e - s;

            m = String.format("took %s milliseconds to execute >\n%s", consumedTime, executableFileName);
            jobExecutionService.onSuccess(m);

            long timeSpentToUploadOutputFile = uploadOutput(outputFilePath, consumedTime);
            insertStats(outputFilePath, consumedTime, -1, -1,
                    timeSpentToDownloadExecutable, timeSpentToUploadOutputFile,
                    size, new File(outputFilePath).length());
        } catch (IOException | ClassNotFoundException | IllegalAccessException |
                InstantiationException | NoSuchMethodException | InvocationTargetException e) {
            e.printStackTrace();
            jobExecutionService.onError(e.getMessage());
            try {
                String errorsFilePath = writeFileOnInternalStorage("errors.csv",
                        e.getMessage().getBytes());
                uploadOutput(errorsFilePath, -1);
            } catch (IOException e2) {
                jobExecutionService.onError(e2.getMessage());
            }
        }
    }

    private HashMap<String, Object> downloadAndSave(String downloadUrl, String filename) throws IOException {
        long s = System.currentTimeMillis();
        InputStream is = new URL(downloadUrl).openConnection().getInputStream();
        BufferedInputStream bis = new BufferedInputStream(is);
        File f = new File(appDir, filename);
        FileOutputStream fos = new FileOutputStream(f);
        byte[] baf = new byte[1024 * 1024];
        int current;
        int acc = 0;
        while ((current = bis.read(baf, 0, baf.length)) != -1) {
            fos.write(baf, 0, current);
            acc += current;
        }
        fos.flush();
        fos.close();
        long e = System.currentTimeMillis();
        String m = String.format("download status >\nfile: %s\ntime: %s milliseconds", downloadUrl, (e - s));
        jobExecutionService.onSuccess(m);
        HashMap<String, Object> output = new HashMap<>();
        output.put(SIZE, acc);
        output.put(PATH, f.getAbsolutePath());
        output.put(TIME, (e - s));
        return output;
    }

    private void createAppDir() {
        if (!appDir.exists()) {
            boolean t = appDir.mkdir();
            String m = String.format("created folder %s ? %s", APP_NAME, t);
            jobExecutionService.onSuccess(m);
        }
    }

    private String writeFileOnInternalStorage(String fileName, byte[] body) throws IOException {
        File f = new File(appDir, fileName);
        String m = String.format("writing file >\npath: %s", f.getAbsoluteFile());
        jobExecutionService.onSuccess(m);
        FileOutputStream fos = new FileOutputStream(f);
        fos.write(body);
        fos.flush();
        fos.close();
        return f.getAbsolutePath();
    }

    private long uploadOutput(String outputPath, long consumedTime) {
        File file = new File(outputPath);
        MultipartBody multipartBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("index", String.valueOf(fraction))
                .addFormDataPart("device_id", jobExecutionService.getDeviceId())
                .addFormDataPart("consumed_time", String.valueOf(consumedTime))
                .addFormDataPart("file", "partial_result_file.out",
                        RequestBody.create(MediaType.parse("text/csv"), file)) // TODO media type may be unknown
                .build();
        Request request = new Request.Builder()
                .url(WEB_ADDRESS + "/jobs/" + jobId + "/partial-results/")
                .post(multipartBody)
                .build();
        try {
            long s = System.currentTimeMillis();
            Response response = new OkHttpClient().newCall(request).execute();
            long e = System.currentTimeMillis();
            int respCode = response.code();
            String respMsg = response.message();
            String respBody = response.body().string();
            String m = String.format("upload status >\nstatus code: %s\nresponse message: %s\nresponse body: %s\nupload time: %s milliseconds",
                    respCode, respMsg, respBody, e - s);
            jobExecutionService.onSuccess(m);
            return e - s;
        } catch (IOException e) {
            jobExecutionService.onError(e.getMessage());
        }
        return -1;
    }

    private void insertStats(String outputFilePath, long consumedTime,
                             int avgCpuUsage, int avgRamUsage,
                             long timeSpentToDownloadExecutable,
                             long timeSpentToUploadOutputFile,
                             long executableSize, long outputFileSize) {
        SQLiteDatabase db = jobExecutionService.getJobDBHelper().getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(JobContract.Job.COLUMN_NAME_ID, jobId);
        values.put(JobContract.Job.COLUMN_NAME_EXECUTABLE_URL, jobExecutableURL);
        values.put(JobContract.Job.COLUMN_NAME_INPUT_FILE_URL, jobInputURL);
        values.put(JobContract.Job.COLUMN_NAME_OUTPUT_FILE_PATH, outputFilePath);
        values.put(JobContract.Job.COLUMN_NAME_FRACTION, fraction);
        values.put(JobContract.Job.COLUMN_NAME_TOTAL_FRACTIONS, totalFractions);
        values.put(JobContract.Job.COLUMN_NAME_CONSUMED_TIME, consumedTime);
        values.put(JobContract.Job.COLUMN_NAME_AVG_CPU_USAGE, avgCpuUsage);
        values.put(JobContract.Job.COLUMN_NAME_AVG_RAM_USAGE, avgRamUsage);
        values.put(JobContract.Job.COLUMN_NAME_AVG_TIME_SPENT_TO_DOWNLOAD_EXECUTABLE, timeSpentToDownloadExecutable);
        values.put(JobContract.Job.COLUMN_NAME_AVG_TIME_SPENT_TO_UPLOAD_OUTPUT_FILE, timeSpentToUploadOutputFile);
        values.put(JobContract.Job.COLUMN_NAME_EXECUTABLE_SIZE, executableSize);
        values.put(JobContract.Job.COLUMN_NAME_OUTPUT_FILE_SIZE, outputFileSize);
        long newRowId = db.insert(JobContract.Job.TABLE_NAME, null, values);
//        String m = String.format("inserted job stats into SQLite:\n%s\nrow id: %s", values, newRowId);
//        jobExecutionService.onSuccess(m);
    }
}