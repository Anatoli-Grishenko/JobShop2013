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

/**
 *
 * @author Anatoli Grishenko <Anatoli.Grishenko@gmail.com>
 */
public class Product {

    String ID;
    ArrayList<Operations> Sequence;
    Operations currentOperation;
    double cost;
    String start, end;

    public Product() {
        clear();
    }

    public Product(String serie) {
        clear();
        try {
            JsonObject jsProd = Json.parse(serie).asObject();
            fromJson(jsProd);
        } catch (Exception ex) {
            clear();
        }
    }

    public Product(JsonObject jsProd) {
        fromJson(jsProd);
    }
    public final void clear() {
        this.ID = Keygen.getAlphaNumKey();
        Sequence = new ArrayList();
        currentOperation=null;
    }

    public String getStart() {
        return start;
    }

    public void setStart(String start) {
        this.start = start;
    }

    public String getEnd() {
        return end;
    }

    public void setEnd(String end) {
        this.end = end;
    }

    public Operations getCurrentOperation() {
        return currentOperation;
    }

    public final void setCurrentOperation(Operations currentOperation) {
        this.currentOperation = currentOperation;
    }

    public int getNOperations() {
        return getSequence().size();
    }

    public void nextOperation() {
        if (getNOperations() > 0) {
            Sequence.remove(0);
            if (getNOperations() > 0) {
                setCurrentOperation(Sequence.get(0));
            } else {
                setCurrentOperation(null);
            }
        }
    }

    public String getID() {
        return ID;
    }

    public final Product setID(String ID) {
        this.ID = ID;
        return this;
    }

    public ArrayList<Operations> getSequence() {
        return Sequence;
    }

    public Product setSequence(ArrayList<Operations> Sequence) {
        this.Sequence = Sequence;
        this.setCurrentOperation(Sequence.get(0));
        return this;
    }

    public final Product addOperation(Operations op) {
        Sequence.add(op);
        if (Sequence.size() == 1) {
            this.setCurrentOperation(op);
        }
        return this;
    }

    public double getCost() {
        return cost;
    }

    public void setCost(double cost) {
        this.cost = cost;
    }

        
    private void fromJson(JsonObject jsProd) {
        clear();
        try {
            this.setID(jsProd.getString("id", Keygen.getAlphaNumKey()));
            this.setStart(jsProd.getString("start", ""));
            this.setEnd(jsProd.getString("end", ""));
            this.setCost(jsProd.getDouble("cost", Integer.MAX_VALUE));
            this.setCurrentOperation(Operations.valueOf(jsProd.getString("current", Operations.BEGIN.name())));
            for (JsonValue jsv : jsProd.get("sequence").asArray()) {
                this.addOperation(Operations.valueOf(jsv.asString()));
            }
        } catch (Exception ex) {

        }
    }

    public JsonObject toJson() {
        JsonObject jsres = new JsonObject();
        JsonArray jsares = new JsonArray();
        jsres.add("id", this.getID());
        jsres.add("start", this.getStart());
        jsres.add("end", this.getEnd());
        jsres.add("current", this.getCurrentOperation().name());
        jsres.add("cost", this.getCost());
       
       this.getSequence().forEach(op -> {
            jsares.add(op.name());
        });
        jsres.add("sequence", jsares);
        return jsres;
    }

    @Override
    public String toString() {
        return toJson().toString();
    }

}
