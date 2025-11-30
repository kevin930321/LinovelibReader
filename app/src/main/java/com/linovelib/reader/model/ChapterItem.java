package com.linovelib.reader.model;

import java.io.Serializable;

public class ChapterItem implements Serializable {
    public static final int TYPE_TEXT = 0;
    public static final int TYPE_IMAGE = 1;
    public static final int TYPE_TITLE = 2;

    private int type;
    private String content; // Text content or Image URL
    private int width;
    private int height;

    public ChapterItem(int type, String content) {
        this.type = type;
        this.content = content;
    }

    public int getType() {
        return type;
    }

    public String getContent() {
        return content;
    }
    
    public void setDimensions(int width, int height) {
        this.width = width;
        this.height = height;
    }
    
    public int getWidth() { return width; }
    public int getHeight() { return height; }
}
