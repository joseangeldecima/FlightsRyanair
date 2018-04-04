package com.ryanair.dto;

import java.util.List;

public class Schedule {

	private Integer month;
	private List<Days> days;

	public Integer getMonth() {
		return month;
	}

	public void setMonth(Integer month) {
		this.month = month;
	}

	public List<Days> getDays() {
		return days;
	}

	public void setDays(List<Days> days) {
		this.days = days;
	}

}
