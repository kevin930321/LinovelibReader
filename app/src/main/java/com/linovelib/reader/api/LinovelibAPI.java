package com.linovelib.reader.api;

import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class LinovelibAPI {
    private static final String TAG = "LinovelibAPI";
    private static final String BASE_URL = "https://tw.linovelib.com";
    private static final int TIMEOUT_SECONDS = 15;
    
    private static LinovelibAPI instance;
    private final OkHttpClient client;

    private LinovelibAPI() {
        client = new OkHttpClient.Builder()
                .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .cookieJar(new CookieJar() {
                    private final HashMap<String, List<Cookie>> cookieStore = new HashMap<>();

                    @Override
                    public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
                        if (cookies != null && !cookies.isEmpty()) {
                            List<Cookie> currentCookies = cookieStore.get(url.host());
                            if (currentCookies == null) {
                                currentCookies = new ArrayList<>();
                                cookieStore.put(url.host(), currentCookies);
                            }
                            // Simple logic: replace old cookies with new ones by name
                            for (Cookie newCookie : cookies) {
                                for (int i = 0; i < currentCookies.size(); i++) {
                                    if (currentCookies.get(i).name().equals(newCookie.name())) {
                                        currentCookies.remove(i);
                                        break;
                                    }
                                }
                                currentCookies.add(newCookie);
                            }
                        }
                    }

                    @Override
                    public List<Cookie> loadForRequest(HttpUrl url) {
                        List<Cookie> cookies = cookieStore.get(url.host());
                        return cookies != null ? cookies : new ArrayList<>();
                    }
                })
                .build();
    }

    public static synchronized LinovelibAPI getInstance() {
        if (instance == null) {
            instance = new LinovelibAPI();
        }
        return instance;
    }

    /**
     * 獲取首頁 HTML
     */
    public String fetchHomePage() throws IOException {
        return fetchUrl(BASE_URL + "/");
    }

    /**
     * 獲取小說詳情頁 HTML
     */
    public String fetchNovelDetail(String novelId) throws IOException {
        return fetchUrl(BASE_URL + "/novel/" + novelId + ".html");
    }

    /**
     * 獲取章節目錄 HTML
     */
    public String fetchCatalog(String novelId) throws IOException {
        return fetchUrl(BASE_URL + "/novel/" + novelId + "/catalog");
    }

    /**
     * 獲取章節內容 HTML (處理分頁)
     */
    public String fetchChapterContent(String chapterUrl) throws IOException {
        // 規範化 URL
        String currentUrl;
        if (chapterUrl.startsWith("http")) {
            currentUrl = chapterUrl;
        } else {
            currentUrl = BASE_URL + chapterUrl;
        }

        StringBuilder fullHtml = new StringBuilder();
        String firstPageHtml = fetchUrl(currentUrl);
        fullHtml.append(firstPageHtml);
        
        // 處理分頁邏輯
        // 從第一頁提取下一頁 URL
        // 我們需要調用 Parser 的靜態方法來檢查
        // 注意：這裡引入了 com.linovelib.reader.parser.LinovelibParser 的依賴
        // 為避免循環依賴或複雜性，我們假設調用者已處理好，但在這裡處理最方便
        
        String nextPageUrl = com.linovelib.reader.parser.LinovelibParser.getNextPageUrl(firstPageHtml);
        String currentChapterId = extractChapterId(currentUrl);

        int maxPages = 10; // 防止死循環
        int pageCount = 1;

        while (nextPageUrl != null && pageCount < maxPages) {
            // 檢查下一頁是否屬於同一章節 (例如 1234.html -> 1234_2.html)
            String nextChapterId = extractChapterId(nextPageUrl);
            
            // 簡單判斷：如果 ID 包含下劃線，去掉下劃線後應該等於原 ID
            // 或者下一頁 ID 就是原 ID + "_數字"
            boolean isSameChapter = false;
            
            if (nextChapterId.equals(currentChapterId)) {
                 isSameChapter = true; // 不太可能，通常 ID 會變
            } else if (nextChapterId.startsWith(currentChapterId + "_")) {
                isSameChapter = true;
            }
            
            if (!isSameChapter) {
                break; // 下一頁是新章節，停止獲取
            }

            Log.d(TAG, "Fetching next page: " + nextPageUrl);
            String nextHtml = fetchUrl(nextPageUrl);
            
            // 只附加內容部分會比較好，但為了 parser 方便，我們先簡單地拼接
            // 更好的做法是解析出 content div 然後拼接
            // 但 Parser 目前是處理整個 HTML。
            // 既然 Parser 會忽略其他部分只抓 #acontent，
            // 我們可以將多個 HTML 內容合併傳遞嗎？
            // 不行，Jsoup.parse(html) 只會解析出一個 DOM。
            // 
            // 修正策略：我們不拼接 HTML，而是返回一個特殊標記的 HTML，
            // 或者我們修改 API 返回 List<String> ? 
            // 為保持兼容性，我們這裡做一個 HACK:
            // 將後續頁面的內容部分提取出來，追加到第一頁的 HTML 中 (在 </body> 之前)
            // 或者更簡單：讓 Parser 支持解析 "多個 accontent" ?
            // 
            // 讓我們採用 "提取 nextHtml 中的 accontent 內容，追加到 firstPageHtml 的 accontent 內" 的策略
            
            int contentStart = nextHtml.indexOf("<div id=\"acontent\"");
            if (contentStart == -1) contentStart = nextHtml.indexOf("<div class=\"acontent\"");
            
            if (contentStart != -1) {
                int contentEnd = nextHtml.indexOf("</div>", contentStart); // 簡單查找，可能不準確
                // 使用 Jsoup 提取更穩妥，但這裡不想引入 Jsoup 依賴
                // 我們這裡簡單追加整個 HTML，讓 Parser 去處理 "多個 HTML" 是不行的。
                
                // 改用分隔符策略
                fullHtml.append("\n<!-- NEXT_PAGE_SPLIT -->\n");
                fullHtml.append(nextHtml);
            } else {
                // 如果找不到內容，就追加整個 HTML
                fullHtml.append("\n<!-- NEXT_PAGE_SPLIT -->\n");
                fullHtml.append(nextHtml);
            }

            nextPageUrl = com.linovelib.reader.parser.LinovelibParser.getNextPageUrl(nextHtml);
            pageCount++;
        }
        
        return fullHtml.toString();
    }

    private String extractChapterId(String url) {
        try {
            return url.replaceAll(".*novel/\\d+/(\\d+)(_\\d+)?\\.html.*", "$1");
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 搜索小說
     */
    public String searchNovels(String keyword) throws IOException {
        // 這裡需要根據實際網站的搜索URL格式調整
        String url = BASE_URL + "/search.php?keyword=" + keyword;
        return fetchUrl(url);
    }

    /**
     * 通用的 URL 請求方法
     */
    private String fetchUrl(String url) throws IOException {
        Log.d(TAG, "Fetching URL: " + url);

        Request request = new Request.Builder()
                .url(url)
                .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                .addHeader("Sec-Ch-Ua", "\"Not_A Brand\";v=\"8\", \"Chromium\";v=\"120\", \"Google Chrome\";v=\"120\"")
                .addHeader("Sec-Ch-Ua-Mobile", "?1")
                .addHeader("Sec-Ch-Ua-Platform", "\"Android\"")
                .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
                .addHeader("Accept-Language", "zh-TW,zh;q=0.9,en-US;q=0.8,en;q=0.7")
                .addHeader("Referer", BASE_URL + "/")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }

            if (response.body() == null) {
                throw new IOException("Response body is null");
            }

            return response.body().string();
        }
    }
}
