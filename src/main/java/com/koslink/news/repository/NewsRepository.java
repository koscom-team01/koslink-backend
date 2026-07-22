package com.koslink.news.repository;

import com.koslink.news.entity.News;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface NewsRepository extends JpaRepository<News, Long> {

    @Query("SELECT n.url FROM News n WHERE n.url IN :urls")
    Set<String> findExistingUrls(@Param("urls") List<String> urls);
}
