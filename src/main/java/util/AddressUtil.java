package util;

import org.jgroups.Address;
import org.jgroups.JChannel;

public final class AddressUtil {

    private AddressUtil() {
    }

    public static Address findAddressForGivenName(JChannel jChannel, String name) {
        return jChannel.view().getMembers().stream().filter(address -> name.equals(address.toString())).findFirst().get();
    }

    public static Address findReplicaAddress(JChannel jChannel, String replicaName){
        return jChannel.view().getMembers().stream().filter(address -> {
            String name = address.toString();
            return name.contains(replicaName);
        }).findFirst().get();
    }

    public static Address fetchAddressForDataTransfer(String rmName, String replicaName, JChannel channel) {
        return channel.view().getMembers().stream().filter(address -> {
            String name = address.toString();
            return name.contains(replicaName) && !name.contains(rmName);
        }).findFirst().get();
    }
}
