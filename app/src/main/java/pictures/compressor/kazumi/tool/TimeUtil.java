package pictures.compressor.kazumi.tool;

import java.text.SimpleDateFormat;

public class TimeUtil {

    private static SimpleDateFormat mFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    // 格式化时间到年月日时分
    public static String src2DataTime(long time) {
        return mFormat.format(time);
    }
}
