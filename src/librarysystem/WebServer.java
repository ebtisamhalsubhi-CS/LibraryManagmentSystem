package librarysystem;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

public class WebServer {

    private static final int MAX_ACCOUNTS = 1000;

    private static final Map<String, User> users = new ConcurrentHashMap<>();
    private static final Map<String, LibrarySystem> libraries = new ConcurrentHashMap<>();
    private static final Map<String, String> sessions = new ConcurrentHashMap<>();
    private static final SecureRandom random = new SecureRandom();
    private static Path webDir;

    public static void main(String[] args) throws IOException {

        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        webDir = Paths.get(System.getenv().getOrDefault("WEB_DIR", "web")).toAbsolutePath().normalize();

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api/", WebServer::handleApi);
        server.createContext("/", WebServer::handleStatic);
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();

        System.out.println("Library website is running on http://localhost:" + port);
    }

    private static void handleApi(HttpExchange ex) throws IOException {

        try {
            String path = ex.getRequestURI().getPath();
            String method = ex.getRequestMethod();
            Map<String, String> in = method.equals("GET") ? parse(ex.getRequestURI().getRawQuery()) : parse(readBody(ex));

            if (path.equals("/api/signup") && method.equals("POST")) {
                signup(ex, in);
                return;
            }

            if (path.equals("/api/login") && method.equals("POST")) {
                login(ex, in);
                return;
            }

            String username = currentUser(ex);

            if (path.equals("/api/me")) {
                send(ex, 200, username == null ? "{\"user\":null}" : "{\"user\":" + str(username) + "}");
                return;
            }

            if (username == null) {
                error(ex, 401, "Please log in first");
                return;
            }

            LibrarySystem system = libraries.get(username);

            synchronized (system) {
                route(ex, path, method, in, system);
            }
        }
        catch (NumberFormatException e) {
            error(ex, 400, "Please enter correct input");
        }
        catch (Exception e) {
            e.printStackTrace();
            error(ex, 500, "Something went wrong");
        }
    }

    private static void route(HttpExchange ex, String path, String method, Map<String, String> in, LibrarySystem system) throws IOException {

        if (path.equals("/api/logout") && method.equals("POST")) {
            String token = cookie(ex, "session");
            if (token != null) {
                sessions.remove(token);
            }
            ex.getResponseHeaders().add("Set-Cookie", "session=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax");
            send(ex, 200, "{\"ok\":true}");
            return;
        }

        if (path.equals("/api/books") && method.equals("GET")) {
            send(ex, 200, books(system.getBooks()));
            return;
        }

        if (path.equals("/api/books") && method.equals("POST")) {
            int id = number(in, "id");
            String title = text(in, "title");
            String author = text(in, "author");
            int pages = number(in, "pages");

            if (title.isEmpty() || author.isEmpty()) {
                error(ex, 422, "Title and author are required");
            }
            else if (id <= 0 || pages <= 0) {
                error(ex, 422, "Book ID and pages must be positive numbers");
            }
            else if (system.searchById(id) != null) {
                error(ex, 422, "A book with this ID already exists");
            }
            else {
                system.addBook(new PrintedBook(id, title, author, pages));
                send(ex, 201, "{\"ok\":true,\"message\":\"Book added successfully\"}");
            }
            return;
        }

        if (path.equals("/api/books/search") && method.equals("GET")) {
            String by = in.getOrDefault("by", "title");
            String q = text(in, "q");
            List<Book> found = new ArrayList<>();

            if (by.equals("id")) {
                Book b = system.searchById(Integer.parseInt(q));
                if (b != null) {
                    found.add(b);
                }
            }
            else if (by.equals("author")) {
                found = system.findByAuthor(q);
            }
            else {
                found = system.findByTitle(q);
            }

            send(ex, 200, books(found));
            return;
        }

        if (path.startsWith("/api/books/")) {
            int id = Integer.parseInt(path.substring("/api/books/".length()));

            if (method.equals("PUT") || method.equals("POST")) {
                String title = text(in, "title");
                String author = text(in, "author");

                if (title.isEmpty() || author.isEmpty()) {
                    error(ex, 422, "Title and author are required");
                }
                else if (system.updateBook(id, title, author)) {
                    send(ex, 200, "{\"ok\":true,\"message\":\"Book updated successfully\"}");
                }
                else {
                    error(ex, 404, "Book not found");
                }
                return;
            }

            if (method.equals("DELETE")) {
                Book b = system.searchById(id);

                if (b != null && !b.isAvailable()) {
                    error(ex, 422, "Return this book before deleting it");
                }
                else if (system.deleteBook(id)) {
                    send(ex, 200, "{\"ok\":true,\"message\":\"Book deleted successfully\"}");
                }
                else {
                    error(ex, 404, "Book not found");
                }
                return;
            }
        }

        if (path.equals("/api/members") && method.equals("GET")) {
            StringBuilder sb = new StringBuilder("[");
            for (Member m : system.getMembers()) {
                if (sb.length() > 1) {
                    sb.append(',');
                }
                int borrowed = 0;
                for (BorrowRecord r : system.getRecords()) {
                    if (r.getMemberId() == m.getMemberId() && !r.isReturned()) {
                        borrowed++;
                    }
                }
                sb.append("{\"id\":").append(m.getMemberId())
                  .append(",\"name\":").append(str(m.getName()))
                  .append(",\"contact\":").append(str(m.getContactDetails()))
                  .append(",\"borrowed\":").append(borrowed).append('}');
            }
            send(ex, 200, sb.append(']').toString());
            return;
        }

        if (path.equals("/api/members") && method.equals("POST")) {
            int id = number(in, "id");
            String name = text(in, "name");
            String phone = text(in, "phone");

            if (name.isEmpty() || phone.isEmpty()) {
                error(ex, 422, "Name and phone are required");
            }
            else if (id <= 0) {
                error(ex, 422, "Member ID must be a positive number");
            }
            else if (system.searchMemberById(id) != null) {
                error(ex, 422, "A member with this ID already exists");
            }
            else {
                system.addMember(new Member(id, name, phone));
                send(ex, 201, "{\"ok\":true,\"message\":\"Member added successfully\"}");
            }
            return;
        }

        if (path.equals("/api/loans") && method.equals("GET")) {
            StringBuilder sb = new StringBuilder("[");
            List<BorrowRecord> records = system.getRecords();
            for (int i = records.size() - 1; i >= 0; i--) {
                BorrowRecord r = records.get(i);
                Book b = system.searchById(r.getBookId());
                Member m = system.searchMemberById(r.getMemberId());
                if (sb.length() > 1) {
                    sb.append(',');
                }
                sb.append("{\"bookId\":").append(r.getBookId())
                  .append(",\"title\":").append(str(b == null ? "Deleted book" : b.getTitle()))
                  .append(",\"memberId\":").append(r.getMemberId())
                  .append(",\"member\":").append(str(m == null ? "Unknown member" : m.getName()))
                  .append(",\"borrowed\":").append(str(r.getBorrowDate().toString()))
                  .append(",\"due\":").append(str(r.getDueDate().toString()))
                  .append(",\"returned\":").append(r.getReturnedAt() == null ? "null" : str(r.getReturnedAt().toString()))
                  .append('}');
            }
            send(ex, 200, sb.append(']').toString());
            return;
        }

        if (path.equals("/api/loans") && method.equals("POST")) {
            int bookId = number(in, "bookId");
            int memberId = number(in, "memberId");
            Book b = system.searchById(bookId);

            if (b == null) {
                error(ex, 404, "Book not found");
            }
            else if (system.searchMemberById(memberId) == null) {
                error(ex, 404, "Member not found");
            }
            else if (system.borrowBook(bookId, memberId)) {
                send(ex, 201, "{\"ok\":true,\"message\":\"Book borrowed successfully\"}");
            }
            else {
                error(ex, 422, "Book is not available");
            }
            return;
        }

        if (path.equals("/api/returns") && method.equals("POST")) {
            if (system.returnBook(number(in, "bookId"))) {
                send(ex, 200, "{\"ok\":true,\"message\":\"Book returned successfully\"}");
            }
            else {
                error(ex, 404, "Borrow record not found");
            }
            return;
        }

        error(ex, 404, "Not found");
    }

    private static void signup(HttpExchange ex, Map<String, String> in) throws IOException {

        String username = text(in, "username");
        String password = in.getOrDefault("password", "");

        if (username.length() < 3 || username.length() > 30) {
            error(ex, 422, "Username must be 3 to 30 characters");
        }
        else if (password.length() < 6) {
            error(ex, 422, "Password must be at least 6 characters");
        }
        else if (users.size() >= MAX_ACCOUNTS) {
            error(ex, 503, "The library is full right now, please try later");
        }
        else if (users.putIfAbsent(username.toLowerCase(), new User(username.toLowerCase(), password)) != null) {
            error(ex, 422, "This username is already taken");
        }
        else {
            libraries.put(username.toLowerCase(), new LibrarySystem());
            startSession(ex, username.toLowerCase());
        }
    }

    private static void login(HttpExchange ex, Map<String, String> in) throws IOException {

        String username = text(in, "username").toLowerCase();
        User user = users.get(username);

        if (user != null && user.login(username, in.getOrDefault("password", ""))) {
            startSession(ex, username);
        }
        else {
            error(ex, 401, "Wrong username or password");
        }
    }

    private static void startSession(HttpExchange ex, String username) throws IOException {

        byte[] bytes = new byte[24];
        random.nextBytes(bytes);
        StringBuilder token = new StringBuilder();
        for (byte b : bytes) {
            token.append(String.format("%02x", b));
        }
        sessions.put(token.toString(), username);
        ex.getResponseHeaders().add("Set-Cookie", "session=" + token + "; Path=/; HttpOnly; SameSite=Lax");
        send(ex, 200, "{\"user\":" + str(username) + "}");
    }

    private static String currentUser(HttpExchange ex) {

        String token = cookie(ex, "session");
        return token == null ? null : sessions.get(token);
    }

    private static String cookie(HttpExchange ex, String name) {

        List<String> headers = ex.getRequestHeaders().get("Cookie");
        if (headers == null) {
            return null;
        }
        for (String header : headers) {
            for (String part : header.split(";")) {
                String[] kv = part.trim().split("=", 2);
                if (kv.length == 2 && kv[0].equals(name)) {
                    return kv[1];
                }
            }
        }
        return null;
    }

    private static String books(List<Book> list) {

        StringBuilder sb = new StringBuilder("[");
        for (Book b : list) {
            if (sb.length() > 1) {
                sb.append(',');
            }
            int pages = b instanceof PrintedBook ? ((PrintedBook) b).getPages() : 0;
            sb.append("{\"id\":").append(b.getBookId())
              .append(",\"title\":").append(str(b.getTitle()))
              .append(",\"author\":").append(str(b.getAuthor()))
              .append(",\"pages\":").append(pages)
              .append(",\"status\":").append(str(b.getStatus().name()))
              .append('}');
        }
        return sb.append(']').toString();
    }

    private static int number(Map<String, String> in, String key) {

        return Integer.parseInt(in.getOrDefault(key, "").trim());
    }

    private static String text(Map<String, String> in, String key) {

        String value = in.getOrDefault(key, "").trim();
        return value.length() > 120 ? value.substring(0, 120) : value;
    }

    private static String readBody(HttpExchange ex) throws IOException {

        try (InputStream body = ex.getRequestBody()) {
            byte[] data = body.readNBytes(10000);
            return new String(data, StandardCharsets.UTF_8);
        }
    }

    private static Map<String, String> parse(String raw) {

        Map<String, String> map = new HashMap<>();
        if (raw == null || raw.isEmpty()) {
            return map;
        }
        for (String pair : raw.split("&")) {
            String[] kv = pair.split("=", 2);
            String key = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
            String value = kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "";
            map.put(key, value);
        }
        return map;
    }

    private static String str(String s) {

        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                case '<': sb.append("\\u003c"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    }
                    else {
                        sb.append(c);
                    }
            }
        }
        return sb.append('"').toString();
    }

    private static void error(HttpExchange ex, int code, String message) throws IOException {

        send(ex, code, "{\"error\":" + str(message) + "}");
    }

    private static void send(HttpExchange ex, int code, String json) throws IOException {

        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.getResponseHeaders().set("Cache-Control", "no-store");
        ex.sendResponseHeaders(code, bytes.length);
        try (OutputStream out = ex.getResponseBody()) {
            out.write(bytes);
        }
    }

    private static void handleStatic(HttpExchange ex) throws IOException {

        String path = ex.getRequestURI().getPath();
        if (path.equals("/")) {
            path = "/index.html";
        }

        Path file = webDir.resolve(path.substring(1)).normalize();

        if (!file.startsWith(webDir) || !Files.isRegularFile(file)) {
            file = webDir.resolve("index.html");
        }

        String name = file.getFileName().toString();
        String type = "text/html; charset=utf-8";
        if (name.endsWith(".css")) {
            type = "text/css; charset=utf-8";
        }
        else if (name.endsWith(".js")) {
            type = "text/javascript; charset=utf-8";
        }
        else if (name.endsWith(".svg")) {
            type = "image/svg+xml";
        }

        byte[] bytes = Files.readAllBytes(file);
        ex.getResponseHeaders().set("Content-Type", type);
        ex.sendResponseHeaders(200, bytes.length);
        try (OutputStream out = ex.getResponseBody()) {
            out.write(bytes);
        }
    }
}
