import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;

class ResDisplay extends JPanel implements PingListener, MouseMotionListener
{
    final static int BLOCK_WIDTH = 30;

    ResBackend backend;

    int min_filt = 0;
    int max_filt = 5;

    boolean diff = false;
    boolean cartdiff = true;

    boolean[] hlines = new boolean[] { true, true, false };
    boolean[] hhide = new boolean[] { false, false, false };

    int viewx = 30;
    int viewy = -40;
    int mx = -1;
    int my = -1;

    JTextArea textarea = null;

    ResDisplay(ResBackend back)
    {
        backend = back;
        addMouseMotionListener(this);

    }

    int getcx(int x) {
        return BLOCK_WIDTH * x + viewx;
    }
    int getcy(int y) {
        return getHeight() - BLOCK_WIDTH * y + viewy;
    }
    int getx(int cx) {
        cx -= viewx;
        cx += BLOCK_WIDTH/2;
        return cx / BLOCK_WIDTH;
    }
    int gety(int cy) {
        cy = cy - getHeight() - viewy;
        cy = -cy;
        cy += BLOCK_WIDTH/2;
        return cy / BLOCK_WIDTH;
    }

    boolean isVisible(Dot d) {
        if(d.nov != -1 && (d.nov < min_filt || d.nov > max_filt))
            return false;
        for(int i = 0; i <= 2; i++) if(hhide[i])
            for(Dot o : d.img.keySet())
                if(o.sq.equals(Sq.HOPF[i]))
                    return false; /* risky -- we can have joint h_i multiples */
        return true;
    }

    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);

        int min_x_visible = getx(-BLOCK_WIDTH);
        int min_y_visible = gety(getHeight()+BLOCK_WIDTH);
        if(min_x_visible < 0) min_x_visible = 0;
        if(min_y_visible < 0) min_y_visible = 0;
        int max_x_visible = getx(getWidth()+BLOCK_WIDTH);
        int max_y_visible = gety(-BLOCK_WIDTH);
        int max_visible = (max_x_visible < max_y_visible) ? max_y_visible : max_x_visible;

        /* draw axes */
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

        Comparator<Dot> dotComparator = new Comparator<Dot>() {
            @Override public int compare(Dot a, Dot b) {
                if(a.s != b.s)
                    return a.s - b.s;
                return a.compareTo(b);   
            }
        };
        Set<Dot> frameVisibles = new TreeSet<Dot>(dotComparator);
        Map<Dot,int[]> pos = new TreeMap<Dot,int[]>(dotComparator);

        /* assign dots a location */
        for(int x = 0; ; x++) {
            int y;
            for(y = 0; ; y++) {
                if(!backend.isComputed(y,x+y))
                    break;

                int cx = getcx(x);
                int cy = getcy(y);

                Dot[] gens = backend.gens(y, x+y);
                int visible = 0;
                for(Dot d : gens) if(isVisible(d)) {
                    frameVisibles.add(d);
                    visible++;
                }
                int offset = -5 * visible / 2;
                for(Dot d : gens) if(frameVisibles.contains(d)) {
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
                
                Dot[] gens = backend.gens(y, x+y);
                Collection<Dot> visibles = new ArrayList<Dot>();
                for(Dot d : gens) if(frameVisibles.contains(d))
                    visibles.add(d);

                /* draw multiplications */
                for(int i = 0; i <= 2; i++) if(hlines[i]) {
                    g.setColor(Color.black);
                    for(Dot d : visibles) {
                        int[] src = pos.get(d);
                        for(Dot o : d.img.keySet()) if(o.sq.equals(Sq.HOPF[i]) && frameVisibles.contains(o.base)) {
                            int[] dest = pos.get(o.base);
                            if(src == null) System.err.printf("src is null, dot %s, s=%d\n", d, d.s);
                            if(dest == null) System.err.printf("dest is null, dot %s, s=%d\n", o, o.s);
                            g.drawLine(src[0], src[1], dest[0], dest[1]);
                        }
                    }
                }

                /* draw potential alg Novikov differentials */
                if(diff && x >= 1) {
                    g.setColor(Color.green);
                    for(int j = 2; ; j++) {
                        if(! backend.isComputed(y+j, x-1 + y+j))
                            break;
                        Dot[] ogen = backend.gens(y+j, x-1 + y+j);
                        for(Dot d : visibles) {
                            int[] src = pos.get(d);
                            for(Dot o : ogen)
                                if(o.nov == d.nov + 1 && frameVisibles.contains(o)) {
                                    int[] dest = pos.get(o);
                                    if(src == null) System.err.printf("src is null, dot %s, s=%d\n", d, d.s);
                                    if(dest == null) System.err.printf("dest is null, dot %s, s=%d\n", o, o.s);
                                    g.drawLine(src[0], src[1], dest[0], dest[1]);
                                }
                        }
                    }
                }

                /* draw potential Cartan differentials */
                if(cartdiff && x >= 1 && backend.isComputed(y+1, y+1 + x-1)) {
                    g.setColor(Color.red);
                    Dot[] ogen = backend.gens(y+1, y+1 + x-1);

                    for(Dot o : ogen) if(frameVisibles.contains(o)) {
                        int[] dest = pos.get(o);
                        for(Dot d : visibles)
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

    }

    void setSelected(int x, int y)
    {
        if(x < 0 || y < 0)
            textarea.setText("");
        
        Dot[] gens = backend.gens(y,x+y);
        if(gens == null)
            return;
        String ret = "("+x+","+y+")\n";
        if(gens.length > 0) {
            ret += "Generators:\n";
            for(Dot d : gens) {
                if(d.nov != -1 && (d.nov > max_filt || d.nov < min_filt))
                    continue;
                ret += "("+x+";"+d.idx+")";
                if(d.nov != -1)
                    ret += "(n="+d.nov+")";
                ret += ":\n      ";
                ret += d.img.toStringDelim("\n     + ");
                ret += "\n";
            }
        }
        if(textarea != null)
            textarea.setText(ret);
    }

    public void mouseMoved(MouseEvent evt)
    {
        mx = evt.getX();
        my = evt.getY();

        int x = getx(mx);
        int y = gety(my);
        if(x >= 0 && y >= 0) {
            setSelected(x,y);
        } else {
            setSelected(-1,-1);
        }
    }

    public void mouseDragged(MouseEvent evt)
    {
        int dx = evt.getX() - mx;
        int dy = evt.getY() - my;

        mx = evt.getX();
        my = evt.getY();

        viewx += dx;
        viewy += dy;

        repaint();
    }

    public void ping()
    {
        repaint();
    }

    public static void constructFrontend(ResBackend back) 
    {
        JFrame fr = new JFrame("Resolution");
        fr.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        fr.setSize(1200,800);
        
        ResDisplay d = new ResDisplay(back);
        back.register_listener(d);
        fr.getContentPane().add(d);
        
        fr.getContentPane().add(new ControlPanel(d), BorderLayout.EAST);
        fr.setVisible(true);
    }

}

class ControlPanel extends Box {

    ControlPanel(final ResDisplay parent)
    {
        super(BoxLayout.Y_AXIS);

        final JSpinner s1 = new JSpinner(new SpinnerNumberModel(0,0,1000,1));
        final JSpinner s2 = new JSpinner(new SpinnerNumberModel(5,0,1000,1));

        s1.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                parent.min_filt = (Integer) s1.getValue();
                parent.repaint();
            }
        });
        
        s2.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                parent.max_filt = (Integer) s2.getValue();
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
            h.add(hlines);
            h.add(hhide);
            
            h.setAlignmentX(-1.0f);
            add(h);
        }
        add(Box.createVerticalStrut(20));

        parent.textarea = new JTextArea();
        Dimension textdim = new Dimension(250,10000);
        parent.textarea.setMaximumSize(textdim);
        parent.textarea.setPreferredSize(textdim);
        parent.textarea.setAlignmentX(-1.0f);
        add(parent.textarea);
    }

}

