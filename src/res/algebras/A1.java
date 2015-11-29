package res.algebras;

import res.*;
import java.util.*;

public class A1 extends AbstractGradedModule<Sq>
{
    private static Sq sq0 = new Sq(0);
    private static Sq sq1 = new Sq(1);
    private static Sq sq2 = new Sq(2);
    private static Sq sq3 = new Sq(3);
    private static Sq sq4 = new Sq(4);
    private static Sq sq5 = new Sq(5);
    private static Sq sq6 = new Sq(6);
    private static Sq sq2sq1 = new Sq(new int[] {2,1});
    private static Sq sq3sq1 = new Sq(new int[] {3,1});
    private static Sq sq4sq1 = new Sq(new int[] {4,1});
    private static Sq sq5sq1 = new Sq(new int[] {5,1});
    private ArrayList<Dot<Sq>> deg3 = new ArrayList<Dot<Sq>>();

    public A1() {
        deg3.add(sq2sq1);
        deg3.add(sq3);
    }

    @Override public Iterable<Dot<Sq>> basis(int deg)
    {
        switch(deg) {
        case 0: return Collections.singleton(sq0);
        case 1: return Collections.singleton(sq1);
        case 2: return Collections.singleton(sq2);
        case 3: return deg3;
        case 4: return Collections.singleton(sq3sq1);
        case 5: return Collections.singleton(sq4sq1);
        case 6: return Collections.singleton(sq5sq1);
        default: return Collections.emptySet();
        }
    }

    @Override public ModSet<Sq> act(Sq o, Sq sq)
    {
        ModSet<Sq> ret = new ModSet<Sq>();
        if(sq.equals(sq0)) ret.add(o,1);
        else if(o.equals(sq0)) {
            if(sq.equals(sq6)) ret.add(sq5sq1,1);
            else if(sq.equals(sq1) || sq.equals(sq2) || sq.equals(sq3) || sq.equals(sq2sq1) || sq.equals(sq3sq1) || sq.equals(sq4sq1) || sq.equals(sq5sq1))
                ret.add(sq,1);
        } else if(o.equals(sq1)) {
            if(sq.equals(sq2)) ret.add(sq2sq2,1);
            else if(sq.equals(sq3)) ret.add(sq3sq1,1);
            else if(sq.equals(sq4)) ret.add(sq4sq1,1);
            else if(sq.equals(sq5)) ret.add(sq5sq1,1);
        } else if(o.equals(sq2)) {
            if(sq.equals(sq1)) ret.add(sq3,1);
            else if(sq.equals(sq2)) ret.add(sq3sq1,1);
            else if(sq.equals(sq2sq1)) ret.add(sq4sq1,1);
            else if(sq.equals(sq3sq1)) ret.add(sq5sq1,1);
        }
        else if(o.equals(sq2sq1)) { if(sq.equals(sq1)) ret.add(sq3sq1,1); else if(sq.equals(sq2sq1)) ret.add(sq5sq1,1); }
        else if(o.equals(sq3)) { if(sq.equals(sq2)) ret.add(sq4sq1,1); else if(sq.equals(sq3)) ret.add(sq5sq1,1); }
        else if(o.equals(sq3sq1)) { if(sq.equals(sq2)) ret.add(sq5sq1,1); }
        else if(o.equals(sq4sq1)) { if(sq.equals(sq1)) ret.add(sq5sq1,1); }
        return ret;
    }
}

