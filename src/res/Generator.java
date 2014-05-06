package res;

import res.algebra.*;

public class Generator<T extends GradedElement<T>> implements Comparable<Generator<T>>
{
    public DModSet<T> img;

    public int s;
    public int t;
    public int idx;
    public int nov;
    
    public Generator(int s, int t, int idx)
    {
        this.s = s;
        this.t = t;
        this.idx = idx;
        img = new DModSet<T>();
    }
            
    @Override public int compareTo(Generator<T> b)
    {
        if(s != b.s) return s - b.s;
        if(t != b.t) return t - b.t;
        if(nov != -1 && b.nov != -1 && nov != b.nov) return nov - b.nov;
        return idx - b.idx;
    }
}
