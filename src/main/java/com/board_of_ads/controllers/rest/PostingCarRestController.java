package com.board_of_ads.controllers.rest;

import com.board_of_ads.models.User;
import com.board_of_ads.models.dto.PostingCarDto;
import com.board_of_ads.models.dto.PostingDto;
import com.board_of_ads.models.posting.autoTransport.cars.PostingCar;
import com.board_of_ads.service.interfaces.AutoAttributesService;
import com.board_of_ads.service.interfaces.PostingService;
import com.board_of_ads.util.Error;
import com.board_of_ads.util.ErrorResponse;
import com.board_of_ads.util.Response;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@RequestMapping("/api/posting")
@AllArgsConstructor
@Slf4j
public class PostingCarRestController {
    @Autowired
    private final PostingService postingService;
    @Autowired
    private final AutoAttributesService autoAttributesService;

    @GetMapping("/car/{isCarNew}")
    public Response<PostingCarDto> getPostingCarDtoMap(@AuthenticationPrincipal User user, @PathVariable String isCarNew) {
        log.info("In Get PostingCarDTO Controller");
        PostingCarDto postingCarDto = postingService.getNewPostingCarDto(user.getId(), isCarNew);
        return  Response.ok(postingCarDto);
    }

    @GetMapping("/car/colors")
    public Response<Set<String>> getCarColorsSet() {
        log.info("In Get Set of Colors Controller");
        return  Response.ok(autoAttributesService.getAllAutoColorsRusNames());
    }

    @GetMapping("/car/brands")
    public Response<Set<String>> getCarBrandsSet() {
        log.info("In Get Set of getCarBrands Controller");
        return  Response.ok(autoAttributesService.getBrandsSet());
    }

    @GetMapping("/car/models/{brand}")
    public Response<Set<String>> getCarBrandsSet(@PathVariable String brand) {
        log.info("In Get Set of getCar Models Controller brand = {}", brand);
        return  Response.ok(autoAttributesService.getModelsSet(brand));
    }

    @GetMapping("/car/models/{brand}/{model}")
    public Response<Set<Short>> getYearsByBrandAndModel(@PathVariable String brand, @PathVariable String model) {
        log.info("In Get Set of getCar Models Controller brand = {} model = {}", brand, model);
        return  Response.ok(autoAttributesService.getYearsByBrandAndModel(brand, model));
    }

    @PostMapping("/car/new-save")
    public Response<Void> createUsedCarPostingDto(@RequestBody JSONObject json) {
        log.info("In POST createUsedCarPosting Controller");
        try {
            PostingCar postingCar = postingService.convertJsonToPostingCar(json);
            postingService.save(postingCar);
            System.out.println("Posting  Saved!");
            return Response.ok().build();
        } catch (Exception e){
            System.out.println("Unable to save Posting : " + e.getMessage());
            return new ErrorResponse<>(new Error(204, "Error of saving post"));
        }
    }
}
