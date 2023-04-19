package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.Message;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class ClientThread extends Main implements Runnable{
    BufferedReader in;
    public volatile boolean running = true;
    public ClientThread(BufferedReader in) {
        this.in = in;
    }
    @Override
    public void run() {
        try {
            while (running) {
                //if socket is closed, close the thread
                String line = in.readLine();
                System.out.println("Received: " + line);
                if (line.equals("Bye")) {
                    System.out.println("Server closed");
                    System.exit(0);
                }
                locking = false;
                newestMessage = line;
                synchronized (condition) {
                    condition.notifyAll();
                }
            }
        } catch (SocketException e) {
            System.out.println("Socket closed");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

public class Main extends Application {
    static PrintWriter out;
    static boolean locking = false;
    public static List<String> messages = new ArrayList<>();
    public static String newestMessage = "";
    static Lock lock = new ReentrantLock();
    public static Condition condition = lock.newCondition();
    public static void main(String[] args) throws IOException {
        new Thread(() -> {
            try {
                receive();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
        launch();
    }

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("main.fxml"));
        stage.setScene(new Scene(fxmlLoader.load()));
        stage.setTitle("Chatting Client");
        stage.setOnCloseRequest(event -> {
            System.out.println("Closing");
            send(new Message("exit", System.currentTimeMillis(), "0", "server", "0"));
            System.exit(0);
        });
        stage.show();

    }
    public static void receive() throws IOException {
        System.out.println("Starting client");
        Socket socket = null;
        socket = new Socket("127.0.0.1", 377);
        System.out.println("Client started");
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        out = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        ClientThread clientThread = new ClientThread(in);
        new Thread(clientThread).start();
        //send message to clients
        String line = reader.readLine();
        while (!line.equals("exit")) {
            System.out.println("Sending: " + line);
            send(line);
            line = reader.readLine();
        }
        clientThread.running = false;
        out.close();
        in.close();
        socket.close();
    }
    @Deprecated
    protected static void send(String content) {
        out.println(content);
    }
    protected static void send(Message message) {
        out.println(message.toString());
    }
}
