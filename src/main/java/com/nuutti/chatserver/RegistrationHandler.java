package com.nuutti.chatserver;

import com.sun.net.httpserver.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import org.json.JSONObject;


public class RegistrationHandler implements HttpHandler {

    ChatAuthenticator ca = null;
    private String responseBody = "";
	final String format = "UTF-8";

    public RegistrationHandler(ChatAuthenticator ca) {
        this.ca = ca;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
		int code = 200;

		try {
			if (exchange.getRequestMethod().equalsIgnoreCase("POST")) {
				code = handleRegistration(exchange);
			} else {
				code = 400;
				responseBody = "Request type not supported.";
			}
		} catch (Exception e) {
			code = 500;
			responseBody = "Error in handling the request: " + e.getMessage();
		}
		if (code < 200 || code > 299) {
			ChatServer.log("Error in RegistrationHandler: " + code + " " + responseBody);
			byte [] bytes = responseBody.toString().getBytes(format);
			exchange.sendResponseHeaders(code, bytes.length);
			OutputStream os = exchange.getResponseBody();
			os.write(bytes);
			os.close();
		}
    }

    private int handleRegistration(HttpExchange exchange) throws Exception {

        int code = 200;
		Headers headers = exchange.getRequestHeaders();
		int contentLength = 0;
		String contentType = "";

		if (headers.containsKey("Content-Length")) {
			contentLength = Integer.parseInt(headers.get("Content-Length").get(0));
		} else {
			code = 411;
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
			JSONObject registrationMsg = new JSONObject(text);

			try {
				String username = registrationMsg.getString("username");
				String password = registrationMsg.getString("password");
				String email = registrationMsg.getString("email");

				if (username.trim().length() > 0 && password.trim().length() > 0 && email.trim().length() > 0) {
						ChatDatabase database = ChatDatabase.getInstance();
						if (database.addUser(username, password, email)) {
                            exchange.sendResponseHeaders(code, -1);
                            ChatServer.log("New user added.");
						} else {
							code = 400;
							responseBody = "Username taken";
							ChatServer.log(responseBody);
						}
				} else {
					code = 400;
					responseBody = "Username, password and email cannot be empty";
					ChatServer.log(responseBody);
				}

			} catch (Exception e){
				code = 500;
				responseBody = "Error in POST: " + e.getMessage();
				return code;
			}

		} else {
			code = 411;
			responseBody = "Content-Type must be application/json";
			ChatServer.log(responseBody);
			return code;
		}
		return code;
	}
}
