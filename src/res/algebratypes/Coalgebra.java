package res.algebratypes;

public interface Coalgebra<T> extends Comodule<T,T>
{
    int counit(T a);
}

