package com.example.authdemo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class WebSecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // Veřejné endpointy
                        .requestMatchers("/login", "/auth/**", "/register", "/css/**", "/js/**","/register/company","/api/**","/dev/**","/verification","/changePassword", "/auth/verification/**","/newPassword").permitAll()
                        .requestMatchers("/logo-provozni-denik.png").permitAll()

                        // --- OPRAVA ZDE: Specifičtější pravidlo musí být PRVNÍ ---
                        // Povolíme přístup přihlášeným uživatelům do sekce vozidel v adminu (konkrétní práva si řeší Controller)
                        .requestMatchers("/admin/vehicles/**").authenticated()

                        // Ostatní admin věci jen pro ADMIN a OWNER
                        .requestMatchers("/admin/**").hasAnyRole("ADMIN", "OWNER")

                        // Vše ostatní pro přihlášené
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .successHandler((request, response, authentication) -> {
                            boolean isAdminOrOwner = authentication.getAuthorities().stream()
                                    .map(a -> a.getAuthority())
                                    .anyMatch(role -> role.equals("ROLE_ADMIN") || role.equals("ROLE_OWNER"));

                            String targetUrl = isAdminOrOwner ? "/admin/dashboard" : "/vehicles/list";
                            response.sendRedirect(targetUrl);
                        })
                        .failureUrl("/login?error=true")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout=true")
                        .permitAll()
                );

        return http.build();
    }
}