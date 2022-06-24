/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Agents;

import agents.LARVAFirstAgent;
import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import data.Transform;
import java.util.ArrayList;
import jobshop.Layout;
import jobshop.Machine;
import jobshop.Operations;
import jobshop.Product;

/**
 *
 * @author Anatoli Grishenko <Anatoli.Grishenko@gmail.com>
 */
public class AgentJobShop extends LARVAFirstAgent {

    protected Layout layout;
    protected String machineRecorder = "";
    protected Product prod;

    @Override
    public void setup() {
        super.setup();
        logger.onTabular();
//        Info("Setup "+Class.class.getName());
    }

    @Override
    public void takeDown() {
        Info("Taking down");
        super.takeDown();
    }
    public void getLayout() {
        Operations op;
        ArrayList<String> Machines = this.DFGetAllProvidersOf("Machine");
        layout = new Layout();
        for (String smachine : Machines) {
            layout.Machines.put(smachine, new Machine(smachine));            
            for (String soperation : Transform.getAllNames(Operations.class)) {
                op = Operations.valueOf(soperation);
                if (this.DFHasService(smachine, soperation)) {
                    if (layout.Capabilities2Machine.get(op) == null) {
                        layout.Capabilities2Machine.put(op, new ArrayList());
                    }
                    if (layout.Machine2Capabilities.get(smachine) == null) {
                        layout.Machine2Capabilities.put(smachine, new ArrayList());
                    }
                    layout.Capabilities2Machine.get(op).add(smachine);
                    layout.Machine2Capabilities.get(smachine).add(op);
                }
            }
        }
    }

    @Override
    public void Execute() {
        inbox = this.LARVAblockingReceive();

    }

    public String outOf(ArrayList<String> al) {
        return al.get((int) (Math.random() * al.size()));
    }


}
