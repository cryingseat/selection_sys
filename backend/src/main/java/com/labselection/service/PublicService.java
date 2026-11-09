package com.labselection.service;

import com.labselection.common.BusinessException;
import com.labselection.common.ErrorCode;
import com.labselection.domain.entity.Announcement;
import com.labselection.domain.entity.FinalAssignment;
import com.labselection.domain.entity.Lab;
import com.labselection.domain.entity.Mentor;
import com.labselection.domain.entity.Publicity;
import com.labselection.domain.entity.SelectionBatch;
import com.labselection.domain.enums.BatchStatus;
import com.labselection.domain.enums.LabStatus;
import com.labselection.domain.enums.PublicityStatus;
import com.labselection.mapper.AnnouncementMapper;
import com.labselection.mapper.AssignmentMapper;
import com.labselection.mapper.LabMapper;
import com.labselection.mapper.MentorMapper;
import com.labselection.mapper.OfferMapper;
import com.labselection.mapper.PublicityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PublicService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("MM月dd日 HH:mm");

    private final StageGuard stageGuard;
    private final LabMapper labMapper;
    private final MentorMapper mentorMapper;
    private final AnnouncementMapper announcementMapper;
    private final PublicityMapper publicityMapper;
    private final AssignmentMapper assignmentMapper;
    private final OfferMapper offerMapper;

    public Map<String, Object> home() {
        SelectionBatch batch = stageGuard.current();
        List<Lab> labs = labMapper.listWithStats(batch.getId());
        for (Lab lab : labs) {
            decorateLab(lab, batch.getId());
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("batch", batch);
        result.put("stages", stages(batch));
        result.put("labs", labs);
        result.put("announcements", announcementMapper.findPublic(6));
        Publicity publicity = publicityMapper.findPublishedOrClosed(batch.getId());
        result.put("publicity", publicity);
        result.put("publicAssignments", publicity == null
                ? List.of() : assignmentMapper.findPublicByBatch(batch.getId()));
        result.put("stats", stats(batch, labs));
        result.put("serverTime", LocalDateTime.now());
        return result;
    }

    public List<Lab> labs() {
        SelectionBatch batch = stageGuard.current();
        List<Lab> labs = labMapper.listWithStats(batch.getId());
        labs.forEach(lab -> decorateLab(lab, batch.getId()));
        return labs;
    }

    public Lab labDetail(Long id) {
        Lab lab = labMapper.findById(id);
        if (lab == null || lab.getStatus() != LabStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "实训室不存在或已停用");
        }
        SelectionBatch batch = stageGuard.current();
        decorateLab(lab, batch.getId());
        return lab;
    }

    public List<Announcement> announcements() {
        return announcementMapper.findPublic(20);
    }

    public Map<String, Object> publicResults(Long batchId) {
        SelectionBatch batch = batchId == null ? stageGuard.current() : stageGuard.require(batchId);
        Publicity publicity = publicityMapper.findPublishedOrClosed(batch.getId());
        if (publicity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "该批次尚未发布公示结果");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("batch", batch);
        result.put("publicity", publicity);
        result.put("assignments", assignmentMapper.findPublicByBatch(batch.getId()));
        return result;
    }

    public List<Map<String, Object>> stages(SelectionBatch batch) {
        int current = currentStep(batch.getStatus());
        List<Map<String, Object>> stages = new ArrayList<>();
        stages.add(stage(1, "集中宣讲", "学院统一介绍项目方向、名额与考核要求", time(batch.getLectureAt(), null), current));
        stages.add(stage(2, "实训室开放日", "实地走访实训室，与导师和高年级学生交流", time(batch.getOpenDayStart(), batch.getOpenDayEnd()), current));
        stages.add(stage(3, "意向填报", "最多 3 个平行导师意向，并选择调剂意愿", time(batch.getIntentStart(), batch.getIntentEnd()), current));
        stages.add(stage(4, "导师初选与面试考核", "实训室统一面试，导师提交拟录取/候补建议", time(batch.getInterviewStart(), batch.getInterviewEnd()), current));
        stages.add(stage(5, "拟录取与学生确认", "多个拟录取意向由学生自主确认其一", time(batch.getConfirmStart(), batch.getConfirmEnd()), current));
        stages.add(stage(6, "分轮补录", "公布剩余名额，候补递补与重新填报", time(batch.getSupplementStart(), batch.getSupplementEnd()), current));
        stages.add(stage(7, "统筹调剂录取", "按内部调剂、跨室调剂、谈话安置依次统筹", time(batch.getAdjustStart(), batch.getAdjustEnd()), current));
        stages.add(stage(8, "结果公示与正式入组", "公示拟录取名单，无异议后正式入组", time(batch.getPublicityStart(), batch.getPublicityEnd()), current));
        return stages;
    }

    public Map<String, Object> stats(SelectionBatch batch, List<Lab> labs) {
        int totalQuota = labs.stream().map(Lab::getTotalQuota).filter(Objects::nonNull).mapToInt(Integer::intValue).sum();
        int admitted = labs.stream().map(Lab::getConfirmedCount).filter(Objects::nonNull).mapToInt(Integer::intValue).sum();
        long mentors = labs.stream().map(Lab::getMentorCount).filter(Objects::nonNull).mapToInt(Integer::intValue).sum();
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("labCount", labs.size());
        stats.put("mentorCount", mentors);
        stats.put("totalQuota", totalQuota);
        stats.put("admittedCount", admitted);
        stats.put("applicationCount", batch.getApplicationCount() == null ? 0 : batch.getApplicationCount());
        return stats;
    }

    private void decorateLab(Lab lab, Long batchId) {
        List<Mentor> mentors = mentorMapper.listWithStats(batchId, lab.getId());
        mentors.removeIf(m -> !Boolean.TRUE.equals(m.getEnabled()));
        for (Mentor mentor : mentors) {
            int used = Math.max(mentor.getOfferedCount() == null ? 0 : mentor.getOfferedCount(),
                    mentor.getConfirmedCount() == null ? 0 : mentor.getConfirmedCount());
            mentor.setApplicantCount(mentor.getApplicantCount());
            mentor.setConfirmedCount(used);
        }
        lab.setMentors(mentors);
        lab.setMentorCount(mentors.size());
        long activeOffers = offerMapper.countActiveByLab(lab.getId(), batchId);
        int confirmed = lab.getConfirmedCount() == null ? 0 : lab.getConfirmedCount();
        int used = (int) Math.max(activeOffers, confirmed);
        lab.setRemainingQuota(Math.max(0, lab.getTotalQuota() - used));
    }

    private int currentStep(BatchStatus status) {
        return switch (status) {
            case DRAFT -> 0;
            case ANNOUNCED -> 2;
            case INTENT_OPEN, INTENT_CLOSED -> 3;
            case INTERVIEW -> 4;
            case CONFIRM -> 5;
            case SUPPLEMENT -> 6;
            case ADJUSTMENT -> 7;
            case PUBLICITY -> 8;
            case FINISHED -> 9;
        };
    }

    private Map<String, Object> stage(int no, String title, String desc, String time, int current) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("no", no);
        map.put("title", title);
        map.put("desc", desc);
        map.put("time", time);
        map.put("state", current > no ? "done" : current == no ? "active" : "upcoming");
        return map;
    }

    private String time(LocalDateTime start, LocalDateTime end) {
        if (start == null && end == null) {
            return "待定";
        }
        if (end == null) {
            return FMT.format(start);
        }
        if (start == null) {
            return FMT.format(end);
        }
        return FMT.format(start) + " — " + FMT.format(end);
    }
}
