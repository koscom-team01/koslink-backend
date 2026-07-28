package com.koslink.corpus.repository;

import com.koslink.corpus.entity.NewsDuplicates;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * NewsDuplicates Repository
 * 중복 판정된 기사 기록
 */
@Repository
public interface NewsDuplicatesRepository extends JpaRepository<NewsDuplicates, Long> {
}
