package com.andreidodu.europalibrary.batch;


import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties("batchdb")
public class BatchDbConfig {
    private String url;
    private String username;
    private String password;
}



