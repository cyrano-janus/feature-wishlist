package com.example.featurewishlist.config;

import com.example.featurewishlist.view.LoginView;
import com.vaadin.flow.spring.security.VaadinWebSecurity;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

@Configuration
public class SecurityConfig extends VaadinWebSecurity {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // Wichtig: lässt Vaadin seine internen Endpunkte /VAADIN/** usw. korrekt zu
        super.configure(http);
        // Login-Route für Spring Security/ Vaadin setzen
        setLoginView(http, LoginView.class);
    }

    @Bean
    public InMemoryUserDetailsManager userDetailsService() {
        // Für Demo-Zwecke: {noop} = keine Passwort-Hashing (einfach)
        UserDetails user = User.withUsername("user").password("{noop}user").roles("USER").build();
        UserDetails admin = User.withUsername("admin").password("{noop}admin").roles("ADMIN").build();
        return new InMemoryUserDetailsManager(user, admin);
    }
}
