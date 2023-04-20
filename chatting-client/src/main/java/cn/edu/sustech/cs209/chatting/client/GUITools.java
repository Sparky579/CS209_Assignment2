package cn.edu.sustech.cs209.chatting.client;

import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

public class GUITools {
    public static File showFileDialog(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择文件");
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));

        // 添加文件类型过滤器
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("所有文件", "*.*"),
                new FileChooser.ExtensionFilter("文本文件", "*.txt", "*.md"),
                new FileChooser.ExtensionFilter("图像文件", "*.png", "*.jpg", "*.gif"),
                new FileChooser.ExtensionFilter("音频文件", "*.mp3", "*.wav"),
                new FileChooser.ExtensionFilter("视频文件", "*.mp4", "*.avi"));

        return fileChooser.showOpenDialog(stage);
    }
}
