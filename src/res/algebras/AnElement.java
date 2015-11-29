package res.algebras;

import res.algebratypes.*;

public class AnElement extends AbstractGradedElement<AnElement>
{
    static final int[] EGR = {};
    ModSet<Sq> modset;
    int deg;
    int idx;
    static int idxcounter = 0;

    static final AnElement UNIT = new AnElement(new ModSet<Sq>(Sq.UNIT), 0);

    AnElement(ModSet<Sq> ms, int d) {
        modset = ms;
        deg = d;
        idx = idxcounter++;
    }

    @Override public int deg() {
        return deg;
    }
    @Override public int compareTo(AnElement o) {
        return idx - o.idx;
    }
    @Override public boolean equals(Object o) {
        return ((AnElement)o).idx == idx;
    }

    @Override public String toString() {
        return "(" + modset.toString() + ")";
    }
}

