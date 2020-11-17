package client;

import CORBA_FE.*;

import static common.ReplicaConstants.*;

import org.omg.CORBA.ORB;
import org.omg.CosNaming.NamingContextExt;
import org.omg.CosNaming.NamingContextExtHelper;

import java.util.Scanner;

public class Manager {
    private final String province;
    private final FrontEnd CORBAFrontEnd;
    private final String managerID;

    public Manager(String managerID, String province) throws Exception {
        this.managerID = managerID;
        this.province = province;
        String[] arguments = new String[]{"-ORBInitialPort", "1234", "-ORBInitialHost", "localhost"};
        ORB orb = ORB.init(arguments, null);
        org.omg.CORBA.Object objRef = orb.resolve_initial_references("NameService");
        NamingContextExt ncRef = NamingContextExtHelper.narrow(objRef);
        this.CORBAFrontEnd = (FrontEnd) FrontEndHelper.narrow(ncRef.resolve_str(FRONT_END_SERVER_NAME));


    }

    public void addItem(String itemID, String itemName, int quantity, int price) {
        System.out.println(CORBAFrontEnd.addItem(this.managerID, itemID, itemName, quantity, price));
    }

    public void removeItem(String itemID, int quantity) {
        System.out.println(CORBAFrontEnd.removeItem(this.managerID, itemID, quantity));
    }

    public void listItemAvailablity() {
        System.out.println(CORBAFrontEnd.listItemAvailability(this.managerID));
    }

    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Please choose your province: QC, ON, BC");
        String province = scanner.next();
        System.out.println("Please enter your id");
        String IDNumber = scanner.next();
        String clientID = province + "M" + IDNumber;
        System.out.println("Your ID is :" + clientID);
        Manager manager = new Manager(clientID, province);
        try {
            int customerOption;
            String itemID;
            String itemName;
            int price;
            int quantity;
            while (true) {
                System.out.println("Please choose your action ");
                System.out.println("1. Add Item");
                System.out.println("2. Remove Item ");
                System.out.println("3. List Item ");
                customerOption = scanner.nextInt();
                switch (customerOption) {
                    case 1:
                        System.out.println("ADD ITEM SELECTED");
                        System.out.println("Enter item ID");
                        itemID = scanner.next();
                        System.out.println("Enter item name");
                        itemName = scanner.next();
                        System.out.println("Enter price");
                        price = scanner.nextInt();
                        System.out.println("Enter quantity");
                        quantity = scanner.nextInt();
                        manager.addItem(itemID, itemName, quantity, price);
                        break;
                    case 2:
                        System.out.println("Remove ITEM SELECTED");
                        System.out.println("Enter item ID");
                        itemID = scanner.next();
                        System.out.println("Enter quantity");
                        quantity = scanner.nextInt();
                        manager.removeItem(itemID, quantity);
                        break;
                    case 3:
                        System.out.println("LIST ITEM AVAILABILITY SELECTED");
                        manager.listItemAvailablity();
                        break;
                }
            }
        } catch (Exception e) {
            System.out.println("ERROR : " + e);
            e.printStackTrace(System.out);
        }
    }
}
