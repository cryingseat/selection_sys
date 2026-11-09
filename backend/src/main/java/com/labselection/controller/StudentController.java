package com.labselection.controller;

import com.labselection.auth.RequireRole;
import com.labselection.common.ApiResponse;
import com.labselection.domain.entity.Application;
import com.labselection.domain.entity.Interview;
import com.labselection.domain.entity.Offer;
import com.labselection.domain.enums.Role;
import com.labselection.dto.ApplicationSaveRequest;
import com.labselection.dto.SupplementApplyRequest;
import com.labselection.service.StudentService;
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
@RequestMapping("/api/student")
@RequireRole(Role.STUDENT)
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    @GetMapping("/dashboard")
    public ApiResponse<Map<String, Object>> dashboard() {
        return ApiResponse.ok(studentService.dashboard());
    }

    @GetMapping("/application")
    public ApiResponse<Application> application() {
        return ApiResponse.ok(studentService.application());
    }

    @PostMapping("/application/draft")
    public ApiResponse<Application> saveDraft(@Valid @RequestBody ApplicationSaveRequest request) {
        return ApiResponse.message("草稿已保存", studentService.saveApplication(request, false));
    }

    @PostMapping("/application/submit")
    public ApiResponse<Application> submit(@Valid @RequestBody ApplicationSaveRequest request) {
        return ApiResponse.message("报名已提交", studentService.saveApplication(request, true));
    }

    @PostMapping("/application/withdraw")
    public ApiResponse<Void> withdraw() {
        studentService.withdraw();
        return ApiResponse.ok();
    }

    @GetMapping("/interviews")
    public ApiResponse<List<Interview>> interviews() {
        return ApiResponse.ok(studentService.interviews());
    }

    @GetMapping("/offers")
    public ApiResponse<List<Offer>> offers() {
        return ApiResponse.ok(studentService.offers());
    }

    @PostMapping("/offers/{id}/confirm")
    public ApiResponse<Map<String, Object>> confirm(@PathVariable Long id,
                                                    @RequestParam(required = false) String reason) {
        return ApiResponse.message("录取意向已确认", studentService.confirmOffer(id, reason));
    }

    @PostMapping("/offers/{id}/decline")
    public ApiResponse<Void> decline(@PathVariable Long id,
                                     @RequestParam(required = false) String reason) {
        studentService.declineOffer(id, reason);
        return ApiResponse.ok();
    }

    @PostMapping("/offers/decline-all")
    public ApiResponse<Map<String, Object>> declineAll(@RequestParam(required = false) String reason) {
        int count = studentService.declineAll(reason);
        return ApiResponse.message("已放弃全部拟录取意向",
                Map.of("count", count));
    }

    @GetMapping("/result")
    public ApiResponse<Map<String, Object>> result() {
        return ApiResponse.ok(studentService.result());
    }

    @GetMapping("/history")
    public ApiResponse<List<Map<String, Object>>> history() {
        return ApiResponse.ok(studentService.history());
    }

    @GetMapping("/supplement")
    public ApiResponse<Map<String, Object>> supplement() {
        return ApiResponse.ok(studentService.supplementState());
    }

    @PostMapping("/supplement/intents")
    public ApiResponse<Map<String, Object>> applySupplement(@Valid @RequestBody SupplementApplyRequest request) {
        return ApiResponse.message("补录意向已提交", studentService.applySupplement(request));
    }
}
