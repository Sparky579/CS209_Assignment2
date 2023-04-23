package cn.edu.sustech.cs209.chatting.server;

import cn.edu.sustech.cs209.chatting.common.Message;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

class ServerThread extends Main implements Runnable{
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String name;
    private OnlineState onlineState = OnlineState.OFFLINE;
    public ServerThread(Socket socket, List<ServerThread> serverThreads) throws IOException {
        this.socket = socket;
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);
    }
    public static boolean valid_name(String name) {
        if (name.equals("server")) return false;
        if (name.contains("#2&")) return false;
        if (name.equals("All")) return false;
        if (name.contains("@")) return false;
        if (name.equals("null")) return false;
        return true;
    }
    public void quit() throws IOException {
        onlineState = OnlineState.OFFLINE;
        serverThreads.remove(this);
        socket.close();
        //stop the thread

    }
    @Override
    public void run() {
        try {
            name = socket.getInetAddress().getHostAddress();
            System.out.println(name + " connected");
            onlineState = OnlineState.ONLINE;
            while (onlineState == OnlineState.ONLINE) {
                String line = in.readLine();
                if (line == null) {
                    System.out.println("Client " + name + " disconnected");
                    quit();
                    break;
                }
                Message message = Message.toMessage(line);
                System.out.println("Received: " + message);
                boolean found = false;
                if (message.getType().equals("name")) {
                    String name1 = message.getData();
                    for (ServerThread serverThread : serverThreads) {
                        if (serverThread.name.equals(name1)) {
                            out.println("Name already exists");
                            found = true;
                        }
                    }
                    if (!valid_name(name1)) found = true;
                    if (found) {
                        continue;
                    }
                    name = name1;
                    out.println("welcome " + name);
                    System.out.println("Rename to " + name);
                    continue;
                }
                if (message.getType().equals("message")) {
                    String to = message.getSendTo();
                    String from = message.getSentBy();
                    String msg = message.getData();
                    for (ServerThread serverThread : serverThreads) {
                        if (to.contains("@" + serverThread.name + "@") || to.equals("All") || serverThread.name.equals(from)) {
                            Message message1 = new Message("server", System.currentTimeMillis(), from, to, msg);
                            serverThread.out.println(message1);
                        }
                    }
                    continue;
                }
                if (message.getType().equals("list")) {
                    System.out.println("List requested");
                    StringBuffer buffer = new StringBuffer("");
                    for (ServerThread serverThread : serverThreads) {
                        if (serverThread.name.equals("127.0.0.1")) continue;
                        buffer.append(serverThread.name).append("#2&");
                    }
                    out.println(buffer);
                    continue;
                }
                if (message.getType().equals("exit")) {
                    out.println("Bye");
                    System.out.println("Client " + name + " disconnected");
                    quit();
                    break;
                }
                if (message.getType().equals("file")) {
                    String to = message.getSendTo();
                    String from = message.getSentBy();
                    String msg = message.getData();
                    long lens = message.getTimestamp();
                    List<ServerThread> sendList = new ArrayList<>();
                    for (ServerThread serverThread : serverThreads) {
                        if (to.contains("@" + serverThread.name + "@") || to.equals("All") || serverThread.name.equals(from)) {
                            sendList.add(serverThread);
                        }
                    }
                    sendList.forEach(x -> {
                        Message message1 = new Message("file" + message.getTimestamp(), System.currentTimeMillis(), from, to, msg);
                        x.out.println(message1);
                    });
                    try{
                        InputStream inputStream = socket.getInputStream();
                        byte[] bytes = new byte[1024];

                        int len;
                        while ((len = inputStream.read(bytes)) != -1) {
//                            fileOut.write(bytes, 0, len);
//                            System.out.println("Received " + len + " bytes");
                            int finalLen = len;
                            sendList.forEach(x -> {
                                try {
                                    x.socket.getOutputStream().write(bytes, 0, finalLen);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            });
                            lens -= len;
                            if (lens <= 0) break;
                        }
                        System.out.println("finish transfer");
                    }
                    catch (IOException e) {
                        //e.printStackTrace();
                        System.out.println("File transfer failed (IO Exception)");
                    }



                }
            }
        }
        catch (SocketException e) {
            System.out.println("Socket Exception");
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
    String getName() {
        return name;
    }
}

public class Main {
    static List<ServerThread> serverThreads = new ArrayList<>();

    public static void work() throws IOException {
        ServerSocket serverSocket = new ServerSocket(377);
        System.out.println("Server started");
        while (true) {
            Socket accept = serverSocket.accept();
            synchronized (serverThreads) {
                ServerThread thread = new ServerThread(accept, serverThreads);
                serverThreads.add(thread);
                new Thread(thread).start();
            }
        }
    }

    public static void main(String[] args) throws IOException {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    work();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        while (true) {
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
            String line = in.readLine();
            if (line.startsWith("list")) {
                synchronized (serverThreads) {
                    for (ServerThread thread : serverThreads) {
                        System.out.println(thread.getName());
                    }
                }
            }
            if (line.startsWith("exit")) {
                System.exit(0);
            }
        }
    }
}
