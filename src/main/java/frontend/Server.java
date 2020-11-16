package frontend;

import CORBA_FE.FrontEndHelper;

import static common.ReplicaConstants.*;

import org.omg.CORBA.ORB;
import org.omg.CosNaming.NameComponent;
import org.omg.CosNaming.NamingContextExt;
import org.omg.CosNaming.NamingContextExtHelper;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;


import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    public static void main(String[] args) {
        try {

            String[] arguments = new String[]{"-ORBInitialPort", "1234", "-ORBInitialHost", "localhost"};
            ORB orb = ORB.init(arguments, null);

            POA rootpoa = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
            rootpoa.the_POAManager().activate();

            Map<String, Response> responseMap = new ConcurrentHashMap<>();
            FrontEnd server = new FrontEnd(FRONT_END_SERVER_NAME, orb, responseMap);

            org.omg.CORBA.Object ref = rootpoa.servant_to_reference(server);
            CORBA_FE.FrontEnd href = (CORBA_FE.FrontEnd) FrontEndHelper.narrow(ref);

            org.omg.CORBA.Object objRef =
                    orb.resolve_initial_references("NameService");

            NamingContextExt ncRef = NamingContextExtHelper.narrow(objRef);

            String name = FRONT_END_SERVER_NAME;
            NameComponent path[] = ncRef.to_name(name);
            ncRef.rebind(path, href);
            orb.run();
        } catch (Exception e) {
            System.err.println("ERROR: " + e);
            e.printStackTrace(System.out);
        }
    }
}
