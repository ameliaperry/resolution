package res.algebra;

import res.*;
import java.util.*;

/* A formal F_p-linear combination of things of type T. */
public class ModSet<T> extends TreeMap<T,Integer>
{
    public ModSet() {}
    public ModSet(T t) {
        add(t,1);
    }
    public ModSet(Comparator<? super T> comp) {
        super(comp);
    }

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

    @Override public String toString()
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
    
    public String toString(Stringifier<T> strf) {
        if(isEmpty())
            return "0";
        String s = "";
        for(Map.Entry<T,Integer> e : entrySet()) {
            if(s.length() != 0)
                s += " + ";
            if(e.getValue() != 1)
                s += e.getValue();
            s += strf.toString(e.getKey());
        }
        return s;
    }
}

