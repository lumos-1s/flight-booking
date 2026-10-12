package com.xs.ai.services;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.xs.ai.data.BookingStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;

@Service
public class BookingTools {

	private static final Logger logger = LoggerFactory.getLogger(BookingTools.class);

	@Autowired
	private FlightBookingService flightBookingService;

	public record BookingDetailsRequest(String bookingNumber, String name) {
	}

	public record ChangeBookingDatesRequest(String bookingNumber, String name, String date, String from, String to) {
	}

	public record CancelBookingRequest(@ToolParam(description = "预定号")
									   String bookingNumber,
									   @ToolParam(description = "姓名")
									   String name) {
	}

	@JsonInclude(Include.NON_NULL)
	public record BookingDetails(String bookingNumber, String name, LocalDate date, BookingStatus bookingStatus,
			String from, String to, String bookingClass) {
	}


	@Tool(description = "获取机票预定详细信息")
	BookingDetails getBookingDetails( @ToolParam(description = "预定号")
									  String bookingNumber,
									  @ToolParam(description = "姓名")
									  String name) {
		try {
			return flightBookingService.getBookingDetails(bookingNumber,name);
		}
		catch (Exception e) {
			logger.warn("Booking details: {}", NestedExceptionUtils.getMostSpecificCause(e).getMessage());
			return new BookingDetails(bookingNumber, name, null, null, null, null, null);
		}
	}


	@Tool(description = "取消机票预定")
	String cancelBooking(@ToolParam(description = "预定号")
						 String bookingNumber,
						 @ToolParam(description = "姓名")
						 String name) {
		flightBookingService.cancelBooking(bookingNumber,
				name );

		return "退订成功";
	}

}
