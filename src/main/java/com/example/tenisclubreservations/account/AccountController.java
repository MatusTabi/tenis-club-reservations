package com.example.tenisclubreservations.account;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.net.URI;
import java.util.List;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Accounts")
@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@ApiResponse(responseCode = "401", description = "Authentication required")
@ApiResponse(responseCode = "403", description = "Admin role required")
public class AccountController {
  private final AccountService service;

  @Operation(summary = "List accounts")
  @ApiResponse(responseCode = "200", description = "Accounts returned")
  @GetMapping
  public List<AccountResponse> list() { return service.list(); }

  @Operation(summary = "Get an account")
  @ApiResponse(responseCode = "200", description = "Account returned")
  @ApiResponse(responseCode = "404", description = "Account not found")
  @GetMapping("/{id}")
  public AccountResponse get(@Parameter(description = "Account ID") @PathVariable long id) { return service.get(id); }

  @Operation(summary = "Create an account")
  @ApiResponse(responseCode = "201", description = "Account created; Location identifies it")
  @ApiResponse(responseCode = "400", description = "Invalid account")
  @ApiResponse(responseCode = "409", description = "Username already exists")
  @PostMapping
  public ResponseEntity<AccountResponse> create(@RequestBody AccountRequest request) {
    AccountResponse result = service.create(request);
    return ResponseEntity.created(URI.create("/api/accounts/" + result.id())).body(result);
  }

  @Operation(summary = "Replace an account")
  @ApiResponse(responseCode = "200", description = "Account updated")
  @ApiResponse(responseCode = "400", description = "Invalid account")
  @ApiResponse(responseCode = "404", description = "Account not found")
  @ApiResponse(responseCode = "409", description = "Username conflict or last admin")
  @PutMapping("/{id}")
  public AccountResponse update(@Parameter(description = "Account ID") @PathVariable long id, @RequestBody AccountRequest request) { return service.update(id, request); }

  @Operation(summary = "Soft-delete an account")
  @ApiResponse(responseCode = "204", description = "Account deleted")
  @ApiResponse(responseCode = "404", description = "Account not found")
  @ApiResponse(responseCode = "409", description = "Cannot delete last admin")
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@Parameter(description = "Account ID") @PathVariable long id) {
    service.delete(id);
    return ResponseEntity.noContent().build();
  }
}
