package com.ks.utils;


import java.sql.Connection;
import java.sql.DriverManager;

public class JDBCUtil {

    //static Connection connection = null;

    public  static  Connection getConnectionByJDBC(){
        Connection connection = null;
        if (connection==null){
            try {
                Class.forName(Constant.MYSQL_DRIVER);
                connection = DriverManager.getConnection(Constant.MYSQL_URL,Constant.MYSQL_USERNAME,Constant.MYSQL_PASSWORD);
                connection.setAutoCommit(false);
            } catch (Exception e) {
                System.out.println("连接建立失败！");
                e.printStackTrace();
            }
        }
        return  connection;
    }
}
