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
                // Permit public access to the root path, login page, and error page.
                .requestMatchers("/", "/contacts/login", "/error").permitAll()
                // Permit public access to static resources (images, webjars, CSS, JS).
                .requestMatchers("/images/**", "/webjars/**", "/css/**", "/js/**").permitAll()
                // Require authentication for all paths under /contacts/.
                .requestMatchers("/contacts/**").authenticated()
                // Specifically require authentication for DELETE requests to /contacts/**.
                .requestMatchers(new AntPathRequestMatcher("/contacts/**", "DELETE")).authenticated()
                // Require authentication for any other request not explicitly defined above.
                .anyRequest().authenticated()
            )
            // Configure OAuth2 Login.
            .oauth2Login(oauth2 -> oauth2
                // Set the login page URL.
                .loginPage("/contacts/login")
                // Set the default success URL after login, always redirecting even after direct access to login page.
                .defaultSuccessUrl("/contacts", true)
            )
            // Configure Logout functionality.
            .logout(logout -> logout
                // Define the logout request path and method (POST for logout).
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout", "POST"))
                // Set the URL to redirect to after successful logout.
                .logoutSuccessUrl("/")
                // Invalidate the HTTP session upon logout.
                .invalidateHttpSession(true)
                // Clear the authentication context.
                .clearAuthentication(true)
                // Delete the JSESSIONID cookie to fully logout.
                .deleteCookies("JSESSIONID")
            )
            // Configure CSRF protection.
            .csrf(csrf -> csrf.disable()); // Disable CSRF for now - **SECURITY WARNING: In production, CSRF should be enabled.**

        return http.build();
    }

    @Bean
    public FilterRegistrationBean<HiddenHttpMethodFilter> hiddenHttpMethodFilterRegistration() {
        // Create a HiddenHttpMethodFilter to allow HTTP method override via _method parameter in forms.
        HiddenHttpMethodFilter filter = new HiddenHttpMethodFilter();
        FilterRegistrationBean<HiddenHttpMethodFilter> registrationBean = new FilterRegistrationBean<>(filter);
        registrationBean.setFilter(filter);
        // Ensure the filter is executed first in the filter chain.
        registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        // Apply the HiddenHttpMethodFilter to /contacts/* and all sub-paths under /contacts/.
        registrationBean.addUrlPatterns("/contacts/*", "/contacts/**/*"); // Broader patterns
        return registrationBean;
    }

    @Bean
    public MvcRequestMatcher.Builder mvc(HandlerMappingIntrospector introspector) {
        // Bean to provide MvcRequestMatcher builder for request matching within Spring MVC context.
        return new MvcRequestMatcher.Builder(introspector);
    }
}