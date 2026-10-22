package ru.library.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/css/**", "/login", "/register", "/error").permitAll()
                        .requestMatchers("/admin/**").hasRole("LIBRARIAN")
                        .requestMatchers("/reader/**").hasRole("READER")
                        .requestMatchers(HttpMethod.POST, "/catalog/*/borrow").hasRole("READER")
                        .requestMatchers(HttpMethod.POST, "/catalog/*/readlist").hasRole("READER")
                        .requestMatchers(HttpMethod.POST, "/catalog/*/readlist").hasRole("READER")
                        .requestMatchers("/home/**").hasRole("READER")
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/", true)
                        .permitAll())
                .logout(logout -> logout.logoutSuccessUrl("/login?logout"));
        return http.build();
    }
}
