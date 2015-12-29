package res.algebras;

import res.algebratypes.*;

public class GradedPolynomialExteriorAlgebra extends PolynomialExteriorAlgebra implements GradedAlgebra<PEMonomial>
{
    public GradedPolynomialExteriorAlgebra(int[][] egenp, String[] enames, int[][] pgenp, String[] pnames) {
        super(egenp, enames, pgenp, pnames);
    }

    public static GradedPolynomialExteriorAlgebra create(int[] egens, String[] enames, int[] pgens, String[] pnames)
    {
        int[][] egenp = new int[egens.length][1];
        for(int i = 0; i < egens.length; i++) egenp[i][0] = egens[i];
        int[][] pgenp = new int[pgens.length][1];
        for(int i = 0; i < pgens.length; i++) pgenp[i][0] = pgens[i];
        return new GradedPolynomialExteriorAlgebra(egenp, enames, pgenp, pnames);
    }

    @Override public Iterable<PEMonomial> gens(int deg) {
        return gens(new int[] {deg});
    } 
}

