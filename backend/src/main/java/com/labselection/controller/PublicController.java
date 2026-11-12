package com.labselection.controller;

import com.labselection.auth.PublicAccess;
import com.labselection.common.ApiResponse;
import com.labselection.domain.entity.Announcement;
import com.labselection.domain.entity.College;
import com.labselection.domain.entity.Lab;
import com.labselection.service.PublicService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@PublicAccess
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicController {

    private final PublicService publicService;

    @GetMapping("/home")
    public ApiResponse<Map<String, Object>> home() {
        return ApiResponse.ok(publicService.home());
    }

    @GetMapping("/colleges")
    public ApiResponse<List<College>> colleges() {
        return ApiResponse.ok(publicService.colleges());
    }

    @GetMapping("/labs")
    public ApiResponse<List<Lab>> labs() {
        return ApiResponse.ok(publicService.labs());
    }

    @GetMapping("/labs/{id}")
    public ApiResponse<Lab> lab(@PathVariable Long id) {
        return ApiResponse.ok(publicService.labDetail(id));
    }

    @GetMapping("/announcements")
    public ApiResponse<List<Announcement>> announcements() {
        return ApiResponse.ok(publicService.announcements());
    }

    @GetMapping("/publicity")
    public ApiResponse<Map<String, Object>> publicity(@RequestParam(required = false) Long batchId) {
        return ApiResponse.ok(publicService.publicResults(batchId));
    }
}
