package res.algebratypes;

public interface Comodule<T,U>
{
    public ModSet<Pair<T,U>> diagonal(T o);
}

