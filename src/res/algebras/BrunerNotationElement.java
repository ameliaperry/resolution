package res.algebras;

import res.algebratypes.*;

public class BrunerNotationElement extends AbstractGradedElement<BrunerNotationElement>
{
    int idx;
    int deg;
    BrunerNotationElement(int idx, int deg) {
        this.idx = idx;
        this.deg = deg;
    }

    @Override public int deg() {
        return deg;
    }

    @Override public String toString() {
        return "#"+idx;
    }

    @Override public int compareTo(BrunerNotationElement o) {
        return idx - o.idx;
    }

    @Override public boolean equals(Object o) {
        return ((BrunerNotationElement) o).idx == idx;
    }
}

