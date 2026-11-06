package com.hacms.app;

public class ChatBotModel {
    private String message;
    private String sender;

    // Default Constructor (Required for Firebase)
    public ChatBotModel() {
        this.message = "";
        this.sender = "";
    }

    // Parameterized Constructor
    public ChatBotModel(String message, String sender) {
        this.message = message;
        this.sender = sender;
    }

    // Getter and Setter for message
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    // Getter and Setter for sender
    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }
}
