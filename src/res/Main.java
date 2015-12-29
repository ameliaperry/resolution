package res;

import res.algebratypes.*;
import res.algebras.*;
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
            JOptionPane.showMessageDialog(null, fail);
            System.exit(1);
        }
    }

    static SettingsDialog sd;
    static SteenrodAlgebra steen;
    public static void main(String[] args)
    {
        sd = new SettingsDialog();
        sd.setVisible(true); /* blocks until dialog has completed */

        if(sd.cancelled)
            System.exit(0);
        
        String s = (String) sd.modcombo.getSelectedItem();

        /* copy over settings immediately, before we start doing algebra */
        Config.P = (Integer) sd.prime.getSelectedItem();
        Config.Q = 2 * (Config.P - 1);
        Config.T_CAP = (Integer) sd.maxt.getValue();
        Config.THREADS = (Integer) sd.threads.getValue();
        Config.MICHAEL_MODE = (s == SettingsDialog.ALGODD);
        Config.MOTIVIC_GRADING = (s == SettingsDialog.ALGMOT);
        
        ResMath.calcInverses();

        /* intervene for the Cartan-Eilenberg option */
        if(sd.algcombo.getSelectedItem() == SettingsDialog.ALGCE) {
            startCE();
            return;
        }

        /* intervene for the polynomial/exterior option */
        if(sd.algcombo.getSelectedItem() == SettingsDialog.ALGPE) {
            startPE();
            return;
        }

        /* module */
        if(s == SettingsDialog.MODBRUNER)
            launch_2(new BrunerNotationModule());
        else if(s == SettingsDialog.MODCOF2)
            launch_2(new CofibHopf(0));
        else if(s == SettingsDialog.MODCOFETA)
            launch_2(new CofibHopf(1));
        else if(s == SettingsDialog.MODCOFNU)
            launch_2(new CofibHopf(2));
        else if(s == SettingsDialog.MODCOFSIGMA)
            launch_2(new CofibHopf(3));
        else if(s == SettingsDialog.MODA1)
            launch_2(new A1());
        else if(s == SettingsDialog.MODEXCESS) {
            int exct = -1;
            while(exct < 0) {
                String excstr = JOptionPane.showInputDialog(null, "Excess less than or equal to what T?");
                try {
                    exct = Integer.parseInt(excstr);
                } catch(NumberFormatException e) {}
            }
            steen = new SteenrodAlgebra();
            launch_2(new ExcessModule(exct,steen));
        } else
            launch_2(new Sphere<Sq>(Sq.UNIT));
    }


    /* continue, with a type parameter M for the module elements */
    static <M extends GradedElement<M>> void launch_2(GradedModule<M,Sq> mod) {

        /* algebra */
        String s = (String) sd.algcombo.getSelectedItem();
        if(s == SettingsDialog.ALGSTEEN || s == SettingsDialog.ALGODD || s == SettingsDialog.ALGMOT) { // steenrod

            if(steen == null) steen = new SteenrodAlgebra();

            startBruner(steen, mod);

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
            AnModuleWrapper<M> anmod = new AnModuleWrapper<M>(mod);

            startBruner(analg, anmod);
        }

    }

    static <M extends MultigradedElement<M>, T extends MultigradedElement<T>> void startBruner(GradedAlgebra<T> alg, GradedModule<M,T> mod)
    {
        /* backend */
        BrunerBackend<M,T> back = new BrunerBackend<M,T>(alg,mod);
        Decorated<Generator<T>, MultigradedAlgebraComputation<Generator<T>>> dec = back.getDecorated();

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
        Decorated<Generator<Sq>, MultigradedComputation<Generator<Sq>>> dec = back.getDecorated();

        /* frontend */
        String s = sd.front.getSelection().getActionCommand();
        if(s == SettingsDialog.FRONT3D)
            ResDisplay3D.constructFrontend(dec);
        else
            ResDisplay.constructFrontend(dec);

        /* off we go */
        back.start();
    }
    
    static void startPE()
    {
        PolynomialExteriorBackend back = new PolynomialExteriorBackend();

        /* frontend */
        String s = sd.front.getSelection().getActionCommand();
        if(s == SettingsDialog.FRONT3D)
            ResDisplay3D.constructFrontend(back.getDecorated());
        else
            ResDisplay.constructFrontend(back.getDecorated());

        /* off we go */
        back.start();
    }

}

