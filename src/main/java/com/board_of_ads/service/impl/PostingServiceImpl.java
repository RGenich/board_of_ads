package com.board_of_ads.service.impl;

import com.board_of_ads.models.City;
import com.board_of_ads.models.Image;
import com.board_of_ads.models.User;
import com.board_of_ads.models.dto.PostingCarDto;
import com.board_of_ads.models.dto.PostingDto;
import com.board_of_ads.models.dto.analytics.ReportUserPostingDto;
import com.board_of_ads.models.posting.Posting;
import com.board_of_ads.models.posting.forBusiness.Business;
import com.board_of_ads.models.posting.personalBelongings.Clothes;
import com.board_of_ads.models.posting.autoTransport.cars.PostingCar;
import com.board_of_ads.models.posting.job.Vacancy;
import com.board_of_ads.repository.CityRepository;
import com.board_of_ads.repository.PostingCarRepository;
import com.board_of_ads.repository.PostingRepository;
import com.board_of_ads.service.interfaces.CategoryService;
import com.board_of_ads.service.interfaces.ImageService;
import com.board_of_ads.service.interfaces.PostingService;
import com.board_of_ads.service.interfaces.RegionService;
import com.board_of_ads.service.interfaces.UserService;
import com.board_of_ads.util.Error;
import com.board_of_ads.util.ErrorResponse;
import com.board_of_ads.util.Response;
import com.board_of_ads.service.interfaces.UserService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
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
    private final ImageService imageService;
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
        if (getPostingById(postingDto.getId()).getCity() != null) {
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
        for (PostingDto dto : postingDtos) {
            dto.setImages(getPostingById(dto.getId()).getImages());
            dto.setCategory(categoryService.getCategoryDtoById(
                    postingRepository.findPostingByTitle(dto.getTitle()).getCategory().getId()).get());
            if (getPostingById(dto.getId()).getCity() != null) {
                dto.setCity(getPostingById(dto.getId()).getCity().getName());
            }
        }
        return postingDtos;
    }

    @Override
    public List<PostingDto> searchPostings(String categorySelect, String citySelect, String searchText, String photoOption) {

        List<PostingDto> postingDtos;
        if (citySelect != null && !(citySelect.equals("undefined"))) {
            if (citySelect.matches("(.*)" + "Область" + "(.*)")
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
            if (photoOption != null) {
                if (photoOption.equals("пункт2")) {
                    if (postingDto.getImages().size() > 0) {
                        photoFlag = true;
                    }
                } else if (photoOption.equals("пункт3")) {
                    if (postingDto.getImages().size() == 0) {
                        photoFlag = true;
                    }
                } else if (photoOption.equals("пункт1")) {
                    photoFlag = true;
                }
            } else {
                photoFlag = true;
            }
            if (searchText != null && !(searchText.equals("")) && !(searchText.equals("undefined"))) {
                if (postingDto.getTitle().toLowerCase().matches("(.*)" + searchText.toLowerCase() + "(.*)")) {
                    textFlag = true;
                }
            } else {
                textFlag = true;
            }

            if (categoryFlag && photoFlag && textFlag) {
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
    public List<PostingDto> getFavDtosFromUser(User user) {
        List<Long> listfavoritsid = new ArrayList<>();
        user.getFavorites().forEach(x -> listfavoritsid.add(x.getId()));
        return postingRepository.findUserFavorites(listfavoritsid);
    }

    @Override
    public List<Long> getFavIDFromUser(User user) {
        List<Long> listfavoritsid = new ArrayList<>();
        user.getFavorites().forEach(x -> listfavoritsid.add(x.getId()));
        return listfavoritsid;
    }

    public Response<Void> savePersonalClothesPosting(Long id, User user, Map<String,
            String> map, List<MultipartFile> photos) {

        Clothes posting;
        try {

            posting = new Clothes(userService.getUserById(user.getId()), categoryService.getCategoryById(id),
                    map.get("title"), map.get("description"), Long.parseLong(map.get("price")), map.get("contact"),
                    true, map.get("contactEmail"), map.get("linkYouTube"), map.get("communicationType"), map.get("state"), map.get("typeAd"), map.get("size"));

            List<Image> images = new ArrayList<>();
            String time = new SimpleDateFormat("yyyy'-'MM'-'dd'_'HHmmss'_'").format(new Date());
            try {
                for (int i = 0; i < photos.size(); i++) {
                    if (!photos.get(i).isEmpty()) {
                        byte[] bytes = photos.get(i).getBytes();
                        File dir = new File("uploaded_files/userID_" + user.getId());
                        dir.mkdirs();
                        File file = new File(dir, time + photos.get(i).getOriginalFilename());
                        BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(file));

                        stream.write(bytes);
                        stream.close();
                        Image image = new Image(dir.toString() + file.toString());
                        imageService.save(image);
                        images.add(imageService.getByPathURL(dir.toString() + file.toString()));
                        log.info("Файл '" + time + photos.get(i).getOriginalFilename() + "' успешно загружен.");
                    } else {
                        log.info("Вам не удалось загрузить файл, потому что он пустой.");
                    }
                }
            } catch (Exception ex) {
                log.info("Вам не удалось загрузить фотографии => " + ex.getMessage());
                return new ErrorResponse<>(new Error(400, "Posting is not created"));
            }
            posting.setImages(images);
            postingRepository.save(posting);
            log.info("Объявление успешно создано пользователем " + user.getEmail());
            return Response.ok().build();
        } catch (Exception ex) {
            log.info("Не удалось создать объявление => " + ex.getMessage());
            return new ErrorResponse<>(new Error(400, "Posting is not created"));
        }
    }

    @Override
    public PostingCarDto getNewPostingCarDto(Long userId, String isCarNew) {
        User user = userService.getUserById(userId);
        PostingCar pc = new PostingCar();
        pc.setUser(user);
        pc.setSellerId(userId);
        pc.setCarIsNew(!isCarNew.equals("used-car"));

        return new PostingCarDto(pc);
    }

    @Override
    public PostingCar convertJsonToPostingCar(JSONObject json) throws JSONException {
        PostingCar pc = new PostingCar();

        pc.setVinCode(json.getString("vinCode"));
        pc.setCarIsNew(json.getBoolean("carIsNew"));
        pc.setTypeOfUsedCarPosting(json.getString("typeOfUsedCarPosting"));
        pc.setStatePlateNumber(json.getString("statePlateNumber"));
        pc.setMileage(json.getInt("mileage"));
        pc.setNumberOfOwners((byte) json.getInt("numberOfOwners"));
        pc.setModelIdInAutoCatalogue(0); //
        pc.setCarColor(json.getString("carColor"));
        pc.setCarBrand(json.getString("carBrand"));
        pc.setCarModel(json.getString("carModel"));
        pc.setCarYear((short) json.getInt("carYear"));
        pc.setCarBodyType(json.getString("carBodyType"));
        pc.setNumberOfDoors((byte) json.getInt("numberOfDoors"));
        pc.setWasInAccident(json.getBoolean("wasInAccident"));
        pc.setDealerServiced(json.getBoolean("dealerServiced"));
        pc.setUnderWarranty(json.getBoolean("underWarranty"));
        pc.setHasServiceBook(json.getBoolean("hasServiceBook"));
        pc.setPowerSteeringType(json.getString("powerSteeringType"));
        pc.setClimateControlType(json.getString("climateControlType"));
        pc.setClimateControlType(json.getString("climateControlType"));
        pc.setOnWheelControl(json.getBoolean("onWheelControl"));
        pc.setThermalGlass(json.getBoolean("thermalGlass"));
        pc.setInteriorType(json.getString("interiorType"));
        pc.setLeatherWheel(json.getBoolean("leatherWheel"));
        pc.setSunroof(json.getBoolean("sunroof"));
        pc.setHeatedFrontSeats(json.getBoolean("heatedFrontSeats"));
        pc.setHeatedRearSeats(json.getBoolean("heatedRearSeats"));
        pc.setHeatedMirrors(json.getBoolean("heatedMirrors"));
        pc.setHeatedRearWindow(json.getBoolean("heatedRearWindow"));
        pc.setHeatedWheel(json.getBoolean("heatedWheel"));
        pc.setPowerWindowsType(json.getString("powerWindowsType"));
        pc.setPowerFrontSeats(json.getBoolean("powerFrontSeats"));
        pc.setPowerRearSeats(json.getBoolean("powerRearSeats"));
        pc.setPowerMirrorRegulation(json.getBoolean("powerMirrorRegulation"));
        pc.setPowerSteeringWheelRegulation(json.getBoolean("powerSteeringWheelRegulation"));
        pc.setPowerMirrorClose(json.getBoolean("powerMirrorClose"));
        pc.setFrontSeatsMemory(json.getBoolean("frontSeatsMemory"));
        pc.setRearSeatsMemory(json.getBoolean("rearSeatsMemory"));
        pc.setMirrorRegulationMemory(json.getBoolean("mirrorRegulationMemory"));
        pc.setSteeringWheelRegulationMemory(json.getBoolean("steeringWheelRegulationMemory"));
        pc.setParkingAssist(json.getBoolean("parkingAssist"));
        pc.setRainSensor(json.getBoolean("rainSensor"));
        pc.setLightSensor(json.getBoolean("lightSensor"));
        pc.setRearParkingSensor(json.getBoolean("rearParkingSensor"));
        pc.setFrontParkingSensor(json.getBoolean("frontParkingSensor"));
        pc.setBlindSpotZoneControl(json.getBoolean("blindSpotZoneControl"));
        pc.setRearCamera(json.getBoolean("rearCamera"));
        pc.setCruiseControl(json.getBoolean("cruiseControl"));
        pc.setOnBoardComp(json.getBoolean("onBoardComp"));
        pc.setAlarmSystem(json.getBoolean("alarmSystem"));
        pc.setPowerDoorBlocking(json.getBoolean("powerDoorBlocking"));
        pc.setImmobilizer(json.getBoolean("immobilizer"));
        pc.setSatelliteAlarmControl(json.getBoolean("satelliteAlarmControl"));
        pc.setFrontalAirbags(json.getBoolean("frontalAirbags"));
        pc.setKneeAirbags(json.getBoolean("kneeAirbags"));
        pc.setSideWindowAirbags(json.getBoolean("sideWindowAirbags"));
        pc.setFrontSideAirbags(json.getBoolean("frontSideAirbags"));
        pc.setRearSideAirbags(json.getBoolean("rearSideAirbags"));
        pc.setAbsSystem(json.getBoolean("absSystem"));
        pc.setDtcSystem(json.getBoolean("dtcSystem"));
        pc.setTrackingControl(json.getBoolean("trackingControl"));
        pc.setBreakAssistSystem(json.getBoolean("breakAssistSystem"));
        pc.setEmergencyBreakSystem(json.getBoolean("emergencyBreakSystem"));
        pc.setDiffLockSystem(json.getBoolean("diffLockSystem"));
        pc.setPedestrianDetectSystem(json.getBoolean("pedestrianDetectSystem"));
        pc.setCdDvdBluRay(json.getBoolean("cdDvdBluRay"));
        pc.setMp3(json.getBoolean("mp3"));
        pc.setRadio(json.getBoolean("radio"));
        pc.setTvSystem(json.getBoolean("tvSystem"));
        pc.setVideoSystem(json.getBoolean("videoSystem"));
        pc.setMediaOnWheelControl(json.getBoolean("mediaOnWheelControl"));
        pc.setUsb(json.getBoolean("usb"));
        pc.setAux(json.getBoolean("aux"));
        pc.setBluetooth(json.getBoolean("bluetooth"));
        pc.setGpsNavigation(json.getBoolean("gpsNavigation"));
        pc.setAudioSystemType(json.getString("audioSystemType"));
        pc.setSubwoofer(json.getBoolean("subwoofer"));
        pc.setFrontLightType(json.getString("frontLightType"));
        pc.setAntifogLights(json.getBoolean("antifogLights"));
        pc.setFrontLightCleaning(json.getBoolean("frontLightCleaning"));
        pc.setAdaptiveLights(json.getBoolean("adaptiveLights"));
        pc.setHowToContactVsSeller(json.getString("howToContactVsSeller"));
        pc.setTyreSize(json.getString("winterTyreSetIncluded"));
        pc.setWinterTyreSetIncluded(json.getBoolean("winterTyreSetIncluded"));
        pc.setTypeOfEngine("Hybrid");
        pc.setWheelDrive("4x4");
        pc.setTransmission("Automatic");
        pc.setModification("Lux");
        pc.setConfiguration(json.getString("configuration"));
        pc.setTitle(json.getString("title"));
        pc.setDescription(json.getString("description"));
        pc.setContact(json.getString("contact"));
        pc.setMeetingAddress(json.getString("meetingAddress"));
        pc.setDatePosting(LocalDateTime.now());
        pc.setCondition(json.getString("condition"));
        pc.setVideoURL(json.getString("videoURL"));
        pc.setContactEmail(json.getString("contactEmail"));
        //  pc.setMessage(json.getString("message"));
        pc.setPrice(json.getLong("price"));
        pc.setIsActive(json.getBoolean("isActive"));
        // pc.setViewNumber(json.getInt("viewNumber"));
        pc.setViewNumber(1);
        long catId = json.getInt("categoryId");
        pc.setCategory(categoryService.getCategoryById(catId));
        return pc;
    }

    @Override
    public void setVacancyCondition(Map<String, String> form, List<String> preferences, User userById,
                                    Vacancy posting, City city, List<Image> images) {
        StringBuilder options = new StringBuilder();
        preferences.forEach(a -> options.append(a).append("/"));

        posting.setUser(userById);
        posting.setCategory(categoryService.getCategoryById(Long.valueOf(form.get("categoryId"))));
        posting.setCity(city);
        posting.setContact(userById.getEmail());
        posting.setDatePosting(LocalDateTime.now());
        posting.setDescription(form.get("description"));
        posting.setTitle(form.get("title"));
        posting.setIsActive(true);
        posting.setSchedule(form.get("schedule"));
        posting.setDuties(form.get("duties"));
        posting.setExperienceValue(form.get("workExperience"));
        posting.setLocation(form.get("location"));
        posting.setPreferences(options.toString());
        posting.setPrice(Long.valueOf(form.get("price")));
        posting.setImages(images);
    }

    @Override
    public Response<Void> saveForBusinessPosting(Long id, User user, Map<String,
            String> map, List<MultipartFile> photos) {

        Business posting;
        try {

            posting = new Business(userService.getUserById(user.getId()), categoryService.getCategoryById(id),
                    map.get("title"), map.get("description"), Long.parseLong(map.get("price")), map.get("contact"),
                    true, map.get("contactEmail"), map.get("linkYouTube"), map.get("communicationType"), map.get("state"));

            List<Image> images = new ArrayList<>();
            String time = new SimpleDateFormat("yyyy'-'MM'-'dd'_'HHmmss'_'").format(new Date());
            try {
                for (int i = 0; i < photos.size(); i++) {
                    if (!photos.get(i).isEmpty()) {
                        byte[] bytes = photos.get(i).getBytes();
                        File dir = new File("uploaded_files/userID_" + user.getId());
                        dir.mkdirs();
                        File file = new File(dir, time + photos.get(i).getOriginalFilename());
                        BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(file));

                        stream.write(bytes);
                        stream.close();
                        Image image = new Image(dir.toString() + file.toString());
                        imageService.save(image);
                        images.add(imageService.getByPathURL(dir.toString() + file.toString()));
                        log.info("Файл '" + time + photos.get(i).getOriginalFilename() + "' успешно загружен.");
                    } else {
                        log.info("Вам не удалось загрузить файл, потому что он пустой.");
                    }
                }
            } catch (Exception ex) {
                log.info("Вам не удалось загрузить фотографии => " + ex.getMessage());
                return new ErrorResponse<>(new Error(400, "Posting is not created"));
            }
            posting.setImages(images);
            postingRepository.save(posting);
            log.info("Объявление успешно создано пользователем " + user.getEmail());
            return Response.ok().build();
        } catch (Exception ex) {
            log.info("Не удалось создать объявление => " + ex.getMessage());
            return new ErrorResponse<>(new Error(400, "Posting is not created"));
        }
    }
}