package com.board_of_ads.service.impl;

import com.board_of_ads.models.City;
import com.board_of_ads.models.User;
import com.board_of_ads.models.dto.PostingCarDto;
import com.board_of_ads.models.dto.PostingDto;
import com.board_of_ads.models.dto.analytics.ReportUserPostingDto;
import com.board_of_ads.models.posting.Posting;
import com.board_of_ads.models.posting.autoTransport.cars.PostingCar;
import com.board_of_ads.repository.CityRepository;
import com.board_of_ads.repository.PostingCarRepository;
import com.board_of_ads.repository.PostingRepository;
import com.board_of_ads.service.interfaces.CategoryService;
import com.board_of_ads.service.interfaces.PostingService;
import com.board_of_ads.service.interfaces.RegionService;
import com.board_of_ads.service.interfaces.UserService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Transactional
@Slf4j
public class PostingServiceImpl implements PostingService {

    private final UserService userService;
    private final PostingRepository postingRepository;
    private final CategoryService categoryService;
    private final RegionService regionService;
    private final CityRepository cityRepository;
    private final PostingCarRepository postingCarRepository;

    @Override
    public void save(Posting posting) {
        postingRepository.save(posting);
    }

    @Override
    public Posting getPostingById(Long id) {
        return postingRepository.getOne(id);
    }

    @Override
    public Optional<Posting> getPostingByTitle(String title) {
        return Optional.ofNullable(postingRepository.findPostingByTitle(title));
    }

    @Override
    public PostingDto getPostingDtoById(Long id) {
        postingRepository.addViewNumber(id);
        PostingDto postingDto = postingRepository.getPostingDtoById(id);
        postingDto.setImages(getPostingById(postingDto.getId()).getImages());
        postingDto.setCategory(categoryService.getCategoryDtoById(
                getPostingById(postingDto.getId()).getCategory().getId()).get());
        if(getPostingById(postingDto.getId()).getCity() != null) {
            postingDto.setCity(getPostingById(postingDto.getId()).getCity().getName());
        }
        return postingDto;
    }

    @Override
    public List<PostingDto> getPostingByCity(City city) {
        List<PostingDto> result = postingRepository.findPostingByCity(city);
        return getPostingDtos(result);
    }

    @Override
    public List<PostingDto> getPostingByFullRegionName(String name) {
        List<PostingDto> result = new ArrayList<>();
        var cities = cityRepository.findCitiesByRegion(
                regionService.findRegionByNameAndFormSubject(name).get());
        cities.forEach(city -> result.addAll(postingRepository.findPostingByCity(city)));
        return getPostingDtos(result);
    }

    @Override
    public List<PostingDto> getAllPostings() {
        List<PostingDto> postingDtos = postingRepository.findAllPostings();
        return getPostingDtos(postingDtos);
    }

    @Override
    public List<PostingDto> getAllUserPostings(Long user_id) {
        List<PostingDto> userPosts = postingRepository.findAllUserPostings(user_id);
        return getPostingDtos(userPosts);
    }

    private List<PostingDto> getPostingDtos(List<PostingDto> postingDtos) {
        for(PostingDto dto : postingDtos) {
           dto.setImages(getPostingById(dto.getId()).getImages());
           dto.setCategory(categoryService.getCategoryDtoById(
                   postingRepository.findPostingByTitle(dto.getTitle()).getCategory().getId()).get());
           if(getPostingById(dto.getId()).getCity() != null) {
               dto.setCity(getPostingById(dto.getId()).getCity().getName());
           }
        }
        return postingDtos;
    }

    @Override
    public List<PostingDto> searchPostings(String categorySelect, String citySelect, String searchText, String photoOption) {

        List<PostingDto> postingDtos;
        if(citySelect != null && !(citySelect.equals("undefined"))) {
            if (citySelect.matches("(.*)" +"Область" + "(.*)")
                    || citySelect.matches("(.*)" + "Край" + "(.*)")
                    || citySelect.matches("(.*)" + "Республика" + "(.*)")
                    || citySelect.matches("(.*)" + "Автономный округ" + "(.*)")
                    || citySelect.matches("(.*)" + "Город" + "(.*)")
            ) {
                postingDtos = getPostingByFullRegionName(citySelect);
            } else {
                postingDtos = getPostingByCity(cityRepository.findCitiesByName(citySelect).get());
            }
        } else {
            postingDtos = getAllPostings();
        }

        List<PostingDto> resultList = new ArrayList<>();

        for (PostingDto postingDto : postingDtos) {

            boolean categoryFlag = false;
            boolean photoFlag = false;
            boolean textFlag = false;

            if (categorySelect.equals("Любая категория")) {
                categoryFlag = true;
            } else if (postingDto.getCategory().equals(categorySelect)) {
                categoryFlag = true;
            }
            if(photoOption != null) {
                if(photoOption.equals("пункт2")) {
                    if(postingDto.getImages().size() > 0) {
                        photoFlag = true;
                    }
                } else if(photoOption.equals("пункт3")) {
                    if(postingDto.getImages().size() == 0) {
                        photoFlag = true;
                    }
                } else if(photoOption.equals("пункт1")) {
                    photoFlag = true;
                }
            } else {
                photoFlag = true;
            }
            if(searchText != null && !(searchText.equals("")) && !(searchText.equals("undefined"))) {
                if(postingDto.getTitle().toLowerCase().matches("(.*)" + searchText.toLowerCase() + "(.*)")) {
                    textFlag = true;
                }
            } else {
                textFlag = true;
            }

            if(categoryFlag && photoFlag && textFlag) {
                resultList.add(postingDto);
            }
        }

        return resultList;
    }

    @Override
    public List<ReportUserPostingDto> getPostBetweenDates(String date) {
        List<LocalDateTime> localDateTimes = dateConvertation(date);
        return postingRepository.findAllByDatePostingBetween(localDateTimes.get(0), localDateTimes.get(1));
    }

    private List<LocalDateTime> dateConvertation(String date) {

        String[] arr = date.split("\\D+");

        List<Integer> dateValues = Arrays.stream(arr)
                .filter(a -> !a.equals(""))
                .map(Integer::parseInt)
                .collect(Collectors.toList());

        LocalDateTime startDateTime = LocalDateTime.of(dateValues.get(2), dateValues.get(1), dateValues.get(0), 0, 0);
        LocalDateTime endDateTime = LocalDateTime.of(dateValues.get(5), dateValues.get(4), dateValues.get(3), 23, 59);

        List<LocalDateTime> localDateTimeList = new ArrayList<>();
        localDateTimeList.add(startDateTime);
        localDateTimeList.add(endDateTime);

        return localDateTimeList;
    }

    @Override
    public void saveNewPostingCar(PostingCar postingCar) {
        postingCarRepository.save(postingCar);
    }

    @Override
    public List<PostingDto> getFavDtosFromUser(User user) {
        List<Long> listfavoritsid = new ArrayList<>();
        user.getFavorites().forEach(x ->listfavoritsid.add(x.getId()));
        return postingRepository.findUserFavorites(listfavoritsid);
    }

    @Override
    public List<Long> getFavIDFromUser(User user) {
        List<Long> listfavoritsid = new ArrayList<>();
        user.getFavorites().forEach(x ->listfavoritsid.add(x.getId()));
        return listfavoritsid;
    }

    @Override
    public PostingCarDto getNewPostingCarDto(Long userId, String isCarNew) {
        User user = userService.getUserById(userId);
        PostingCar pc = new PostingCar();
        pc.setUser(user);
        pc.setSellerId(userId);
        pc.setCarNew(!isCarNew.equals("used-car"));

        return new PostingCarDto(pc);
    }

}