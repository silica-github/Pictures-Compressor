package pictures.compressor.kazumi.tool;

import java.io.File;
import java.io.FileInputStream;

public class FileSizeUtil {

    private static final String TAG = "FileSizeUtil";

    public static int getFileSize(File file) throws Exception {
        long size = 0;

        if (file.exists()) {
            FileInputStream fis = null;
            fis = new FileInputStream(file);
            size = fis.available();
        } else {
            file.createNewFile();
        }

        if (size == 0) {
            return 0;
        }

        return (int) (double) size / 1024;
    }
}
