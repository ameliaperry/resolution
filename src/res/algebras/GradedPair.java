package res.algebras;

import res.algebratypes.*;

public class GradedPair<T extends GradedElement<T>, U extends GradedElement<U>>
    extends Pair<T,U> implements GradedElement<GradedPair<T,U>>
{

    public GradedPair(T a, U b) {
        super(a,b);
    }

    @Override public int deg() {
        return a.deg() + b.deg();
    }

    @Override public int[] multideg() {
        return new int[] { deg() };
    }

    @Override public String extraInfo() {
        return "";
    }

    @Override public String toString() {
        return a.toString() + " \u2297 " + b.toString(); // tensor product
    }

    @Override public int compareTo(GradedPair<T,U> o) {
        int c = a.compareTo(o.a);
        if(c != 0) return c;
        return b.compareTo(o.b);
    }
}

