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

class CofibHopf extends AMod
{
    int i;
    Dot d1, d2;
    CofibHopf(int i) {
        this.i = i;
        d1 = new Dot(-1,0,0);
        d2 = new Dot(d1, Sq.HOPF[i]);
    }

    @Override public Iterable<Dot> basis(int deg) {
        List<Dot> ret = new ArrayList<Dot>();
        if(deg == 0)
            ret.add(d1);
        if(deg == d2.t)
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
        if(o.t == d1.t && sq.equals(Sq.HOPF[i]))
            ret.add(d2,1);
        return ret;
    }
}

