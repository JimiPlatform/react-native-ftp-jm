package com.jimi.ftpapi.backups;

import java.io.File;
import java.io.FilenameFilter;
import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;

/**
 * Created by Liuzhixue on 2017/9/25.
 */

public final class FTPFileComparator implements Comparator<File> {
    private static final SimpleDateFormat fileFormat = new SimpleDateFormat("yyyy-MM-dd");
    public static final String ACTION_SORT = "action_sort";
    public static final String ACTION_GROUP = "action_group";
    private final long MILLIS_IN_DAY = 24 * 3600 * 1000;
    private final static Date fileDate = new Date();
    private String action;

    public FTPFileComparator(String action) {
        this.action = action;
    }

    @Override
    public int compare(File lhs, File rhs) {
        return compareFile(lhs, rhs);
    }

    public static final FilenameFilter MEDIAFILTER = (dir, filename) -> filename.endsWith(".mp4") || filename.endsWith(".3gp") ||filename.endsWith(".avi")|| filename.endsWith("" +
            ".jpg") || filename.endsWith("" + ".png");


    private int compareFile(File lhs, File rhs) {


        long lhsDateMillis;
        long rhsDateMillis;
        String lhsDateString = FTPFileUtils.getFileNameNoExtension(lhs).replace("_","-");
        String rhsDateString = FTPFileUtils.getFileNameNoExtension(rhs).replace("_","-");
        fileFormat.setLenient(false);
        try {
            Date lhsDate = fileFormat.parse(lhsDateString);
            lhsDateMillis = lhsDate.getTime();
        } catch (ParseException pE) {
            lhsDateMillis = getFileTimeForDays(lhs.lastModified());
        }
        try {
            Date rhsDate = fileFormat.parse(rhsDateString);
            rhsDateMillis = rhsDate.getTime();
        } catch (ParseException pE) {
            rhsDateMillis = getFileTimeForDays(rhs.lastModified());
        }


        if (lhsDateMillis < rhsDateMillis) {
            return 1;
        } else if (lhsDateMillis == rhsDateMillis) {
            return 0;
        } else {

            return -1;
        }


    }

    public static long getFileTime(File targetFile) {
        String fileDateString = FTPFileUtils.getFileNameNoExtension(targetFile);
        long fileDateMillis;
        fileFormat.setLenient(false);
        try {
            Date lhsDate = fileFormat.parse(fileDateString);
            fileDateMillis = lhsDate.getTime();
        } catch (ParseException pE) {
            fileDateMillis = getFileTimeForDays(targetFile.lastModified());
        }
        return fileDateMillis;
    }

    public static String generateDateByTime(long time) {
        fileDate.setTime(time);
        return fileFormat.format(fileDate);
    }

    public static String generateMediaDate(File pFile) {
        return generateDateByTime(getFileTime(pFile));
    }


    public static long getFileTimeForDays(long timeMillis) {
        fileDate.setTime(timeMillis);
        String dayString = fileFormat.format(fileDate);
        try {
            Date tempDate = fileFormat.parse(dayString);
            return tempDate.getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static String generateMediaTime(long time) {
        int totalSeconds = (int) (time / 1000);
        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours = totalSeconds / 3600;
        return hours > 0 ? String.format("%02d:%02d:%02d", hours, minutes, seconds) : String
                .format("%02d:%02d", minutes, seconds);
    }

    public static String generateFileTimeOnlyHours(Serializable value) {
        String result = "00:00:00";
        if (value instanceof File) {
            long timeMillis = ((File) value).lastModified() - getFileTimeForDays(((File) value).lastModified());
            result = generateMediaTime(timeMillis);
        } else if (value instanceof FTPMediaFile) {
            result = ((FTPMediaFile) value).time;
        }
        return result;
    }

}
