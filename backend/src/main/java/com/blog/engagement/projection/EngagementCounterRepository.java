package com.blog.engagement.projection;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EngagementCounterRepository extends JpaRepository<EngagementCounter, Long> {

    List<EngagementCounter> findAllByOrderByPostIdAsc();
}
