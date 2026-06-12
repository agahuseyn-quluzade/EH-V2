package com.ehi.payment.mapper;

import com.ehi.payment.dto.response.PaymentDto;
import com.ehi.payment.entity.Payment;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

    PaymentDto toDto(Payment payment);
}
