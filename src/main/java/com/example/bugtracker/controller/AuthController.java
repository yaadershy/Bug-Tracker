package com.example.bugtracker.controller;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    // simple in-memory users: username -> password
    private final Map<String, String> users = Map.of(
            "admin", "password",
            "dev", "devpass");

    // token -> username
    private final Map<String, String> sessions = Collections.synchronizedMap(new HashMap<>());

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        if (username == null || password == null)
            return ResponseEntity.badRequest().build();

        String expected = users.get(username);
        if (expected != null && expected.equals(password)) {
            String token = UUID.randomUUID().toString();
            sessions.put(token, username);
            return ResponseEntity.ok(Map.of("token", token, "username", username));
        } else {
            return ResponseEntity.status(401).build();
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader(name = "Authorization", required = false) String auth) {
        String token = extract(auth);
        if (token != null)
            sessions.remove(token);
        return ResponseEntity.noContent().build();
    }

    // helper
    private String extract(String auth) {
        if (auth == null)
            return null;
        auth = auth.trim();
        if (auth.startsWith("Bearer "))
            return auth.substring(7);
        return auth;
    }

    // utility endpoint to resolve token -> username (used by frontend)
    @GetMapping("/whoami")
    public ResponseEntity<Map<String, String>> whoami(
            @RequestHeader(name = "Authorization", required = false) String auth) {
        String token = extract(auth);
        String user = token == null ? null : sessions.get(token);
        if (user == null)
            return ResponseEntity.status(401).build();
        return ResponseEntity.ok(Map.of("username", user));
    }

    // expose a method to be used by other controllers (not a great pattern for
    // prod, but fine here)
    public String usernameFromToken(String authHeader) {
        String t = extract(authHeader);
        return t == null ? null : sessions.get(t);
    }
}
