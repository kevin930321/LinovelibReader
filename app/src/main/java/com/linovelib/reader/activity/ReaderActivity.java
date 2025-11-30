package com.linovelib.reader.activity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.linovelib.reader.R;
import com.linovelib.reader.adapter.ChapterAdapter;
import com.linovelib.reader.api.LinovelibAPI;
import com.linovelib.reader.database.ReadingHistoryDao;
import com.linovelib.reader.model.ChapterContent;
import com.linovelib.reader.model.ChapterItem;
import com.linovelib.reader.parser.LinovelibParser;

import java.util.ArrayList;
import java.util.List;

public class ReaderActivity extends AppCompatActivity {
    private static final String TAG = "ReaderActivity";

    private RecyclerView recyclerView;
    private ChapterAdapter adapter;
    private LinearLayout bottomNav;
    private Button btnPrevChapter, btnChapterList, btnNextChapter;
    private ProgressBar progressBar;

    private String novelId;
    private String chapterUrl;
    private String chapterTitle;
    private ChapterContent currentContent;
    private ReadingHistoryDao historyDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reader);

        initViews();

        historyDao = new ReadingHistoryDao(this);

        novelId = getIntent().getStringExtra("novel_id");
        chapterUrl = getIntent().getStringExtra("chapter_url");
        chapterTitle = getIntent().getStringExtra("chapter_title");

        loadChapter(chapterUrl);

        btnPrevChapter.setOnClickListener(v -> {
            if (currentContent != null && currentContent.getPrevChapterUrl() != null) {
                loadChapter(currentContent.getPrevChapterUrl());
            } else {
                Toast.makeText(this, "已經是第一章", Toast.LENGTH_SHORT).show();
            }
        });

        btnNextChapter.setOnClickListener(v -> {
            if (currentContent != null && currentContent.getNextChapterUrl() != null) {
                loadChapter(currentContent.getNextChapterUrl());
            } else {
                Toast.makeText(this, "已經是最後一章", Toast.LENGTH_SHORT).show();
            }
        });

        btnChapterList.setOnClickListener(v -> finish());
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChapterAdapter();
        adapter.setOnItemClickListener(this::toggleNavigation);
        recyclerView.setAdapter(adapter);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (Math.abs(dy) > 20 && bottomNav.getVisibility() == View.VISIBLE) {
                    bottomNav.setVisibility(View.GONE);
                }
            }
        });

        bottomNav = findViewById(R.id.bottomNav);
        btnPrevChapter = findViewById(R.id.btnPrevChapter);
        btnChapterList = findViewById(R.id.btnChapterList);
        btnNextChapter = findViewById(R.id.btnNextChapter);
        progressBar = findViewById(R.id.progressBar);
    }

    private void loadChapter(String url) {
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        bottomNav.setVisibility(View.GONE);

        new Thread(() -> {
            try {
                String html = LinovelibAPI.getInstance().fetchChapterContent(url);
                ChapterContent content = LinovelibParser.parseChapterContent(html);

                runOnUiThread(() -> {
                    currentContent = content;
                    chapterUrl = url;
                    displayContent(content);
                    progressBar.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);

                    // Save reading progress
                    if (novelId != null && chapterTitle != null) {
                        saveReadingProgress();
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error loading chapter", e);
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "載入失敗：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void displayContent(ChapterContent content) {
        List<ChapterItem> items = new ArrayList<>();
        
        // Add Title
        String title = content.getTitle() != null ? content.getTitle() : chapterTitle;
        items.add(new ChapterItem(ChapterItem.TYPE_TITLE, title));
        
        // Add content items
        if (content.getItems() != null && !content.getItems().isEmpty()) {
            items.addAll(content.getItems());
        } else if (content.getContent() != null) {
             // Fallback
             items.add(new ChapterItem(ChapterItem.TYPE_TEXT, content.getContent()));
        }
        
        adapter.setItems(items);

        // Scroll to top or restore position if implementing precise restore logic
        recyclerView.scrollToPosition(0);
        
        // Note: Restoration from historyDao requires logic here if we want to support it on restart
        // But user didn't ask for persistent position fix, just image display.
    }

    private void toggleNavigation() {
        if (bottomNav.getVisibility() == View.VISIBLE) {
            bottomNav.setVisibility(View.GONE);
        } else {
            bottomNav.setVisibility(View.VISIBLE);
        }
    }

    private void saveReadingProgress() {
        if (recyclerView.getLayoutManager() instanceof LinearLayoutManager) {
            int position = ((LinearLayoutManager) recyclerView.getLayoutManager()).findFirstVisibleItemPosition();
            new Thread(() -> {
                historyDao.saveReadingProgress(novelId, chapterUrl, chapterTitle, position);
            }).start();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (novelId != null && chapterTitle != null) {
            saveReadingProgress();
        }
    }
}