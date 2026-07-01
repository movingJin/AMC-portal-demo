package com.backend.amc_portal.project.service;

import com.backend.amc_portal.auth.entity.User;
import com.backend.amc_portal.auth.repository.UserRepository;
import com.backend.amc_portal.common.exception.ApiException;
import com.backend.amc_portal.project.dto.ProjectAddMembersRequest;
import com.backend.amc_portal.project.dto.ProjectDeleteMembersRequest;
import com.backend.amc_portal.project.dto.ProjectMemberHistoryResponse;
import com.backend.amc_portal.project.dto.ProjectMemberResponse;
import com.backend.amc_portal.project.dto.ProjectMemberRoleRequest;
import com.backend.amc_portal.project.dto.ProjectRequest;
import com.backend.amc_portal.project.dto.ProjectResponse;
import com.backend.amc_portal.project.entity.Project;
import com.backend.amc_portal.project.entity.ProjectMember;
import com.backend.amc_portal.project.entity.ProjectMemberHistory;
import com.backend.amc_portal.project.enums.ProjectMemberEventType;
import com.backend.amc_portal.project.enums.ProjectRole;
import com.backend.amc_portal.project.repository.ProjectMemberHistoryRepository;
import com.backend.amc_portal.project.repository.ProjectMemberRepository;
import com.backend.amc_portal.project.repository.ProjectRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProjectService {

  private final ProjectRepository projectRepository;
  private final ProjectMemberRepository projectMemberRepository;
  private final ProjectMemberHistoryRepository projectMemberHistoryRepository;
  private final UserRepository userRepository;

  private void requireAdmin(Long projectId, Long userId) {
    if (!projectMemberRepository.existsByProjectIdAndUserIdAndRole(
        projectId, userId, ProjectRole.ADMIN)) {
      throw ApiException.forbidden("관리자 권한이 필요합니다.");
    }
  }

  @Transactional(readOnly = true)
  public List<ProjectResponse> listProjects() {
    return projectRepository.findAllByOrderByCreatedAtDesc().stream()
        .map(ProjectResponse::from)
        .toList();
  }

  @Transactional
  public ProjectResponse create(Long userId, ProjectRequest req) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> ApiException.unauthorized("사용자를 찾을 수 없습니다."));
    Project project =
        projectRepository.save(
            Project.builder()
                .name(req.name())
                .description(req.description())
                .createdBy(user)
                .build());
    projectMemberRepository.save(
        ProjectMember.builder().project(project).user(user).role(ProjectRole.ADMIN).build());
    projectMemberHistoryRepository.save(
        ProjectMemberHistory.builder()
            .project(project)
            .user(user)
            .role(ProjectRole.ADMIN)
            .eventType(ProjectMemberEventType.JOINED)
            .actedBy(user)
            .build());
    return ProjectResponse.from(project);
  }

  @Transactional
  public ProjectResponse update(Long projectId, Long userId, ProjectRequest req) {
    Project project =
        projectRepository
            .findById(projectId)
            .orElseThrow(() -> ApiException.notFound("프로젝트를 찾을 수 없습니다."));
    requireAdmin(projectId, userId);
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> ApiException.unauthorized("사용자를 찾을 수 없습니다."));
    project.update(req.name(), req.description(), user);
    return ProjectResponse.from(project);
  }

  @Transactional
  public List<ProjectMemberResponse> addMembers(
      Long projectId, Long requesterId, ProjectAddMembersRequest req) {
    Project project =
        projectRepository
            .findById(projectId)
            .orElseThrow(() -> ApiException.notFound("프로젝트를 찾을 수 없습니다."));
    requireAdmin(projectId, requesterId);
    User actedBy =
        userRepository
            .findById(requesterId)
            .orElseThrow(() -> ApiException.unauthorized("사용자를 찾을 수 없습니다."));

    for (Long userId : req.userIds()) {
      if (projectMemberRepository.existsByProjectIdAndUserId(projectId, userId)) continue;
      User user =
          userRepository
              .findById(userId)
              .orElseThrow(() -> ApiException.notFound("사용자를 찾을 수 없습니다. id=" + userId));
      projectMemberRepository.save(
          ProjectMember.builder().project(project).user(user).role(ProjectRole.VIEWER).build());
      projectMemberHistoryRepository.save(
          ProjectMemberHistory.builder()
              .project(project)
              .user(user)
              .role(ProjectRole.VIEWER)
              .eventType(ProjectMemberEventType.JOINED)
              .actedBy(actedBy)
              .build());
    }

    return projectMemberRepository.findAllByProjectId(projectId).stream()
        .map(ProjectMemberResponse::from)
        .toList();
  }

  @Transactional
  public List<ProjectMemberResponse> deleteMembers(
      Long projectId, Long requesterId, ProjectDeleteMembersRequest req) {
    projectRepository
        .findById(projectId)
        .orElseThrow(() -> ApiException.notFound("프로젝트를 찾을 수 없습니다."));
    requireAdmin(projectId, requesterId);
    User actedBy =
        userRepository
            .findById(requesterId)
            .orElseThrow(() -> ApiException.unauthorized("사용자를 찾을 수 없습니다."));

    List<ProjectMember> toDelete =
        req.memberIds().stream()
            .map(projectMemberRepository::findById)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .filter(m -> m.getProject().getId().equals(projectId))
            .toList();

    long adminCount = projectMemberRepository.countByProjectIdAndRole(projectId, ProjectRole.ADMIN);
    long deletingAdminCount =
        toDelete.stream().filter(m -> m.getRole() == ProjectRole.ADMIN).count();
    if (adminCount - deletingAdminCount < 1) {
      throw ApiException.forbidden("최소 1명의 관리자는 남아야 합니다.");
    }

    for (ProjectMember member : toDelete) {
      projectMemberHistoryRepository.save(
          ProjectMemberHistory.builder()
              .project(member.getProject())
              .user(member.getUser())
              .role(member.getRole())
              .eventType(ProjectMemberEventType.REMOVED)
              .actedBy(actedBy)
              .build());
      projectMemberRepository.delete(member);
    }

    return projectMemberRepository.findAllByProjectId(projectId).stream()
        .map(ProjectMemberResponse::from)
        .toList();
  }

  @Transactional
  public ProjectMemberResponse updateMemberRole(
      Long projectId, Long memberId, Long requesterId, ProjectMemberRoleRequest req) {
    projectRepository
        .findById(projectId)
        .orElseThrow(() -> ApiException.notFound("프로젝트를 찾을 수 없습니다."));
    requireAdmin(projectId, requesterId);
    User actedBy =
        userRepository
            .findById(requesterId)
            .orElseThrow(() -> ApiException.unauthorized("사용자를 찾을 수 없습니다."));
    ProjectMember member =
        projectMemberRepository
            .findById(memberId)
            .orElseThrow(() -> ApiException.notFound("멤버를 찾을 수 없습니다."));
    if (!member.getProject().getId().equals(projectId)) {
      throw ApiException.forbidden("해당 프로젝트의 멤버가 아닙니다.");
    }
    if (member.getRole() == ProjectRole.ADMIN && req.role() != ProjectRole.ADMIN) {
      long adminCount =
          projectMemberRepository.countByProjectIdAndRole(projectId, ProjectRole.ADMIN);
      if (adminCount <= 1) {
        throw ApiException.forbidden("마지막 관리자의 역할은 변경할 수 없습니다.");
      }
    }
    member.updateRole(req.role());
    projectMemberHistoryRepository.save(
        ProjectMemberHistory.builder()
            .project(member.getProject())
            .user(member.getUser())
            .role(req.role())
            .eventType(ProjectMemberEventType.ROLE_CHANGED)
            .actedBy(actedBy)
            .build());
    return ProjectMemberResponse.from(member);
  }

  @Transactional(readOnly = true)
  public List<ProjectMemberHistoryResponse> listMemberHistory(Long projectId) {
    if (!projectRepository.existsById(projectId)) {
      throw ApiException.notFound("프로젝트를 찾을 수 없습니다.");
    }
    return projectMemberHistoryRepository.findAllByProjectIdOrderByActedAtDesc(projectId).stream()
        .map(ProjectMemberHistoryResponse::from)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<ProjectMemberResponse> listMembers(Long projectId) {
    if (!projectRepository.existsById(projectId)) {
      throw ApiException.notFound("프로젝트를 찾을 수 없습니다.");
    }
    return projectMemberRepository.findAllByProjectId(projectId).stream()
        .map(ProjectMemberResponse::from)
        .toList();
  }
}
