package biacolo.com.cit.contactintegration.Config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.MvcRequestMatcher;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.filter.HiddenHttpMethodFilter;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/contacts/login", "/error").permitAll()
                .requestMatchers("/images/**", "/webjars/**", "/css/**", "/js/**").permitAll()
                .requestMatchers("/contacts/**").authenticated()
                .requestMatchers(new AntPathRequestMatcher("/contacts/**", "DELETE")).authenticated()
                .anyRequest().authenticated()
            )
            // Configure OAuth2 Login.
            .oauth2Login(oauth2 -> oauth2
                .loginPage("/contacts/login")
                .defaultSuccessUrl("/contacts", true)
            )
            // Configure Logout functionality.
            .logout(logout -> logout
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout", "POST"))
                .logoutSuccessUrl("/")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID")
            )
            // Configure CSRF protection.
            .csrf(csrf -> csrf.disable());

        return http.build();
    }

    @Bean
    public FilterRegistrationBean<HiddenHttpMethodFilter> hiddenHttpMethodFilterRegistration() {
        HiddenHttpMethodFilter filter = new HiddenHttpMethodFilter();
        FilterRegistrationBean<HiddenHttpMethodFilter> registrationBean = new FilterRegistrationBean<>(filter);
        registrationBean.setFilter(filter);
        registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registrationBean.addUrlPatterns("/contacts/*", "/contacts/**/*"); // Broader patterns
        return registrationBean;
    }

    @Bean
    public MvcRequestMatcher.Builder mvc(HandlerMappingIntrospector introspector) {
        return new MvcRequestMatcher.Builder(introspector);
    }
}