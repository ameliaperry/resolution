import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;

class ResDisplay3D extends JPanel implements PingListener, MouseMotionListener, MouseWheelListener
{
    ResBackend backend;

    static final double SCALENOTCH = 0.9;
    static final double SCALEPIX = 0.994;
    static final double ANGLE = 0.015;

    int[] bounds = { 0, 100, 0, 100, 0, 100 };
    boolean diff = false;
    boolean cartdiff = true;
    double magnify_n = 1.0;

    int mx = -1;
    int my = -1;

    double dist = 100.0;
    double viewscale = 1000.0;
    double[][] mtx = {{1,0,0},{0,1,0},{0,0,1}};
    double[] center = new double[] {30,30,0};
    boolean perspective = true;


    ResDisplay3D(ResBackend back)
    {
        backend = back;
        addMouseMotionListener(this);
        addMouseWheelListener(this);
    }

    String trideg_key(int x, int y, int n)
    {
        return x + "/" + y + "/" + n;
    }

    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);

        /* draw bounding box */
        int[][][][] vts = new int[2][2][2][2];
        for(int i = 0; i <= 1; i++)
            for(int j = 0; j <= 1; j++)
                for(int k = 0; k <= 1; k++)
                    vts[i][j][k] = full_transform(new double[] { bounds[i], bounds[2+j], bounds[4+k] });
        g.setColor(Color.red);
        g.drawLine(vts[0][0][0][0], vts[0][0][0][1], vts[1][0][0][0], vts[1][0][0][1]);
        g.drawLine(vts[0][0][1][0], vts[0][0][1][1], vts[1][0][1][0], vts[1][0][1][1]);
        g.drawLine(vts[0][1][0][0], vts[0][1][0][1], vts[1][1][0][0], vts[1][1][0][1]);
        g.drawLine(vts[0][1][1][0], vts[0][1][1][1], vts[1][1][1][0], vts[1][1][1][1]);
        g.setColor(Color.green);
        g.drawLine(vts[0][0][0][0], vts[0][0][0][1], vts[0][1][0][0], vts[0][1][0][1]);
        g.drawLine(vts[0][0][1][0], vts[0][0][1][1], vts[0][1][1][0], vts[0][1][1][1]);
        g.drawLine(vts[1][0][0][0], vts[1][0][0][1], vts[1][1][0][0], vts[1][1][0][1]);
        g.drawLine(vts[1][0][1][0], vts[1][0][1][1], vts[1][1][1][0], vts[1][1][1][1]);
        g.setColor(Color.blue);
        g.drawLine(vts[0][0][0][0], vts[0][0][0][1], vts[0][0][1][0], vts[0][0][1][1]);
        g.drawLine(vts[0][1][0][0], vts[0][1][0][1], vts[0][1][1][0], vts[0][1][1][1]);
        g.drawLine(vts[1][0][0][0], vts[1][0][0][1], vts[1][0][1][0], vts[1][0][1][1]);
        g.drawLine(vts[1][1][0][0], vts[1][1][0][1], vts[1][1][1][0], vts[1][1][1][1]);

        /* make sure vertices are up to date */
        ArrayList<Vertex> vertices = new ArrayList<Vertex>();
        Map<String, Vertex> tridegs = new HashMap<String, Vertex>();
        for(int x = bounds[0]; x <= bounds[1]; x++) {
            for(int y = bounds[2]; y <= bounds[3]; y++) {
                if(y == 0) continue;

                Dot[] gens = backend.gens(y, x+y);
                if(gens == null) break;
                for(int i = 0; i < gens.length; i++) {
                    Dot d = gens[i];
                    if(d.nov < bounds[4] || d.nov > bounds[5])
                        continue;

                    int offset = 0;
                    for(int j = i+1; j < gens.length; j++)
                        if(gens[j].nov == d.nov)
                            offset++;

                    Vertex v = new Vertex(x, y, i, d.nov);
                    v.offset(offset);
                    v.tp = full_transform(v.p);
                    vertices.add(v);
                    tridegs.put(trideg_key(x,y,d.nov), v);
                }
            } 
        }

        /* draw vertices */
        g.setColor(Color.black);
        for(Vertex v : vertices)
            g.drawRect(v.tp[0] - 1, v.tp[1] - 1, 3, 3);


        /* draw potential alg Novikov differentials */
        if(diff) {
            g.setColor(Color.green);
            for(Vertex v : vertices) {
                for(int y = v.y + 2; y < v.x; y++) {
                    Vertex o = tridegs.get(trideg_key(v.x - 1, y, v.n + 1));
                    if(o != null)
                        g.drawLine(v.tp[0], v.tp[1], o.tp[0], o.tp[1]);
                }
            } 
        }

        /* draw potential Cartan differentials */
        if(cartdiff) {
            g.setColor(Color.red);
            for(Vertex v : vertices) {
                for(int n = v.n + 2; n <= v.y + 1; n++) {
                    Vertex o = tridegs.get(trideg_key(v.x - 1, v.y + 1, n));
                    if(o != null)
                        g.drawLine(v.tp[0], v.tp[1], o.tp[0], o.tp[1]);
                }
            }
        }
    }

    @Override public void mouseDragged(MouseEvent evt)
    {
        int dx = evt.getX() - mx;
        int dy = evt.getY() - my;
        mx = evt.getX();
        my = evt.getY();
        int mod = evt.getModifiers();

        if((mod & MouseEvent.CTRL_MASK) != 0) {
            /* zoom */
            dist *= Math.pow(SCALEPIX, dy);
        } else if((mod & MouseEvent.SHIFT_MASK) != 0) {
            /* pan */
            /* mtx is orthogonal, so transpose is inverse */
            double[][] mtxinv = Matrices.transpose3(mtx);
            /* get unit vectors in screen x and y direction */
            double[] vx = Matrices.transform3(mtxinv, new double[]{-1,0,0});
            double[] vy = Matrices.transform3(mtxinv, new double[]{0,-1,0});
            /* scale them to pixel length */
            for(int i = 0; i < 3; i++) {
                vx[i] *= dist / viewscale;
                vy[i] *= dist / viewscale;
            }
            /* add to current center */
            for(int i = 0; i < 3; i++)
                center[i] += vx[i] * dx + vy[i] * dy;

        } else {
            /* rotate */
            if(dx != 0) {
                mtx = Matrices.mmult3(new double[][] {
                    { Math.cos(dx * ANGLE), 0, -Math.sin(dx * ANGLE) },
                    { 0, 1, 0 },
                    { Math.sin(dx * ANGLE), 0, Math.cos(dx * ANGLE) }
                }, mtx);
            }
            if(dy != 0) {
                mtx = Matrices.mmult3(new double[][] {
                    { 1, 0, 0 },
                    { 0, Math.cos(dy * ANGLE), -Math.sin(dy * ANGLE) },
                    { 0, Math.sin(dy * ANGLE), Math.cos(dy * ANGLE) }
                }, mtx);
            }
        }

        repaint();
    }


    @Override public void mouseWheelMoved(MouseWheelEvent e)
    { /* XXX use precise wheel rotation */
        dist *= Math.pow(SCALENOTCH, e.getWheelRotation());
        repaint();
    }

    public void mouseMoved(MouseEvent evt)
    {
        mx = evt.getX();
        my = evt.getY();
    }

    int[] full_transform(double[] p)
    {
        /* magnify n */
        p[2] *= magnify_n;

        /* translate */
        double[] pv = new double[] {
            p[0] - center[0],
            p[1] - center[1],
            p[2] - center[2]
        };

        /* flip one axis */
        pv[1] *= -1;

        /* rotate */
        double[] t = Matrices.transform3(mtx,pv);

        /* apply perspective and scale */
        if(!perspective) t[2] = 0;
        return new int[] {
            (int) (viewscale * t[0] / (dist + t[2])) + getWidth() / 2,
            (int) (viewscale * t[1] / (dist + t[2])) + getHeight() / 2
        };
    }

    public void ping(int s, int t)
    {
        repaint();
    }

    public static void constructFrontend(ResBackend back) 
    {
        JFrame fr = new JFrame("Resolution 3D");
        fr.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        fr.setSize(1200,800);
        
        ResDisplay3D d = new ResDisplay3D(back);
        fr.getContentPane().add(d);
        back.register_listener(d);

        ControlPanel3D p = new ControlPanel3D(d);
        fr.getContentPane().add(p, BorderLayout.EAST);

        fr.setVisible(true);
    }

}

class Vertex {
    double[] p;
    int[] tp;
    int i;
    int x;
    int y; 
    int n;

    Vertex(int x, int y, int i, int n) {
        this.x = x;
        this.y = y;
        this.i = i;
        this.n = n;
        p = new double[] {x,y,n};
    }

    void offset(int o)
    {
        p[0] += o * 0.1;
        p[1] += o * 0.1;
        p[2] += o * 0.1;
    }
}

class ControlPanel3D extends Box {

    private JSpinner produceJSpinner(final int i, final ResDisplay3D parent)
    {
        final JSpinner ret = new JSpinner(new SpinnerNumberModel(parent.bounds[i],0,1000,1));

        ret.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                parent.bounds[i] = (Integer) ret.getValue();
                parent.repaint();
            }
        });
        
        Dimension smin = new Dimension(0,30);
        ret.setMinimumSize(smin);
        ret.setPreferredSize(smin);
        return ret;
    }

    ControlPanel3D(final ResDisplay3D parent)
    {
        super(BoxLayout.Y_AXIS);

        add(new JLabel("Bounding box:"));
        Box bx = Box.createHorizontalBox();
        bx.add(new JLabel("x:"));
        bx.add(produceJSpinner(0,parent));
        bx.add(produceJSpinner(1,parent));
        add(bx);
        Box by = Box.createHorizontalBox();
        by.add(new JLabel("y:"));
        by.add(produceJSpinner(2,parent));
        by.add(produceJSpinner(3,parent));
        add(by);
        Box bn = Box.createHorizontalBox();
        bn.add(new JLabel("n:"));
        bn.add(produceJSpinner(4,parent));
        bn.add(produceJSpinner(5,parent));
        add(bn);
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
        
        final JCheckBox persp = new JCheckBox("Perspective");
        persp.setSelected(true);
        persp.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                parent.perspective = persp.isSelected();
                parent.repaint();
            }
        });
        add(persp);
        add(Box.createVerticalStrut(20));

        final JSlider mag = new JSlider(1,10,1);
        mag.addChangeListener(new ChangeListener() {
            @Override public void stateChanged(ChangeEvent evt) {
                parent.magnify_n = mag.getValue();
                parent.repaint();
            }
        });
        add(mag);

    }

}

