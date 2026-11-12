package com.labselection.service;

import com.labselection.auth.AuthContext;
import com.labselection.auth.CurrentUser;
import com.labselection.auth.PasswordService;
import com.labselection.auth.TokenService;
import com.labselection.common.BusinessException;
import com.labselection.common.ErrorCode;
import com.labselection.domain.entity.College;
import com.labselection.domain.entity.Mentor;
import com.labselection.domain.entity.Student;
import com.labselection.domain.entity.SysUser;
import com.labselection.dto.LoginRequest;
import com.labselection.dto.LoginResponse;
import com.labselection.dto.UserInfo;
import com.labselection.mapper.CollegeMapper;
import com.labselection.mapper.MentorMapper;
import com.labselection.mapper.StudentMapper;
import com.labselection.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final StudentMapper studentMapper;
    private final MentorMapper mentorMapper;
    private final CollegeMapper collegeMapper;
    private final PasswordService passwordService;
    private final TokenService tokenService;
    private final AuditService auditService;

    public LoginResponse login(LoginRequest request) {
        SysUser user = userMapper.findByAccount(request.account());
        if (user == null || !passwordService.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.BAD_CREDENTIALS);
        }
        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }
        College college = collegeMapper.findById(request.collegeId());
        if (college == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "所选学院不存在");
        }
        if (user.getCollegeId() == null || !user.getCollegeId().equals(college.getId())) {
            throw new BusinessException(ErrorCode.COLLEGE_MISMATCH);
        }
        String token = tokenService.createToken(user);
        UserInfo info = buildUserInfo(user);
        auditService.log(null, "LOGIN", "USER", user.getId(),
                "账号登录", null);
        return new LoginResponse(token, info);
    }

    public UserInfo me() {
        CurrentUser current = AuthContext.get();
        SysUser user = userMapper.findById(current.userId());
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return buildUserInfo(user);
    }

    public void logout(String rawToken) {
        tokenService.revoke(rawToken);
    }

    private UserInfo buildUserInfo(SysUser user) {
        Object profile = null;
        Student student = studentMapper.findByUserId(user.getId());
        if (student != null) {
            profile = student;
        } else {
            Mentor mentor = mentorMapper.findByUserId(user.getId());
            if (mentor != null) {
                profile = mentor;
            }
        }
        College college = user.getCollegeId() == null ? null : collegeMapper.findById(user.getCollegeId());
        return new UserInfo(user.getId(), user.getAccount(), user.getDisplayName(), user.getRole(),
                user.getCollegeId(), college == null ? null : college.getName(), profile);
    }
}
