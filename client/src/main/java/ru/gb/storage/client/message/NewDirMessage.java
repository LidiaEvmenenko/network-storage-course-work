package ru.gb.storage.client.message;
import java.nio.file.Path;

public class NewDirMessage extends Message{
    private Path path;
    private String message;

    public NewDirMessage() {
    }

    public Path getPath() {
        return path;
    }

    public void setPath(Path path) {
        this.path = path;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}