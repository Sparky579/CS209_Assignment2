package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.Message;
import cn.edu.sustech.cs209.chatting.common.Tools;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.*;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class ClientThread extends Main implements Runnable{
    BufferedReader in;
    public volatile boolean running = true;

    public ClientThread(BufferedReader in) {
        this.in = in;
    }
    public void notifyClient() {
        locking = false;
        synchronized (condition) {
            condition.notifyAll();
        }
    }
    @Override
    public void run() {
        try {
            while (running) {
                //if socket is closed, close the thread
                String line = in.readLine();
                if (line.equals("Bye")) {
                    System.out.println("Server closed");
                    System.exit(0);
                }
                else if (line.startsWith("file")) {
                    Message message = Message.toMessage(line);
                    long lens = Long.parseLong(message.getType().substring(4));
                    System.out.println("Receiving file " + message.getData() + " of size " + lens + " bytes");
                    try {

                        InputStream inputStream = socket.getInputStream();
//                        FileOutputStream fileOut = new FileOutputStream(msg);
                        //if the file is not exist, create it

                        File file = Tools.makeFile(userID, message.getData());
                        OutputStream output = new FileOutputStream(file);
                        byte[] bytes = new byte[1024];
                        //store all bytes

                        int len;
                        while ((len = inputStream.read(bytes)) != -1) {
                            output.write(bytes, 0, len);
//                            System.out.println("Writing " + len + " bytes");
                            lens -= len;
                            if (lens <= 0) {
                                break;
                            }
                        }
                        //close the file
                        output.close();
                    } catch (IOException e) {
                        //e.printStackTrace();
                        System.out.println("File transfer failed (IO Exception)");
                    }
                    newestMessage = line;
                    notifyClient();
                }
                else {
                    newestMessage = line;
                    notifyClient();
                }
            }
        } catch (SocketException e){
            System.out.println("Socket closed");
            closeTrigger.set(true);
            notifyClient();
        } catch (IOException e){
            e.printStackTrace();
        }
    }
}

public class Main extends Application {
    static PrintWriter out;
    public static String username, userID;
    static boolean locking = false;
    final static AtomicReference<Boolean> closeTrigger = new AtomicReference<>(false);
    public static List<String> messages = new ArrayList<>();
    public static String newestMessage = "";
    static Lock lock = new ReentrantLock();
    static Socket socket;
    public static final Condition condition = lock.newCondition();

    public static void main(String[] args) throws IOException {
        new Thread(() -> {
            try {
                receive();
            }catch (ConnectException e) {
                System.out.println("Server not found");
                System.exit(0);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }).start();
        launch();
    }

    /**
     * Send a message to the server
     * @param stage
     * @throws IOException
     */
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
    protected static void sendFile(File file, String userName, String sendTo) throws IOException {
        send(new Message("file", file.length(), userName, sendTo, file.getName()));
        byte [] buffer = new byte[1024];
        int bytesRead;
        OutputStream socketOut = new DataOutputStream(socket.getOutputStream());
        InputStream inputStream = new FileInputStream(file);
        while (true) {
            try {
                bytesRead = inputStream.read(buffer);
                if (bytesRead == -1) {
                    break;
                }
                socketOut.write(buffer, 0, bytesRead);
//                System.out.println("Sending file: " + bytesRead + " bytes");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}