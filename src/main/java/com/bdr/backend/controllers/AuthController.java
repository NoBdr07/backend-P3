package com.bdr.backend.controllers;

import java.util.HashMap;
import java.util.Map;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.bdr.backend.models.dtos.UserDto;
import com.bdr.backend.models.entities.User;
import com.bdr.backend.models.requests.LoginRequest;
import com.bdr.backend.models.requests.RegisterRequest;
import com.bdr.backend.services.JwtService;
import com.bdr.backend.services.UserService;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@CrossOrigin(origins = "http://localhost:4200")
@Tag(name = "AuthController", description = "Routes related to authentication")
public class AuthController {

	@Autowired
	private JwtService jwtService;
	
	@Autowired
	private UserService userService;
	
	@Autowired
	private PasswordEncoder passwordEncoder;
	
	/**
	 * Register a new user
	 * 
	 * @param request The request body containing the user's email, password and name
	 * @return ResponseEntity<Map<String, String>>	The response containing the token
	 */
	@PostMapping("api/auth/register")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "User registered successfully", content = @Content(examples = @ExampleObject(value = "{\"token\": \"jwt\"}"))),
			@ApiResponse(responseCode = "400", description = "Input missing", content = @Content(schema = @Schema())), })
	
	public ResponseEntity<Map<String, String>> register(@Valid @RequestBody RegisterRequest request) {
		
		// Create new user in the database
		userService.createUser(request.getEmail(), passwordEncoder.encode(request.getPassword()), request.getName());

		// Generate token
		Map<String, String> tokenResponse = new HashMap<>();
		tokenResponse.put("token", jwtService.generateToken(request.getEmail()));

		return new ResponseEntity<>(tokenResponse, HttpStatus.OK);

	}

	/**
	 * Login a user
	 * 
	 * @param request The request body containing the user's email and password
	 * @return ResponseEntity<Map<String, String>> The response containing the token
	 */
	@PostMapping("api/auth/login")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "User logged in successfully", content = @Content(examples = @ExampleObject(value = "{\"token\": \"jwt\"}"), schema = @Schema())),
			@ApiResponse(responseCode = "401", description = "Invalid input", content = @Content(examples = @ExampleObject(value = "{\"message\": \"error\"}"), schema = @Schema())), })	
	
	public ResponseEntity<Map<String, String>> login(@Valid @RequestBody LoginRequest request) {
		
		User user = userService.getUserByEmail(request.getEmail()).get();
		
		// check if the user exists and the password is correct
		if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
			Map<String, String> errorResponse = new HashMap<>();
	        errorResponse.put("message", "error");
			return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
		}

		Map<String, String> tokenResponse = new HashMap<>();
		tokenResponse.put("token", jwtService.generateToken(user.getEmail()));

		return new ResponseEntity<>(tokenResponse, HttpStatus.OK);
	}

	/**
	 * Get the current user
	 * 
	 * @return UserDto The user information except the password
	 */
	@GetMapping("api/auth/me")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "User info loaded successfully", content = @Content(mediaType = "application/json", 
					examples = @ExampleObject(value = "{\"userId\": \"1\", \"email\": \"test@test.com\", \"name\": \"test\", \"createdAt\": \"2022/02/02\", \"updatedAt\": \"2022/08/02\"}"), 
					schema = @Schema(implementation = UserDto.class))),
			@ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema())), })
	
	public UserDto getCurrentUser() {
	    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

	    if (authentication == null || !(authentication.getPrincipal() instanceof Jwt)) {
	        throw new IllegalStateException("User is not authenticated");
	    }

	    Jwt jwt = (Jwt) authentication.getPrincipal();
	    String login = jwt.getClaim("login");

	    if (login == null) {
	        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Login claim not found in token");
	    }

	    return userService.getUserByEmail(login)
	            .map(userService::convertToDto)
	            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
	}


}
