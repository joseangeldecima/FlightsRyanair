package com.ryanair.dto;

import java.util.ArrayList;
import java.util.List;

public class Interconnections {

	public Interconnections() {
		legs = new ArrayList<>();
	}
	private Integer stops;
	private List<Legs> legs;
	
	public Integer getStops() {
		return stops;
	}
	public void setStops(Integer stops) {
		this.stops = stops;
	}
	public List<Legs> getLegs() {
		return legs;
	}
	public void setLegs(List<Legs> legs) {
		this.legs = legs;
	}
	
	
}
