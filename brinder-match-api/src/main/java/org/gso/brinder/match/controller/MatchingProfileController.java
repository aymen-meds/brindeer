package org.gso.brinder.match.controller;

import org.gso.brinder.match.model.MatchedUser;
import org.gso.brinder.match.service.MatchedUserService;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/match")
public class MatchingProfileController {

    private final MatchedUserService matchedUserService;

    public MatchingProfileController(MatchedUserService matchedUserService) {
        this.matchedUserService = matchedUserService;
    }

    // Adjusted to use address in request body for creating a location profile
    @PostMapping("/create")
    public ResponseEntity<MatchedUser> createLocationProfile(@RequestBody String address, @AuthenticationPrincipal JwtAuthenticationToken principal) {
        String userId = principal.getToken().getClaimAsString("sub");
        String firstName = principal.getToken().getClaimAsString("given_name");
        String lastName = principal.getToken().getClaimAsString("family_name");

        // Convert address to GeoJsonPoint within the service method
        MatchedUser savedProfile = matchedUserService.createLocationProfile(userId, firstName, lastName, address);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(savedProfile.getIdMatchedUser()).toUri();
        return ResponseEntity.created(location).body(savedProfile);
    }

    @GetMapping
    public ResponseEntity<List<MatchedUser>> getAllUserMatchProfiles(Pageable pageable) {
        List<MatchedUser> profiles = matchedUserService.findLocationsProfile(pageable);
        return ResponseEntity.ok(profiles);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MatchedUser> getUserMatchProfile(@PathVariable String id) {
        MatchedUser profile = matchedUserService.getById(id);
        return ResponseEntity.ok(profile);
    }

    @GetMapping("/nearby")
    public ResponseEntity<List<MatchedUser>> findNearbyUserMatchProfiles(@AuthenticationPrincipal JwtAuthenticationToken principal) {
        String userId = principal.getToken().getClaimAsString("sub");
        List<MatchedUser> nearbyProfiles = matchedUserService.findByDistance(userId);
        return ResponseEntity.ok(nearbyProfiles);
    }

    @PutMapping("/{id}")
    public ResponseEntity<MatchedUser> updateMatchProfile(@PathVariable String id, @RequestBody String address) {
        MatchedUser updatedProfile = matchedUserService.updateLocationProfile(id, address);
        return ResponseEntity.ok(updatedProfile);
    }
}
