package replicaThree.client;
import java.util.HashMap;

public class customerClient{
    private final String customerID;
    private int budget = 1000;
    private HashMap<String, Boolean> eligibility = new HashMap<String, Boolean>();
    public customerClient(String customerID){
        this.customerID = customerID;
        this.eligibility.put("QC",true);
        this.eligibility.put("ON",true);
        this.eligibility.put("BC",true);
    }
    public String getID(){
        return this.customerID;
    }
    public int getBudget(){
        return this.budget;
    }
    public void setBudget(int budget){
        this.budget = budget;
    }
    public void setEligibility(String province, boolean eligibility){
        this.eligibility.replace(province,eligibility);
    }
    public boolean checkEligible(String province){
        return this.eligibility.get(province);
    }


}