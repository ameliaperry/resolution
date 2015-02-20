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

    static SettingsDialog sd;
    public static void main(String[] args)
    {
        String s;
        sd = new SettingsDialog();
        sd.setVisible(true); /* blocks until dialog has completed */

        if(sd.cancelled)
            System.exit(0);

        /* prime */
        Config.P = (Integer) sd.prime.getSelectedItem();
        Config.Q = 2 * (Config.P - 1);
        ResMath.calcInverses();

        /* T cap */
        Config.T_CAP = (Integer) sd.maxt.getValue();

        /* threads */
        Config.THREADS = (Integer) sd.threads.getValue();

        /* intervene for the Cartan-Eilenberg option */
        if(sd.algcombo.getSelectedItem() == SettingsDialog.ALGCE) {
            startCE();
            return;
        }

        /* module */
        GradedAlgebra<Sq> steen = null;
        GradedModule<Sq> sqmod;
        s = (String) sd.modcombo.getSelectedItem();
        if(s == SettingsDialog.MODBRUNER)
            sqmod = new BrunerNotationModule();
        else if(s == SettingsDialog.MODCOF2)
            sqmod = new CofibHopf(0);
        else if(s == SettingsDialog.MODCOFETA)
            sqmod = new CofibHopf(1);
        else if(s == SettingsDialog.MODCOFNU)
            sqmod = new CofibHopf(2);
        else if(s == SettingsDialog.MODCOFSIGMA)
            sqmod = new CofibHopf(3);
        else if(s == SettingsDialog.MODA1)
            sqmod = new A1();
        else if(s == SettingsDialog.MODEXCESS) {
            int exct = -1;
            while(exct < 0) {
                String excstr = JOptionPane.showInputDialog(null, "Excess less than or equal to what T?");
                try {
                    exct = Integer.parseInt(excstr);
                } catch(NumberFormatException e) {}
            }
            steen = new SteenrodAlgebra();
            sqmod = new ExcessModule(exct,steen);
        } else
            sqmod = new Sphere<Sq>(Sq.UNIT);


        /* algebra */
        s = (String) sd.algcombo.getSelectedItem();
        Config.MICHAEL_MODE = (s == SettingsDialog.ALGODD);
        Config.MOTIVIC_GRADING = (s == SettingsDialog.ALGMOT);
        if(s == SettingsDialog.ALGSTEEN || s == SettingsDialog.ALGODD || s == SettingsDialog.ALGMOT) { // steenrod

            if(steen == null)
                steen = new SteenrodAlgebra();

            startBruner(steen, sqmod);

        } else { // A(n)

            int N = -1;
            if(s == SettingsDialog.ALGA1) N = 1;
            else if(s == SettingsDialog.ALGA2) N = 2;
            else if(s == SettingsDialog.ALGAN) {
                while(N < 0) {
                    String nstr = JOptionPane.showInputDialog(null, "Ext over A(n) for what n?");
                    try {
                        N = Integer.parseInt(nstr);
                    } catch(NumberFormatException e) {}
                    if(N > 20) N = -1; // huge inputs will break due to overflow
                }
            }

            AnAlgebra analg = new AnAlgebra(N);
            GradedModule<AnElement> anmod = new AnModuleWrapper(sqmod);

            startBruner(analg, anmod);
        }


    }

    static <T extends GradedElement<T>> void startBruner(GradedAlgebra<T> alg, GradedModule<T> mod)
    {
        /* backend */
        BrunerBackend<T> back = new BrunerBackend<T>(alg);
        back.setModule(mod);
        Decorated<Generator<T>, ? extends MultigradedVectorSpace<Generator<T>>> dec = back.getDecorated();

        /* frontend */
        String s = sd.front.getSelection().getActionCommand();
        if(s == SettingsDialog.FRONT3D)
            ResDisplay3D.constructFrontend(dec);
        else
            ResDisplay.constructFrontend(dec);

        /* off we go */
        back.start();
    }

    static void startCE()
    {
        CotorLiftingBackend back = new CotorLiftingBackend();
        Decorated<Generator<Sq>, ? extends MultigradedVectorSpace<Generator<Sq>>> dec = back.getDecorated();

        /* frontend */
        String s = sd.front.getSelection().getActionCommand();
        if(s == SettingsDialog.FRONT3D)
            ResDisplay3D.constructFrontend(dec);
        else
            ResDisplay.constructFrontend(dec);

        /* off we go */
        back.start();
    }

}

