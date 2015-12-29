package res.frontend;

import res.*;
import res.algebratypes.*;
import res.algebras.*;
import res.transform.*;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;

public class ResDisplay<U extends MultigradedElement<U>> extends JPanel implements PingListener, MouseMotionListener, MouseListener, MouseWheelListener
{
    final static int DEFAULT_MINFILT = 0;
    final static int DEFAULT_MAXFILT = 100;
    int BLOCK_WIDTH = 30;

    Decorated<U, ? extends MultigradedComputation<U>> dec;
    MultigradedComputation<U> under;

    int[] minfilt;
    int[] maxfilt;

    int viewx = 45;
    int viewy = -45;
    int zoom = 0;
    int selx = -1;
    int sely = -1;
    int mx = -1;
    int my = -1;

    JTextArea textarea = null;

    private ResDisplay(Decorated<U, ? extends MultigradedComputation<U>> dec)
    {
        setBackend(dec);
        addMouseListener(this);
        addMouseMotionListener(this);
        addMouseWheelListener(this);
    }

    public void setBackend(Decorated<U, ? extends MultigradedComputation<U>> dec)
    {
        if(under != null)
            under.removeListener(this);

        this.dec = dec;
        under = dec.underlying();
        under.addListener(this);

        int i;
        if(minfilt == null) {
            i = 0;
            minfilt = new int[under.num_gradings()];
            maxfilt = new int[under.num_gradings()];
        } else if(minfilt.length != under.num_gradings()) {
            i = minfilt.length;
            minfilt = Arrays.copyOf(minfilt, under.num_gradings());
            maxfilt = Arrays.copyOf(maxfilt, under.num_gradings());
        } else return;

        for(; i < minfilt.length; i++) {
            minfilt[i] = DEFAULT_MINFILT;
            maxfilt[i] = DEFAULT_MAXFILT;
        } 
    }


    private int getcx(int x) {
        return BLOCK_WIDTH * x + viewx;
    }
    private int getcy(int y) {
        return getHeight() - BLOCK_WIDTH * y + viewy;
    }
    private int getx(int cx) {
        cx -= viewx;
        cx += BLOCK_WIDTH/2;
        return cx / BLOCK_WIDTH;
    }
    private int gety(int cy) {
        cy = cy - getHeight() - viewy;
        cy = -cy;
        cy += BLOCK_WIDTH/2;
        return cy / BLOCK_WIDTH;
    }

    private boolean isVisible(U d)
    {
        int[] deg = d.multideg();
        for(int i = 2; i < minfilt.length; i++)
            if(deg[i] < minfilt[i] || deg[i] > maxfilt[i])
                return false;
        return dec.isVisible(d);
    }

    private int[] multideg(int x, int y)
    {
        return new int[] {y,x+y};
/*        int[] ret = new int[under.num_gradings()];
        ret[0] = y;
        ret[1] = x+y;
        for(int i = 2; i < ret.length; i++)
            ret[i] = Multidegrees.WILDCARD;
        return ret; */
    }

    @Override public void paintComponent(Graphics g)
    {
        super.paintComponent(g);

        int min_x_visible = getx(-3*BLOCK_WIDTH);
        int min_y_visible = gety(getHeight() + 3*BLOCK_WIDTH);
        if(min_x_visible < 0) min_x_visible = 0;
        if(min_y_visible < 0) min_y_visible = 0;
        int max_x_visible = getx(getWidth() + 3*BLOCK_WIDTH);
        int max_y_visible = gety(-3*BLOCK_WIDTH);
        int max_visible = (max_x_visible < max_y_visible) ? max_y_visible : max_x_visible;

        /* draw grid */
        int zoomscale = 5;
        for(int i = -zoom; i > 0; i -= 10) zoomscale *= 2;
        for(int x = 0; x <= max_visible; x++) {
            g.setColor(Color.lightGray);
            g.drawLine(getcx(x)-BLOCK_WIDTH/2, getcy(0)+BLOCK_WIDTH/2, getcx(x)-BLOCK_WIDTH/2, 0);
            g.drawLine(getcx(0)-BLOCK_WIDTH/2, getcy(x)+BLOCK_WIDTH/2, getWidth(), getcy(x)+BLOCK_WIDTH/2);

            if(x % zoomscale == 0) {
                g.setColor(Color.black);
                g.drawString(String.valueOf(x), getcx(x)-8, getcy(0)+(18*BLOCK_WIDTH/30)+17);
                g.drawString(String.valueOf(x), getcx(0)-(19*BLOCK_WIDTH/30)-19, getcy(x)+5);
            }
        }


        /* assign dots a location; at this point we definitively decide what's visible */
        Set<U> frameVisibles = new TreeSet<U>();
        TreeMap<U,int[]> pos = new TreeMap<U,int[]>();
        for(int x = min_x_visible; x <= max_x_visible; x++) {
            for(int y = min_y_visible; y <= max_y_visible; y++) {
                int cx = getcx(x);
                int cy = getcy(y);
                switch(under.getState(multideg(x,y))) {
                    case MultigradedComputation.STATE_NOT_COMPUTED:
                        g.setColor(Color.black);
                        g.fillRect(cx-BLOCK_WIDTH/2, cy-BLOCK_WIDTH/2, BLOCK_WIDTH, BLOCK_WIDTH);
                        continue;
                    case MultigradedComputation.STATE_QUEUED:
                        g.setColor(Color.darkGray);
                        g.fillRect(cx-BLOCK_WIDTH/2, cy-BLOCK_WIDTH/2, BLOCK_WIDTH, BLOCK_WIDTH);
                        continue;
                    case MultigradedComputation.STATE_STARTED:
                        g.setColor(new Color(128,0,0));
                        g.fillRect(cx-BLOCK_WIDTH/2, cy-BLOCK_WIDTH/2, BLOCK_WIDTH, BLOCK_WIDTH);
                        continue;
                    case MultigradedComputation.STATE_PARTIAL:
                        g.setColor(Color.yellow);
                        g.fillRect(cx-BLOCK_WIDTH/2, cy-BLOCK_WIDTH/2, BLOCK_WIDTH, BLOCK_WIDTH);
                        break;
                    case MultigradedComputation.STATE_FORMALLY_VANISHES:
                        g.setColor(Color.lightGray);
                        g.fillRect(cx-BLOCK_WIDTH/2, cy-BLOCK_WIDTH/2, BLOCK_WIDTH, BLOCK_WIDTH);
                        continue;
                    default:
                        break;
                }
                if(x == selx && y == sely) {
                    g.setColor(Color.orange);
                    g.fillRect(cx - BLOCK_WIDTH/2, cy - BLOCK_WIDTH/2, BLOCK_WIDTH, BLOCK_WIDTH);
                }
        
                Iterable<U> gens = under.gens(multideg(x,y));

                int visible = 0;
                synchronized(under.gens_lock) {
                    for(U d : gens) if(isVisible(d)) {
                        frameVisibles.add(d);
                        visible++;
                    }
                    int offset = -5 * (visible-1) / 2;
                    for(U d : gens) if(frameVisibles.contains(d)) {
                        int[] newpos = new int[] { cx + offset, cy - offset/2 };
                        pos.put(d, newpos);
                        offset += 5;
                    }
                }
            }
        }

        /* draw decorations */
        for(U u : frameVisibles) {
            int[] p1 = pos.get(u);

            /* based */
            for(BasedLineDecoration<U> d : dec.getBasedLineDecorations(u)) {
                if(! frameVisibles.contains(d.dest))
                    continue;
                g.setColor(d.color);
                int[] p2 = pos.get(d.dest);
                g.drawLine(p1[0], p1[1], p2[0], p2[1]);
            }
            
            /* unbased */
            for(UnbasedLineDecoration<U> d : dec.getUnbasedLineDecorations(u)) {
                g.setColor(d.color);
                int destx = getcx(d.dest[0]);
                int desty = getcy(d.dest[1]);
                g.drawLine(p1[0], p1[1], destx, desty);
            }
        }

        /* draw dots */
        g.setColor(Color.black);
//        for(int[] p : pos.values()) {
        for(U u : frameVisibles) {
            int[] p = pos.get(u);
            g.fillOval(p[0]-2, p[1]-2, 5, 5);
        }

        /* draw axes */
        final int MARGIN_WID = 30;
        int bmy = getHeight() - MARGIN_WID;
        g.setColor(getBackground());
        g.fillRect(0, 0, MARGIN_WID, getHeight());
        g.fillRect(0, bmy, getWidth(), MARGIN_WID);
        g.setColor(Color.gray);
        g.drawLine(MARGIN_WID, 0, MARGIN_WID, bmy);
        g.drawLine(MARGIN_WID, bmy, getWidth(), bmy);
        g.setColor(Color.black);
        for(int x = 0; x <= max_visible; x += zoomscale) {
            g.drawString(String.valueOf(x), getcx(x)-8, getHeight()-10);
            g.drawString(String.valueOf(x), 10, getcy(x)+5);
        }

    }

    @SuppressWarnings("fallthrough")
    void setSelected(int x, int y)
    {
        selx = x;
        sely = y;
        repaint();

        if(textarea == null) return;

        String ret = "";
        switch(under.getState(multideg(x,y))) {
            case MultigradedComputation.STATE_FORMALLY_VANISHES:
                ret = "This degree vanishes formally.";
                break;
            case MultigradedComputation.STATE_NOT_COMPUTED:
                ret = "This degree has not yet been computed.";
                break;
            case MultigradedComputation.STATE_STARTED:
                ret = "This degree is still being computed.";
                break;
            case MultigradedComputation.STATE_PARTIAL:
                ret = "This degree might not yet be complete.\n";
                // case falls through
            case MultigradedComputation.STATE_DONE:
            default:
                ret += "Bidegree ("+x+","+y+")\n";
                Iterable<U> gens = under.gens(multideg(x,y));
                synchronized(under.gens_lock) {
                    for(U d : gens) if(isVisible(d)) {
                        ret += "\n" + d.toString();
                        ret += "\n" + d.extraInfo();
                        ret += "\n";
                    }
                }
        }

        textarea.setText(ret);
    }


    @Override public void mouseClicked(MouseEvent evt)
    {
        int x = getx(evt.getX());
        int y = gety(evt.getY());
        if(x >= 0 && y >= 0) {
            setSelected(x,y);
        } else {
            setSelected(-1,-1);
        }
    }
    @Override public void mousePressed(MouseEvent evt) { }
    @Override public void mouseReleased(MouseEvent evt) { }
    @Override public void mouseEntered(MouseEvent evt) { }
    @Override public void mouseExited(MouseEvent evt) { }

    @Override public void mouseMoved(MouseEvent evt)
    {
        mx = evt.getX();
        my = evt.getY();
    }

    @Override public void mouseDragged(MouseEvent evt)
    {
        int dx = evt.getX() - mx;
        int dy = evt.getY() - my;

        mx = evt.getX();
        my = evt.getY();

        viewx += dx;
        viewy += dy;

        repaint();
    }

    @Override public void mouseWheelMoved(MouseWheelEvent evt)
    {
        zoom -= evt.getWheelRotation();
        BLOCK_WIDTH = 1 + (int) (29.0 * Math.pow(1.1,zoom));
        repaint();
    }

    @Override public void ping(int[] deg)
    {
        repaint();
        int[] s = multideg(selx, sely);
        if(deg[0] == s[0] && deg[1] == s[1])
            setSelected(selx,sely); // updates text
    }

    public static <U extends MultigradedElement<U>> void constructFrontend(Decorated<U, ? extends MultigradedComputation<U>> back) 
    {
        JFrame fr = new JFrame("Resolution");
        fr.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        fr.setSize(1200,800);
        
        ResDisplay<U> d = new ResDisplay<U>(back);
        fr.getContentPane().add(d, BorderLayout.CENTER);
        
        fr.getContentPane().add(new ControlPanel2D(d), BorderLayout.EAST);
        fr.setVisible(true);
    }

}

class ControlPanel2D extends Box {

    ControlPanel2D(final ResDisplay<?> parent)
    {
        super(BoxLayout.Y_AXIS);

        setup_gui(parent);
    }

    void setup_gui(final ResDisplay<?> parent)
    {

        /* filtration sliders */
        for(int i = 2; i < parent.minfilt.length; i++) {
            final int icopy = i;

            final JSpinner s1 = new JSpinner(new SpinnerNumberModel(parent.minfilt[i],0,1000,1));
            final JSpinner s2 = new JSpinner(new SpinnerNumberModel(parent.maxfilt[i],0,1000,1));

            s1.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent e) {
                    parent.minfilt[icopy] = (Integer) s1.getValue();
                    parent.setSelected(parent.selx, parent.sely);
                    parent.repaint();
                }
            });
            
            s2.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent e) {
                    parent.maxfilt[icopy] = (Integer) s2.getValue();
                    parent.setSelected(parent.selx, parent.sely);
                    parent.repaint();
                }
            });

            if(i == 2) {
                KeyboardFocusManager.getCurrentKeyboardFocusManager()
                    .addKeyEventDispatcher(new KeyEventDispatcher() {
                    @Override public boolean dispatchKeyEvent(KeyEvent e) {
                        if(e.getID() != KeyEvent.KEY_PRESSED)
                            return false;
                        switch(e.getKeyCode()) {
                            case KeyEvent.VK_PAGE_UP:
                                s1.setValue( ((Integer) s1.getValue()) + 1);
                                s2.setValue( ((Integer) s2.getValue()) + 1);
                                return true;
                            case KeyEvent.VK_PAGE_DOWN:
                                s1.setValue( ((Integer) s1.getValue()) - 1);
                                s2.setValue( ((Integer) s2.getValue()) - 1);
                                return true;
                            default:
                                return false;
                        }
                    }
                });
            }

            Dimension smin = new Dimension(0,30);
            s1.setMinimumSize(smin);
            s2.setMinimumSize(smin);
            s1.setPreferredSize(smin);
            s2.setPreferredSize(smin);

            add(new JLabel("Filtration "+(i-1)+":"));
            add(new JLabel("min:"));
            add(s1);
            add(new JLabel("max:"));
            add(s2);
            add(Box.createVerticalStrut(20));
        }

        /*
        final JCheckBox diff = new JCheckBox("Alg Novikov differentials");
        diff.setSelected(false);
        diff.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                parent.diff = diff.isSelected();
                parent.repaint();
            }
        });
        add(diff);
        
        final JCheckBox cartdiff = new JCheckBox("Cartan differentials");
        cartdiff.setSelected(true);
        cartdiff.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                parent.cartdiff = cartdiff.isSelected();
                parent.repaint();
            }
        });
        add(cartdiff);
        add(Box.createVerticalStrut(20));

        for(int i = 0; i <= 2; i++) {
            final int j = i;

            Box h = Box.createHorizontalBox();
            h.add(new JLabel("h"+i+":"));
            final JCheckBox hlines = new JCheckBox("lines", parent.hlines[i]);
            hlines.addActionListener(new ActionListener() {
                @Override public void actionPerformed(ActionEvent evt) {
                    parent.hlines[j] = hlines.isSelected();
                    parent.repaint();
                }
            });
            final JCheckBox hhide = new JCheckBox("hide", parent.hhide[i]);
            hhide.addActionListener(new ActionListener() {
                @Override public void actionPerformed(ActionEvent evt) {
                    parent.hhide[j] = hhide.isSelected();
                    parent.repaint();
                }
            });
            final JCheckBox htowers = new JCheckBox("towers", parent.htowers[i]);
            htowers.addActionListener(new ActionListener() {
                @Override public void actionPerformed(ActionEvent evt) {
                    parent.htowers[j] = htowers.isSelected();
                    parent.repaint();
                }
            });
            h.add(hlines);
            h.add(hhide);
            h.add(htowers);
            
            h.setAlignmentX(-1.0f);
            add(h);
        }
        add(Box.createVerticalStrut(20));
        */

        parent.textarea = new JTextArea();
        parent.textarea.setMaximumSize(new Dimension(250,3000));
        parent.textarea.setPreferredSize(new Dimension(250,3000));
        parent.textarea.setEditable(false);
        parent.textarea.setLineWrap(true);
        JScrollPane textsp = new JScrollPane(parent.textarea, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        textsp.setMaximumSize(new Dimension(250,3000));
        textsp.setPreferredSize(new Dimension(250,3000));
        textsp.setAlignmentX(-1.0f);
        add(textsp);
    }

}

