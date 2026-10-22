package com.example.human_ai_collaboration_platform;

public class MessageModel {
    private String senderId;
    private String messageId;
    private String message;
    private long timestamp;

    public MessageModel(String senderId, String message, String messageId, long timestamp) {
        this.senderId = senderId;
        this.message = message;
        this.messageId = messageId;
        this.timestamp = timestamp;
    }

    public MessageModel() {
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
