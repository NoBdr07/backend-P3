package com.bdr.backend.controllers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.bdr.backend.models.dtos.RentalDto;
import com.bdr.backend.models.entities.Rental;
import com.bdr.backend.models.entities.User;
import com.bdr.backend.services.RentalService;
import com.bdr.backend.services.UserService;
import com.bdr.backend.utils.PictureUtils;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@CrossOrigin(origins = "http://localhost:4200")
@Tag(name = "RentalController", description = "Routes related to rentals")
public class RentalController {

	private RentalService rentalService;
	private UserService userService;

	public RentalController(RentalService rentalService, UserService userService) {
		this.rentalService = rentalService;
		this.userService = userService;
	}

	private static final Logger logger = LoggerFactory.getLogger(MessageController.class);

	// Get all rentals
	@GetMapping("api/rentals")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Rentals info loaded successfully", content = @Content(examples = @ExampleObject(value = "{\"rentalId\": \"1\", "
					+ "\"name\": \"maison 1\", " + "\"surface\": \"170\", " + "\"price\": \"540000\","
					+ "\"description\": \"bdr of description\", " + "\"owner_id\": \"1\","
					+ " \"createdAt\": \"2012/12/02\", "
					+ "\"updatedAt\": \"2012/12/02\"},"), schema = @Schema())),
			@ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema())), })
	
	public Map<String, List<RentalDto>> getRentals() {
		Map<String, List<RentalDto>> rentalsDto = rentalService.getAllRentals();

		if (rentalsDto.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No rentals found");
		}
		
		 rentalsDto.forEach((k, v) -> v.forEach(rental -> {
	            rental.setPicture(rentalService.constructFullUrl(rental.getPicture()));
	        }));

		return rentalsDto;
	}

	// Get rental by id
	@GetMapping("api/rentals/{rentalId}")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Rental info loaded successfully", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"rentalId\": \"1\", "
					+ "\"name\": \"maison 1\", " + "\"surface\": \"170\", " + "\"price\": \"540000\","
					+ "\"description\": \"bdr of description\", " + "\"owner_id\": \"1\","
					+ " \"createdAt\": \"2012/12/02\", "
					+ "\"updatedAt\": \"2012/12/02\"} "), schema = @Schema())),
			@ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema())), })

	public RentalDto getRental(@PathVariable int rentalId) {
		Optional<Rental> optionalRental = rentalService.getRentalById(rentalId);

		if (!optionalRental.isPresent()) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Rental not found");
		}

		RentalDto rentalDto = rentalService.convertToDto(optionalRental.get());
        rentalDto.setPicture(rentalService.constructFullUrl(rentalDto.getPicture()));
        logger.info("Picture url: " + rentalDto.getPicture());
        return rentalDto;
	}

	// Create a new Rental
	@PostMapping("api/rentals")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Rental created with success", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"message\": \"Rental created !\"}"))),
			@ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema())), })
	
	public ResponseEntity<Map<String, String>> createRental(@RequestParam(value = "name", required = false) String name,
			@RequestParam(value = "surface", required = false) Integer surface,
			@RequestParam(value = "price", required = false) Integer price,
			@RequestParam(value = "picture", required = false) MultipartFile picture,
			@RequestParam(value = "description", required = false) String description) {
		
		// Add userId since the front-end is not sending it
					Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

					Jwt jwt = (Jwt) authentication.getPrincipal();
					String login = jwt.getClaim("login");
					User user = userService.getUserByEmail(login).get();
					Integer userId = userService.convertToDto(user).getUserId();
					
		// Manage the file upload
		String filePath = PictureUtils.uploadFile(picture);

		Rental newRental = rentalService.createRental(name, surface, price, filePath, description, userId);
		logger.info("Rental created with id: " + newRental.getRentalId());

		Map<String, String> response = new HashMap<>();
		response.put("message", "Rental created !");
		return ResponseEntity.ok(response);
	}

	// Update a rental
	@PutMapping("api/rentals/{rentalId}")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Rental updated !", content = @Content(examples = @ExampleObject(value = "{\"message\": \"Rental updated !\"}"))),
			@ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema())), })
	
	public ResponseEntity<Map<String, String>> updateRental(@PathVariable int rentalId,
			@RequestParam(value = "name", required = false) String name,
			@RequestParam(value = "surface", required = false) Integer surface,
			@RequestParam(value = "price", required = false) Integer price,
			@RequestParam(value = "picture", required = false) MultipartFile picture,
			@RequestParam(value = "description", required = false) String description,
			@RequestParam(value = "owner_id", required = false) Integer owner_id) {
		
		Optional<Rental> optionalRental = rentalService.getRentalById(rentalId);
		
		if (!optionalRental.isPresent()) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Rental not found");
		}
		
		String filePath = PictureUtils.uploadFile(picture);
		
		rentalService.updateRental(rentalId, name, surface, price, filePath, description, owner_id);
		
		Map<String, String> response = new HashMap<>();
		response.put("message", "Rental updated !");
		return ResponseEntity.ok(response);
	}
}
