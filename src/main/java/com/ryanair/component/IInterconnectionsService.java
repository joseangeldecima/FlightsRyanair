package com.ryanair.component;

import java.time.LocalDateTime;
import java.util.List;

import com.ryanair.dto.Interconnections;

public interface IInterconnectionsService {
	/**
	 * Main method whom process the service
	 * 
	 * @param departure
	 * @param arrival
	 * @param departureDateTime
	 * @param arrivalDateTime
	 * @return
	 */
	public List<Interconnections> getInterconnectionsFlights(String departure, String arrival,
			LocalDateTime departureDateTime, LocalDateTime arrivalDateTime);
}
