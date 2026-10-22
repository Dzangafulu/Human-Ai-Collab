package com.example.human_ai_collaboration_platform;

public class Task {

    private int id;
    // Added for Firestore syncing. Existing integer ID remains unchanged.
    private String documentId;
    private String title;
    private String description;
    private String assignedTo;
    private String status;
    private String priority;
    private String dueDate;

    public Task(
            int id,
            String title,
            String description,
            String assignedTo,
            String status,
            String priority,
            String dueDate
    ) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.assignedTo = assignedTo;
        this.status = status;
        this.priority = priority;
        this.dueDate = dueDate;
    }

    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getAssignedTo() {
        return assignedTo;
    }

    public String getStatus() {
        return status;
    }

    public String getPriority() {
        return priority;
    }

    public String getDueDate() {
        return dueDate;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setAssignedTo(String assignedTo) {
        this.assignedTo = assignedTo;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public void setDueDate(String dueDate) {
        this.dueDate = dueDate;
    }
}
