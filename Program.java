import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;
// import java.util.Map;
// import java.util.LinkedHashMap;
import java.net.URLDecoder;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import com.github.pemistahl.lingua.api.*;
//import static com.github.pemistahl.lingua.api.Language.*; 

import org.json.*;

public class Program {

    private static void runServer(HttpHandler handler) throws Exception {
        int port = 8080;
        String envPort = System.getenv("LINGUA_API_PORT");
        if(envPort != null && !envPort.isEmpty()) {
            port = Integer.parseInt(envPort);
        }
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", handler);
        server.setExecutor(null);
        System.out.printf("Listening @ %d\n", port);
        server.start();
    }

    public static void main(String[] args) throws Exception {
        runServer(new LinguaHandler());
    }

    static class LinguaHandler implements HttpHandler {

        private final LanguageDetector detector;

        public LinguaHandler() {
            System.out.println("Wait. Loading models...");
            detector = LanguageDetectorBuilder.fromAllLanguages().withPreloadedLanguageModels().build();
        }

        private void makePredictionAndAbort(HttpExchange t, String qw) throws Exception {
            if(qw == null || qw.isEmpty()) {
                t.sendResponseHeaders(400, -1);
                return;
            }

            var vals = detector.computeLanguageConfidenceValues(qw);

            JSONArray a = new JSONArray();
            for (var entry : vals.entrySet()) {
                //isoVals.put(entry.getKey().getIsoCode639_1().toString(), entry.getValue());

                JSONObject jsonObj = new JSONObject();
                jsonObj.put("lang", entry.getKey().getIsoCode639_1().toString());
                jsonObj.put("score", Math.round(entry.getValue()*100)/100);
                a.put(jsonObj);
            }
            
            var res = a.toString();

            t.sendResponseHeaders(200, res.length());
            OutputStream os = t.getResponseBody();
            os.write(res.getBytes());
            os.close();
        }

        private void handleGet(HttpExchange t) throws Exception {
            var uri = t.getRequestURI();
            var query = uri.getRawQuery();
            if(query == null) {
                t.sendResponseHeaders(400, -1);
                return;
            }
            String qw = "";
            for(String el : query.split("&")) {
                var parts = el.split("=");
                if(parts.length != 2) {
                    continue;
                }
                if(!parts[0].equals("q")) {
                    continue;
                }
                qw = URLDecoder.decode(parts[1], "UTF-8");
            }

            makePredictionAndAbort(t, qw);
        }

        private void handlePost(HttpExchange t) throws Exception {
            var i = t.getRequestBody();
            var body = i.readAllBytes();
            var str = new String(body);
            i.close();
            var j = new JSONObject(str);
            var q = (String)j.get("q");
            makePredictionAndAbort(t, q);
        }

        @Override
        public void handle(HttpExchange t) throws IOException {
            try {
                var method = t.getRequestMethod();
                switch(method) {
                case "GET":
                    handleGet(t);
                    break;
                case "POST":
                    handlePost(t);
                    break;
                default:
                    t.sendResponseHeaders(404, -1);
                    break;
                }
            } catch(URISyntaxException | JSONException ex) {
                System.out.println(ex);
                t.sendResponseHeaders(400, -1);
                return;
            } catch(Exception ex) {
                System.out.println(ex);
                t.sendResponseHeaders(500, -1);
                return;
            }
        }
    }

}
