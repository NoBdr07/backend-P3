package com.bdr.backend.services;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.bdr.backend.models.dtos.RentalDto;
import com.bdr.backend.models.entities.Rental;
import com.bdr.backend.repositories.RentalRepository;
import com.bdr.backend.utils.DateUtils;

@Service
public class RentalService {
	
	@Autowired
	private RentalRepository rentalRepository;
	
	@Autowired
	private ModelMapper modelMapper;
	
	public List<Rental> getAllRentals() {
		return rentalRepository.findAll();
	}
	
	public Optional<Rental> getRentalById(int id) {
		return rentalRepository.findById(id);
	}
	
	public Rental createRental(String name, Integer surface, Integer price, String picture, String description, Integer ownerId) {
		Rental newRental = new Rental();
		newRental.setName(name);
		newRental.setSurface(surface);
		newRental.setPrice(price);
		newRental.setPicture(picture);
		newRental.setDescription(description);
		newRental.setOwnerId(ownerId);
		rentalRepository.save(newRental);
		
		return newRental;
	}
	
	public Rental updateRental(int rentalId, String name, Integer surface, Integer price, String filePath, String description, Integer owner_id) {
	    Optional<Rental> optionalRental = getRentalById(rentalId);
	    if (!optionalRental.isPresent()) {
	        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Rental not found");
	    }
	    Rental rental = optionalRental.get();
	    if (name != null) {
	        rental.setName(name);
	    }
	    if (surface != null) {
	        rental.setSurface(surface);
	    }
	    if (price != null) {
	        rental.setPrice(price);
	    }
	    if (filePath != null) {
	        rental.setPicture(filePath);
	    }
	    if (description != null) {
	        rental.setDescription(description);
	    }
	    if (owner_id != null) {
	        rental.setOwnerId(owner_id);
	    }
	    
	    rental.setUpdatedAt(DateUtils.formatToMySQLDateTime(new Date()));
	    
	    return rentalRepository.save(rental);
	}
	
	public RentalDto convertToDto(Rental rental) {
		return modelMapper.map(rental, RentalDto.class);
	}
	
	public List<RentalDto> convertListToDto(List<Rental> rentals) {
		return rentals.stream().map(rental -> convertToDto(rental)).toList();
	}

}