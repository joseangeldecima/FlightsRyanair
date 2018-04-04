package com.ryanair.component;

import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.ryanair.dto.Days;
import com.ryanair.dto.Flight;
import com.ryanair.dto.Interconnections;
import com.ryanair.dto.Legs;
import com.ryanair.dto.Route;
import com.ryanair.dto.Schedule;

@Component
public class InterconnectionsServiceImpl implements IInterconnectionsService {

	public static final String RYANAIR_API_URI = "https://api.ryanair.com/";
	public static final String URI_ROUTES = "/core/3/routes";
	public static final String URI_SCHEDULES = "/timetable/3/schedules/";

	private Set<Route> fromAirports;
	private Set<Route> toAirports;
	private Set<Route> directFlights;
	private List<Interconnections> listInterconnections;
	private LocalDateTime departureDateTime;
	private LocalDateTime arrivalDateTime;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.ryanair.component.IInterconnectionsService#getInterconnectionsFlights(
	 * java.lang.String, java.lang.String, java.time.LocalDateTime,
	 * java.time.LocalDateTime)
	 */
	@Override
	public List<Interconnections> getInterconnectionsFlights(String departure, String arrival, LocalDateTime departDate,
			LocalDateTime arrivalDate) {

		initFields(departDate, arrivalDate);

		// get all routes
		getRoutes(departure, arrival);

		// getting direct flights
		getSchedules(directFlights);
		checkScheduleDirectFligths();

		// to avoid too many calls to api service, filter routes without possibilities
		// to be OK
		filterWrongRoutes();

		// getting fights with one stop
		getSchedules(fromAirports);
		getSchedules(toAirports);
		checkScheduleInterconnectedFligths();

		return listInterconnections;
	}

	/**
	 * Reset global variables for each execution
	 * 
	 * @param depart
	 * @param arrival
	 */
	private void initFields(LocalDateTime depart, LocalDateTime arrival) {
		fromAirports = new HashSet<>();
		toAirports = new HashSet<>();
		directFlights = new HashSet<>();
		listInterconnections = new ArrayList<>();
		departureDateTime = depart;
		arrivalDateTime = arrival;
	}

	/**
	 * get the schedule for each route
	 * 
	 * @param flights
	 */
	private void getSchedules(Set<Route> flights) {

		for (Route r : flights) {
			Schedule schedule;
			try {
				schedule = new RestTemplate().getForObject(
						RYANAIR_API_URI + URI_SCHEDULES + "{departure}/{arrival}/years/{year}/months/{month}",
						Schedule.class, r.getAirportFrom(), r.getAirportTo(), departureDateTime.getYear(),
						departureDateTime.getMonthValue());
				r.setSchedule(schedule);
			} catch (RestClientException e) {
				Logger.getGlobal().log(Level.WARNING,
						() -> String.format("%s. Could not retrieve flight from :%s to:%s", e.getMessage(),
								r.getAirportFrom(), r.getAirportTo()));
			}
		}

	}

	/**
	 * Call the service to obtain all routes
	 * 
	 * @param departure
	 * @param arrival
	 */
	private void getRoutes(String departure, String arrival) {

		Route[] routes = new RestTemplate().getForObject(RYANAIR_API_URI + URI_ROUTES, Route[].class);

		if (null != routes) {
			for (Route route : routes) {
				// only empty connecting airports
				if (null == route.getConnectingAirport() || "".equals(route.getConnectingAirport())) {
					getlValidRoutes(departure, arrival, route);
				}

			}
		}
	}

	/**
	 * get the valid routes from all possible routes
	 * 
	 * @param departure
	 * @param arrival
	 * @param route
	 */
	private void getlValidRoutes(String departure, String arrival, Route route) {
		// direct flights
		if (departure.equals(route.getAirportFrom()) && arrival.equals(route.getAirportTo())) {
			directFlights.add(route);
		}
		// flights with the required depart airport
		if (departure.equals(route.getAirportFrom())) {
			fromAirports.add(route);
		}
		// flights with the required arrival airport
		if (arrival.equals(route.getAirportTo())) {
			toAirports.add(route);
		}
	}

	/**
	 * remove all routes which can't connect between departure and arrival airports
	 */
	private void filterWrongRoutes() {
		Set<Route> routesOrigOk = new HashSet<>();
		Set<Route> routesDestOk = new HashSet<>();

		for (Route routeFrom : fromAirports) {
			for (Route routeTo : toAirports) {
				if (routeFrom.getAirportTo().equals(routeTo.getAirportFrom())) {
					routesOrigOk.add(routeFrom);
					routesDestOk.add(routeTo);
					break;
				}
			}
		}
		fromAirports.clear();
		fromAirports.addAll(routesOrigOk); // refill the original Set

		toAirports.clear();
		toAirports.addAll(routesDestOk); // refill the original Set
	}

	/**
	 * if the fly is direct, we check the schedules. If it's ok --> add to response
	 */
	private void checkScheduleDirectFligths() {

		for (Route r : directFlights) { // for each route
			if (null != r.getSchedule() && null != r.getSchedule().getDays()) {
				for (Days d : r.getSchedule().getDays()) { // for each day of the month
					for (Flight f : d.getFlights()) { // for each flight of the day

						// getting the date/hour of the flights
						LocalDateTime depart = createDate(departureDateTime, d.getDay(), r.getSchedule().getMonth(),
								f.getDepartureTime());
						LocalDateTime arrival = createDate(arrivalDateTime, d.getDay(), r.getSchedule().getMonth(),
								f.getArrivalTime());

						addDirectFlightToResponse(r, depart, arrival);
					}
				}
			}
		}

	}

	private void addDirectFlightToResponse(Route r, LocalDateTime depart, LocalDateTime arrival) {
		if (!depart.isBefore(departureDateTime) && !arrival.isAfter(arrivalDateTime)) {
			// is a match, add to response
			Interconnections interconnections = new Interconnections();
			interconnections.getLegs().add(createLeg(r.getAirportFrom(), r.getAirportTo(), depart, arrival));
			interconnections.setStops(interconnections.getLegs().size() - 1);
			listInterconnections.add(interconnections);
			Logger.getGlobal().log(Level.INFO,
					() -> String.format("Route added from:%s to:%s", r.getAirportFrom(), r.getAirportTo()));

		}
	}

	/**
	 * for each non direct fly, we check the schedule and compare with any possible
	 * connections. If it's ok --> add to response with both flights
	 */
	private void checkScheduleInterconnectedFligths() {

		for (Route routeFrom : fromAirports) { // for each route whom starts in origin
			if (null != routeFrom.getSchedule() && null != routeFrom.getSchedule().getDays()) {
				for (Days dayFrom : routeFrom.getSchedule().getDays()) { // for each day of the month
					for (Flight flightFrom : dayFrom.getFlights()) { // for each flight of the day
						// getting the date/hour of the flights
						LocalDateTime departFrom = createDate(departureDateTime, dayFrom.getDay(),
								routeFrom.getSchedule().getMonth(), flightFrom.getDepartureTime());
						LocalDateTime arrivalFrom = createDate(arrivalDateTime, dayFrom.getDay(),
								routeFrom.getSchedule().getMonth(), flightFrom.getArrivalTime());

						// search all arrival airports to check his schedules
						findArrivalAirport(routeFrom, departFrom, arrivalFrom);
					}
				}
			}
		}

	}

	/**
	 * @param routeFrom
	 * @param departFrom
	 * @param arrivalFrom
	 */
	private void findArrivalAirport(Route routeFrom, LocalDateTime departFrom, LocalDateTime arrivalFrom) {
		// convert set to map
		Map<String, Route> mapFromSet = toAirports.stream().collect(Collectors.toMap(Route::getAirportFrom, r -> r));

		Route routeTo = mapFromSet.get(routeFrom.getAirportTo()); // get the interconnected flight
		if (null != routeTo && null != routeTo.getSchedule() && null != routeTo.getSchedule().getDays()) {
			for (Days dayTo : routeTo.getSchedule().getDays()) { // for each day of the month
				for (Flight flightTo : dayTo.getFlights()) { // for each flight of the day
					// getting the date/hour of the flights
					LocalDateTime departTo = createDate(departureDateTime, dayTo.getDay(),
							routeTo.getSchedule().getMonth(), flightTo.getDepartureTime());
					LocalDateTime arrivalTo = createDate(arrivalDateTime, dayTo.getDay(),
							routeTo.getSchedule().getMonth(), flightTo.getArrivalTime());

					addInterconnectedFlightsToResponse(routeFrom, departFrom, arrivalFrom, routeTo, departTo,
							arrivalTo);
				}
			}
		}
	}

	private void addInterconnectedFlightsToResponse(Route routeFrom, LocalDateTime departFrom,
			LocalDateTime arrivalFrom, Route routeTo, LocalDateTime departTo, LocalDateTime arrivalTo) {
		if (!departFrom.isBefore(departureDateTime) && !arrivalFrom.plusHours(2).isAfter(departTo)
				&& !arrivalTo.isAfter(arrivalDateTime)) {
			// is a match, add to response
			Interconnections interconnections = new Interconnections();
			interconnections.getLegs()
					.add(createLeg(routeFrom.getAirportFrom(), routeFrom.getAirportTo(), departFrom, arrivalFrom));
			interconnections.getLegs()
					.add(createLeg(routeTo.getAirportFrom(), routeTo.getAirportTo(), departTo, arrivalTo));
			interconnections.setStops(interconnections.getLegs().size() - 1);
			listInterconnections.add(interconnections);
			Logger.getGlobal().log(Level.INFO,
					() -> String.format("Route added from:%s to:%s and from :%s to:%s", routeFrom.getAirportFrom(),
							routeFrom.getAirportTo(), routeTo.getAirportFrom(), routeTo.getAirportTo()));
		}
	}

	/**
	 * create a "leg" with the airports and his schedule
	 * 
	 * @param airportFrom
	 * @param airportTo
	 * @param depart
	 * @param arrival
	 * @return
	 */
	private Legs createLeg(String airportFrom, String airportTo, LocalDateTime depart, LocalDateTime arrival) {
		Legs leg = new Legs();
		leg.setDepartureAirport(airportFrom);
		leg.setDepartureDateTime(depart.format(DateTimeFormatter.ISO_DATE_TIME));
		leg.setArrivalAirport(airportTo);
		leg.setArrivalDateTime(arrival.format(DateTimeFormatter.ISO_DATE_TIME));
		return leg;
	}

	/**
	 * Generate a LocalDateTime object
	 * 
	 * @param dateFlight
	 * @param day
	 * @param month
	 * @param hourMinute
	 * @return
	 */
	private LocalDateTime createDate(LocalDateTime dateFlight, Integer day, Integer month, String hourMinute) {
		return LocalDateTime.of(dateFlight.getYear(), Month.of(month), day, Integer.valueOf(hourMinute.substring(0, 2)),
				Integer.valueOf(hourMinute.substring(3)));
	}

}
