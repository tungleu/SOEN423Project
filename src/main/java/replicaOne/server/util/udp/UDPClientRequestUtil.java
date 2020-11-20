package replicaOne.server.util.udp;

import replicaOne.model.Request;
import replicaOne.server.requests.RequestType;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;

import static common.ReplicaConstants.BC_SERVER_NAME;
import static common.ReplicaConstants.QC_SERVER_NAME;
import static replicaOne.server.util.TimeUtil.generateTimestamp;

/**
 * Created by Kevin Tan 2020-09-21
 */
public final class UDPClientRequestUtil {

    public static final int QC_PORT = 1011;
    public static final int BC_PORT = 1012;
    public static final int ON_PORT = 1013;

    public static final int[] PORTS = new int[]{QC_PORT, BC_PORT, ON_PORT};
    static final int MAX_BYTE_BUFFER = 65508;

    private UDPClientRequestUtil() {
    }

    public static String requestFromStore(RequestType requestType, int storePort, String... params) {
        try (DatagramSocket aSocket = new DatagramSocket()) {
            byte[] req = generateUDPRequest(requestType, params);
            if (req != null) {
                DatagramPacket request = new DatagramPacket(req, req.length, InetAddress.getLocalHost(), storePort);
                aSocket.send(request);
                byte[] buffer = new byte[MAX_BYTE_BUFFER];
                DatagramPacket reply = new DatagramPacket(buffer, buffer.length);
                aSocket.receive(reply);
                System.out.println(
                        String.format("%s Received response back from server //%s:%d", generateTimestamp(), request.getAddress().toString(),
                                request.getPort()));
                String responseString = new String(reply.getData());
                System.out.println("Reply: " + responseString);
                return responseString.trim();
            } else {
                throw new Exception("Error: cannot serialize request object.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "Error: No response.";
    }

    public static int getPortForServer(String server) {
        switch (server) {
            case QC_SERVER_NAME:
                return QC_PORT;
            case BC_SERVER_NAME:
                return BC_PORT;
            default:
                return ON_PORT;
        }
    }

    private static byte[] generateUDPRequest(RequestType requestType, String... params) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Request request = new Request(requestType, Arrays.asList(params));
        try (ObjectOutputStream out = new ObjectOutputStream(bos)) {
            out.writeObject(request);
            out.flush();
            return bos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
