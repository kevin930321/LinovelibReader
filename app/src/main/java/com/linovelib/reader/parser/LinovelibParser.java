package com.linovelib.reader.parser;

import android.util.Log;

import com.linovelib.reader.model.Chapter;
import com.linovelib.reader.model.ChapterContent;
import com.linovelib.reader.model.ChapterItem;
import com.linovelib.reader.model.Novel;
import com.linovelib.reader.model.Volume;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

public class LinovelibParser {
    private static final String TAG = "LinovelibParser";
    private static final String BASE_URL = "https://tw.linovelib.com";

    /**
     * 解析首頁推薦小說列表
     * 實際結構：a.module-slide-a 包含 img, figcaption (標題), p>span (作者)
     */
    public static List<Novel> parseNovelList(String html) {
        List<Novel> novels = new ArrayList<>();
        
        try {
            Document doc = Jsoup.parse(html);
            
            // 實際的選擇器：a.module-slide-a
            Elements novelElements = doc.select("a.module-slide-a");
            
            for (Element element : novelElements) {
                try {
                    Novel novel = new Novel();
                    
                    // 提取小說ID和標題從 href
                    String href = element.attr("href");
                    if (href.contains("/novel/")) {
                        // 從 URL 中提取 novel ID: /novel/4649.html -> 4649
                        String novelId = href.replaceAll(".*/novel/(\\d+)\\.html.*", "$1");
                        novel.setNovelId(novelId);
                    }
                    
                    // 提取標題 - 在 figcaption 標籤中
                    Element titleElement = element.selectFirst("figcaption");
                    if (titleElement != null) {
                        novel.setTitle(titleElement.text());
                    }
                    
                    // 提取封面 - img 標籤
                    Element cover = element.selectFirst("img");
                    if (cover != null) {
                        String coverUrl = cover.attr("data-src");
                        if (coverUrl.isEmpty()) {
                            coverUrl = cover.attr("src");
                        }
                        
                        // 如果是相對路徑，補充完整 URL
                        if (!coverUrl.startsWith("http")) {
                            coverUrl = BASE_URL + coverUrl;
                        }
                        // 過濾 SVG 圖片，因為 Glide 默認不支持
                        if (coverUrl != null && !coverUrl.toLowerCase().endsWith(".svg")) {
                            novel.setCoverUrl(coverUrl);
                        }
                    }
                    
                    // 提取作者 - p > span
                    // 結構: <p class="module-slide-author"><span class="clip">作者：</span><span class="gray">作者名</span></p>
                    Element authorSpan = element.selectFirst("p span.gray");
                    if (authorSpan != null) {
                        novel.setAuthor(authorSpan.text());
                    } else {
                        // Fallback logic
                        authorSpan = element.selectFirst("p span");
                        if (authorSpan != null) {
                            String text = authorSpan.text();
                            if (text.startsWith("作者：")) {
                                text = text.replace("作者：", "");
                            }
                            if (!text.isEmpty()) {
                                novel.setAuthor(text);
                            }
                        }
                    }
                    
                    if (novel.getNovelId() != null && novel.getTitle() != null) {
                        novels.add(novel);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing novel item", e);
                }
            }
            
            Log.d(TAG, "Parsed " + novels.size() + " novels from homepage");
        } catch (Exception e) {
            Log.e(TAG, "Error parsing novel list", e);
        }
        
        return novels;
    }

    /**
     * 解析小說詳情頁
     * 實際結構：
     * - 作者: a[href*=authorarticle]
     * - 插畫師: a[href*=illustratorarticle]
     * - 翻譯者: a[href*=translatorarticle]
     * - 標籤: a[href*=tagarticle]
     */
    public static Novel parseNovelDetail(String html) {
        Novel novel = new Novel();
        
        try {
            Document doc = Jsoup.parse(html);
            
            // 提取標題 - 通常在 h1 或特定 class 中
            Element title = doc.selectFirst("h1");
            if (title == null) {
                title = doc.selectFirst("div.book-name, div.book-title");
            }
            if (title != null) {
                novel.setTitle(title.text());
            }
            
            // 提取作者 - a[href*=authorarticle]
            Element authorLink = doc.selectFirst("a[href*=authorarticle]");
            if (authorLink != null) {
                novel.setAuthor(authorLink.text());
            }
            
            // 提取插畫師 - a[href*=illustratorarticle]
            Element illustratorLink = doc.selectFirst("a[href*=illustratorarticle]");
            if (illustratorLink != null) {
                novel.setIllustrator(illustratorLink.text());
            }
            
            // 提取翻譯者 - a[href*=translatorarticle]
            Element translatorLink = doc.selectFirst("a[href*=translatorarticle]");
            if (translatorLink != null) {
                novel.setTranslator(translatorLink.text());
            }
            
            // 提取封面
            Element cover = doc.selectFirst("div.book-img img, div.novel-cover img, img.book-cover");
            if (cover != null) {
                String coverUrl = cover.attr("src");
                if (!coverUrl.startsWith("http")) {
                    coverUrl = BASE_URL + coverUrl;
                }
                // 過濾 SVG 圖片，因為 Glide 默認不支持
                if (coverUrl != null && !coverUrl.toLowerCase().endsWith(".svg")) {
                    novel.setCoverUrl(coverUrl);
                }
            }
            
            // 提取簡介
            Element desc = doc.selectFirst("p.introduce, div.introduce, div.book-intro");
            if (desc != null) {
                novel.setDescription(desc.text());
            }
            
            // 提取標籤 - a[href*=tagarticle]
            List<String> tags = new ArrayList<>();
            Elements tagElements = doc.select("a[href*=tagarticle]");
            for (Element tag : tagElements) {
                tags.add(tag.text());
            }
            if (!tags.isEmpty()) {
                novel.setTags(tags);
            }
            
            // 提取評分
            Element rating = doc.selectFirst("div.score, span.score");
            if (rating != null) {
                try {
                    novel.setRating(Float.parseFloat(rating.text().trim()));
                } catch (NumberFormatException e) {
                    Log.w(TAG, "Failed to parse rating", e);
                }
            }
            
            Log.d(TAG, "Parsed novel detail: " + novel.getTitle());
        } catch (Exception e) {
            Log.e(TAG, "Error parsing novel detail", e);
        }
        
        return novel;
    }

    /**
     * 解析章節目錄
     * 實際結構：章節為 a.chapter-li-a，包含 span (章節標題)
     * 卷標題可能是文本節點或特定元素
     */
    public static List<Volume> parseCatalog(String html) {
        List<Volume> volumes = new ArrayList<>();
        
        try {
            Document doc = Jsoup.parse(html);
            
            // 查找所有章節連結 - a.chapter-li-a
            Elements chapterLinks = doc.select("a.chapter-li-a");
            
            if (chapterLinks.isEmpty()) {
                Log.w(TAG, "No chapters found with selector a.chapter-li-a");
                return volumes;
            }
            
            // 創建一個默認卷來存放所有章節
            // 實際網站可能需要更複雜的邏輯來識別卷分隔
            Volume currentVolume = new Volume("vol_1", "第一卷");
            
            // 嘗試查找卷標題
            Elements volumeTitles = doc.select("h3.volume-title, div.volume-name, h2:contains(第)");
            int volumeIndex = 0;
            
            // 如果找到卷標題，按卷分組
            if (!volumeTitles.isEmpty() && volumeTitles.size() > 1) {
                // 有多個卷的情況
                Element parent = chapterLinks.first().parent().parent();
                Elements allElements = parent.children();
                
                for (Element elem : allElements) {
                    // 檢查是否是卷標題
                    String text = elem.text();
                    if (text.matches(".*第.{1,3}卷.*") || text.matches(".*卷.*")) {
                        // 這是一個卷標題，創建新卷
                        if (!currentVolume.getChapters().isEmpty()) {
                            volumes.add(currentVolume);
                        }
                        volumeIndex++;
                        currentVolume = new Volume("vol_" + volumeIndex, text);
                    } else if (elem.tagName().equals("a") && elem.hasClass("chapter-li-a")) {
                        // 這是一個章節
                        Chapter chapter = parseChapterElement(elem);
                        if (chapter != null) {
                            currentVolume.addChapter(chapter);
                        }
                    }
                }
            } else {
                // 沒有明確的卷分隔，所有章節放在一個卷中
                for (Element chapterLink : chapterLinks) {
                    Chapter chapter = parseChapterElement(chapterLink);
                    if (chapter != null) {
                        currentVolume.addChapter(chapter);
                    }
                }
            }
            
            // 添加最後一個卷
            if (!currentVolume.getChapters().isEmpty()) {
                volumes.add(currentVolume);
            }
            
            Log.d(TAG, "Parsed " + volumes.size() + " volumes with total chapters: " + 
                  volumes.stream().mapToInt(v -> v.getChapters().size()).sum());
            
        } catch (Exception e) {
            Log.e(TAG, "Error parsing catalog", e);
        }
        
        return volumes;
    }

    /**
     * 解析單個章節元素
     * 實際結構：a.chapter-li-a > span (章節標題)
     */
    private static Chapter parseChapterElement(Element chapterLink) {
        try {
            String chapterUrl = chapterLink.attr("href");
            
            // 提取章節標題 - 在 span 中
            Element titleSpan = chapterLink.selectFirst("span");
            String chapterTitle = titleSpan != null ? titleSpan.text() : chapterLink.text();
            
            // 從 URL 中提取章節 ID
            String chapterId = chapterUrl.replaceAll(".*/novel/\\d+/(\\d+)\\.html.*", "$1");
            
            // 確保 URL 是完整的
            if (!chapterUrl.startsWith("http")) {
                chapterUrl = BASE_URL + chapterUrl;
            }
            
            return new Chapter(chapterId, chapterTitle, chapterUrl);
        } catch (Exception e) {
            Log.e(TAG, "Error parsing chapter element", e);
            return null;
        }
    }

    /**
     * 解析章節內容
     */
    public static ChapterContent parseChapterContent(String html) {
        ChapterContent content = new ChapterContent();
        List<ChapterItem> allItems = new ArrayList<>();
        StringBuilder fullTextBuilder = new StringBuilder();
        
        // 支援多頁內容拼接
        String[] pages = html.split("<!-- NEXT_PAGE_SPLIT -->");
        
        for (int i = 0; i < pages.length; i++) {
            String pageHtml = pages[i];
            try {
                Document doc = Jsoup.parse(pageHtml);
                
                // Cloudflare detection (只在第一頁檢查)
                if (i == 0) {
                    String pageTitle = doc.title();
                    if (pageTitle.contains("Cloudflare") || pageTitle.contains("Attention Required") || 
                        pageTitle.contains("Just a moment") || pageHtml.contains("cf-wrapper")) {
                        content.setTitle("無法讀取：Cloudflare 驗證");
                        content.setContent("檢測到網站啟用 Cloudflare 防護，App 無法自動通過驗證。\n\n建議：\n1. 稍後再試\n2. 使用瀏覽器打開網站");
                        return content;
                    }
                    
                    // 提取章節標題 (只從第一頁提取)
                    Element title = doc.selectFirst("h1, h2.chapter-title, div.chapter-title");
                    if (title != null) {
                        content.setTitle(title.text());
                    }
                }
                
                // 提取章節內容
                Element contentElement = doc.selectFirst("#acontent");
                if (contentElement == null) contentElement = doc.selectFirst("div.acontent");
                if (contentElement == null) contentElement = doc.selectFirst("#TextContent");
                if (contentElement == null) contentElement = doc.selectFirst("div.content");
                if (contentElement == null) contentElement = doc.selectFirst("div.chapter-content");
                if (contentElement == null) contentElement = doc.selectFirst("div#content");
                
                if (contentElement != null) {
                    contentElement.select("script, style, div.ads, div.google-auto-placed").remove();
                    
                    // 使用遞歸遍歷處理所有節點，確保順序正確
                    traverseNodes(contentElement, allItems, fullTextBuilder);
                }
                
                // 提取導航鏈接
                if (i == 0) {
                    Element prevLink = doc.selectFirst("a:contains(上一章), a.prev, a#pt_prev");
                    if (prevLink != null) {
                        String prevUrl = prevLink.attr("href");
                        if (!prevUrl.startsWith("http") && !prevUrl.isEmpty()) {
                            prevUrl = BASE_URL + prevUrl;
                        }
                        content.setPrevChapterUrl(prevUrl);
                    }
                }
                
                if (i == pages.length - 1) {
                    Element nextLink = doc.selectFirst("a:contains(下一章), a.next, a#pt_next");
                    if (nextLink != null) {
                        String nextUrl = nextLink.attr("href");
                        if (!nextUrl.startsWith("http") && !nextUrl.isEmpty()) {
                            nextUrl = BASE_URL + nextUrl;
                        }
                        content.setNextChapterUrl(nextUrl);
                    }
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error parsing chapter page " + i, e);
            }
        }
        
        content.setItems(allItems);
        content.setContent(fullTextBuilder.toString().trim());
        Log.d(TAG, "Parsed chapter content, total items: " + content.getItems().size());
        
        return content;
    }
    
    private static void traverseNodes(Node node, List<ChapterItem> items, StringBuilder fullText) {
        if (node instanceof TextNode) {
            String text = ((TextNode) node).getWholeText();
            // 暫存文本，等待區塊結束或遇到圖片時刷新
            // 但為了簡化邏輯，這裡如果是純文本節點且非空白，直接作為 TextItem
            // 注意：getWholeText保留了換行符，如果只是換行符應忽略
            if (!text.trim().isEmpty()) {
                 String cleaned = text.trim();
                 if (!cleaned.isEmpty()) {
                     items.add(new ChapterItem(ChapterItem.TYPE_TEXT, cleaned));
                     fullText.append(cleaned).append("\n\n");
                 }
            }
        } else if (node instanceof Element) {
            Element element = (Element) node;
            String tagName = element.tagName().toLowerCase();
            
            if (tagName.equals("img")) {
                String src = element.attr("data-src");
                if (src.isEmpty()) src = element.attr("src");
                
                if (!src.isEmpty()) {
                    if (!src.startsWith("http")) {
                        src = BASE_URL + src;
                    }
                    if (!src.contains("icon") && !src.endsWith(".svg")) {
                        items.add(new ChapterItem(ChapterItem.TYPE_IMAGE, src));
                    }
                }
            } else if (tagName.equals("br")) {
                // BR 標籤本身不產生 Item，但意味著如果前面有未處理的文本應該結束
                // 由於我們對 TextNode 直接處理了，這裡可以忽略，或者添加一個空行 Item (視需求)
            } else {
                // 對於容器元素 (div, p, span 等)，遞歸處理其子節點
                // 如果是 p 或 div，它們是塊級元素，這意味著前後有換行
                // 這裡的簡單遞歸會把 p 內部的 TextNode 提取出來
                for (Node child : element.childNodes()) {
                    traverseNodes(child, items, fullText);
                }
            }
        }
    }

    /**
     * 提取下一頁 URL (用於分頁章節)
     */
    public static String getNextPageUrl(String html) {
        try {
            // 嘗試從 JavaScript 變量中提取
            // var ReadParams={...,url_next:'/novel/4613/269999_2.html',...}
            int startIndex = html.indexOf("url_next:'");
            if (startIndex != -1) {
                startIndex += 10; // len("url_next:'")
                int endIndex = html.indexOf("'", startIndex);
                if (endIndex != -1) {
                    String url = html.substring(startIndex, endIndex);
                    if (!url.isEmpty() && !url.equals("#")) {
                        if (!url.startsWith("http")) {
                            url = BASE_URL + url;
                        }
                        return url;
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing next page url", e);
        }
        return null;
    }
}