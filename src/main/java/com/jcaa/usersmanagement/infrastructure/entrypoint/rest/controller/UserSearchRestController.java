package com.jcaa.usersmanagement.infrastructure.entrypoint.rest.controller;

import com.jcaa.usersmanagement.application.port.in.ReindexUsersUseCase;
import com.jcaa.usersmanagement.application.port.in.SearchUsersUseCase;
import com.jcaa.usersmanagement.infrastructure.entrypoint.rest.dto.response.ReindexRestResponse;
import com.jcaa.usersmanagement.infrastructure.entrypoint.rest.dto.response.UserSearchRestResponse;
import com.jcaa.usersmanagement.infrastructure.entrypoint.rest.mapper.UserRestMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/search")
@RequiredArgsConstructor
public class UserSearchRestController implements UserSearchRestControllerDocs {

  private static final String DEFAULT_SIZE = "20";

  private final SearchUsersUseCase searchUsersUseCase;
  private final ReindexUsersUseCase reindexUsersUseCase;

  @GetMapping
  public List<UserSearchRestResponse> search(
      @RequestParam(name = "q", required = false) final String text,
      @RequestParam(name = "role", required = false) final String role,
      @RequestParam(name = "status", required = false) final String status,
      @RequestParam(name = "size", defaultValue = DEFAULT_SIZE) final int size) {
    return UserRestMapper.toSearchResponseList(
        searchUsersUseCase.execute(UserRestMapper.toSearchQuery(text, role, status, size)));
  }

  @PostMapping("/reindex")
  public ReindexRestResponse reindex() {
    return new ReindexRestResponse(reindexUsersUseCase.execute());
  }
}
