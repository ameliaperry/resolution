import java.util.*;

interface AMod<T>
{
    Iterable<T> basis(int deg);
    ModSet<T> act(T t, Sq sq);
}

class Sphere implements AMod<Object>
{
    @Override public Iterable<Object> basis(int deg) {
        List<Object> ret = new ArrayList<Object>();
        if(deg == 0)
            ret.add(new Object());
        return ret;
    }

    @Override public ModSet<Object> act(Object o, Sq sq)
    {
        ModSet<Object> ret = new ModSet<Object>();
        if(sq.equals(Sq.ID))
            ret.add(o,1);
        return ret;
    }
}

