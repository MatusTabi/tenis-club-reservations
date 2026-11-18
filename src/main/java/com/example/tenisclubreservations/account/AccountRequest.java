package com.example.tenisclubreservations.account;

import io.swagger.v3.oas.annotations.media.Schema;

public record AccountRequest(String username, @Schema(accessMode = Schema.AccessMode.WRITE_ONLY) String password, RoleType role) {}
