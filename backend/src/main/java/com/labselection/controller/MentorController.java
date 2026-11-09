package com.labselection.controller;

import com.labselection.auth.RequireRole;
import com.labselection.common.ApiResponse;
import com.labselection.domain.entity.ApplicationIntent;
import com.labselection.domain.entity.FinalAssignment;
import com.labselection.domain.entity.Interview;
import com.labselection.domain.entity.Offer;
import com.labselection.domain.entity.SupplementIntent;
import com.labselection.domain.enums.Role;
import com.labselection.dto.DecisionRequest;
import com.labselection.dto.InterviewRequest;
import com.labselection.dto.SupplementDecisionRequest;
import com.labselection.service.MentorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mentor")
@RequireRole(Role.MENTOR)
@RequiredArgsConstructor
public class MentorController {

    private final MentorService mentorService;

    @GetMapping("/dashboard")
    public ApiResponse<Map<String, Object>> dashboard() {
        return ApiResponse.ok(mentorService.dashboard());
    }

    @GetMapping("/applicants")
    public ApiResponse<Map<String, Object>> applicants(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(mentorService.applicants(status, keyword, page, size));
    }

    @GetMapping("/applicants/{applicationId}")
    public ApiResponse<Map<String, Object>> applicant(@PathVariable Long applicationId) {
        return ApiResponse.ok(mentorService.applicantDetail(applicationId));
    }

    @GetMapping("/interviews")
    public ApiResponse<List<Interview>> interviews() {
        return ApiResponse.ok(mentorService.interviews());
    }

    @PostMapping("/interviews")
    public ApiResponse<Interview> saveInterview(@Valid @RequestBody InterviewRequest request) {
        return ApiResponse.message("面试记录已保存", mentorService.saveInterview(request));
    }

    @GetMapping("/decisions")
    public ApiResponse<Map<String, Object>> decisions() {
        return ApiResponse.ok(mentorService.decisions());
    }

    @PostMapping("/decisions")
    public ApiResponse<ApplicationIntent> decide(@Valid @RequestBody DecisionRequest request) {
        return ApiResponse.message("初选结论已提交", mentorService.decide(request));
    }

    @GetMapping("/offers")
    public ApiResponse<List<Offer>> offers() {
        return ApiResponse.ok(mentorService.offers());
    }

    @GetMapping("/students")
    public ApiResponse<List<FinalAssignment>> students() {
        return ApiResponse.ok(mentorService.finalStudents());
    }

    @GetMapping("/supplement")
    public ApiResponse<Map<String, Object>> supplement() {
        return ApiResponse.ok(mentorService.supplementIntents());
    }

    @PostMapping("/supplement/decisions")
    public ApiResponse<SupplementIntent> decideSupplement(
            @Valid @RequestBody SupplementDecisionRequest request) {
        return ApiResponse.message("补录结论已提交", mentorService.decideSupplement(request));
    }
}
