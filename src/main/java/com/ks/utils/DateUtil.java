package com.ks.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 判断日期是否有效
 */
public class DateUtil {

    private  static  final String  C_DATE_FORMAT = "yyyy-MM-dd";

    public   static  boolean  isDate(String createDate,String startDate){
        try {
            SimpleDateFormat simpleDateFormat  = new SimpleDateFormat(C_DATE_FORMAT);
            Date cdate = simpleDateFormat.parse(createDate);
            Date sdate = simpleDateFormat.parse(startDate);
            if (cdate.getTime()>=sdate.getTime()){
                return  true;
            }else{
                return  false;
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return  false;
    }
}
