package com.dingding.mid.utils;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;

import java.util.Calendar;
import java.util.Date;

public class Demo {
    public static void main(String[] args) {
        DateTime date = DateUtil.date();
        int year = date.year();
        int month = date.month();
        int i = date.dayOfMonth();
        System.err.println("");

    }
}
