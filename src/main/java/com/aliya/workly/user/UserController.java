package com.aliya.workly.user;


import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class UserController {


private final UserService userService;

  @PutMapping("/{id}/company")
    @PreAuthorize("hasRole('ADMIN')")

    public ResponseEntity<Void> assignCompany(@PathVariable Long id , @RequestBody CompanyAssignmentDto dto){

      userService.assignCompanyToUser(id, dto.getCompanyId());
      return ResponseEntity.noContent().build();
  }


}
