package com.board_of_ads.controllers.rest;

import com.board_of_ads.models.dto.PostingDto;
import com.board_of_ads.models.dto.analytics.ReportUserPostingDto;
import com.board_of_ads.models.posting.Posting;
import com.board_of_ads.models.posting.forHomeAndGarden.HouseholdAppliancesPosting;
import com.board_of_ads.service.interfaces.CityService;
import com.board_of_ads.service.interfaces.PostingService;
import com.board_of_ads.util.Error;
import com.board_of_ads.util.ErrorResponse;
import com.board_of_ads.util.Response;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/posting")
@AllArgsConstructor
@Slf4j
public class PostingRestController {

    private final CityService cityService;
    private final PostingService postingService;

    @GetMapping
    public Response<List<PostingDto>> findAllPosts() {
        log.info("Use this default logger");
        var postings = postingService.getAllPostings();
        return (postings.size() > 0)
                ? Response.ok(postings)
                : new ErrorResponse<>(new Error(204, "No found postings"));
    }
    @GetMapping("/{id}")
    public Response<PostingDto> findPostingDto(@PathVariable Long id) {
        var postingDto = postingService.getPostingDtoById(id);
        return (postingDto != null)
                ? Response.ok(postingDto)
                : new ErrorResponse<>(new Error(204, "No found postings"));
    }

    @GetMapping("/city/{name}")
    public Response<List<PostingDto>> findPostingsByCityName(@PathVariable String name) {
        var postings = postingService.getPostingByCity(cityService.findCityByName(name).get());
        return Response.ok(postings);
    }

    @GetMapping("/region/{name}")
    public Response<List<PostingDto>> findPostingsByRegionName(@PathVariable String name) {
        var postings = postingService.getPostingByFullRegionName(name);
        return Response.ok(postings);
    }

    @GetMapping("/userpost/{id}")
    public Response<List<PostingDto>> findPostingsByUserId(@PathVariable Long id) {
        var postings = postingService.getAllUserPostings(id);
        return (postings.size() > 0)
                ? Response.ok(postings)
                : new ErrorResponse<>(new Error(204, "No found postings"));
    }

    @GetMapping("/search")
    public Response<List<PostingDto>> findAllPostings(@RequestParam(name="catSel") String categorySelect,
                                                      @RequestParam(name="citSel",required = false)String citySelect,
                                                      @RequestParam(name="searchT",required = false) String searchText,
                                                      @RequestParam(name="phOpt",required = false) String photoOption) {
        log.info("Use this default logger");
        var postings = postingService

                .searchPostings(categorySelect, citySelect, searchText, photoOption);
        return (postings != null)
                ? Response.ok(postings)
                : new ErrorResponse<>(new Error(204, "No found postings"));
    }

    @PostMapping("/date")
    public Response<List<ReportUserPostingDto>> findByDate(@RequestBody String date) {
        return Response.ok(postingService.getPostBetweenDates(date));
    }

    @PostMapping("/new")
    public Response<Void> createPosting(@RequestBody Posting posting) {
        //postingService.save(posting);
        return Response.ok().build();
    }

    @PostMapping("/new/householdAppliances/{id}")
    public Response<Void> createHouseholdAppliancesPosting(@RequestParam Long id
                                                           /*@RequestBody HouseholdAppliancesPosting posting*/) {
        //postingService.save(posting);
        return Response.ok().build();
    }
}