package client;

import CORBA_FE.*;

import static common.ReplicaConstants.*;

import org.omg.CORBA.ORB;
import org.omg.CosNaming.NamingContextExt;
import org.omg.CosNaming.NamingContextExtHelper;

import java.util.Scanner;

public class Customer {
    private FrontEnd CORBAFrontEnd;
    private String customerID;
    private String province;

    public Customer(String customerID, String province) throws Exception {
        this.customerID = customerID;
        this.province = province;
        String[] arguments = new String[]{"-ORBInitialPort", "1234", "-ORBInitialHost", "localhost"};
        ORB orb = ORB.init(arguments, null);
        org.omg.CORBA.Object objRef = orb.resolve_initial_references("NameService");
        NamingContextExt ncRef = NamingContextExtHelper.narrow(objRef);
        this.CORBAFrontEnd = (FrontEnd) FrontEndHelper.narrow(ncRef.resolve_str(FRONT_END_SERVER_NAME));
    }

    public void purchaseItem(String itemID, String inputDate) {
        String result = this.CORBAFrontEnd.purchaseItem(this.customerID, itemID, inputDate);
        System.out.println(result);
        if (result.indexOf("waitlist") > 0) {
            Scanner scanner = new Scanner(System.in);
            String option = scanner.next();
            if (option.equals("yes")) {
                System.out.println(CORBAFrontEnd.addWaitList(this.customerID, itemID));
            }
        }

    }

    public void findItem(String itemName) {
        System.out.println(this.CORBAFrontEnd.findItem(this.customerID, itemName));
    }

    public void returnItem(String itemID, String inputDate) {
        System.out.println(this.CORBAFrontEnd.returnItem(this.customerID, itemID, inputDate));
    }

    public void exchangeItem(String newItemID, String itemID, String inputDate) {
        System.out.println(this.CORBAFrontEnd.exchangeItem(this.customerID, newItemID, itemID, inputDate));
    }

    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Please choose your province: QC, ON, BC");
        String province = scanner.next();
        System.out.println("Please enter your id");
        String IDNumber = scanner.next();
        String clientID = province + "U" + IDNumber;
        System.out.println("Your ID is :" + clientID);
        Customer customer = new Customer(clientID, province);
        try {
            int customerOption;
            String itemID;
            String inputDate;
            String itemName;
            String newItemID;
            while (true) {
                System.out.println("Please choose your action ");
                System.out.println("1. Purchase Item");
                System.out.println("2. Find Item ");
                System.out.println("3. Return Item ");
                System.out.println("4. Exchange Item ");
                customerOption = scanner.nextInt();
                switch (customerOption) {
                    case 1:
                        System.out.println("PURCHASE SELECTED");
                        System.out.println("Enter item ID");
                        itemID = scanner.next();
                        scanner.nextLine();
                        System.out.println("Enter the date of purchase in this form: ddmmyyyy ");
                        inputDate = scanner.nextLine();
                        customer.purchaseItem(itemID, inputDate);
                        break;
                    case 2:
                        System.out.println("FIND ITEM SELECTED");
                        System.out.println("Enter the name of item:");
                        itemName = scanner.next();
                        customer.findItem(itemName);
                        break;
                    case 3:
                        System.out.println("RETURN ITEM SELECTED");
                        System.out.println("Enter item ID");
                        itemID = scanner.next();
                        scanner.nextLine();
                        System.out.println("Enter the date of return in this form: ddmmyyyy ");
                        inputDate = scanner.nextLine();
                        customer.returnItem(itemID, inputDate);
                        break;
                    case 4:
                        System.out.println("EXCHANGE ITEM SELECTED");
                        System.out.println("Enter old item ID:");
                        itemID = scanner.next();
                        scanner.nextLine();
                        System.out.println("Enter new item ID:");
                        newItemID = scanner.next();
                        scanner.nextLine();
                        System.out.println("Enter the date of return in this form: ddmmyyyy ");
                        inputDate = scanner.nextLine();
                        customer.exchangeItem(newItemID, itemID, inputDate);
                        break;
                }
            }
        } catch (Exception e) {
            System.out.println("ERROR : " + e);
            e.printStackTrace(System.out);
        }
    }
}

