package cn.edu.sustech.cs209.chatting.common;

public class Message {

    private Long timestamp;

    private String sentBy;

    private String sendTo;

    private String data;

    private String type;

    public Message(String type, Long timestamp, String sentBy, String sendTo, String data) {
        this.type = type;
        this.timestamp = timestamp;
        this.sentBy = sentBy;
        this.sendTo = sendTo;
        this.data = data;
    }

    public String getType() {
        return type;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public String getSentBy() {
        return sentBy;
    }

    public String getSendTo() {
        return sendTo;
    }

    public String getData() {
        return data;
    }

    public void reviseData() {
        data = data.replace(Tools.newLine, "\n");
    }

    public static Message toMessage(String content) {
        String[] split = content.split(":");
        String type = split[0];
//        System.out.println(content);
        Long timestamp = Long.parseLong(split[1]);
        String sentBy = split[2];
        String sendTo = split[3];
        //content may contain ':', so data is the rest of the string
//        for (int i = 5; i < split.length; i++) {
//            split[4] += ":" + split[i];
//        }
        //but if the message ends with "::", then the last ':' will be removed
        int cnt = 0;
        for (int i = 0; i < content.length() - 1; i++) {
            if (content.charAt(i) == ':') {
                cnt++;
            }
            if (cnt == 4) {
                split[4] = content.substring(i + 1);
                break;
            }
        }
        String data = split[4];
        return new Message(type, timestamp, sentBy, sendTo, data);
    }
    public String toString() {
        return type + ":" + timestamp + ":" + sentBy + ":" + sendTo + ":" + data;
    }
}
