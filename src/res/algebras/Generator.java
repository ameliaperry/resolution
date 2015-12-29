package res.algebras;

import res.algebratypes.*;

public class Generator<T> implements MultigradedElement<Generator<T>>
{
    public ModSet<?> img;

    public int[] deg;
    public int idx;
    public String extraInfo = "";
    
    public <U> Generator(int[] deg, int idx)
    {
        this.deg = deg;
        this.idx = idx;
        img = new ModSet<U>();
    }
            
    @Override public int compareTo(Generator<T> b)
    {
        if(deg.length != b.deg.length)
            return deg.length - b.deg.length;
        for(int i = 0; i < deg.length; i++)
            if(deg[i] != b.deg[i])
                return deg[i] - b.deg[i];
        return idx - b.idx;
    }

    @Override public int[] multideg()
    {
        return deg;
    }

    @Override public boolean equals(Object o)
    {
        Generator<?> g = (Generator<?>) o;
        for(int i = 0; i < idx; i++)
            if(deg[i] != g.deg[i])
                return false;
        return (idx == g.idx);
    }

    @Override public String extraInfo()
    {
        String ret = extraInfo;
        ret += "Image: "+img;
        return ret;
    }
}

