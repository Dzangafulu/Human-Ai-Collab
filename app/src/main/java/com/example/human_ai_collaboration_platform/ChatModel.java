package com.example.human_ai_collaboration_platform;

public class ChatModel {
    private String message;
    private String sender;

    // Default Constructor (Required for Firebase)
    public ChatModel() {
        this.message = "";
        this.sender = "";
    }

    // Parameterized Constructor
    public ChatModel(String message, String sender) {
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
