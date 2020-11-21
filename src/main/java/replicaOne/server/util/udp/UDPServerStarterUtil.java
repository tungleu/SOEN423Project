package replicaOne.server.util.udp;

import replicaOne.model.ServerInventory;
import replicaOne.server.requests.UDPServerRequestHandler;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import static replicaOne.server.util.TimeUtil.generateTimestamp;

/**
 * Created by Kevin Tan 2020-09-21
 */
public final class UDPServerStarterUtil {

    private UDPServerStarterUtil() {
    }

    public static DatagramSocket startUDPServer(Logger logger, int port, ServerInventory serverInventory, AtomicBoolean atomicBoolean) {
        try {
            DatagramSocket aSocket = new DatagramSocket(port);
            new Thread(() -> {
                UDPServerRequestHandler udpServerRequestHandler = new UDPServerRequestHandler(serverInventory);
                while (atomicBoolean.get()) {
                    try {
                        byte[] buffer = new byte[UDPClientRequestUtil.MAX_BYTE_BUFFER];
                        DatagramPacket request = new DatagramPacket(buffer, buffer.length);
                        System.out.println(
                                String.format("%s UDP Server on port %d ready to receive next request.", generateTimestamp(), port));
                        aSocket.receive(request);
                        System.out.println(String.format("%s UDP Server on port %d request received from //%s:%d, handling on new thread.",
                                                         generateTimestamp(), port, request.getAddress().toString(), request.getPort()));
                        udpServerRequestHandler.handleRequestAsync(aSocket, request);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                logger.info("Terminating UDP server on PORT: " + port);
            }).start();
            return aSocket;
        } catch (SocketException e) {
            logger.info(e.getMessage());
        }
        return null;
    }
}
