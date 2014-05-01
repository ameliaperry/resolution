import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;

class ResDisplay extends JPanel implements PingListener, MouseMotionListener {

    int min_filt = 0;
    int max_filt = 5;

    boolean diff = false;
    boolean cartdiff = true;

    int viewx = 0;
    int viewy = 0;
    int mx = -1;
    int my = -1;

    JTextArea textarea = null;

    ResDisplay() {
        addMouseMotionListener(this);
    }

    int getcx(int x) {
        return 30 + 20 * x + viewx;
    }
    int getcy(int y) {
        return getHeight() - 40 - 20 * y + viewy;
    }
    int getx(int cx) {
        cx -= (30 + viewx);
        cx += 10;
        return cx / 20;
    }
    int gety(int cy) {
        cy = cy - getHeight() + 40 - viewy;
        cy = -cy;
        cy += 10;
        return cy / 20;
    }

    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);

        /* draw axes */
        for(int x = 0; x < 100; x++) {
            g.setColor(Color.lightGray);
            g.drawLine(getcx(x)-10, getcy(0)+10,getcx(x)-10,getcy(100)+10);
            g.drawLine(getcx(0)-10, getcy(x)+10,getcx(100)+10,getcy(x)+10);

            if(x % 5 == 0) {
                g.setColor(Color.black);
                g.drawString(String.valueOf(x), getcx(x)-8, getcy(-1)+5);
                g.drawString(String.valueOf(x), getcx(-1)-8, getcy(x)+5);
            }
        }

        for(int x = 0; x < 100; x++) {
            for(int y = 1; y < 100; y++) {
                int cx = getcx(x);
                int cy = getcy(y);

                TreeSet<Integer> nov = new TreeSet<Integer>();
                int degree = 0;
                int[] gr = ResMain.extra_grading(y, x+y);
                if(gr != null) {
                    for(int i : gr) {
                        if(i >= min_filt && i <= max_filt) {
                            degree++;
                            if(i != max_filt) nov.add(i+1);
                        }
                    }
                } else {
                    degree = ResMain.ngens(y, x+y);
                }

                g.setColor(Color.black);
                if(degree > 0) {
                    g.drawString("" + degree, cx-3, cy+5);
                } else if(degree < 0) {
                    g.fillRect(cx-10,cy-10,20,20);
                }
                     
                /* draw potential alg Novikov differentials */
                if(diff && ! nov.isEmpty()) {
                    for(int j = 2; ; j++) {
                        int[] ogr = ResMain.extra_grading(y+j, x-1+y+j);
                        if(ogr == null) break;
                        boolean found = false;
                        for(int i : ogr)
                            if(nov.contains(i))
                               found = true; 
                        if(!found) continue;
                        g.setColor(Color.green);
                        g.drawLine(cx-2, cy-2, getcx(x-1)+2, getcy(y+j)+2);
                    }
                }

                /* draw potential Cartan differentials */
                if(cartdiff && ! nov.isEmpty()) {
                    int[] ogr = ResMain.extra_grading(y+1, x+y);
                    if(ogr != null && ogr.length > 0) {
                        boolean found = false;
                        for(int i : ogr) {
                            if(i >= nov.first() + 1) {
                                found = true;
                                break;
                            }
                        }
                        if(found) {
                            g.setColor(Color.red);
                            g.drawLine(cx-2, cy-2, getcx(x-1)+2, getcy(y+1)+2);
                        }
                    }
                }

            }
        }

    }

    void setSelected(int x, int y)
    {
        if(x < 0 || y < 0)
            textarea.setText("");
        
        DModSet[] gimg = ResMain.gimg(y,x+y);
        int[] nov = ResMain.extra_grading(y,x+y);
        if(gimg == null)
            return;
        String ret = "("+x+","+y+")\n";
        if(gimg.length > 0) {
            ret += "Generators:\n";
            for(int i = 0; i < gimg.length; i++) {
                if(nov != null && (nov[i] > max_filt || nov[i] < min_filt))
                    continue;
                ret += "("+x+";"+i+")";
                if(nov != null)
                    ret += "(n="+nov[i]+")";
                ret += ":\n      ";
                ret += gimg[i].toStringDelim("\n     + ");
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
        if(x >= 0 && x <= 100 && y >= 1 && y <= 100) {
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

    public static void main(String[] args) 
    {
        JFrame fr = new JFrame("Resolution");
        fr.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        fr.setSize(1200,800);
        
        ResDisplay d = new ResDisplay();
        fr.getContentPane().add(d);
        ResMain.register_listener(d);
        
        fr.getContentPane().add(new ControlPanel(d), BorderLayout.EAST);

        fr.setVisible(true);

        ResMain.main(args);
    }

}

class ControlPanel extends Box {

    ControlPanel(final ResDisplay parent)
    {
        super(BoxLayout.Y_AXIS);

        final JSpinner s1 = new JSpinner(new SpinnerNumberModel(0,0,100,1));
        final JSpinner s2 = new JSpinner(new SpinnerNumberModel(5,0,100,1));

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

        parent.textarea = new JTextArea();
        Dimension textdim = new Dimension(250,10000);
        parent.textarea.setMaximumSize(textdim);
        parent.textarea.setPreferredSize(textdim);
        parent.textarea.setAlignmentX(-1.0f);
        add(parent.textarea);
    }

}

