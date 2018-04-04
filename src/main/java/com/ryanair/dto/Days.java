package com.ryanair.dto;

import java.util.List;

public class Days {

	private Integer day;
	private List<Flight> flights;

	public Integer getDay() {
		return day;
	}

	public void setDay(Integer day) {
		this.day = day;
	}

	public List<Flight> getFlights() {
		return flights;
	}

	public void setFlights(List<Flight> flights) {
		this.flights = flights;
	}

}
