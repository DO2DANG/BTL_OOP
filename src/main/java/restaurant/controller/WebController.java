package restaurant.controller;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;

public interface WebController {
    void register(HttpServer server) throws IOException;
}
