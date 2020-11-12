package util;

import jdk.internal.jline.internal.Nullable;
import model.UDPRequestMessage;
import model.UDPResponseMessage;
import org.jgroups.Address;
import org.jgroups.Message;

import java.io.Serializable;

public final class MessageUtil {

    private MessageUtil() {
    }

    public static UDPRequestMessage messageToUDPRequest(Message message) {
        return message.getObject(UDPRequestMessage.class.getClassLoader());
    }

    public static UDPResponseMessage messageToUDPResponse(Message message) {
        return message.getObject(UDPResponseMessage.class.getClassLoader());
    }

    public static Message createMessageFor(@Nullable Address dst, Serializable ob) {
        return new Message(dst, ob);
    }

}
