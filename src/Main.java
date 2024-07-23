import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {
    public static void main(String[] args) {
        // Sample data
        JSONArray dataArray = new JSONArray();
        dataArray.put(new JSONObject().put("Sno", 1).put("name", "John Doe").put("profession", "Software Engineer"));
        dataArray.put(new JSONObject().put("Sno", 2).put("name", "Jane Smith").put("profession", "Doctor"));
        dataArray.put(new JSONObject().put("Sno", 3).put("name", "Emily Johnson").put("profession", "Teacher"));

        try (ServerSocket server = new ServerSocket(8081)) {
            System.out.println("Server listening on 8081");
            while (true) {
                try (Socket client = server.accept();
                     InputStreamReader isr = new InputStreamReader(client.getInputStream());
                     BufferedReader reader = new BufferedReader(isr);
                     OutputStream os = client.getOutputStream();
                     PrintWriter writer = new PrintWriter(os, true)) {

                    String line = reader.readLine();
                    StringBuilder requestBuilder = new StringBuilder();
                    String clientAddress = client.getInetAddress().getHostAddress();
                    System.out.println("Received request from: " + clientAddress);

                    int contentLength = 0;
                    while (line != null && !line.isEmpty()) {
                        requestBuilder.append(line).append("\r\n");
                        if (line.startsWith("Content-Length:")) {
                            contentLength = Integer.parseInt(line.split(":")[1].trim());
                        }
                        line = reader.readLine();
                    }
                    requestBuilder.append("\r\n");

                    String request = requestBuilder.toString();
                    System.out.println("Request: " + request);

                    if (request.startsWith("POST")) {
                        char[] bodyChars = new char[contentLength];
                        reader.read(bodyChars, 0, contentLength);
                        String body = new String(bodyChars);
                        System.out.println("Body: " + body);

                        JSONObject newPerson = new JSONObject(body);
                        dataArray.put(newPerson);

                        String httpResponse = "HTTP/1.1 200 OK\r\n" +
                                "Content-Type: text/plain\r\n" +
                                "Content-Length: 11\r\n" +
                                "\r\n" +
                                "User Added.";
                        writer.write(httpResponse);
                    } else if (request.startsWith("GET")) {
                        // Send JSON data as a response to GET request
                        String jsonResponse = dataArray.toString();
                        String httpResponse = "HTTP/1.1 200 OK\r\n" +
                                "Content-Type: application/json\r\n" +
                                "Content-Length: " + jsonResponse.length() + "\r\n" +
                                "\r\n" +
                                jsonResponse;
                        writer.write(httpResponse);
                    } else if (request.startsWith("PUT")) {
                        // Read the body of the PUT request
                        String[] headers = request.split("\r\n");
                        for (String header : headers) {
                            if (header.startsWith("Content-Length:")) {
                                contentLength = Integer.parseInt(header.split(":")[1].trim());
                            }
                        }

                        char[] bodyChars = new char[contentLength];
                        reader.read(bodyChars, 0, contentLength);
                        String body = new String(bodyChars);
                        System.out.println("Body: " + body);

                        // Parse the body as JSON and update the person in the array
                        JSONObject updatedPerson = new JSONObject(body);
                        int snoToUpdate = updatedPerson.getInt("Sno");
                        boolean updated = false;

                        for (int i = 0; i < dataArray.length(); i++) {
                            if (dataArray.getJSONObject(i).getInt("Sno") == snoToUpdate) {
                                dataArray.put(i, updatedPerson);
                                updated = true;
                                break;
                            }
                        }

                        String httpResponse;
                        if (updated) {
                            httpResponse = "HTTP/1.1 200 OK\r\n" +
                                    "Content-Type: text/plain\r\n" +
                                    "Content-Length: 11\r\n" +
                                    "\r\n" +
                                    "User Deleted.";
                        } else {
                            httpResponse = "HTTP/1.1 404 Not Found\r\n" +
                                    "Content-Type: text/plain\r\n" +
                                    "Content-Length: 14\r\n" +
                                    "\r\n" +
                                    "User Not Found.";
                        }
                        writer.write(httpResponse);
                    } else if (request.startsWith("DELETE")) {
                        // Read the body of the DELETE request
                        String[] headers = request.split("\r\n");
                        for (String header : headers) {
                            if (header.startsWith("Content-Length:")) {
                                contentLength = Integer.parseInt(header.split(":")[1].trim());
                            }
                        }

                        char[] bodyChars = new char[contentLength];
                        reader.read(bodyChars, 0, contentLength);
                        String body = new String(bodyChars);
                        System.out.println("Body: " + body);

                        // Parse the body as JSON and delete the person from the array
                        JSONObject personToDelete = new JSONObject(body);
                        int snoToDelete = personToDelete.getInt("Sno");
                        boolean deleted = false;

                        for (int i = 0; i < dataArray.length(); i++) {
                            if (dataArray.getJSONObject(i).getInt("Sno") == snoToDelete) {
                                dataArray.remove(i);
                                deleted = true;
                                break;
                            }
                        }

                        String httpResponse;
                        if (deleted) {
                            httpResponse = "HTTP/1.1 200 OK\r\n" +
                                    "Content-Type: text/plain\r\n" +
                                    "Content-Length: 11\r\n" +
                                    "\r\n" +
                                    "User Deleted.";
                        } else {
                            httpResponse = "HTTP/1.1 404 Not Found\r\n" +
                                    "Content-Type: text/plain\r\n" +
                                    "Content-Length: 14\r\n" +
                                    "\r\n" +
                                    "User Not Found.";
                        }
                        writer.write(httpResponse);
                    } else {
                        // Send a response for other types of requests
                        String httpResponse = "HTTP/1.1 400 Bad Request\r\n" +
                                "Content-Type: text/plain\r\n" +
                                "Content-Length: 15\r\n" +
                                "\r\n" +
                                "Unsupported request";
                        writer.write(httpResponse);
                    }

                    writer.flush();
                } catch (IOException e) {
                    System.err.println("Error handling client connection: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }
}
