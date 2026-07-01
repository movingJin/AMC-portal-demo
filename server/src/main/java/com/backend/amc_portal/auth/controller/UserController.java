package com.backend.amc_portal.auth.controller;

import com.backend.amc_portal.auth.dto.UserSearchResponse;
import com.backend.amc_portal.auth.repository.UserRepository;
import com.backend.amc_portal.common.dto.ApiResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

  private final UserRepository userRepository;

  @GetMapping("/search")
  public ApiResponse<List<UserSearchResponse>> search(@RequestParam(defaultValue = "") String q) {
    List<UserSearchResponse> result =
        userRepository.searchByKeyword(q.trim(), PageRequest.of(0, 20)).stream()
            .map(UserSearchResponse::from)
            .toList();
    return ApiResponse.ok(result);
  }
}
