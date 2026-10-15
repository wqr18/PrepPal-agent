package com.preppal.common;

import java.time.LocalDateTime;
import java.time.ZoneId;

/** 时间转换工具 */
public final class TimeUtils {

    private TimeUtils() {
    }

    /** LocalDateTime → epoch 毫秒；前端用它做相对时间展示 */
    public static Long toMillis(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}
