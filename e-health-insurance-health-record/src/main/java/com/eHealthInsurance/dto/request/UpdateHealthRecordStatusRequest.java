package com.eHealthInsurance.dto.request;

import com.eHealthInsurance.entity.enums.RecordStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateHealthRecordStatusRequest(
        @NotNull RecordStatus status
) {
}
