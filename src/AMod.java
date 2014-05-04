import java.util.*;

abstract class AMod
{
    abstract Iterable<Dot> basis(int deg);
    abstract DModSet act(Dot o, Sq sq);

    public Iterable<DModSet> basis_wrap(final int deg)
    {
        return new Iterable<DModSet>() {
            @Override public Iterator<DModSet> iterator() {
                return new Iterator<DModSet>() {
                    Iterator<Dot> underlying = basis(deg).iterator();
                    @Override public boolean hasNext() { return underlying.hasNext(); }
                    @Override public DModSet next() { return new DModSet(underlying.next()); }
                    @Override public void remove() { underlying.remove(); }
                };
            }
        };
    }
}

class Sphere extends AMod
{
    Dot d = new Dot(-1,0,0);
    Sphere() {
        d = new Dot(-1,0,0);
        d.nov = 0;
    }

    @Override public Iterable<Dot> basis(int deg) {
        List<Dot> ret = new ArrayList<Dot>();
        if(deg == 0)
            ret.add(d);
        return ret;
    }

    @Override public DModSet act(Dot o, Sq sq)
    {
        DModSet ret = new DModSet();
        if(sq.equals(Sq.ID))
            ret.add(d,1);
        return ret;
    }
}

class CofibEta extends AMod
{
    Dot d1 = new Dot(-1,0,0);
    Dot d2 = new Dot(d1, Sq.HOPF[0]);
    @Override public Iterable<Dot> basis(int deg) {
        List<Dot> ret = new ArrayList<Dot>();
        if(deg == 0)
            ret.add(d1);
        if(deg == Sq.HOPF[0].deg())
            ret.add(d2);
        return ret;
    }

    @Override public DModSet act(Dot o, Sq sq)
    {
        DModSet ret = new DModSet();
        if(o.t == d1.t && sq.equals(Sq.ID))
            ret.add(d1,1);
        if(o.t == d2.t && sq.equals(Sq.ID))
            ret.add(d2,1);
        if(o.t == d1.t && sq.equals(Sq.HOPF[0]))
            ret.add(d2,1);
        return ret;
    }
}

