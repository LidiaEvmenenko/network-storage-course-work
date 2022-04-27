package ru.gb.storage.commons.message;

public class DeleteMessage extends Message {
    private String fileName;

    public DeleteMessage() {
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    @Override
    public String toString() {
        return "DeleteMessage{" +
                "fileName='" + fileName + '\'' +
                '}';
    }
}