package com.koslink.news.repository;

import com.koslink.news.entity.News;
import com.koslink.news.entity.NewsStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface NewsRepository extends JpaRepository<News, Long> {

    @Query("SELECT n.url FROM News n WHERE n.url IN :urls")
    Set<String> findExistingUrls(@Param("urls") List<String> urls);

    /**
     * status = DONE인 뉴스를 최신순으로 조회
     */
    List<News> findByStatusOrderByPublishedAtDesc(NewsStatus status, Pageable pageable);

    /**
     * status = DONE이고 ID가 cursorId보다 작은 뉴스를 최신순으로 조회
     */
    List<News> findByStatusAndNewsIdLessThanOrderByPublishedAtDesc(
            NewsStatus status,
            Long cursorId,
            Pageable pageable
    );
}
