package dji.sampleV5.aircraft;


import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import dji.sampleV5.aircraft.virtualstick.JoystickBCIController;
import fi.iki.elonen.NanoHTTPD;

public class BCIHttpServer extends NanoHTTPD {

    private final JoystickBCIController controller;

    public BCIHttpServer(int port, JoystickBCIController controller) {
        super(port);
        this.controller = controller;
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        Method method = session.getMethod();

        try {
            // Parse body for POST requests or use query parameters for GET
            Map<String, String> params;
            if (Method.POST.equals(method)) {
                session.parseBody(null);
                params = convertParameters(session.getParameters());
            } else {
                params = session.getQueryParameterString() != null ?
                        parseQuery(session.getQueryParameterString()) :
                        java.util.Collections.emptyMap();
            }

            switch (uri) {
                case "/enable_bci":
                    controller.enableBCIControl(null);
                    return newFixedLengthResponse("BCI enabled");
                case "/disable_bci":
                    controller.disableBCIControl(null);
                    return newFixedLengthResponse("BCI disabled");
                case "/takeoff":
                    controller.initiateTakeoff(null);
                    return newFixedLengthResponse("Takeoff initiated");
                case "/land":
                    controller.initiateLanding(null);
                    return newFixedLengthResponse("Landing initiated");
                case "/fland":
                    controller.completeLanding(null);
                    return newFixedLengthResponse("Finish landing");
                case "/send_control":
                    // Parse control data from parameters
                    float roll = Float.parseFloat(Objects.requireNonNull(params.getOrDefault("roll", "0")));
                    float pitch = Float.parseFloat(Objects.requireNonNull(params.getOrDefault("pitch", "0")));
                    float yaw = Float.parseFloat(Objects.requireNonNull(params.getOrDefault("yaw", "0")));
                    float vertical = Float.parseFloat(Objects.requireNonNull(params.getOrDefault("vertical", "0")));
                    controller.sendBCIControlData(roll, pitch, yaw, vertical);
                    return newFixedLengthResponse("Control sent");
                default:
                    return newFixedLengthResponse("Unknown endpoint");
            }
        } catch (IOException | ResponseException e) {
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT,
                    "Error processing request: " + e.getMessage());
        } catch (NumberFormatException e) {
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT,
                    "Invalid number format in parameters");
        }
    }

    private Map<String, String> parseQuery(String query) {
        Map<String, String> result = new java.util.HashMap<>();
        if (query != null && !query.isEmpty()) {
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                String[] keyValue = pair.split("=", 2);
                if (keyValue.length == 2) {
                    result.put(keyValue[0], keyValue[1]);
                }
            }
        }
        return result;
    }

    private Map<String, String> convertParameters(Map<String, List<String>> parameters) {
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : parameters.entrySet()) {
            List<String> values = entry.getValue();
            if (values != null && !values.isEmpty()) {
                result.put(entry.getKey(), values.get(0)); // Take first value
            }
        }
        return result;
    }

    //remove after testing
    public String getDeviceIpAddress() {
        try {
            for (java.util.Enumeration<java.net.NetworkInterface> en = java.net.NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                java.net.NetworkInterface intf = en.nextElement();
                for (java.util.Enumeration<java.net.InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    java.net.InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof java.net.Inet4Address) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (java.net.SocketException ex) {
            ex.printStackTrace();
        }
        return "127.0.0.1"; // fallback to localhost
    }

}
