/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jobshop;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import crypto.Keygen;
import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * @author Anatoli Grishenko <Anatoli.Grishenko@gmail.com>
 */
public class Machine {

    String ID;
    ArrayList<Operations> capabilities;
    HashMap<Operations, Double> processingTime;
    HashMap<Operations, Double> processingCost;
    boolean available;
    Product processing;

    public Machine(String ID) {
        clear();
        this.ID = ID;
    }

    public final void clear() {
        ID = "";
        capabilities = new ArrayList();
        processingTime = new HashMap();
        processingCost = new HashMap();
        available = true;        
    }
    public String getID() {
        return ID;
    }

    public void setID(String ID) {
        this.ID = ID;
    }

    public ArrayList<Operations> getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(ArrayList<Operations> capabilities) {
        this.capabilities = capabilities;
    }

    public void addCapability(Operations op, double time) {
        capabilities.add(op);
        processingTime.put(op, time);
        processingCost.put(op, 0.0);
        
    }

    public void addCapability(Operations op, double time, double cost) {
        capabilities.add(op);
        processingTime.put(op, time);
        processingCost.put(op, cost);
        
    }

    public boolean hasCapability(Operations op) {
        return capabilities.contains(op);
    }

    public double processingTime(Operations op) {
        if (hasCapability(op)) {
            return processingTime.get(op);
        } else {
            return Integer.MAX_VALUE;
        }
    }

    public double processingCost(Operations op) {
        if (hasCapability(op)) {
            return processingCost.get(op);
        } else {
            return Integer.MAX_VALUE;
        }
    }

    public HashMap<Operations, Double> getProcessingTime() {
        return processingTime;
    }

    public void setProcessingTime(HashMap<Operations, Double> processingTime) {
        this.processingTime = processingTime;
    }

    public String serialize() {
        JsonObject jsres = new JsonObject();
        JsonArray jsacapab = new JsonArray();

        jsres.add("id", this.getID());
        jsres.add("available", isAvailable());
        if (processing != null) {
            jsres.add("processing",processing.toJson());
        }
        capabilities.forEach(op -> {
            jsacapab.add(new JsonObject()
                    .add("operation", op.name())
                    .add("time", processingTime.get(op))
                    .add("cost", processingCost.get(op))
            );
        });
        jsres.add("configuration", jsacapab);
        return jsres.toString();
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    
    public boolean deSerialize(String serie) {
        JsonObject jsres, jsaux;
        JsonArray jsacapab;

        try {
            jsres = Json.parse(serie).asObject();
            clear();
            this.setID(jsres.getString("id", Keygen.getAlphaNumKey(10)));
            this.setAvailable(jsres.getBoolean("available", false));
            if (jsres.get("processing")!=null)
            this.setProcessing(new Product(jsres.get("processing").asObject()));
            for (JsonValue jsv : jsres.get("configuration").asArray()) {
                jsaux = jsv.asObject();
                this.addCapability(Operations.valueOf(jsaux.getString("operation", "")), 
                        jsaux.getDouble("time", Integer.MAX_VALUE),
                        jsaux.getDouble("cost", Integer.MAX_VALUE));
            }
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public Product getProcessing() {
        return processing;
    }

    public void setProcessing(Product processing) {
        this.processing = processing;
    }

}
