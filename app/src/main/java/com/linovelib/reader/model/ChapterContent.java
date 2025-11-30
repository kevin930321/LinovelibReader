package com.linovelib.reader.model;

import java.io.Serializable;

import java.util.ArrayList;
import java.util.List;

public class ChapterContent implements Serializable {
    private String chapterId;
    private String title;
    private String content;
    private List<ChapterItem> items = new ArrayList<>();
    private String prevChapterUrl;
    private String nextChapterUrl;

    public ChapterContent() {
    }

    public ChapterContent(String chapterId, String title, String content) {
        this.chapterId = chapterId;
        this.title = title;
        this.content = content;
    }

    // Getters
    public String getChapterId() { return chapterId; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public List<ChapterItem> getItems() { return items; }
    public String getPrevChapterUrl() { return prevChapterUrl; }
    public String getNextChapterUrl() { return nextChapterUrl; }

    // Setters
    public void setChapterId(String chapterId) { this.chapterId = chapterId; }
    public void setTitle(String title) { this.title = title; }
    public void setContent(String content) { this.content = content; }
    public void setItems(List<ChapterItem> items) { this.items = items; }
    public void addItem(ChapterItem item) { this.items.add(item); }
    public void setPrevChapterUrl(String prevChapterUrl) { this.prevChapterUrl = prevChapterUrl; }
    public void setNextChapterUrl(String nextChapterUrl) { this.nextChapterUrl = nextChapterUrl; }
}
