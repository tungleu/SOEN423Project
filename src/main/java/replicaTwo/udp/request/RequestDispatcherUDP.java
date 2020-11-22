package replicaTwo.udp.request;

import replicaTwo.udp.data.DataHandlerUDP;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class RequestDispatcherUDP implements RequestDispatcher {
    private static class StoreClientUDP implements Callable<List<String>> {
        private final List<String> args;
        private final int destinationPort;
        public StoreClientUDP(List<String> args, int destinationPort) {
            this.args = new ArrayList<>(args);
            this.destinationPort = destinationPort;
        }

        @Override
        public List<String> call() {
            try (DatagramSocket aSocket = new DatagramSocket()) {
                InetAddress aHost = InetAddress.getByName("localhost");
                byte[] buf = DataHandlerUDP.marshall(this.args);
                DatagramPacket request = new DatagramPacket(buf, buf.length, aHost, this.destinationPort);
                aSocket.send(request);

                byte[] buffer = new byte[20000];
                DatagramPacket reply = new DatagramPacket(buffer, buffer.length);
                aSocket.receive(reply);

                return DataHandlerUDP.unmarshall(reply);
            } catch (SocketException e) {
                System.out.println("Socket: " + e.getMessage());
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("IO: " + e.getMessage());
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    private final Map<String, DatagramSocket> socketGroup;
    public RequestDispatcherUDP(String locationName, Map<String, DatagramSocket> portsConfig) {
        super();
        this.socketGroup = new HashMap<>(portsConfig);
        this.socketGroup.remove(locationName);
    }

    @Override
    public List<String> unicast(List<String> args, String destinationName) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<List<String>> future = executor.submit(new StoreClientUDP(args, this.socketGroup.get(destinationName).getLocalPort()));
        List<String> result = new ArrayList<>();
        try {
            result.addAll(future.get());
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        } finally {
            executor.shutdown();
        }
        return result;
    }

    @Override
    public List<String> broadcastCollect(List<String> args) {
        ExecutorService executor = Executors.newWorkStealingPool();
        List<StoreClientUDP> storeCallables = this.prepareGroupCallables(args);
        List<String> collectedResuls = new ArrayList<>();
        try {
            collectedResuls.addAll(executor.invokeAll(storeCallables)
                    .stream()
                    .map(future -> {
                        List<String> requestedItems = new ArrayList<>();
                        try {
                            requestedItems.addAll(future.get());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        return requestedItems;
                    })
                    .flatMap(List::stream)
                    .collect(Collectors.toList()));
            executor.shutdown();
            executor.awaitTermination(60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return collectedResuls;
    }

    private List<StoreClientUDP> prepareGroupCallables(List<String> args) {
        return this.socketGroup.values().stream()
                .map(socket -> new StoreClientUDP(args, socket.getLocalPort()))
                .collect(Collectors.toList());
    }
}
