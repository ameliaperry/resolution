package res.algebras;

import res.algebratypes.*;
import java.util.*;

public class CofibHopf extends AbstractGradedModule<Sq,Sq>
{
    private int i;
    private int hideg;

    /* maybe subtle issues on the number of gradings? should this follow the number of extra gradings on the algebra in use? */
    public CofibHopf(int i)
    {
        this.i = i;
        this.hideg = 1<<i;
    }

    @Override public Iterable<Sq> gens(int deg)
    {
        if(deg == 0) return Collections.singleton(Sq.UNIT);
        if(deg == hideg) return Collections.singleton(Sq.HOPF[i]);
        return Collections.emptySet();
    }

    @Override public ModSet<Sq> times(Sq o, Sq sq)
    {
        ModSet<Sq> ret = new ModSet<Sq>();
        if(o.equals(Sq.UNIT)) {
            if(sq.equals(Sq.UNIT) || sq.equals(Sq.HOPF[i]))
                ret.add(sq,1);
        } else if(sq.equals(Sq.UNIT) && o.equals(Sq.HOPF[i]))
            ret.add(Sq.HOPF[i],1);
        return ret;
    }
}

