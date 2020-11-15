package replicaOne.server.util.udp;

import replicaOne.model.ServerInventory;
import replicaOne.server.logger.ServerLogger;
import replicaOne.server.requests.UDPServerRequestHandler;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

import static replicaOne.server.util.TimeUtil.generateTimestamp;

/**
 * Created by Kevin Tan 2020-09-21
 */
final class UDPServerStarterUtil {

    private UDPServerStarterUtil() {
    }

    static void startUDPServer(int port, ServerInventory serverInventory, ServerLogger serverLogger) {
        new Thread(() -> {
            try (DatagramSocket aSocket = new DatagramSocket(port)) {
                UDPServerRequestHandler udpServerRequestHandler = new UDPServerRequestHandler(serverInventory, serverLogger);
                while (true) {
                    byte[] buffer = new byte[UDPClientRequestUtil.MAX_BYTE_BUFFER];
                    DatagramPacket request = new DatagramPacket(buffer, buffer.length);
                    System.out.println(String.format("%s UDP Server on port %d ready to receive next request.", generateTimestamp(), port));
                    aSocket.receive(request);
                    System.out.println(String.format("%s UDP Server on port %d request received from //%s:%d, handling on new thread.",
                            generateTimestamp(), port, request.getAddress().toString(), request.getPort()));
                    udpServerRequestHandler.handleRequestAsync(aSocket, request);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
