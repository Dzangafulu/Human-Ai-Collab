package com.hacms.app;

public class Task {

    private int id;
    private int progressPercent=0;
    public int getProgressPercent(){return progressPercent;}
    public void setProgressPercent(int x){progressPercent=Math.max(0,Math.min(100,x));}
    private String documentId;
    private String dueTime="23:59";
    private String assigneeUid="";
    private String groupId="";
    private String ownerUid="";
    public String getDocumentId() {return documentId;}
    public void setDocumentId(String x) {documentId=x;}
    public String getDueTime(){return dueTime;}
    public void setDueTime(String x){dueTime=x;}
    public String getAssigneeUid(){return assigneeUid;}
    public void setAssigneeUid(String x){assigneeUid=x;}
    public String getGroupId(){return groupId;}
    public void setGroupId(String x){groupId=x;}
    public String getOwnerUid(){return ownerUid;}
    public void setOwnerUid(String x){ownerUid=x;}

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
