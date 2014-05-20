package res.algebra;

public class Generator<T extends GradedElement<T>> implements MultigradedElement<Generator<T>>
{
    public DModSet<T> img;

    public int[] deg;
    int idx;
    
    public Generator(int[] deg, int idx)
    {
        this.deg = deg;
        this.idx = idx;
        img = new DModSet<T>();
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

    @Override public int[] deg()
    {
        return deg;
    }
}
