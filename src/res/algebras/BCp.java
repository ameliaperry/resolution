package res.algebras;

import res.Config;
import res.algebratypes.*;

public class BCp extends GradedPolynomialExteriorAlgebra implements GradedAlgebraWithAction<PEMonomial,Sq>
{
    private BCp(int[][] edegs, String[] enames, int[][] pdegs, String[] pnames) {
        super(edegs, enames, pdegs, pnames);
    }

    public static BCp create()
    {
        if(Config.P == 2)
            return new BCp(new int[][] {}, new String[] {}, new int[][] {new int[] {1}}, new String[] {"\u03c4"});
        else
            return new BCp(new int[][] {new int[] {1}}, new String[] {"\u03c3"}, new int[][] {new int[] {2}}, new String[] {"\u03c4"});
    }

    /* needed for Borel cohomology twisted product */
    @Override public ModSet<PEMonomial> times_r(PEMonomial m, Sq q) {
        if(q.equals(Sq.UNIT)) return new ModSet<PEMonomial>(m);
        /* TODO ! */
        return null;
    }
}

