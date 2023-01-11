package com.RuleApi.common;

import com.alibaba.fastjson.JSONObject;
import org.springframework.boot.system.ApplicationHome;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EditFile {

    /**
     * 替换文本文件中的 非法字符串
     */
    public void replacTextContent(String srcStr,String replaceStr) throws IOException {

        ApplicationHome h = new ApplicationHome(getClass());
        File jarF = h.getSource();
        /* 配置文件路径 */
        String path = jarF.getParentFile().toString()+"/application.properties";
        // 读
        File file = new File(path);
        FileReader in = new FileReader(file);
        BufferedReader bufIn = new BufferedReader(in);
        // 内存流, 作为临时流
        CharArrayWriter tempStream = new CharArrayWriter();
        // 替换
        String line = null;
        while ((line = bufIn.readLine()) != null) {
            // 替换每行中, 符合条件的字符串
            line = line.replaceAll(srcStr, replaceStr);
            // 将该行写入内存
            tempStream.write(line);
            // 添加换行符
            tempStream.append(System.getProperty("line.separator"));
        }
        // 关闭 输入流
        bufIn.close();
        // 将内存中的流 写入 文件
        FileWriter out = new FileWriter(file);
        tempStream.writeTo(out);
        out.close();
        System.out.println("====path:" + path);
    }
    /**
     * unicode转中文
     * @param str
     * @return
     * @author yutao
     * @date 2017年1月24日上午10:33:25
     */
    public static String unicodeToString(String str) {

        Pattern compile = Pattern.compile("(\\\\u(\\p{XDigit}{4}))");

        Matcher matcher = compile.matcher(str);

        char ch;

        while (matcher.find()) {

            ch = (char) Integer.parseInt(matcher.group(2), 16);

            str = str.replace(matcher.group(1), ch+"" );

        }

        return str;

    }
    //写入操作日志txt
    public void setLog(String text){
        System.out.println("添加日志"+text);
        try{
            Calendar cal = Calendar.getInstance();
            int year = cal.get(Calendar.YEAR);
            int month=cal.get(Calendar.MONTH)+1;
            int day=cal.get(Calendar.DATE);
            ApplicationHome h = new ApplicationHome(getClass());
            File jarF = h.getSource();
            /* 读取文件 */
            String pathname = jarF.getParentFile().toString()+"/userlog/"+year+month+day+".txt";
            File file = new File(pathname);
            createFile(file);
            String encoding = "UTF-8";

            Long filelength = file.length();
            String oldText = "";
            byte[] filecontent = new byte[filelength.intValue()];
            try {
                FileInputStream in = new FileInputStream(file);
                in.read(filecontent);
                oldText = new String(filecontent, encoding);
                oldText = unicodeToString(oldText);

            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("文件"+pathname+"读取出错");
            }
            Long date = System.currentTimeMillis();
            String created = String.valueOf(date).substring(0,10);
            text = oldText + "\n\r"+created+"||"+text+",";
            BufferedWriter writer = new BufferedWriter(new FileWriter(new File(pathname)));
            writer.write(text);
            writer.close();
        }catch(Exception e){
            e.printStackTrace();
            System.out.println("文件修改出错"+e);
        }
    }
    public static void createFile(File file) {
        if (file.exists()) {
            System.out.println("File exists");
        } else {
            System.out.println("File not exists, create it ...");
            //getParentFile() 获取上级目录(包含文件名时无法直接创建目录的)
            if (!file.getParentFile().exists()) {
                System.out.println("not exists");
                //创建上级目录
                file.getParentFile().mkdirs();
            }
            try {
                //在上级目录里创建文件
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


}
