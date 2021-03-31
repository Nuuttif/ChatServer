package com.nuutti.chatserver;

import com.sun.net.httpserver.*;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.nio.charset.StandardCharsets;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class ChatHandler implements HttpHandler {

	private String responseBody = "";
	private ArrayList<ChatMessage> messages = new ArrayList<ChatMessage>();
	final String format = "UTF-8";

	@Override
	public void handle(HttpExchange exchange) throws IOException {
		int code = 200;

		try {
				System.out.println("Request thread: " + Thread.currentThread().getId());
			if (exchange.getRequestMethod().equalsIgnoreCase("POST")) {
				code = handleChatMessage(exchange);
			} else if (exchange.getRequestMethod().equalsIgnoreCase("GET")) {
				code = handleGetRequest(exchange);
			} else {
				code = 400;
				responseBody = "Request type not supported.";
			}

			if (code < 200 || code > 299) {
				ChatServer.log("Error in ChatHandler: " + code + " " + responseBody);
				byte [] bytes;
				bytes = responseBody.toString().getBytes(format);
				exchange.sendResponseHeaders(code, bytes.length);
				OutputStream os = exchange.getResponseBody();
				os.write(bytes);
				os.close();
			}

		} catch (Exception e) {
			code = 500;
			responseBody = "Error in handling the request: " + e.getMessage();
			ChatServer.log("Error in handling the request: " + e.getMessage());
		}
	}

	// POST request
	private int handleChatMessage(HttpExchange exchange) throws Exception {
		int code = 200;
		int contentLength = 0;
		String contentType = "";

		try {
		Headers headers = exchange.getRequestHeaders();

			if (headers.containsKey("Content-Length")) {
				contentLength = Integer.parseInt(headers.get("Content-Length").get(0));
			} else {
				code = 411;
				responseBody = "No Content-Length";
				return code;
			}

			if (headers.containsKey("Content-Type")) {
				contentType = headers.get("Content-Type").get(0);
			} else {
				code = 400;
				responseBody = "No content type in request.";
				return code;
			}

			if (contentType.equalsIgnoreCase("application/json")) {
				InputStream stream = exchange.getRequestBody();
				String text = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
				.lines()
				.collect(Collectors.joining("\n"));
				ChatServer.log(text);
				stream.close();

				JSONObject message = new JSONObject(text);
				String dateStr = message.getString("sent");
				OffsetDateTime odt = OffsetDateTime.parse(dateStr);

				String user = message.getString("user");
				String mes = message.getString("message");

				if (user.trim().length() > 0 && mes.trim().length() > 0) {
					ChatDatabase database = ChatDatabase.getInstance();
					database.addMessage(new ChatMessage(odt.toLocalDateTime(), user, mes));
					exchange.sendResponseHeaders(code, -1);
				} else {
					code = 400;
					responseBody = "Message can't be empty";
				}

			} else {
				code = 411;
				responseBody = "Content-Type must be application/json";
				ChatServer.log(responseBody);
			}

		} catch (Exception e){
			code = 500;
			responseBody = "Error in handling the request: " + e.getMessage();
			ChatServer.log("Error in handling a POST request: " + e.getMessage());
			return code;
		}
		return code;
	}
	
		//GET Request
		private int handleGetRequest(HttpExchange exchange) throws IOException {
			int code = 200;

			try {
				Headers headers = exchange.getRequestHeaders(); 
				long messagesSince = -1;
				DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss z");

				if (headers.containsKey("If-Modified-Since")) {
					ZonedDateTime lastmodified = ZonedDateTime.parse(headers.get("If-Modified-Since").get(0), formatter);
					LocalDateTime fromWhichDate = lastmodified.toLocalDateTime();
					messagesSince = fromWhichDate.toInstant(ZoneOffset.UTC).toEpochMilli();
				}
				ChatDatabase database = ChatDatabase.getInstance();
				messages = database.getMessages(messagesSince);

				if (messages.size() <= 1) {
					code = 204;
					exchange.sendResponseHeaders(code, -1);
					return code;
				}

				JSONArray responseMessages = new JSONArray();
				formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");
				LocalDateTime newest = null;
				for (ChatMessage message : messages) {
					if (newest == null) newest = message.sent;
					if (newest.compareTo(message.sent) < 0) {
						newest = message.sent;
					}

					ZonedDateTime utcSentTime = message.sent.atZone(ZoneId.of("UTC"));
					JSONObject obj = new JSONObject();
				    obj.put("user", message.user).put("message", message.message).put("sent",utcSentTime.format(formatter));
       			    responseMessages.put(obj);
				}
				
				// Format latest message time for headers
				formatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss z");
				ZonedDateTime utcSentTime = newest.atZone(ZoneId.of("GMT"));
				headers = exchange.getResponseHeaders();
				headers.add("Last-Modified", utcSentTime.format(formatter));

				ChatServer.log("Delivering " + messages.size() + " messages to client");
				byte [] bytes;
				bytes = responseMessages.toString().getBytes(format);
				exchange.sendResponseHeaders(code, bytes.length);
				OutputStream os = exchange.getResponseBody();
				os.write(bytes);
				os.close();
				return code;

			} catch (Exception e){
				code = 500;
				responseBody = "Error in GET: " + e.getMessage();
				ChatServer.log("Error in GET: " + e.getMessage());
				return code;
			}
		}
}


