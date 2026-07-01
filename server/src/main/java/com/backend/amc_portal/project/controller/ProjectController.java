package com.backend.amc_portal.project.controller;

import com.backend.amc_portal.common.dto.ApiResponse;
import com.backend.amc_portal.common.security.UserPrincipal;
import com.backend.amc_portal.project.dto.ProjectAddMembersRequest;
import com.backend.amc_portal.project.dto.ProjectDeleteMembersRequest;
import com.backend.amc_portal.project.dto.ProjectMemberHistoryResponse;
import com.backend.amc_portal.project.dto.ProjectMemberResponse;
import com.backend.amc_portal.project.dto.ProjectMemberRoleRequest;
import com.backend.amc_portal.project.dto.ProjectRequest;
import com.backend.amc_portal.project.dto.ProjectResponse;
import com.backend.amc_portal.project.service.ProjectService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

  private final ProjectService projectService;

  @GetMapping
  public ApiResponse<List<ProjectResponse>> list() {
    return ApiResponse.ok(projectService.listProjects());
  }

  @PostMapping
  public ApiResponse<ProjectResponse> create(
      @AuthenticationPrincipal UserPrincipal p, @Valid @RequestBody ProjectRequest req) {
    return ApiResponse.ok(projectService.create(p.id(), req));
  }

  @PatchMapping("/{id}")
  public ApiResponse<ProjectResponse> update(
      @PathVariable Long id,
      @AuthenticationPrincipal UserPrincipal p,
      @Valid @RequestBody ProjectRequest req) {
    return ApiResponse.ok(projectService.update(id, p.id(), req));
  }

  @PostMapping("/{id}/members")
  public ApiResponse<List<ProjectMemberResponse>> addMembers(
      @PathVariable Long id,
      @AuthenticationPrincipal UserPrincipal p,
      @Valid @RequestBody ProjectAddMembersRequest req) {
    return ApiResponse.ok(projectService.addMembers(id, p.id(), req));
  }

  @DeleteMapping("/{id}/members")
  public ApiResponse<List<ProjectMemberResponse>> deleteMembers(
      @PathVariable Long id,
      @AuthenticationPrincipal UserPrincipal p,
      @Valid @RequestBody ProjectDeleteMembersRequest req) {
    return ApiResponse.ok(projectService.deleteMembers(id, p.id(), req));
  }

  @PatchMapping("/{id}/members/{memberId}/role")
  public ApiResponse<ProjectMemberResponse> updateMemberRole(
      @PathVariable Long id,
      @PathVariable Long memberId,
      @AuthenticationPrincipal UserPrincipal p,
      @Valid @RequestBody ProjectMemberRoleRequest req) {
    return ApiResponse.ok(projectService.updateMemberRole(id, memberId, p.id(), req));
  }

  @GetMapping("/{id}/members")
  public ApiResponse<List<ProjectMemberResponse>> members(@PathVariable Long id) {
    return ApiResponse.ok(projectService.listMembers(id));
  }

  @GetMapping("/{id}/member-history")
  public ApiResponse<List<ProjectMemberHistoryResponse>> memberHistory(@PathVariable Long id) {
    return ApiResponse.ok(projectService.listMemberHistory(id));
  }
}
