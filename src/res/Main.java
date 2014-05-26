package res;

import res.algebra.*;
import res.backend.*;
import res.frontend.*;
import res.transform.*;

import java.awt.Color;
import java.util.*;
import javax.swing.JOptionPane;

public class Main {
    
    public static void die_if(boolean test, String fail)
    {
        if(test) {
            System.err.println(fail);
            Thread.dumpStack();
            System.err.println("Failing.");
            System.exit(1);
        }
    }

    public static void main(String[] args)
    {
        String s;
        SettingsDialog sd = new SettingsDialog();
        sd.setVisible(true); /* blocks until dialog has completed */

        if(sd.cancelled)
            System.exit(0);

        /* prime */
        Config.P = (Integer) sd.prime.getSelectedItem();
        Config.Q = 2 * (Config.P - 1);
        ResMath.calcInverses();

        /* p=2 mode */
        Config.MICHAEL_MODE = sd.oddrel.isSelected();

        /* T cap */
        Config.T_CAP = (Integer) sd.maxt.getValue();

        /* threads */
        Config.THREADS = (Integer) sd.threads.getValue();

        /* at some point we'll have to think about how to do this process in a properly generic way */

        /* algebra */
        GradedAlgebra<Sq> alg = new SteenrodAlgebra();

        /* module */
        GradedModule<Sq> mod;
        s = (String) sd.modcombo.getSelectedItem();
        if(s == SettingsDialog.MODCOF2)
            mod = new CofibHopf(0,alg);
        else if(s == SettingsDialog.MODCOFETA)
            mod = new CofibHopf(1,alg);
        else if(s == SettingsDialog.MODCOFNU)
            mod = new CofibHopf(2,alg);
        else if(s == SettingsDialog.MODCOFSIGMA)
            mod = new CofibHopf(3,alg);
        else if(s == SettingsDialog.MODEXCESS) {
            int exct = -1;
            while(exct < 0) {
                String excstr = JOptionPane.showInputDialog(null, "Excess less than or equal to what T?");
                try {
                    exct = Integer.parseInt(excstr);
                } catch(NumberFormatException e) {}
            }
            mod = new ExcessModule(exct,alg);
        } else
            mod = new Sphere<Sq>(alg);

        /* backend */
        s = sd.back.getSelection().getActionCommand();
        Backend<Generator<Sq>, ? extends MultigradedVectorSpace<Generator<Sq>>> back = null;
        if(s == SettingsDialog.BACKBRUNER) {
            BrunerBackend<Sq> brunerback = new BrunerBackend<Sq>(alg);
            brunerback.setModule(mod);
            back = brunerback;
        } else if(s == SettingsDialog.BACKQ0)
            back = new CotorLiftingBackend();
        Decorated<Generator<Sq>, ? extends MultigradedVectorSpace<Generator<Sq>>> dec = back.getDecorated();

        /* frontend */
        s = sd.front.getSelection().getActionCommand();
        if(s == SettingsDialog.FRONT3D)
            ResDisplay3D.constructFrontend(dec);
        else
            ResDisplay.constructFrontend(dec);


        /* off we go */
        back.start();
    }

}

