/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jobshop;

import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * @author Anatoli Grishenko <Anatoli.Grishenko@gmail.com>
 */
public class Layout {

    public HashMap<String, Machine> Machines;
    public HashMap<Operations, ArrayList<String>> Capabilities2Machine;
    public HashMap<String, ArrayList<Operations>> Machine2Capabilities;

    public Layout() {
        Machines = new HashMap();
        Capabilities2Machine = new HashMap();
        Machine2Capabilities = new HashMap();
    }

    @Override
    public String toString() {
        String res = "";
        for (String smachine : Machines.keySet()) {
            res += smachine;
            for (Operations op : Machine2Capabilities.get(smachine)) {
                res += "[" + op.name() + "] ";
            }
            res += "\n";
        }
        return res;
    }
}
