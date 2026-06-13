package com.ehi.payment.mapper;

import com.ehi.payment.dto.response.PaymentDto;
import com.ehi.payment.entity.Payment;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-06-13T06:36:21+0400",
    comments = "version: 1.5.5.Final, compiler: IncrementalProcessingEnvironment from gradle-language-java-8.10.2.jar, environment: Java 17.0.19 (Eclipse Adoptium)"
)
@Component
public class PaymentMapperImpl implements PaymentMapper {

    @Override
    public PaymentDto toDto(Payment payment) {
        if ( payment == null ) {
            return null;
        }

        PaymentDto.PaymentDtoBuilder paymentDto = PaymentDto.builder();

        paymentDto.id( payment.getId() );
        paymentDto.userId( payment.getUserId() );
        paymentDto.referenceId( payment.getReferenceId() );
        paymentDto.referenceType( payment.getReferenceType() );
        paymentDto.amount( payment.getAmount() );
        paymentDto.status( payment.getStatus() );
        paymentDto.transactionId( payment.getTransactionId() );
        paymentDto.failureReason( payment.getFailureReason() );
        paymentDto.checkoutUrl( payment.getCheckoutUrl() );
        paymentDto.epointTransaction( payment.getEpointTransaction() );
        paymentDto.cardMask( payment.getCardMask() );
        paymentDto.createdAt( payment.getCreatedAt() );

        return paymentDto.build();
    }
}
