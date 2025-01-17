package org.synyx.urlaubsverwaltung.security.simple;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.synyx.urlaubsverwaltung.person.PersonService;

@Configuration
@ConditionalOnProperty(name = "uv.security.auth", havingValue = "default")
public class DefaultAuthConfiguration {

    @Bean
    public AuthenticationProvider defaultAuthenticationProvider(PersonService personService) {
        return new SimpleAuthenticationProvider(personService);
    }
}
