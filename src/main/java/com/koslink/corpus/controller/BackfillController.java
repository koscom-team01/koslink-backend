package com.koslink.corpus.controller;

import com.koslink.corpus.dto.BackfillResult;
import com.koslink.corpus.service.NewsCorpusBackfillService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

/**
 * 뉴스 코퍼스 백필 컨트롤러
 * RAG 학습용 과거 뉴스 데이터 수집 API
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/admin/backfill")
public class BackfillController {

    private final NewsCorpusBackfillService backfillService;

    /**
     * 뉴스 코퍼스 백필 실행
     * 비동기로 처리되며, 즉시 응답 반환
     *
     * @return 백필 결과
     */
    @PostMapping("/corpus")
    @Async
    public CompletableFuture<BackfillResult> backfillCorpus() {
        log.info("Backfill corpus requested");
        return CompletableFuture.completedFuture(backfillService.backfillAll());
    }
}
