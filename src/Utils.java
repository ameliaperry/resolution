import java.util.*;

/* A formal F_p-linear combination of things of type T. */
class ModSet<T> extends TreeMap<T,Integer>
{
    public void add(T d, int mult)
    {
        int c;
        if(containsKey(d)) c = get(d);
        else c = 0;

        c = ResMath.dmod(c + mult);

        if(c == 0) 
            remove(d);
        else
            put(d, c);
    } 

    public void add(ModSet<T> d, int mult)
    {
        for(Map.Entry<T,Integer> e : d.entrySet())
            add(e.getKey(), e.getValue());
    }

    public int getsafe(T d)
    {
        Integer i = get(d);
        if(i == null)
            return 0;
        return i;
    }

    public boolean contains(T d)
    {
        return (getsafe(d) % Config.P != 0);
    }

    public void union(ModSet<T> s)
    {
        for(T d : s.keySet()) {
            if(!containsKey(d))
                put(d,1);
        }
    }

    public String toString()
    {
        return toStringDelim(" + ");
    }

    public String toStringDelim(String delim)
    {
        if(isEmpty())
            return "0";
        String s = "";
        for(Map.Entry<T,Integer> e : entrySet()) {
            if(s.length() != 0)
                s += delim;
            if(e.getValue() != 1)
                s += e.getValue();
            s += e.getKey().toString();
        }
        return s;
    }
}

class Dot implements Comparable<Dot>
{
    static int id_count = 0;

    /* kernel basis vector */
    Sq sq;
    Dot base;
    int s;
    int t;
    int idx = -1;
    int nov = -1;
    DModSet img;

    Dot(Dot base, Sq sq) { /* square dot */
        this.base = base;
        this.sq = sq;
        s = base.s;
        t = base.t + sq.deg();
        nov = sq.containsBeta() ? base.nov : base.nov + 1;
    }
    Dot(int s, int t, int idx) { /* generator dot */
        this.s = s;
        this.t = t;
        this.idx = idx;
        sq = Sq.ID;
        base = this;
        img = new DModSet();
    }

    public int hashCode()
    {
        int hash = sq.hashCode();
        hash *= 27863521;
        hash ^= t;
        hash *= 27863521;
        hash ^= base.idx;
        return hash;
    }
    public String toString()
    {
        String ret = sq.toString() + "(" + base.t + ";" + base.idx + ")";
        if(nov != -1)
            ret += "(n=" + nov + ")";
        return ret;
    }
    public boolean equals(Object o)
    {
        Dot d = (Dot) o;
        return (d.base.t == base.t && d.base.idx == base.idx && d.sq.equals(sq));
    }

    @Override public int compareTo(Dot o)
    {
//        System.out.printf("compare: %s versus %s\n", a, b);
        /* XXX tweak this for performance */
        if(base.t != o.base.t)
            return base.t - o.base.t;
        if(base.idx != o.base.idx)
            return o.base.idx - base.idx;
        /* they're in the same degree, so this should work */
        for(int i = 0; i < sq.q.length; i++) {
            if(Config.DEBUG) Main.die_if(i >= o.sq.q.length, "SQ MISMATCH "+sq+" vs "+o.sq+". Comparing "+this+" with "+o);
            if(sq.q[i] != o.sq.q[i])
                return -o.sq.q[i] + sq.q[i];
        }
        return 0;
    }
}

class DModSet extends ModSet<Dot> { /* to work around generic array restrictions */
    DModSet() {}
    DModSet(Dot d) {
        add(d,1);
    }
    public Dot[] toArray() {
        return keySet().toArray(new Dot[] {});
    }

    public DModSet times(Sq sq)
    {
        DModSet ret = new DModSet();
        for(Map.Entry<Dot,Integer> e1 : entrySet()) {
            Dot d = e1.getKey();
            ModSet<Sq> prod = sq.times(d.sq);
            for(Map.Entry<Sq,Integer> e2 : prod.entrySet()) {
                ret.add(new Dot(d.base, e2.getKey()), e1.getValue() * e2.getValue());
            }
        }
        return ret;
    }
}
