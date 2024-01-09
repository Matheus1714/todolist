package br.com.matheusmota.todolist.filter;

import java.io.IOException;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import at.favre.lib.crypto.bcrypt.BCrypt;
import br.com.matheusmota.todolist.user.IUserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class FilterTaskAuth extends OncePerRequestFilter{

    @Autowired
    private IUserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String servletPath = request.getServletPath();

        if (isTaskEndpoint(servletPath)) {
            processTaskEndpoint(request, response, filterChain);
        } else {
            filterChain.doFilter(request, response);
        }
    }

    private boolean isTaskEndpoint(String servletPath) {
        return servletPath.equals("/tasks/");
    }

    private void processTaskEndpoint(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws IOException, ServletException {
        String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader != null && authorizationHeader.startsWith("Basic")) {
            String credentials = extractCredentials(authorizationHeader);

            if (credentials != null) {
                String[] usernamePassword = credentials.split(":");
                String username = usernamePassword[0];
                String password = usernamePassword[1];

                authenticateUser(username, password, request, response, filterChain);
            } else {
                sendErrorResponse(response, 401);
            }
        } else {
            sendErrorResponse(response, 401);
        }
    }

    private String extractCredentials(String authorizationHeader) {
        try {
            String encodedCredentials = authorizationHeader.substring("Basic".length()).trim();
            byte[] decodedCredentials = Base64.getDecoder().decode(encodedCredentials);
            return new String(decodedCredentials);
        } catch (Exception e) {
            return null;
        }
    }

    private void authenticateUser(String username, String password, HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws IOException, ServletException {
        var user = this.userRepository.findByUsername(username);

        if (user != null && BCrypt.verifyer().verify(password.toCharArray(), user.getPassword()).verified) {
            request.setAttribute("idUser", user.getId());
            filterChain.doFilter(request, response);
        } else {
            sendErrorResponse(response, 401);
        }
    }

    private void sendErrorResponse(HttpServletResponse response, int errorCode) throws IOException {
        response.sendError(errorCode);
    }
}
