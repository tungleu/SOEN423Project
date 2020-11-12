package replicaTwo.udp.data;

import java.net.DatagramPacket;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class DataHandlerUDP {
    public static final String UDP_DELIM = "/";

    public static byte[] marshall(List<String> args) {
        String str = String.join(UDP_DELIM, args);
        return str.getBytes();
    }


    public static List<String> unmarshall(DatagramPacket request) {
        String str = new String(request.getData(), request.getOffset(), request.getLength());
        String[] strs = str.split(UDP_DELIM);
        return Arrays.stream(strs).collect(Collectors.toList());
    }
}
