package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.Message;
import cn.edu.sustech.cs209.chatting.common.Tools;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.File;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class Controller extends Main implements Initializable {

    @FXML
    ListView<Message> chatContentList;

    @FXML
    Label chatBottomLabel;

    @FXML
    ListView<String> chatList;

    @FXML
    TextArea notifyText;

    @FXML
    TextArea inputArea;

    MessageHistory messageHistory = new MessageHistory();

    String username;

    AtomicReference<String> currentChatting = new AtomicReference<>("All");

    Map<String, String> actualChatting = new HashMap<>();

    private String addToActualChatting(List<String> chattingUsers, String chatName) {
        //concatenate every users except the current user to a string, add "#2&" to the end of every user
        //for example, {"A", "B", "C"} -> "A#2&B#2&C#2&"
        //use stream
        if (chatList.getItems().contains(chatName)) {
            int adding = 1;
            while (true) {
                String newName = chatName + "<" + adding + ">";
                if (!chatList.getItems().contains(newName)) {
                    chatName = newName;
                    break;
                }
                adding += 1;
            }
        }
        String chattingUsersString = chattingUsers.stream().reduce("", (a, b) -> a + b + "@");
        //if the values of map contains the string, just return the key
        if (actualChatting.containsValue("GROUP@" + chattingUsersString)) {
            for (Map.Entry<String, String> entry : actualChatting.entrySet()) {
                if (entry.getValue().equals("GROUP@" + chattingUsersString)) {
                    return entry.getKey();
                }
            }
        }
        actualChatting.put(chatName, "GROUP@" + chattingUsersString);
        return chatName;
    }

    private String addToActualChatting(String chattingUser, String chatName) {
        String actual;
        if (username.compareTo(chattingUser) > 0) {
            actual = "FRIEND@" + chattingUser + "@" + username + "@";
        } else {
            actual = "FRIEND@" + username + "@" + chattingUser + "@";
        }
        actualChatting.put(chatName, actual);
        return chatName;
    }



    private String addToActualByActual(String actual) {
        String[] actualSplit = actual.split("@");
        String chatName;
        if (actualSplit[0].equals("FRIEND")) {
            if (username.equals(actualSplit[1])) {
                chatName = actualSplit[2];
            } else {
                chatName = actualSplit[1];
            }
        } else {
            if (actualSplit.length == 4) {
                chatName = actualSplit[1] + ", " + actualSplit[2] + ", " +
                        actualSplit[3] + " (" + (actualSplit.length - 1) + ")";
            }
            else chatName = actualSplit[1] + ", " + actualSplit[2] + ", "
                    + actualSplit[3] + "... (" + (actualSplit.length - 1) + ")";
            if (chatList.getItems().contains(chatName)) {
                int adding = 1;
                while (true) {
                    String newName = chatName + "<" + adding + ">";
                    if (!chatList.getItems().contains(newName)) {
                        chatName = newName;
                        break;
                    }
                    adding += 1;
                }
            }
        }
        actualChatting.put(chatName, actual);
        return chatName;
    }

    public boolean serverAvailable() {
        return !closeTrigger.get();
    }

    private void setChatBottomLabel() {
        chatBottomLabel.setText("Current User: " + username + " | Current Chatting: " + currentChatting.get());
    }

    //important one! To detect all messages from server
    private void detectChanges() {
        while (true) {

            locking = true;
            try {
                synchronized (condition) {
                    if (locking) {
                        condition.wait();
                    }
                }
//                System.out.println("Detecting changes...");
//                System.out.println(closeTrigger.get());
                if (closeTrigger.get()) {
                    notifyText.setText("Server closed!!");
                    continue;
                }
                if (newestMessage.startsWith("server")) {
                    Message message = Message.toMessage(newestMessage);
                    message.reviseData();
                    messageHistory.insertMessage(message.getSendTo(), message);
                    if (!message.getSendTo().equals("All") && !actualChatting.containsValue(message.getSendTo())) {
                        Platform.runLater(() -> chatList.getItems().add(addToActualByActual(message.getSendTo())));
                    }
                    Platform.runLater(() -> {
                        if (getActual().equals(message.getSendTo())) {
                            chatContentList.getItems().add(message);
                        }
                        else {
//                            chatContentList.getItems().set(0,
//                                    new Message("x", message.getTimestamp(), "server", "All", "New message from " + message.getSendTo()));
                            if (message.getSendTo().startsWith("FRIEND")) notifyText.setText("New message from " + message.getSentBy() + "\n" + "At time: " + message.getTimestamp());
                            else if (message.getSendTo().startsWith("GROUP")) {
                                for (Map.Entry<String, String> entry : actualChatting.entrySet()) {
                                    if (entry.getValue().equals(message.getSendTo())) {
                                        notifyText.setText("New message from group: " + entry.getKey() + "\nAt time:" + message.getTimestamp());
                                        break;
                                    }
                                }
                            }
                            else {
                                notifyText.setText("New message from All" + "\n" + "At time: " + message.getTimestamp());
                            }
                        }
                    });
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public String getActual() {
        String actual;
        actual = actualChatting.getOrDefault(currentChatting.toString(), "All");
        return actual;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

//        Dialog<String> dialog = new TextInputDialog();
//        dialog.setTitle("Login");
//        dialog.setHeaderText(null);
//        dialog.setContentText("Username:");

        TextInputDialog dialog = new TextInputDialog();
        dialog.setHeaderText(null);
        dialog.setContentText("Username:");
        dialog.setTitle("登录");
        dialog.setHeaderText("请输入您的账号和密码：");


        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        Label lblUserName = new Label("账号:");
        TextField txtUserName = new TextField();
        Label lblPassword = new Label("密码:");
        PasswordField txtPassword = new PasswordField();

        grid.add(lblUserName, 1, 1);
        grid.add(txtUserName, 2, 1);
        grid.add(lblPassword, 1, 2);
        grid.add(txtPassword, 2, 2);

        dialog.getDialogPane().setContent(grid);

        String input = null;
        String password = null;
        /*
           TODO: Check if there is a user with the same name among the currently logged-in users,
                 if so, ask the user to change the username
         */
//        dialog.setOnCloseRequest(event -> {
//            send(new Message("exit", System.currentTimeMillis(), username, "server", username));
//            Platform.exit();
//        });
        boolean ok = false;
        while (!ok) {
            Optional<String> result = dialog.showAndWait();
            if (result.isPresent()){
                input = txtUserName.getText();
                password = txtPassword.getText();
                // 进行相应的操作
            }
            //if the user clicks the cancel button, exit the program

            //hold on the cancel or exit button

            if (input == null || input.isEmpty()) {
                System.out.println("Invalid username " + input + ", exiting");
                send(new Message("exit", System.currentTimeMillis(), username, "server", username));
                Platform.exit();
            }

            username = input;
            send(new Message("name", System.currentTimeMillis(), username, "server", username));
            locking = true;
            try {
                synchronized (condition) {
                    if (locking) {
                        condition.wait();
                    }
                }
                if (newestMessage.startsWith("welcome")) {
                    ok = true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
//            else dialog.setContentText("Username already exists, please change another one:");
        }
        notifyText.setEditable(false);
        chatContentList.setCellFactory(new MessageCellFactory());
        chatContentList.getItems().add(new Message("message", System.currentTimeMillis(),
                "server", username, "Welcome to the chat room!"));
        chatList.getItems().add("All");
        chatList.getSelectionModel().select(0);
        setChatBottomLabel();
        chatList.getSelectionModel().selectedItemProperty().addListener((observableValue, s, t1) -> {
            currentChatting.set(t1);
            setChatBottomLabel();
        });
        Thread thread = new Thread(this::detectChanges);
        thread.start();
    }

    private ArrayList<String> getUserList() {
        ArrayList<String> users = new ArrayList<>();
        send(new Message("list", System.currentTimeMillis(), username, "server", username));
        locking = true;
        try{
            synchronized (condition) {
                if (locking) {
                    condition.wait();
                }
            }
            String[] userArray = newestMessage.split("#2&");
            for (String u : userArray) {
                users.add(u);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return users;
    }

    @FXML
    public void createPrivateChat() {

        if (!serverAvailable()) return;

        Stage stage = new Stage();
        ComboBox<String> userSel = new ComboBox<>();

        // FIXME: get the user list from server, the current user's name should be filtered out
        send(new Message("list", System.currentTimeMillis(), username, "server", username));
        locking = true;
        List<String> users = getUserList();

        users.forEach(u -> {
            if (!Objects.equals(u, username)) {
                userSel.getItems().add((String) u);
            }
        });


        Button okBtn = new Button("OK");
        okBtn.setOnAction(e -> {
            if (userSel.getSelectionModel().getSelectedItem() == null) {
                return;
            }
            changeCurrentChatting(addToActualChatting(userSel.getSelectionModel().getSelectedItem(),
                    userSel.getSelectionModel().getSelectedItem()));
//            currentChatting.set(userSel.getSelectionModel().getSelectedItem());
            stage.close();
        });

        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(20, 20, 20, 20));
        box.getChildren().addAll(userSel, okBtn);
        stage.setScene(new Scene(box));
        stage.showAndWait();

        // TODO: if the current user already chatted with the selected user, just open the chat with that user
        // TODO: otherwise, create a new chat item in the left panel, the title should be the selected user's name
    }

    private void changeCurrentChatting() {
        currentChatting.set(chatList.getSelectionModel().getSelectedItem());
        setChatBottomLabel();
        chatContentList.getItems().clear();
        chatContentList.getItems().addAll(messageHistory.getHistory(getActual()));
    }

    private void changeCurrentChatting(String chatName) {
        currentChatting.set(chatName);
        if (!chatList.getItems().contains(currentChatting.get()))
            chatList.getItems().add(currentChatting.get());
        chatList.getSelectionModel().select(currentChatting.get());
        changeCurrentChatting();
    }

    private String toGroupName(ArrayList<String> userList) {
        Collections.sort(userList);
        if (userList.size() > 3) {
            return userList.get(0) + ", " + userList.get(1) + ", " + userList.get(2) + "... (" + userList.size() + ")";
        } else {
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < userList.size(); i++) {
                sb.append(userList.get(i));
                if (i != userList.size() - 1) sb.append(", ");
            }
            sb.append(" (").append(userList.size()).append(")");
            return sb.toString();
        }
    }

    /**
     * A new dialog should contain a multi-select list, showing all user's name.
     * You can select several users that will be joined in the group chat, including yourself.
     * <p>
     * The naming rule for group chats is similar to WeChat:
     * If there are > 3 users: display the first three usernames, sorted in lexicographic order, then use ellipsis with the number of users, for example:
     * UserA, UserB, UserC... (10)
     * If there are <= 3 users: do not display the ellipsis, for example:
     * UserA, UserB (2)
     */
    @FXML
    public void createGroupChat() {
        if (!serverAvailable()) return;
        //create a multi-select list
        Stage stage = new Stage();
        ListView<String> userSel = new ListView<>();
        userSel.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        List userList = getUserList();
        userList.forEach(u -> {
            if (!Objects.equals(u, username)) {
                userSel.getItems().add((String) u);
            }
        });
        //show the list
        Button okBtn = new Button("OK");
        okBtn.setOnAction(e -> {
            ObservableList<String> selectedUsers = userSel.getSelectionModel().getSelectedItems();
            ArrayList<String> copyList = new ArrayList<>(selectedUsers);
            copyList.add(username);
            Collections.sort(copyList);
            if (copyList.size() <= 2) {
                stage.close();
                return;
            }
            String chatName = toGroupName(copyList);
            changeCurrentChatting(addToActualChatting(new ArrayList<>(copyList), chatName
            ));
//            currentChatting.set(chatName);
            stage.close();
        });
        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(20, 20, 20, 20));
        box.getChildren().addAll(userSel, okBtn);
        stage.setScene(new Scene(box));
        stage.showAndWait();
    }

    /**
     * Sends the message to the <b>currently selected</b> chat.
     * <p>
     * Blank messages are not allowed.
     * After sending the message, you should clear the text input field.
     */
    @FXML
    public void doSendMessage() {
        // TODO
        String msg = inputArea.getText();
        inputArea.clear();
        //replace all the \n with space
        msg = msg.replaceAll("\n", Tools.newLine);
        if (msg.isEmpty()) return;
        if (msg.replaceAll(Tools.newLine, "").replaceAll(" ", "").isEmpty()) return;

//        System.out.println(actualChatting.get(currentChatting.toString()));
        String actual = getActual();
        Message message = new Message("message", System.currentTimeMillis(), username, actual, msg);
        send(message);
    }

    @FXML
    public void doSendFile() {
        Stage stage = new Stage();
        File file = GUITools.showFileDialog(stage);
        try{
            System.out.println(file.getName());
        }
        catch (NullPointerException e) {
            System.out.println("file does not exist!");
            return;
        }
    }

    @FXML
    public void changeChatting() {
        changeCurrentChatting();
    }

    /**
     * You may change the cell factory if you changed the design of {@code Message} model.
     * Hint: you may also define a cell factory for the chats displayed in the left panel, or simply override the toString method.
     */
    //bug: 超过5条消息后，会出现消息重复的情况
        //already fixed
    private class MessageCellFactory implements Callback<ListView<Message>, ListCell<Message>> {
        @Override
        public ListCell<Message> call(ListView<Message> param) {
            return new ListCell<Message>() {
                HBox wrapper = new HBox();
                Label nameLabel = new Label();
                Label msgLabel = new Label();

                // Constructor
                {
                    nameLabel.setPrefSize(50, 19);
                    nameLabel.setWrapText(true);
                    nameLabel.setStyle("-fx-border-color: black; -fx-border-width: 1px;");
                }

                @Override
                public void updateItem(Message msg, boolean empty) {
                    super.updateItem(msg, empty);
                    if (empty || Objects.isNull(msg)) {
                        setGraphic(null);
                        setText(null);
                        return;
                    }

                    nameLabel.setText(msg.getSentBy());
                    msgLabel.setText(msg.getData());

                    if (username.equals(msg.getSentBy())) {
                        wrapper.setAlignment(Pos.TOP_RIGHT);
                        wrapper.getChildren().setAll(msgLabel, nameLabel);
                        msgLabel.setPadding(new Insets(0, 20, 0, 0));
                    } else {
                        wrapper.setAlignment(Pos.TOP_LEFT);
                        wrapper.getChildren().setAll(nameLabel, msgLabel);
                        msgLabel.setPadding(new Insets(0, 0, 0, 20));
                    }

                    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                    setGraphic(wrapper);
                }
            };
        }
    }

}
