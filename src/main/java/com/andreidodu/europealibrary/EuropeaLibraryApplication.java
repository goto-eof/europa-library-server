package com.andreidodu.europealibrary;

import com.andreidodu.europealibrary.config.security.RsaKeyConfigProperties;
import com.andreidodu.europealibrary.util.PostApplicationRunUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication(exclude = HibernateJpaAutoConfiguration.class)
@RequiredArgsConstructor
@EnableConfigurationProperties(RsaKeyConfigProperties.class)
public class EuropeaLibraryApplication {

    public static void main(String[] args) {
        var context = SpringApplication.run(EuropeaLibraryApplication.class, args);
        postApplicationRun(context);
    }

    private static void postApplicationRun(ConfigurableApplicationContext context) {
        PostApplicationRunUtil postApplicationRunUtil = context.getBean(PostApplicationRunUtil.class);
        postApplicationRunUtil.loadCache();
        postApplicationRunUtil.addDefaultUsersAndRolesIfNecessary();
        postApplicationRunUtil.createDefaultApplicationSettings();
    }


}
