package org.gso.brinder.match.service;

import lombok.extern.slf4j.Slf4j;
import org.gso.brinder.match.model.GeoCoordinates;
import org.gso.brinder.match.model.MatchedUser;
import org.gso.brinder.match.repository.MatchingRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class MatchedUserService {

    private final MatchingRepository matchingRepository ;
    private final MongoTemplate mongoTemplate;
    private final String googleMapsApiKey = "AIzaSyBaiuyRPIv8cQHqg2zDY8sF53JzT3nTh7w";

    public MatchedUserService(MatchingRepository matchingRepository, MongoTemplate mongoTemplate) {
        this.matchingRepository = matchingRepository;
        this.mongoTemplate = mongoTemplate;
    }

    public MatchedUser getById(String idMatch) {
        return matchingRepository.findById(idMatch)
                .orElseThrow(() -> new IllegalStateException("User Match Profile not found with id: " + idMatch));
    }

    public MatchedUser createLocationProfile(String userId, String firstName, String lastName, String address) {
        GeoJsonPoint location = addressToCoordinates(address); // Convert address to GeoJsonPoint
        if (location == null) {
            throw new IllegalStateException("Could not convert address to coordinates");
        }
        MatchedUser userMatchProfile = MatchedUser.builder()
                .idMatchedUser(userId)
                .firstName(firstName)
                .lastName(lastName)
                .location(location) // Use the converted location
                .build();
        return matchingRepository.save(userMatchProfile); // Save the new user profile
    }

    public List<MatchedUser> findLocationsProfile(Pageable pageable) {
        log.info("Fetching all User Match Profiles with pagination");
        return matchingRepository.findAll(pageable).getContent();
    }

    public List<MatchedUser> findByDistance(String userId) {
        MatchedUser matchedUser = getById(userId);
        GeoJsonPoint location = matchedUser.getLocation();
        Distance distance = new Distance(0.1, Metrics.KILOMETERS); // 100 meters

        Query query = new Query(Criteria.where("location").nearSphere(location).maxDistance(distance.getNormalizedValue()));
        return mongoTemplate.find(query, MatchedUser.class);
    }
    public MatchedUser updateProfileMatch(MatchedUser userMatchProfile) {
        log.info("Updating User Match Profile with id: {}", userMatchProfile.getIdMatchedUser());
        return matchingRepository.save(userMatchProfile);
    }

    // Additional methods for Google Maps integration
    public GeoJsonPoint addressToCoordinates(String address) {
        try {
            String url = "https://maps.googleapis.com/maps/api/geocode/json?key=" + googleMapsApiKey +
                    "&address=" + java.net.URLEncoder.encode(address, StandardCharsets.UTF_8).replace("+", "%20");
            log.info("Requesting Google Maps for address: {}", url);

            RestTemplate restTemplate = new RestTemplate();
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("results");
            Map<String, Object> location = (Map<String, Object>) ((Map<String, Object>) results.get(0).get("geometry")).get("location");
            double latitude = (Double) location.get("lat");
            double longitude = (Double) location.get("lng");

            return new GeoJsonPoint(longitude, latitude); // Note: GeoJsonPoint expects longitude first
        } catch (Exception e) {
            log.error("Error when requesting Google Maps: ", e);
            return null;
        }
    }
    public MatchedUser updateLocationProfile(String idMatch, String address) {
        GeoJsonPoint coordinates = addressToCoordinates(address);
        if (coordinates != null) {
            MatchedUser matchedUser = getById(idMatch);
            matchedUser.setLocation(coordinates); // Update the location with GeoJsonPoint
            return matchingRepository.save(matchedUser);
        }
        throw new IllegalStateException("Could not update location for profile with id: " + idMatch);
    }

}
