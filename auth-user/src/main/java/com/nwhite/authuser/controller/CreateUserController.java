package com.nwhite.authuser.controller;

import com.nwhite.authuser.dto.CreateUserRequest;
import com.nwhite.authuser.service.Auth0UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST controller, responsible for provisioning new users to Auth0.
 *
 * <p>This controller exposes an internal administrative endpoint, used by backend
 * services (such as customer-service after KYC approval) to create new database
 * users in Auth0. It is secured with RBAC and requires the caller to have the
 * {@code admin:users.write} permission on their access token.</p>
 *
 * <p>Design notes:</p>
 * <ul>
 *     <li>This controller is a part of the "identity-admin" service and is the only component
 *     allowed to interact directly with the Auth0 Management API.</li>
 *     <li>Access tokens must include the {@code SCOPE_admin:users.write} authority for the
 *     request to be accepted.</li>
 *     <li>The service uses client credentials flow for M2M calls and should never be
 *     exposed to front-end clients directly.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1")
public class CreateUserController {
    private final Auth0UserService auth0;

    /**
     * Constructs a {@code CreateUserController} with the provided {@link Auth0UserService}
     *
     * @param auth0 the service component, that communicates with Auth0 Management API
     */
    public CreateUserController(Auth0UserService auth0) {
        this.auth0 = auth0;
    }

    @PreAuthorize("hasAuthority('SCOPE_admin:users.write')")
    @PostMapping("/iam/users")
    public ResponseEntity<Map> createUser(@RequestBody CreateUserRequest req) {
        Map u = auth0.createDbUser(req.getEmail(), req.getPassword(), req.getCustomerId());
        return ResponseEntity.ok(u);
    }
}
