package res.algebras;

import res.algebratypes.*;

// should be MultigradedElement<>, but the Graded case is important too
public class PEMonomial implements GradedElement<PEMonomial>
{
    public int[] deg;
    public int[] exponents;
    public String name;

    @Override public int[] multideg() {
        return deg;
    }

    @Override public int deg() {
        return deg[0];
    }

    @Override public String toString() {
        return name;
    }

    @Override public String extraInfo() {
        return "";
    }

    @Override public int compareTo(PEMonomial o)
    {
        for(int i = 0; i < exponents.length; i++) {
            int d = exponents[i] - o.exponents[i];
            if(d != 0) return d;
        }
        return 0;
    }
}
