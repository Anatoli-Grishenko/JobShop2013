/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Network;

import StrongHierarchy.*;
import jobshop.Operations;

/**
 *
 * @author Anatoli Grishenko <Anatoli.Grishenko@gmail.com>
 */
public class NMPolisher extends NMachine{
    
    @Override
    public void setup(){
        super.setup();
        myMachine.addCapability(Operations.POLISH, 3, 7);
        this.DFAddMyServices(new String []{Operations.POLISH.name()});
        Info("Setup "+this.getClass().getName());
    }

}
