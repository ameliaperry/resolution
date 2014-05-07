package res.frontend;

import res.*;
import res.algebra.Sq;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import java.util.List; /* for precedence over java.awt.List */

public class ResDisplay extends JPanel implements PingListener, MouseMotionListener, MouseListener
{
    final static int BLOCK_WIDTH = 30;

    final static int TOWER_CUTOFF = 5;

    Backend<Sq> backend;

    int min_filt = 0;
    int max_filt = 5;

    boolean diff = false;
    boolean cartdiff = true;

    boolean[] hlines = new boolean[] { true, true, false };
    boolean[] hhide = new boolean[] { false, false, false };
    boolean[] htowers = new boolean[] { false, false, false };

    int viewx = 30;
    int viewy = -40;
    int selx = -1;
    int sely = -1;
    int mx = -1;
    int my = -1;
        
    List<Set<Generator<Sq>>> towers = null;
    List<Set<Generator<Sq>>> towergen = null;

    JTextArea textarea = null;

    private ResDisplay(Backend<Sq> back)
    {
        backend = back;
        addMouseListener(this);
        addMouseMotionListener(this);
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

    private boolean isVisible(Generator<Sq> d) {
        if(d.nov != -1 && (d.nov < min_filt || d.nov > max_filt))
            return false;
        for(int i = 0; i <= 2; i++) if(hhide[i])
            for(Dot<Sq> o : d.img.keySet())
                if(o.sq.equals(Sq.HOPF[i]))
                    return false; /* XXX risky -- we can have joint h_i multiples */
        for(int i = 0; i <= 2; i++) if(htowers[i] && towers != null)
            if(towers.get(i).contains(d))
                return false;
        return true;
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

        /* draw selection */
        if(selx >= 0 && sely >= 0) {
            g.setColor(Color.orange);
            int cx = getcx(selx);
            int cy = getcy(sely);
            g.fillRect(cx - BLOCK_WIDTH/2, cy - BLOCK_WIDTH/2, BLOCK_WIDTH, BLOCK_WIDTH);
        }

        /* draw grid */
        for(int x = 0; x <= max_visible; x++) {
            g.setColor(Color.lightGray);
            g.drawLine(getcx(x)-BLOCK_WIDTH/2, getcy(0)+BLOCK_WIDTH/2, getcx(x)-BLOCK_WIDTH/2, 0);
            g.drawLine(getcx(0)-BLOCK_WIDTH/2, getcy(x)+BLOCK_WIDTH/2, getWidth(), getcy(x)+BLOCK_WIDTH/2);

            if(x % 5 == 0) {
                g.setColor(Color.black);
                g.drawString(String.valueOf(x), getcx(x)-8, getcy(-1)+5);
                g.drawString(String.valueOf(x), getcx(-1)-8, getcy(x)+5);
            }
        }

        Set<Generator<Sq>> frameVisibles = new TreeSet<Generator<Sq>>();
        Map<Generator<Sq>,int[]> pos = new TreeMap<Generator<Sq>,int[]>();


        /* assign dots a location */
        for(int x = 0; ; x++) {
            int y;
            for(y = 0; ; y++) {
                if(!backend.isComputed(y,x+y))
                    break;
        
                Collection<Generator<Sq>> gens = backend.gens(y,x+y);

                int cx = getcx(x);
                int cy = getcy(y);

                int visible = 0;
                for(Generator<Sq> d : gens) if(isVisible(d)) {
                    frameVisibles.add(d);
                    visible++;
                }
                int offset = -5 * visible / 2;
                for(Generator<Sq> d : gens) if(frameVisibles.contains(d)) {
                    pos.put(d, new int[] { cx + offset, cy - offset/2 });
                    offset += 5;
                }
            }
            if(y == 0) break;
        }

        /* draw differentials, multiplications, and black blocks */
        for(int x = min_x_visible; x <= max_x_visible; x++) {
            for(int y = min_y_visible; y <= max_y_visible; y++) {

                /* black non-computed region */
                if(! backend.isComputed(y,x+y)) {
                    int cx = getcx(x);
                    int cy = getcy(y);
                    g.setColor(Color.black);
                    g.fillRect(cx-BLOCK_WIDTH/2, cy-BLOCK_WIDTH/2, BLOCK_WIDTH, BLOCK_WIDTH);
                    continue;
                }
                
                Collection<Generator<Sq>> gens = backend.gens(y,x+y);
                
                Set<Generator<Sq>> visibles = new TreeSet<Generator<Sq>>();
                for(Generator<Sq> d : gens) if(frameVisibles.contains(d))
                    visibles.add(d);

                /* draw multiplications */
                for(int i = 0; i <= 2; i++) if(hlines[i]) {
                    g.setColor(Color.black);
                    for(Generator<Sq> d : visibles) {
                        int[] src = pos.get(d);
                        for(Dot<Sq> o : d.img.keySet()) if(o.sq.equals(Sq.HOPF[i]) && frameVisibles.contains(o.base)) {
                            int[] dest = pos.get(o.base);
                            g.drawLine(src[0], src[1], dest[0], dest[1]);
                        }
                    }
                }
                
                /* draw towers */
                for(int i = 0; i <= 2; i++) if(htowers[i] && towergen != null) {
                    g.setColor(Color.blue);
                    for(Generator<Sq> d : towergen.get(i)) if(frameVisibles.contains(d)) {
                        int[] src = pos.get(d);
                        int[] dest = new int[] { src[0] + ((1 << i) - 1) * BLOCK_WIDTH * 3 / 4, src[1] - BLOCK_WIDTH * 3 / 4 };
                        g.drawLine(src[0], src[1], dest[0], dest[1]);
                    }
                }

                /* draw potential alg Novikov differentials */
                if(diff && x >= 1) {
                    g.setColor(Color.green);
                    for(int j = 2; ; j++) {
                        if(! backend.isComputed(y+j, x-1 + y+j))
                            break;
                        Collection<Generator<Sq>> ogen = backend.gens(y+j, x-1 + y+j);
                        for(Generator<Sq> d : visibles) {
                            int[] src = pos.get(d);
                            for(Generator<Sq> o : ogen)
                                if(o.nov == d.nov + 1 && frameVisibles.contains(o)) {
                                    int[] dest = pos.get(o);
                                    g.drawLine(src[0], src[1], dest[0], dest[1]);
                                }
                        }
                    }
                }

                /* draw potential Cartan differentials */
                if(cartdiff && x >= 1 && backend.isComputed(y+1, y+1 + x-1)) {
                    g.setColor(Color.red);
                    Collection<Generator<Sq>> ogen = backend.gens(y+1, y+1 + x-1);

                    for(Generator<Sq> o : ogen) if(frameVisibles.contains(o)) {
                        int[] dest = pos.get(o);
                        for(Generator<Sq> d : visibles)
                            if(o.nov >= d.nov + 2) {
                                int[] src = pos.get(d);
                                if(src == null) System.err.printf("src is null, dot %s, s=%d\n", d, d.s);
                                if(dest == null) System.err.printf("dest is null, dot %s, s=%d\n", o, o.s);
                                g.drawLine(src[0], src[1], dest[0], dest[1]);
                            }
                    }
                }


            }
        }

        /* draw dots and black blocks */
        g.setColor(Color.black);
        for(int[] p : pos.values()) {
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
        for(int x = 0; x <= max_visible; x += 5) {
            g.drawString(String.valueOf(x), getcx(x)-8, getHeight()-10);
            g.drawString(String.valueOf(x), 10, getcy(x)+5);
        }

    }

    void setSelected(int x, int y)
    {
        selx = x;
        sely = y;
        repaint();
        if(x < 0 || y < 0) {
            textarea.setText("");
            return;
        }
        
        if(! backend.isComputed(y,x+y)) {
            textarea.setText("Not yet computed.");
            return;
        }

        Collection<Generator<Sq>> gens = backend.gens(y,x+y);
//        Arrays.sort(gens);

        String ret = "Bidegree ("+x+","+y+")\n";
        for(Generator<Sq> d : gens) {
            ret += "\n";
            if(d.nov != -1 && (d.nov > max_filt || d.nov < min_filt))
                continue;
            ret += "("+x+";"+d.idx+")";
            if(d.nov != -1)
                ret += "(n="+d.nov+")";
            ret += " --->\n      ";
            ret += d.img.toStringDelim("\n     + ");
        }
        if(textarea != null)
            textarea.setText(ret);
    }

    private void computeTowers(int tmax)
    {
        List<Set<Generator<Sq>>> newtowers = new ArrayList<Set<Generator<Sq>>>();
        List<Set<Generator<Sq>>> newtowergen = new ArrayList<Set<Generator<Sq>>>();

        ArrayList<Generator<Sq>> templist = new ArrayList<Generator<Sq>>();

        for(int i = 0; i <= 2; i++) {
            newtowers.add(new TreeSet<Generator<Sq>>());
            newtowergen.add(new TreeSet<Generator<Sq>>());

            for(int t = tmax-(1<<i)+1; t <= tmax; t++) for(int s = 0; s <= t; s++) {
                if(! backend.isComputed(s,t)) break;

                /* for each generator in high degree */
                for(Generator<Sq> d : backend.gens(s,t)) {
                    templist.clear();

                    boolean fork = false;
                    /* follow it backwards and see if we get a long enough tower */
                    while(d != null) {
                        templist.add(d);
                        Generator<Sq> d_new = null;
                        if(d.img != null) {
                            for(Dot<Sq> o : d.img.keySet()) if(o.sq.equals(Sq.HOPF[i])) {
                                if(d_new != null) fork = true;
                                d_new = o.base;
                            }
                        }
                        d = d_new;
                    }

//                    System.out.printf("h%d tower of length %d\n", i, templist.size());
                    if(templist.size() < TOWER_CUTOFF)
                        continue;

                    if(fork)
                        System.err.println("Warning: tower fork");

                    /* pop the last element back off as a generator */
                    newtowergen.get(i).add(templist.remove(templist.size() - 1));
                    /* the rest are tower elements */
                    newtowers.get(i).addAll(templist);
                }
            }
        }

        towers = newtowers;
        towergen = newtowergen;
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

    @Override public void ping(int s, int t)
    {
        if(s == t && t >= 30)
            computeTowers(t);
        repaint();
    }

    public static void constructFrontend(Backend<Sq> back) 
    {
        JFrame fr = new JFrame("Resolution");
        fr.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        fr.setSize(1200,800);
        
        ResDisplay d = new ResDisplay(back);
        back.register_listener(d);
        fr.getContentPane().add(d, BorderLayout.CENTER);
        
        fr.getContentPane().add(new ControlPanel2D(d), BorderLayout.EAST);
        fr.setVisible(true);
    }

}

class ControlPanel2D extends Box {

    ControlPanel2D(final ResDisplay parent)
    {
        super(BoxLayout.Y_AXIS);

        final JSpinner s1 = new JSpinner(new SpinnerNumberModel(0,0,1000,1));
        final JSpinner s2 = new JSpinner(new SpinnerNumberModel(5,0,1000,1));

        s1.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                parent.min_filt = (Integer) s1.getValue();
                parent.setSelected(parent.selx, parent.sely);
                parent.repaint();
            }
        });
        
        s2.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                parent.max_filt = (Integer) s2.getValue();
                parent.setSelected(parent.selx, parent.sely);
                parent.repaint();
            }
        });

        /* hack in a key listener to globally handle pgup/pgdn presses */
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

        Dimension smin = new Dimension(0,30);
        s1.setMinimumSize(smin);
        s2.setMinimumSize(smin);
        s1.setPreferredSize(smin);
        s2.setPreferredSize(smin);

        add(new JLabel("Novikov filtration:"));
        add(new JLabel("min:"));
        add(s1);
        add(new JLabel("max:"));
        add(s2);
        add(Box.createVerticalStrut(20));

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

        parent.textarea = new JTextArea();
        parent.textarea.setMaximumSize(new Dimension(250,3000));
        parent.textarea.setPreferredSize(new Dimension(250,3000));
        parent.textarea.setEditable(false);
        JScrollPane textsp = new JScrollPane(parent.textarea, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        textsp.setMaximumSize(new Dimension(250,3000));
        textsp.setPreferredSize(new Dimension(250,3000));
        textsp.setAlignmentX(-1.0f);
        add(textsp);
    }

}

