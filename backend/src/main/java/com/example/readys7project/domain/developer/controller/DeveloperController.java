package com.example.readys7project.domain.developer.controller;

import com.example.readys7project.domain.developer.dto.DeveloperDto;
import com.example.readys7project.domain.developer.dto.request.DeveloperProfileRequest;
import com.example.readys7project.domain.developer.service.DeveloperService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/developers")
@RequiredArgsConstructor
public class DeveloperController {

    private final DeveloperService developerService;

    @GetMapping
    public ResponseEntity<List<DeveloperDto>> getAllDevelopers() {
        return ResponseEntity.ok(developerService.getAllDevelopers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DeveloperDto> getDeveloperById(@PathVariable Long id) {
        return ResponseEntity.ok(developerService.getDeveloperById(id));
    }

    @GetMapping("/search")
    public ResponseEntity<List<DeveloperDto>> searchDevelopers(
            @RequestParam(required = false) String skill,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) Double minRating) {
        return ResponseEntity.ok(developerService.searchDevelopers(skill, location, minRating));
    }

    @PutMapping("/profile")
    public ResponseEntity<DeveloperDto> updateProfile(
            @Valid @RequestBody DeveloperProfileRequest request,
            Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(developerService.updateProfile(request, email));
    }
}
