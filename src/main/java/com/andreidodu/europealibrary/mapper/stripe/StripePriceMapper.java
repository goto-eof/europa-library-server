package com.andreidodu.europealibrary.mapper.stripe;

import com.andreidodu.europealibrary.dto.stripe.StripePriceDTO;
import com.andreidodu.europealibrary.model.stripe.StripePrice;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Slf4j
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR, uses = {StripeProductMapper.class})
public abstract class StripePriceMapper {

    @Mapping(ignore = true, target = "stripeProduct")
    @Mapping(ignore = true, target = "stripePriceId")
    public abstract StripePrice toModel(StripePriceDTO dto);

    public abstract StripePriceDTO toDTO(StripePrice model);

    @Mapping(target = "stripeProduct", ignore = true)
    @Mapping(target = "stripePriceId", ignore = true)
    public abstract void map(@MappingTarget StripePrice target, StripePriceDTO source);


}