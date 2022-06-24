/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Agents;

import agents.LARVAFirstAgent;
import com.eclipsesource.json.WriterConfig;
import crypto.Keygen;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import java.util.ArrayList;
import jobshop.Machine;
import jobshop.Operations;
import jobshop.Product;

/**
 *
 * @author Anatoli Grishenko <Anatoli.Grishenko@gmail.com>
 */
public class AgentProducer extends AgentJobShop {

    protected Product p;
    protected String controllerName;
    protected ArrayList<String> inProduction;
    protected int MaxProducts = 5;

    @Override
    public void setup() {
        super.setup();
        Info("Setup ");
        inProduction = new ArrayList();
        this.LARVAwait(1000);
        controllerName = this.DFGetAllProvidersOf("Controller").get(0);
        this.openRemote();
    }

    @Override
    public void Execute() {
        Info("Ready");
        if (MaxProducts > 0) {
            if (Math.random() > 0.0) {
//                p = genProduct(0);
                p = genProduct((int) (Math.random() * 5));
                Info("Product arrival:\n" + p.toJson().toString(WriterConfig.PRETTY_PRINT));
                outbox = new ACLMessage(ACLMessage.REQUEST);
                outbox.setSender(getAID());
                outbox.addReceiver(new AID(controllerName, AID.ISLOCALNAME));
                outbox.setContent(p.toString());
                this.LARVAsend(outbox);
                inProduction.add(p.getID());
                MaxProducts--;
            }
        }
        inbox = this.LARVAblockingReceive(1000);
        if (inbox != null) {
            if (inbox.getPerformative() == ACLMessage.INFORM) {
                if (inProduction.contains(inbox.getContent())) {
                    inProduction.remove(inbox.getContent());
                }
            }
        }
        if (MaxProducts == 0 && inProduction.isEmpty()) {
            doExit();
        }
//        if (this.getNCycles()>5)
//            doExit();
//        else {
//            this.LARVAwait(1000);
//        }
    }

    public Product genProduct(int type) {
        Product p;
        p = new Product();
        p.setID(""+this.getNCycles()); //String.format("P%02d",this.getNCycles()));
//        p = new Product(Keygen.getWordo(8));
        p.addOperation(Operations.BEGIN);
        switch (type) {
            case 0:
            default:
                p.addOperation(Operations.SLOWCUT);
                break;
            case 1:
                p.addOperation(Operations.SLOWCUT);
                p.addOperation(Operations.DRILL);
                p.addOperation(Operations.POLISH);
                break;
            case 2:
                p.addOperation(Operations.DRILL);
                p.addOperation(Operations.FASTCUT);
                p.addOperation(Operations.POLISH);
                p.addOperation(Operations.DRILL);
                
                break;
            case 3:
                p.addOperation(Operations.MOULD);
                p.addOperation(Operations.POLISH);
                break;
        }
        p.addOperation(Operations.END);
        return p;
    }
}
