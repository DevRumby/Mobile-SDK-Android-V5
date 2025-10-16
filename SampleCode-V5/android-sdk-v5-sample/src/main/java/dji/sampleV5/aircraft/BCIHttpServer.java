package dji.sampleV5.aircraft;


import android.content.Context;
import android.widget.Toast;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import dji.sampleV5.aircraft.virtualstick.JoystickBCIController;
import fi.iki.elonen.NanoHTTPD;

public class BCIHttpServer extends NanoHTTPD {

    private final JoystickBCIController controller;

    private Context context;


    public BCIHttpServer(int port, JoystickBCIController controller, Context context) {
        super(port);
        this.controller = controller;
        this.context = context;
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        Method method = session.getMethod();
        if (!"/".equals(uri) && !isAuthenticated(session)) {
            Response response = newFixedLengthResponse(Response.Status.UNAUTHORIZED, MIME_PLAINTEXT, "Authentication required");
            response.addHeader("WWW-Authenticate", "Basic realm=\"BCI Control\"");
            return response;
        }

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
                case "/test":
                    return newFixedLengthResponse("Server is working! Context is " + (context != null ? "initialized" : "null"));
                case "/debug":
                    StringBuilder debugInfo = new StringBuilder();
                    debugInfo.append("=== RC2 Controller Debug Info ===" + System.lineSeparator());
                    debugInfo.append("Server Status: OK" + System.lineSeparator());
                    debugInfo.append("Context: ").append(context != null ? "initialized" : "null").append(System.lineSeparator());
                    debugInfo.append("Controller: ").append(controller != null ? "initialized" : "null").append(System.lineSeparator());
                    
                    if (controller != null) {
                        try {
                            debugInfo.append("BCI Active: ").append(controller.isBCIControlActive()).append(System.lineSeparator());
                            debugInfo.append("Virtual Stick Enabled: ").append(controller.isVirtualStickEnabled()).append(System.lineSeparator());
                            debugInfo.append("Controller Status: ").append(controller.getControllerStatus()).append(System.lineSeparator());
                        } catch (Exception e) {
                            debugInfo.append("Controller Error: ").append(e.getMessage()).append(System.lineSeparator());
                        }
                    }
                    
                    // Add SDK Manager info
                    try {
                        debugInfo.append("SDK Registered: ").append(dji.v5.manager.SDKManager.getInstance().isRegistered()).append(System.lineSeparator());
                    } catch (Exception e) {
                        debugInfo.append("SDK Check Error: ").append(e.getMessage()).append(System.lineSeparator());
                    }
                    
                    debugInfo.append("Platform: RC2 Controller" + System.lineSeparator());
                    debugInfo.append("Aircraft: Mini 4 Pro" + System.lineSeparator());
                    return newFixedLengthResponse(Response.Status.OK, "text/plain; charset=utf-8", debugInfo.toString());
                case "/camera_debug":
                    // Camera debug logging endpoint - accessible without ADB
                    try {
                        String cameraDebugInfo = CameraDebug.getCameraDebugInfo();
                        
                        // Check for clear parameter
                        if ("true".equals(params.get("clear"))) {
                            cameraDebugInfo += System.lineSeparator() + CameraDebug.clearCameraDebugLogs() + System.lineSeparator();
                        }
                        
                        return newFixedLengthResponse(Response.Status.OK, "text/plain; charset=utf-8", cameraDebugInfo);
                    } catch (Exception e) {
                        return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain; charset=utf-8", 
                            "Error generating camera debug info: " + e.getMessage());
                    }
                case "/logs":
                    // Simple logging endpoint for RC2 debugging
                    StringBuilder logs = new StringBuilder();
                    logs.append("=== Recent Log Messages ===" + System.lineSeparator());
                    try {
                        if (controller != null) {
                            // Test if we can call a simple method
                            boolean bciActive = controller.isBCIControlActive();
                            logs.append("Controller method call test: SUCCESS (BCI Active: ").append(bciActive).append(")" + System.lineSeparator());
                        } else {
                            logs.append("Controller is null" + System.lineSeparator());
                        }
                    } catch (Exception e) {
                        logs.append("Controller method call test: FAILED - ").append(e.getMessage()).append(System.lineSeparator());
                    }
                    return newFixedLengthResponse(Response.Status.OK, "text/plain; charset=utf-8", logs.toString());
                case "/send_control_simple":
                    // Simplified version for testing
                    return newFixedLengthResponse("Simple control endpoint works: params=" + params.toString());
                case "/send_control":
                    // Wrap the entire endpoint in a try-catch to prevent empty responses
                    try {
                        if (controller == null) {
                            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Controller is null");
                        }
                        
                        // Parse parameters - this might be where it's failing
                        String rollStr = params.getOrDefault("roll", "0");
                        String pitchStr = params.getOrDefault("pitch", "0");
                        String yawStr = params.getOrDefault("yaw", "0");
                        String verticalStr = params.getOrDefault("vertical", "0");
                        
                        // Test parsing
                        float roll = Float.parseFloat(rollStr);
                        float pitch = Float.parseFloat(pitchStr);
                        float yaw = Float.parseFloat(yawStr);
                        float vertical = Float.parseFloat(verticalStr);
                        controller.sendBCIControlData(roll, pitch, yaw, vertical);
                        
                        // Don't call the actual control method yet - just return success
                        return newFixedLengthResponse("Parameters parsed successfully: r=" + roll + " p=" + pitch + " y=" + yaw + " v=" + vertical);
                        
                    } catch (Throwable t) {
                        // Catch any throwable to prevent empty responses
                        return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, 
                            "Throwable caught: " + t.getClass().getSimpleName() + " - " + t.getMessage());
                    }
                default:
                    return newFixedLengthResponse("Unknown endpoint");
            }
        } catch (IOException | ResponseException e) {
            if (context != null) {
                Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT,
                    "Error processing request: " + e.getMessage());
        } catch (NumberFormatException e) {
            if (context != null) {
                Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT,
                    "Invalid number format in parameters");
        }
    }

    private boolean isAuthenticated(IHTTPSession session) {
        String authHeader = session.getHeaders().get("authorization");
        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            return false;
        }

        try {
            String encoded = authHeader.substring(6); // Remove "Basic " prefix
            String decoded = new String(android.util.Base64.decode(encoded, android.util.Base64.DEFAULT));
            String[] credentials = decoded.split(":", 2);

            if (credentials.length != 2) {
                return false;
            }

            String username = credentials[0];
            String password = credentials[1];

            //Update to be encrypted in a property file
            return "BCITeam".equals(username) && "DronesRCool".equals(password);

        } catch (Exception e) {
            return false;
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
