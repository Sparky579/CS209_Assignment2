package cn.edu.sustech.cs209.chatting.common;

import java.io.File;

public class Tools {
    private static class toolClass {
        private static final Tools instance = new Tools();
    }
    private Tools() {}
    public static Tools getInstance() {
        return toolClass.instance;
    }
    public static String newLine = "惎";
    public static void makeDir(String userID) {
        File file = new File("data");
        file.mkdir();
        File file2 = new File("data/" + userID);
        file2.mkdir();
    }
    public static String toUserID(String userName) {
        return userName + userName.hashCode() % 2000000000;
    }
    public static File makeFile(String userID, String fileName) {
        makeDir(userID);
        File file = new File("data/" + userID + "/" + fileName);
        System.out.println(file.getAbsolutePath());
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return file;
    }
    public static String getFilePath(String userID, String fileName) {
        return "data/" + userID + "/" + fileName;
    }
}
