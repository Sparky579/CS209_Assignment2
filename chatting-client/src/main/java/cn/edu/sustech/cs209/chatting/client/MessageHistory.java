package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.Message;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MessageHistory implements Serializable {
    Map<String, List<Message>> history;
    int passwordHash;
    public MessageHistory(Map<String, List<Message>> history) {
        this.history = history;
    }
    public MessageHistory() {
        this.history = new java.util.HashMap<>();
    }
    public List<Message> getHistory(String name) {
        if (!history.containsKey(name)) {
            history.put(name, new ArrayList<>());
        }
        return history.get(name);
    }
    public void writeToFile(String filename) throws FileNotFoundException {
        //by serialization
        //if not exist, create a new file
        File file = new File(filename);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        FileOutputStream underStream = new FileOutputStream(filename);
        try {
            ObjectOutputStream out = new ObjectOutputStream(underStream);
            out.writeObject(this);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    public static MessageHistory readFromFile(String filename) throws FileNotFoundException {
        FileInputStream underStream = new FileInputStream(filename);
        try {
            ObjectInputStream in = new ObjectInputStream(underStream);
            return (MessageHistory) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }
    public void insertMessage(String name, Message message) {
        List<Message> list = history.computeIfAbsent(name, k -> new ArrayList<>());
        list.add(message);
    }
}
