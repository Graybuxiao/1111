package com.dingding.mid.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class GetGMTTime {
    /***************************************************************************
     * The number of milliseconds in an hour.
     */
    private final static long MS_IN_HOUR = 60 * 60 * 1000;
    /***************************************************************************
     * The number of milliseconds in a minute.
     */
    private final static long MS_IN_MINUTE = 60 * 1000;
    /**
     * iSO 8601 Date format
     */
    private final static SimpleDateFormat isoFormat_ = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    /***************************************************************************
     * Format the given date and return the resulting string in ISO 8601 format.
     * The format is as follows: "yyyy-MM-dd'T'HH:mm:ss.SSS[Z|[+|-]HH:mm]".
     *
     * @param inputDate  The date to be converted into string format.
     * @return The formatted date/time string.
     */
    public static String isoFormat(Date inputDate) {
        // Setup the date format and convert the given date.
        // SimpleDateFormat dateFormat = new
        // SimpleDateFormat(ISO_FORMAT);
        String dateString = isoFormat_.format(inputDate);
        // Determine the time zone and concatenate the time zone
        // designator
        // onto the formatted date/time string.
        TimeZone tz = isoFormat_.getTimeZone();
        String tzName = tz.getDisplayName();
        if (tzName.equals("Greenwich Mean Time")) {
            dateString = dateString.concat("Z");
        } else {
            // Determine the hour offset. Add an hour if daylight
            // savings
            // is in effect.
            long tzOffsetMS = tz.getRawOffset();
            long tzOffsetHH = tzOffsetMS / MS_IN_HOUR;
            if (tz.inDaylightTime(inputDate)) {
                tzOffsetHH = tzOffsetHH + 1;
            }
            String hourString = String.valueOf(Math.abs(tzOffsetHH));
            if (hourString.length() == 1) {
                hourString = "0" + hourString;
            }
            // Determine the minute offset.
            long tzOffsetMMMS = tzOffsetMS % MS_IN_HOUR;
            long tzOffsetMM = 0;
            if (tzOffsetMMMS != 0) {
                tzOffsetMM = tzOffsetMMMS / MS_IN_MINUTE;
            }
            String minuteString = String.valueOf(tzOffsetMM);
            if (minuteString.length() == 1) {
                minuteString = "0" + minuteString;
            }
            // Determine the sign of the offset.
            String sign = "+";
            if (String.valueOf(tzOffsetMS).indexOf("-") == 0) {
                sign = "-";
            }
//          if (String.valueOf(tzOffsetMS).indexOf("+") != -1) {
//              sign = "-";
//          }
            dateString = dateString.concat(sign + hourString + ":"+ minuteString);
        }

        return (dateString);
    }


}
