package util;

import model.UDPRequestMessage;
import org.jgroups.Address;
import org.jgroups.Message;

import javax.annotation.Nullable;
import java.io.Serializable;

public final class MessageUtil {

    private MessageUtil() {
    }

    public static UDPRequestMessage messageToUDPRequest(Message message) {
        return message.getObject(UDPRequestMessage.class.getClassLoader());
    }

    public static Message createMessageFor(@Nullable Address dst, Serializable ob) {
        return new Message(dst, ob);
    }

    public static String fetchTargetStore(UDPRequestMessage requestMessage) {
        return requestMessage.getParameters().get(0).substring(0, 2);
    }

}
