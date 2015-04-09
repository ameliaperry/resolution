package res.algebra;

import res.*;
import java.util.*;

public class A1 extends GradedModule<Sq>
{
    private Generator<Sq> g = new Generator<Sq>(new int[] {-1,0,0}, 0);
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
    private Dot<Sq> d0 = new Dot<Sq>(g,sq0);
    private Dot<Sq> d1 = new Dot<Sq>(g,sq1);
    private Dot<Sq> d2 = new Dot<Sq>(g,sq2);
    private Dot<Sq> d3 = new Dot<Sq>(g,sq2sq1);
    private Dot<Sq> d4 = new Dot<Sq>(g,sq3);
    private Dot<Sq> d5 = new Dot<Sq>(g,sq3sq1);
    private Dot<Sq> d6 = new Dot<Sq>(g,sq4sq1);
    private Dot<Sq> d7 = new Dot<Sq>(g,sq5sq1);
    private ArrayList<Dot<Sq>> deg3 = new ArrayList<Dot<Sq>>();

    public A1() {
        deg3.add(d3);
        deg3.add(d4);
    }

    @Override public Iterable<Dot<Sq>> basis(int deg)
    {
        switch(deg) {
        case 0: return Collections.singleton(d0);
        case 1: return Collections.singleton(d1);
        case 2: return Collections.singleton(d2);
        case 3: return deg3;
        case 4: return Collections.singleton(d5);
        case 5: return Collections.singleton(d6);
        case 6: return Collections.singleton(d7);
        default: return Collections.emptySet();
        }
    }

    @Override public DModSet<Sq> act(Dot<Sq> o, Sq sq)
    {
        DModSet<Sq> ret = new DModSet<Sq>();
        if(sq.equals(sq0)) ret.add(o,1);
        if(o.equals(d0)) {
            if(sq.equals(sq1)) ret.add(d1,1);
            else if(sq.equals(sq2)) ret.add(d2,1);
            else if(sq.equals(sq3)) ret.add(d4,1);
            else if(sq.equals(sq2sq1)) ret.add(d3,1);
            else if(sq.equals(sq3sq1)) ret.add(d5,1);
            else if(sq.equals(sq4sq1)) ret.add(d6,1);
            else if(sq.equals(sq5sq1) || sq.equals(sq6)) ret.add(d7,1);
        }
        else if(o.equals(d1)) {
            if(sq.equals(sq2)) ret.add(d3,1);
            else if(sq.equals(sq3)) ret.add(d5,1);
            else if(sq.equals(sq4)) ret.add(d6,1);
            else if(sq.equals(sq5)) ret.add(d7,1);
        }
        else if(o.equals(d2)) {
            if(sq.equals(sq1)) ret.add(d4,1);
            else if(sq.equals(sq2)) ret.add(d5,1);
            else if(sq.equals(sq2sq1)) ret.add(d6,1);
            else if(sq.equals(sq3sq1)) ret.add(d7,1);
        }
        else if(o.equals(d3)) { if(sq.equals(sq1)) ret.add(d5,1); else if(sq.equals(sq2sq1)) ret.add(d7,1); }
        else if(o.equals(d4)) { if(sq.equals(sq2)) ret.add(d6,1); else if(sq.equals(sq3)) ret.add(d7,1); }
        else if(o.equals(d5)) { if(sq.equals(sq2)) ret.add(d7,1); }
        else if(o.equals(d6)) { if(sq.equals(sq1)) ret.add(d7,1); }
        return ret;
    }
}

