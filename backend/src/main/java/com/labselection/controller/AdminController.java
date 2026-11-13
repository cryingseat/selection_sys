package com.labselection.controller;

import com.labselection.auth.RequireRole;
import com.labselection.common.ApiResponse;
import com.labselection.common.PageResult;
import com.labselection.domain.entity.Announcement;
import com.labselection.domain.entity.AuditLog;
import com.labselection.domain.entity.Lab;
import com.labselection.domain.entity.Mentor;
import com.labselection.domain.entity.Publicity;
import com.labselection.domain.entity.SelectionBatch;
import com.labselection.domain.entity.Student;
import com.labselection.domain.entity.SupplementRound;
import com.labselection.domain.enums.Role;
import com.labselection.dto.AdjustmentRequest;
import com.labselection.dto.MentorSaveRequest;
import com.labselection.dto.PasswordRequest;
import com.labselection.dto.StageRequest;
import com.labselection.dto.StudentSaveRequest;
import com.labselection.dto.SupplementRoundRequest;
import com.labselection.service.AdminService;
import com.labselection.service.AdjustmentService;
import com.labselection.service.CatalogService;
import com.labselection.service.PublicityService;
import com.labselection.service.SupplementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequireRole(Role.ADMIN)
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final CatalogService catalogService;
    private final SupplementService supplementService;
    private final AdjustmentService adjustmentService;
    private final PublicityService publicityService;

    // ---------- 总览与批次 ----------

    @GetMapping("/dashboard")
    public ApiResponse<Map<String, Object>> dashboard() {
        return ApiResponse.ok(adminService.dashboard());
    }

    @GetMapping("/state-machine")
    public ApiResponse<Map<String, Object>> stateMachine() {
        return ApiResponse.ok(adminService.stateMachine());
    }

    @GetMapping("/batches")
    public ApiResponse<List<SelectionBatch>> batches() {
        return ApiResponse.ok(adminService.batches());
    }

    @PostMapping("/batches")
    public ApiResponse<SelectionBatch> createBatch(@RequestBody SelectionBatch request) {
        return ApiResponse.message("批次已创建", adminService.createBatch(request));
    }

    @GetMapping("/batches/{id}")
    public ApiResponse<Map<String, Object>> batch(@PathVariable Long id) {
        return ApiResponse.ok(adminService.batchDetail(id));
    }

    @PutMapping("/batches/{id}")
    public ApiResponse<SelectionBatch> updateBatch(@PathVariable Long id,
                                                   @RequestBody SelectionBatch request) {
        return ApiResponse.message("批次已更新", adminService.updateBatch(id, request));
    }

    @PostMapping("/batches/{id}/stage")
    public ApiResponse<SelectionBatch> changeStage(@PathVariable Long id,
                                                   @Valid @RequestBody StageRequest request) {
        return ApiResponse.message("阶段已切换", adminService.changeStage(id, request));
    }

    // ---------- 审核与结果 ----------

    @GetMapping("/review")
    public ApiResponse<Map<String, Object>> review(@RequestParam(required = false) Long batchId) {
        Long id = batchId;
        if (id == null) {
            Object current = adminService.dashboard().get("batch");
            if (!(current instanceof SelectionBatch batch)) {
                return ApiResponse.error(40401, "尚未创建批次");
            }
            id = batch.getId();
        }
        return ApiResponse.ok(adminService.review(id));
    }

    @PostMapping("/review/expire-offers")
    public ApiResponse<Map<String, Object>> expireOffers(@RequestParam Long batchId,
                                                         @RequestParam(required = false) String reason) {
        int count = adminService.expireOffers(batchId, reason);
        return ApiResponse.message("逾期拟录取已按放弃处理", Map.of("count", count));
    }

    @GetMapping("/applications")
    public ApiResponse<Map<String, Object>> applications(
            @RequestParam(required = false) Long batchId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(adminService.applications(batchId, status, keyword, page, size));
    }

    @GetMapping("/applications/{id}")
    public ApiResponse<Map<String, Object>> application(@PathVariable Long id) {
        return ApiResponse.ok(adminService.applicationDetail(id));
    }

    @GetMapping("/offers")
    public ApiResponse<Map<String, Object>> offers(@RequestParam(required = false) Long batchId) {
        return ApiResponse.ok(adminService.offers(batchId));
    }

    // ---------- 实训室 ----------

    @GetMapping("/labs")
    public ApiResponse<List<Lab>> labs() {
        return ApiResponse.ok(catalogService.labs());
    }

    @PostMapping("/labs")
    public ApiResponse<Lab> createLab(@RequestBody Lab request) {
        return ApiResponse.message("实训室已创建", catalogService.saveLab(request, null));
    }

    @PutMapping("/labs/{id}")
    public ApiResponse<Lab> updateLab(@PathVariable Long id, @RequestBody Lab request) {
        return ApiResponse.message("实训室已更新", catalogService.saveLab(request, id));
    }

    @PatchMapping("/labs/{id}/status")
    public ApiResponse<Void> toggleLab(@PathVariable Long id, @RequestParam boolean enabled) {
        catalogService.toggleLab(id, enabled);
        return ApiResponse.ok();
    }

    // ---------- 导师 ----------

    @GetMapping("/mentors")
    public ApiResponse<PageResult<Mentor>> mentors(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long labId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(catalogService.mentors(keyword, labId, page, size));
    }

    @PostMapping("/mentors")
    public ApiResponse<Mentor> createMentor(@Valid @RequestBody MentorSaveRequest request) {
        return ApiResponse.message("导师已创建", catalogService.createMentor(request));
    }

    @PutMapping("/mentors/{id}")
    public ApiResponse<Mentor> updateMentor(@PathVariable Long id,
                                            @Valid @RequestBody MentorSaveRequest request) {
        return ApiResponse.message("导师已更新", catalogService.updateMentor(id, request));
    }

    @PatchMapping("/mentors/{id}/status")
    public ApiResponse<Void> toggleMentor(@PathVariable Long id, @RequestParam boolean enabled) {
        catalogService.toggleMentor(id, enabled);
        return ApiResponse.ok();
    }

    @PostMapping("/mentors/{id}/password")
    public ApiResponse<Void> resetMentorPassword(@PathVariable Long id,
                                                 @Valid @RequestBody PasswordRequest request) {
        catalogService.resetMentorPassword(id, request.password());
        return ApiResponse.ok();
    }

    // ---------- 学生 ----------

    @GetMapping("/students")
    public ApiResponse<PageResult<Student>> students(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String grade,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(catalogService.students(keyword, grade, page, size));
    }

    @PostMapping("/students")
    public ApiResponse<Student> createStudent(@Valid @RequestBody StudentSaveRequest request) {
        return ApiResponse.message("学生已创建", catalogService.createStudent(request));
    }

    @PutMapping("/students/{id}")
    public ApiResponse<Student> updateStudent(@PathVariable Long id,
                                              @Valid @RequestBody StudentSaveRequest request) {
        return ApiResponse.message("学生已更新", catalogService.updateStudent(id, request));
    }

    @PatchMapping("/students/{id}/status")
    public ApiResponse<Void> toggleStudent(@PathVariable Long id, @RequestParam boolean enabled) {
        catalogService.toggleStudent(id, enabled);
        return ApiResponse.ok();
    }

    @PostMapping("/students/{id}/password")
    public ApiResponse<Void> resetStudentPassword(@PathVariable Long id,
                                                  @Valid @RequestBody PasswordRequest request) {
        catalogService.resetStudentPassword(id, request.password());
        return ApiResponse.ok();
    }

    // ---------- 公告 ----------

    @GetMapping("/announcements")
    public ApiResponse<List<Announcement>> announcements(@RequestParam(required = false) Long batchId) {
        return ApiResponse.ok(catalogService.announcements(batchId));
    }

    @PostMapping("/announcements")
    public ApiResponse<Announcement> createAnnouncement(@RequestBody Announcement request) {
        return ApiResponse.message("公告已保存", catalogService.saveAnnouncement(request, null));
    }

    @PutMapping("/announcements/{id}")
    public ApiResponse<Announcement> updateAnnouncement(@PathVariable Long id,
                                                        @RequestBody Announcement request) {
        return ApiResponse.message("公告已更新", catalogService.saveAnnouncement(request, id));
    }

    @PostMapping("/announcements/{id}/publish")
    public ApiResponse<Void> publishAnnouncement(@PathVariable Long id) {
        catalogService.publishAnnouncement(id);
        return ApiResponse.ok();
    }

    @DeleteMapping("/announcements/{id}")
    public ApiResponse<Void> deleteAnnouncement(@PathVariable Long id) {
        catalogService.deleteAnnouncement(id);
        return ApiResponse.ok();
    }

    // ---------- 补录 ----------

    @GetMapping("/supplement/rounds")
    public ApiResponse<List<SupplementRound>> rounds(@RequestParam(required = false) Long batchId) {
        return ApiResponse.ok(supplementService.rounds(batchId));
    }

    @PostMapping("/supplement/rounds")
    public ApiResponse<SupplementRound> createRound(@Valid @RequestBody SupplementRoundRequest request) {
        return ApiResponse.message("补录轮次已创建", supplementService.createRound(request));
    }

    @PostMapping("/supplement/rounds/{id}/open")
    public ApiResponse<SupplementRound> openRound(@PathVariable Long id) {
        return ApiResponse.message("补录轮次已开放", supplementService.openRound(id));
    }

    @PostMapping("/supplement/rounds/{id}/close")
    public ApiResponse<Map<String, Object>> closeRound(@PathVariable Long id) {
        return ApiResponse.message("补录轮次已结束", supplementService.closeRound(id));
    }

    @PostMapping("/supplement/rounds/{id}/promote")
    public ApiResponse<Map<String, Object>> promote(@PathVariable Long id) {
        return ApiResponse.message("候补递补执行完成", supplementService.promoteWaitlist(id));
    }

    @GetMapping("/supplement/waitlist")
    public ApiResponse<Map<String, Object>> waitlist(@RequestParam(required = false) Long batchId) {
        return ApiResponse.ok(supplementService.waitlistOverview(batchId));
    }

    // ---------- 调剂 ----------

    @GetMapping("/adjustments")
    public ApiResponse<List<com.labselection.domain.entity.AdjustmentRecord>> adjustments(
            @RequestParam(required = false) Long batchId) {
        return ApiResponse.ok(adjustmentService.records(batchId));
    }

    @GetMapping("/adjustments/workspace")
    public ApiResponse<Map<String, Object>> adjustmentWorkspace() {
        return ApiResponse.ok(adjustmentService.workspace());
    }

    @PostMapping("/adjustments/auto")
    public ApiResponse<Map<String, Object>> autoAdjust() {
        return ApiResponse.message("统筹调剂执行完成", adjustmentService.autoAdjust());
    }

    @PostMapping("/adjustments/manual")
    public ApiResponse<Map<String, Object>> manualAdjust(@Valid @RequestBody AdjustmentRequest request) {
        return ApiResponse.message("调剂已办理",
                adjustmentService.manualAdjust(request.studentId(), request.toMentorId(), request.reason()));
    }

    @PostMapping("/adjustments/{studentId}/talk")
    public ApiResponse<Void> talk(@PathVariable Long studentId,
                                  @RequestParam(required = false) String reason) {
        adjustmentService.talk(studentId, reason);
        return ApiResponse.ok();
    }

    // ---------- 公示 ----------

    @GetMapping("/publicities")
    public ApiResponse<List<Publicity>> publicities(@RequestParam(required = false) Long batchId) {
        return ApiResponse.ok(publicityService.list(batchId));
    }

    @PostMapping("/publicities")
    public ApiResponse<Publicity> savePublicity(@RequestBody Publicity request) {
        return ApiResponse.message("公示草稿已保存", publicityService.saveDraft(request));
    }

    @GetMapping("/publicities/{id}")
    public ApiResponse<Map<String, Object>> publicity(@PathVariable Long id) {
        return ApiResponse.ok(publicityService.detail(id));
    }

    @PostMapping("/publicities/{id}/publish")
    public ApiResponse<Publicity> publishPublicity(@PathVariable Long id) {
        return ApiResponse.message("公示已发布", publicityService.publish(id));
    }

    @PostMapping("/publicities/{id}/close")
    public ApiResponse<Publicity> closePublicity(@PathVariable Long id) {
        return ApiResponse.message("公示已结束，结果已锁定", publicityService.close(id));
    }

    @PostMapping("/publicities/{id}/revoke")
    public ApiResponse<Publicity> revokePublicity(@PathVariable Long id,
                                                  @RequestParam(required = false) String reason) {
        return ApiResponse.message("公示已撤回", publicityService.revoke(id, reason));
    }

    // ---------- 审计 ----------

    @GetMapping("/audit-logs")
    public ApiResponse<PageResult<AuditLog>> auditLogs(
            @RequestParam(required = false) Long batchId,
            @RequestParam(required = false) String action,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(adminService.auditLogs(batchId, action, page, size));
    }
}
