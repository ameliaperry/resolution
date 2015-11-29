package res.algebras;

import res.*;
import java.util.*;

public class CofibHopf extends GradedModule<Sq,Sq>
{
    private int i;
    private int hideg;

    /* maybe subtle issues on the number of gradings? should this follow the number of extra gradings on the algebra in use? */
    public CofibHopf(int i)
    {
        this.i = i;
        this.hideg = Sq.HOPF[i].deg[1];
    }

    @Override public Iterable<Sq> basis(int deg)
    {
        if(deg == 0) return Collections.singleton(Sq.UNIT);
        if(deg == hideg) return Collections.singleton(Sq.HOPF[i]);
        return Collections.emptySet();
    }

    @Override public ModSet<Sq> times(Sq o, Sq sq)
    {
        ModSet<Sq> ret = new ModSet<Sq>();
        if(o.deg[1] == 0) {
            if(sq.equals(Sq.UNIT) || sq.equals(Sq.HOPF[i]))
            ret.add(sq,1);
        } else if(o.deg[1] == hideg && sq.equals(Sq.UNIT))
            ret.add(Sq.HOPF[i],1);
        return ret;
    }
}

