package com.ehi.gw.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "gateway.security")
@Data
public class GatewaySecurityProperties {

    private List<String> publicPaths = List.of();
    private List<String> publicGetPaths = List.of();
}
