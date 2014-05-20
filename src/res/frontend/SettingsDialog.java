package res.frontend;

import res.Config;
import java.awt.event.*;
import javax.swing.*;

public class SettingsDialog extends JDialog
{
    public static final String FRONT2D = "front2d";
    public static final String FRONT3D = "front3d";
    public static final String MODSPHERE = "Sphere";
    public static final String MODCOF2 = "cofib(2)";
    public static final String MODCOFETA = "cofib(\u03b7)";
    public static final String MODCOFNU = "cofib(\u03bd)";
    public static final String MODCOFSIGMA = "cofib(\u03c3)";
    public static final String MODEXCESS = "excess\u2264t";
    public static final String BACKBRUNER = "backbruner";
//    public static final String BACKOLD = "backold";

    static final Integer[] PRIMES = new Integer[] { 2, 3, 5, 7, 11, 13 };
    static final String[] MODULES = new String[] { MODSPHERE, MODCOF2, MODCOFETA, MODCOFNU, MODCOFSIGMA, MODEXCESS };

    /* XXX instead of publically exposing UI elements, should offer getters */
    public ButtonGroup front;
    public ButtonGroup back;

    public JComboBox<Integer> prime;
    public JComboBox<String> modcombo;
    public JCheckBox oddrel;
    public JSpinner maxt;
    public JSpinner threads;

    public boolean cancelled = true;


    public SettingsDialog()
    {
        super((java.awt.Frame)null, "Launch settings", true);
        getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));

        front = new ButtonGroup();
        back = new ButtonGroup();

        Box b_front = Box.createHorizontalBox();
        JRadioButton front_2d = new JRadioButton("2D", true);
        JRadioButton front_3d = new JRadioButton("3D");
        front_2d.setActionCommand(FRONT2D);
        front_3d.setActionCommand(FRONT3D);
        front.add(front_2d);
        front.add(front_3d);
        b_front.add(new JLabel("Frontend:"));
        b_front.add(front_2d);
        b_front.add(front_3d);

        Box b_mod = Box.createHorizontalBox();
        modcombo = new JComboBox<String>(MODULES);
        b_mod.add(new JLabel("Module:"));
        b_mod.add(modcombo);

        Box b_prime = Box.createHorizontalBox();
        prime = new JComboBox<Integer>(PRIMES);
        b_prime.add(new JLabel("Prime:"));
        b_prime.add(prime);

        Box b_oddrel = Box.createHorizontalBox();
        oddrel = new JCheckBox("Odd relations for p=2:", Config.MICHAEL_MODE);
        b_oddrel.add(oddrel);

        Box b_maxt = Box.createHorizontalBox();
        maxt = new JSpinner(new SpinnerNumberModel(Config.T_CAP, 1, 10000, 1));
        b_maxt.add(new JLabel("Max t:"));
        b_maxt.add(maxt);
        b_maxt.add(Box.createHorizontalGlue());

        Box b_back = Box.createHorizontalBox();
        JRadioButton back_bruner = new JRadioButton("bruner", true);
//        JRadioButton back_old = new JRadioButton("old");
        back_bruner.setActionCommand(BACKBRUNER);
//        back_old.setActionCommand(BACKOLD);
        back.add(back_bruner);
//        back.add(back_old);
        b_back.add(new JLabel("Backend:"));
        b_back.add(back_bruner);
//        b_back.add(back_old);
        
        Box b_threads = Box.createHorizontalBox();
        threads = new JSpinner(new SpinnerNumberModel(Config.THREADS, 1, 10000, 1));
        b_threads.add(new JLabel("Threads:"));
        b_threads.add(threads);


        Box b_ok = Box.createHorizontalBox();
        JButton ok = new JButton(new AbstractAction("OK") {
            @Override public void actionPerformed(ActionEvent evt) {
                cancelled = false;
                setVisible(false);
            }
        });
        JButton cancel = new JButton(new AbstractAction("Cancel") {
            @Override public void actionPerformed(ActionEvent evt) {
                cancelled = true;
                setVisible(false);
            }
        });
        b_ok.add(ok);
        b_ok.add(cancel);

        getContentPane().add(b_front);
        getContentPane().add(b_mod);
        getContentPane().add(b_prime);
        getContentPane().add(b_oddrel);
        getContentPane().add(b_maxt);
        getContentPane().add(b_back);
        getContentPane().add(b_threads);
        getContentPane().add(b_ok);

        pack();
    }

}
