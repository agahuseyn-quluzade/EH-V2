package com.ehi.iam.dto.request;

import com.ehi.infra.enums.UserRole;
import jakarta.validation.constraints.NotNull;

public record ChangeRoleRequest(
        @NotNull UserRole role
) {
}
