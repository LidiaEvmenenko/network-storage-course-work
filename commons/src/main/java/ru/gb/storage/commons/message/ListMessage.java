package ru.gb.storage.commons.message;

import java.nio.file.Path;
import java.util.List;

public class ListMessage extends Message {
    private List<String> pathList;

    public List<String> getPathList() {
        return pathList;
    }

    public void setPathList(List<String> pathList) {
        this.pathList = pathList;
    }
}