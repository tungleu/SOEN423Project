package replicaOne.server.util.udp;

import replicaOne.model.ServerInventory;
import replicaOne.server.requests.UDPServerRequestHandler;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

import static replicaOne.server.util.TimeUtil.generateTimestamp;

/**
 * Created by Kevin Tan 2020-09-21
 */
public final class UDPServerStarterUtil {

    private UDPServerStarterUtil() {
    }

    public static void startUDPServer(int port, ServerInventory serverInventory) {
        new Thread(() -> {
            try (DatagramSocket aSocket = new DatagramSocket(port)) {
                UDPServerRequestHandler udpServerRequestHandler = new UDPServerRequestHandler(serverInventory);
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
