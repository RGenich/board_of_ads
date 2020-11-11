package com.board_of_ads.repository;

import com.board_of_ads.models.Region;
import com.board_of_ads.models.dto.analytics.ReportRegionPostingDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Repository
public interface RegionRepository extends JpaRepository<Region, Long> {

    Region findRegionByRegionNumber(String regionNumber);

    boolean existsRegionByName(String name);

    Region findRegionByName(String name);


    @Query("select new com.board_of_ads.models.dto.analytics.ReportRegionPostingDto(" +
            "r.name, count (r.name), sum (case when p.isActive = true then 1 else 0 end), sum (case when p.isActive = true then 0 else 1 end)" +
            ")" +
            " from Region r, Posting p where p.city.region = r AND p.datePosting BETWEEN :startDate and :endDate GROUP BY r.name")
    List<ReportRegionPostingDto> findAllByDatePostingBetween(LocalDateTime startDate, LocalDateTime endDate);
}