package com.ryanair;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ryanair.component.IInterconnectionsService;
import com.ryanair.dto.Interconnections;

@RestController
@RequestMapping("/")
public class InterconnectionsController {
	@Autowired
	private IInterconnectionsService interconnectionsService;

	@RequestMapping("/interconnections")
	public List<Interconnections> getInterconnectionsFlights(@RequestParam(value = "departure") String departure,
			@RequestParam(value = "arrival") String arrival,
			@RequestParam(value = "departureDateTime") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime departureDateTime,
			@RequestParam(value = "arrivalDateTime") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime arrivalDateTime) {
		return interconnectionsService.getInterconnectionsFlights(departure, arrival, departureDateTime, arrivalDateTime);
	}
}