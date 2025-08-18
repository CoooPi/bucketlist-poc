package com.bucketlist.api;

import com.bucketlist.domain.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class ProfileController {
    
    private final ProfileService profileService;
    
    @PostMapping
    public ResponseEntity<CreateProfileResponse> createProfile(@Valid @RequestBody CreateProfileRequest request) {
        CreateProfileResponse response = profileService.createProfile(request);
        return ResponseEntity.ok(response);
    }
}