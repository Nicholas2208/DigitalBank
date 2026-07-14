package com.nwhite.authuser.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service, responsible for interacting with the Auth0 Management API.
 *
 * <p>This class encapsulates all calls to Auth0's Management API for:</p>
 * <ul>
 *     <li>Creating new database (Username-Password-Authentication) users</li>
 *     <li>Assigning roles to users after creation</li>
 * </ul>
 *
 * <p>By centralizing Auth0 API calls we ensure that:</p>
 * <ul>
 *     <li>Only this service knows about the Management API credentials</li>
 *     <li>All microservices can delegate identity provisioning to this service instead of calling Auth0 directly</li>
 *     <li>Our code follows the principal of least privilege - one service holds admin capabilities</li>
 * </ul>
 */
@Service
public class Auth0UserService {
    @Value("${auth0.domain}")
    private String domain;

    private final ManagementTokenService tokens;
    private final RestTemplate rt = new RestTemplate();

    /**
     * Constructor injection of the ManagementTokenService, which is responsible for
     * obtaining a valid Management API access token using the Client Credentials flow
     *
     * @param tokens service, that provides the Bearer token for Auth0 Management API calls
     */
    public Auth0UserService(ManagementTokenService tokens) {
        this.tokens = tokens;
    }

    /**
     * Creates a new user in Auth0's Username-Password-Authentication database connection.
     *
     * <p>This is typically called after a customer completes KYC in CustomerService.
     * The method uses the Management API to provision the user with an email, password,
     * username (mapped to customerId) and optional custom metadata.</p>
     *
     * <h3>HTTP Request</h3>
     * POST {domain}/api/v2/users
     *
     * @param email         the user's email
     * @param password      the initial password
     * @param customerId    external customer ID (used as username and stored in app_metadata)
     * @return a {@link Map} containing Auth0's created user object (user_id, email, etc)
     */
    public Map createDbUser(String email, String password, String customerId) {
        // Retrieve the Management API bearer token
        String auth = tokens.getBearer();

        // Set up HTTP headers
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.set("Authorizaton", auth);

        // Build the request body for the new user
        Map<String, Object> body = new HashMap<>();
        List<String> roles = new ArrayList<>();
        body.put("email", email);
        body.put("password", password);
        body.put("username", customerId); // customerId is used as a username
        body.put("connection", "Username-Password-Authentication");

        // Include custom metadata (e.g. customer_id) to link Auth0 user back to domain model
        Map<String, Object> appMeta = new HashMap<>();
        appMeta.put("customer_id", customerId);

        body.put("app_metadata", appMeta);

        // Call Auth0's Management API to create the user
        ResponseEntity<Map> resp = rt.postForEntity(
                domain + "/api/v2/users",
                    new HttpEntity<>(body, h),
                    Map.class
        );

        // Throw an exception if creation failed
        if (!resp.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Auth0 user creation failed: " + resp.getStatusCode());
        }

        Map createdUser = resp.getBody();
        String userId = (String) createdUser.get("user_id"); // e.g. auth0|abc123
        if (userId == null || userId.isBlank()) {
            throw new IllegalStateException("Auth0 user created, but user_id is missing");
        }

        assignRole(userId, "rol_c7PHGjx2QtuPyVBE", auth);

        return resp.getBody();
    }

    private void assignRole(String userId, String roleId, String bearer) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.set("Authorizaton", bearer);

        Map<String, Object> body = Map.of("roles", List.of(roleId));

        String url = domain + "/api/v2/users/" + userId + "/roles"; // use raw user_id e.g. auth0|xxxxx

        rt.postForEntity(
                url,
                new HttpEntity<>(body, h),
                Void.class
        );

    }
}
