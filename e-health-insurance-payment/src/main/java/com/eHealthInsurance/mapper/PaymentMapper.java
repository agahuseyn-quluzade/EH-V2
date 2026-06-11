package com.eHealthInsurance.mapper;

import com.eHealthInsurance.dto.response.InvoiceResponse;
import com.eHealthInsurance.dto.response.PaymentResponse;
import com.eHealthInsurance.dto.response.RefundResponse;
import com.eHealthInsurance.entity.Invoice;
import com.eHealthInsurance.entity.Payment;
import com.eHealthInsurance.entity.Refund;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

    @Mapping(target = "provider", source = "provider", qualifiedByName = "enumToString")
    @Mapping(target = "status", source = "status", qualifiedByName = "enumToString")
    PaymentResponse toResponse(Payment payment);

    @Mapping(target = "status", source = "status", qualifiedByName = "enumToString")
    InvoiceResponse toInvoiceResponse(Invoice invoice);

    @Mapping(target = "status", source = "status", qualifiedByName = "enumToString")
    RefundResponse toRefundResponse(Refund refund);

    @Named("enumToString")
    default String enumToString(Enum<?> value) {
        return value != null ? value.name() : null;
    }
}
