package utils.data;

import android.provider.BaseColumns;

public final class JobContract {
    private JobContract() {
    }

    public static class Job implements BaseColumns {
        public static final String TABLE_NAME = "job";
        public static final String COLUMN_NAME_ID = "id";
        public static final String COLUMN_NAME_EXECUTABLE_URL = "executable_url";
        public static final String COLUMN_NAME_INPUT_FILE_URL = "input_file_url";
        public static final String COLUMN_NAME_OUTPUT_FILE_PATH = "output_file_path";
        public static final String COLUMN_NAME_FRACTION = "fraction";
        public static final String COLUMN_NAME_TOTAL_FRACTIONS = "total_fractions";
        public static final String COLUMN_NAME_CONSUMED_TIME = "consumed_time"; // milliseconds
        public static final String COLUMN_NAME_AVG_CPU_USAGE = "avg_cpu_usage";
        public static final String COLUMN_NAME_AVG_RAM_USAGE = "avg_ram_usage";
        public static final String COLUMN_NAME_AVG_TIME_SPENT_TO_DOWNLOAD_EXECUTABLE =
                "time_spent_to_download_executable"; // milliseconds
        public static final String COLUMN_NAME_AVG_TIME_SPENT_TO_DOWNLOAD_INPUT_FILE =
                "time_spent_to_download_input_file"; // milliseconds
        public static final String COLUMN_NAME_AVG_TIME_SPENT_TO_UPLOAD_OUTPUT_FILE =
                "time_spent_to_upload_output_file"; // milliseconds
        public static final String COLUMN_NAME_EXECUTABLE_SIZE = "executable_size"; // bytes
        public static final String COLUMN_NAME_INPUT_FILE_SIZE = "input_file_size"; // bytes
        public static final String COLUMN_NAME_OUTPUT_FILE_SIZE = "output_file_size"; // bytes
        public static final String COLUMN_NAME_CREATED_AT = "created_at";
    }
}