package utils.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class JobDBHelper extends SQLiteOpenHelper {
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "Job.db";

    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + JobContract.Job.TABLE_NAME + " (" +
                    JobContract.Job._ID + " INTEGER PRIMARY KEY," +
                    JobContract.Job.COLUMN_NAME_ID + " TEXT," +
                    JobContract.Job.COLUMN_NAME_EXECUTABLE_URL + " TEXT," +
                    JobContract.Job.COLUMN_NAME_INPUT_FILE_URL + " TEXT," +
                    JobContract.Job.COLUMN_NAME_OUTPUT_FILE_PATH + " TEXT," +
                    JobContract.Job.COLUMN_NAME_FRACTION + " INT," +
                    JobContract.Job.COLUMN_NAME_TOTAL_FRACTIONS + " INT," +
                    JobContract.Job.COLUMN_NAME_CONSUMED_TIME + " INT," +
                    JobContract.Job.COLUMN_NAME_AVG_CPU_USAGE + " INT," +
                    JobContract.Job.COLUMN_NAME_AVG_RAM_USAGE + " INT," +
                    JobContract.Job.COLUMN_NAME_AVG_TIME_SPENT_TO_DOWNLOAD_EXECUTABLE + " INT," +
                    JobContract.Job.COLUMN_NAME_AVG_TIME_SPENT_TO_UPLOAD_OUTPUT_FILE + " INT," +
                    JobContract.Job.COLUMN_NAME_EXECUTABLE_SIZE + " INT," +
                    JobContract.Job.COLUMN_NAME_OUTPUT_FILE_SIZE + " INT," +
                    JobContract.Job.COLUMN_NAME_CREATED_AT + " DATETIME NOT NULL DEFAULT (datetime(CURRENT_TIMESTAMP, 'localtime')));";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + JobContract.Job.TABLE_NAME;

    public JobDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }


    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }
}