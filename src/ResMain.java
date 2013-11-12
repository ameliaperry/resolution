import java.util.*;


/* Computes Ext_A^{s,t} (M, Z/2) through a minimal resolution of M. */
/* This seems to work effectively through about t=75, and then becomes prohibitively slow. */

public class ResMain
{

    /* upper bound on total degree to compute */
    static final int T_CAP = 75;
    static final boolean DEBUG = false;

    static HashMap<String,CellData> output = new HashMap<String,CellData>();
    static String keystr(int s, int t) {
        return "("+s+","+t+","+Sq.P+")";
    }

    /* convenience methods for cell data lookup */
    static int ngens(int s, int t) {
        CellData dat = output.get(keystr(s,t));
        die_if(dat == null, "Data null in ("+s+","+t+")");
        return dat.gimg.length;
    }
    static DModSet[] kbasis(int s, int t) {
        CellData dat = output.get(keystr(s,t));
        die_if(dat == null, "Data null in ("+s+","+t+")");
        return dat.kbasis;
    }
    static DModSet[] gimg(int s, int t) {
        CellData dat = output.get(keystr(s,t));
        die_if(dat == null, "Data null in ("+s+","+t+")");
        return dat.gimg;
    }


    /* The Steenrod algebra. */
    static Iterable<Sq> steenrod(int n)
    {
        Iterable<int[]> p = part(n,n);
        Collection<Sq> ret = new ArrayList<Sq>();

        for(int[] q : p)
            ret.add(new Sq(q));

        return ret;
    }

    static Map<String,Iterable<int[]>> part_cache = new TreeMap<String,Iterable<int[]>>();
    static String part_cache_keystr(int n, int max) {
        return "("+n+"/"+max+"/"+Sq.P+")";
    }

    static Iterable<int[]> part(int n, int max)
    {
        if(n == 0) { /* the trivial solution */
            Collection<int[]> ret = new ArrayList<int[]>();
            ret.add(new int[] {});
            return ret;
        }
        if(max == 0) return new ArrayList<int[]>(); /* no solutions */
        Iterable<int[]> ret0 = part_cache.get(part_cache_keystr(n,max));
        if(ret0 != null) return ret0;

        Collection<int[]> ret = new ArrayList<int[]>();

        for(int i = (n+1)/2; i <= max; i++) {
            for(int[] q0 : part(n-i, i/2)) {
                int[] q1 = new int[q0.length + 1];
                q1[0] = i;
                for(int j = 0; j < q0.length; j++)
                    q1[j+1] = q0[j];
                ret.add(q1);
            }
        }

        part_cache.put(part_cache_keystr(n,max), ret);

        return ret;
    }


    /* Computes a basis complement to the image of mat inside the span of okbasis */

    static DModSet[] calc_gimg(DotMatrix mat, DModSet[] okbasis)
    {
        /* sketch idea:
         * do RREF on okbasis.
         * apply the same row ops (matrix mult) to mat.
         * (should find that the zero lines of okbasis are zero in mat)
         * finish RREFing mat from this form
         * find the overall complement of mat, excluding these zero lines
         * transform back
         *
         * To do this more efficiently, we basically rref a huge augmentation: bokbasis | mat | id.
         */

        /* choose an ordering on all keys and values */
        Dot[] keys = mat.keySet().toArray(new Dot[] {});
        DModSet val_set = new DModSet();
        for(DModSet ms : okbasis)
            val_set.union(ms);
        if(DEBUG) System.out.println("val_set: "+val_set);
        Dot[] values = val_set.toArray(new Dot[] {}); 

        /* old code path
         
        /* convert mat to an augmented matrix of booleans *//*
        boolean[][] bmat = new boolean[values.length][keys.length + values.length];
        for(int i = 0; i < values.length; i++) {
            for(int j = 0; j < keys.length; j++) {
                bmat[i][j] = mat.get(keys[j]).contains(values[i]);
            }
            for(int j = 0; j < values.length; j++)
                bmat[i][j + keys.length] = (i == j);
        }
        Matrices.printMatrix("bmat", bmat);  

        /* convert okbasis to an augmented matrix of booleans *//*
        boolean[][] bokbasis = new boolean[values.length][okbasis.length + values.length];
        for(int i = 0; i < values.length; i++) {
            for(int j = 0; j < okbasis.length; j++)
                bokbasis[i][j] = okbasis[j].contains(values[i]);
            for(int j = 0; j < values.length; j++)
                bokbasis[i][j + okbasis.length] = (i == j);
        }
        Matrices.printMatrix("bokbasis", bokbasis);

        /* rref the okbasis *//*
        int[] okb_leading_cols = Matrices.rref(bokbasis, true);
        Matrices.printMatrix("rref(bokbasis)", bokbasis);

        /* extract the row op matrix from the augmented part *//*
        boolean[][] rowop1 = new boolean[values.length][values.length];
        for(int i = 0; i < values.length; i++)
            for(int j = 0; j < values.length; j++)
                rowop1[i][j] = bokbasis[i][j + okbasis.length];
        Matrices.printMatrix("rowop1", rowop1);

        /* use this to partially rref mat *//*
        boolean[][] bmatrr = Matrices.mult(rowop1, bmat);
        Matrices.printMatrix("bmatrr", bmatrr);

        /* finish rrefing mat *//*
        int[] bmat_leading_cols = Matrices.rref(bmatrr, true);
        Matrices.printMatrix("rref(bmatrr)", bmatrr);

        /* At this stage, bokbasis has zero lines starting at l1 (excluding the
         * augmented part), and likewise bmatrr starting at l2 zero lines, where
         * l2 <= l1. Those lines in the difference correspond to our complement.
         * To return this complement to the original basis, feed the unit
         * vectors corresponding to these zero lines into the inverse row
         * transform matrix.
         *//*

        /* determine l1, l2 *//*
        int l1 = okb_leading_cols.length;
        int l2 = bmat_leading_cols.length;
        if(DEBUG) System.out.printf("l1: %2d   l2: %2d\n", l1, l2);
        die_if(l2 > l1, "Unexpected zero-row inequality in calc_gimg()");

        */

        /* NOPE, do it a different way. new code path.
         * this is a little tighter, and avoids using mult() */

        /* construct our huge augmented behemoth */
        boolean[][] aug = new boolean[values.length][okbasis.length + keys.length + values.length];
        for(int i = 0; i < values.length; i++) {
            for(int j = 0; j < okbasis.length; j++)
                aug[i][j] = okbasis[j].contains(values[i]);
            for(int j = 0; j < keys.length; j++)
                aug[i][j + okbasis.length] = mat.get(keys[j]).contains(values[i]);
            for(int j = 0; j < values.length; j++)
                aug[i][j + okbasis.length + keys.length] = (i == j);
        }
        Matrices.printMatrix("aug", aug);

        /* rref it */
        int l1 = Matrices.rref(aug, keys.length + values.length).length;
        Matrices.printMatrix("rref(aug)", aug);

        /* extract mat | id */
        boolean[][] bmatrr = new boolean[values.length][keys.length + values.length];
        for(int i = 0; i < values.length; i++)
            for(int j = 0; j < keys.length + values.length; j++)
                bmatrr[i][j] = aug[i][j + okbasis.length];
        Matrices.printMatrix("bmatrr", bmatrr);

        /* rref it some more */
        int l2 = Matrices.rref(bmatrr, values.length).length;
        Matrices.printMatrix("rref(bmatrr)", bmatrr);
        if(DEBUG) System.out.printf("l1: %2d   l2: %2d\n", l1, l2);


        /* now we're back to the old code path */
        
        /* extract and invert (via rref) the row transform matrix */
        boolean[][] transf = new boolean[values.length][2 * values.length];
        for(int i = 0; i < values.length; i++) {
            for(int j = 0; j < values.length; j++)
                transf[i][j] = bmatrr[i][j + keys.length];
            for(int j = 0; j < values.length; j++)
                transf[i][j + values.length] = (i == j);
        }
        Matrices.printMatrix("transf", transf);
        Matrices.rref(transf, values.length);
        Matrices.printMatrix("rref(transf)", transf); 

        /* read off the output */
        DModSet[] ret = new DModSet[l1 - l2];
        for(int j = l2; j < l1; j++) {
            DModSet out = new DModSet();
            for(int i = 0; i < values.length; i++) {
                if(transf[i][j + values.length])
                    out.add(values[i]);
            }
            ret[j - l2] = out;
        }

        return ret;
    }


    /* Main minimal-resolution procedure. */

    static void resolve(AMod a)
    {

        for(int t = 0; t <= T_CAP; t++) {

            /* first handle the s=0 case: process the input */
            /* XXX TMP just using the sphere as input */
            CellData dat0 = new CellData();
            dat0.gimg = new DModSet[] {};
            if(t == 0) {
                dat0.kbasis = new DModSet[] {};
            } else {
                List<DModSet> kbasis0 = new ArrayList<DModSet>();
                for(Sq q : steenrod(t)) {
                    DModSet ms = new DModSet();
                    ms.add(new Dot(q,0,0));
                    kbasis0.add(ms);
                }
                dat0.kbasis = kbasis0.toArray(new DModSet[] {});
            }
            System.out.printf("(%2d,%2d): %2d gen, %2d ker\n", 0, t, 0, dat0.kbasis.length);
            output.put(keystr(0,t), dat0);


            /* now the typical s>0 case */

            for(int s = 1; s <= t; s++) {

                /* compute the basis for this resolution bidegree */
                ArrayList<Dot> basis_l = new ArrayList<Dot>();
                for(int gt = s; gt < t; gt++) {
                    for(int i = 0; i < ngens(s,gt); i++) {
                        for(Sq q : steenrod(t - gt)) {
                            basis_l.add(new Dot(q,gt,i));
                        }
                    }
                }
                Dot[] basis = basis_l.toArray(new Dot[]{});
                DModSet[] okbasis = kbasis(s-1,t);

                /* compute what the map does in this basis */
                DotMatrix mat = new DotMatrix();
                if(DEBUG) System.out.printf("(%d,%d) Map:\n",s,t);
                for(int i = 0; i < basis.length; i++) {

                    /* compute the image of this basis vector */
                    DModSet image = new DModSet();
                    for(Dot d : gimg(s,basis[i].t)[basis[i].idx]) {
                        Iterable<Sq> c = basis[i].sq.times(d.sq);
                        for(Sq q : c)
                            image.add(new Dot(q, d.t, d.idx));
                    }

                    if(DEBUG) System.out.println("Image of "+basis[i]+" is "+image);

                    mat.put(basis[i], image);

                }

                /* the kernel of mat is kbasis */
                DModSet[] kbasis = mat.ker();
                if(DEBUG && kbasis.length != 0) {
                    System.out.println("Kernel:");
                    for(DModSet dm : kbasis)
                        System.out.println(dm);
                }


                /* from mat and okbasis, produce gimg */
                if(DEBUG) System.out.printf("\ngimg at (%d,%d)\n", s,t);
                DModSet[] gimg = calc_gimg(mat, okbasis);
                if(DEBUG) System.out.println();

                System.out.printf("(%2d,%2d): %2d gen, %2d ker\n", s, t, gimg.length, kbasis.length);
                if(DEBUG && gimg.length > 0) {
                    System.out.println("Generators:");
                    for(DModSet g : gimg) System.out.println(g);
                }
                if(DEBUG) System.out.println();
                output.put(keystr(s,t), new CellData(gimg, kbasis));

                /* XXX */
                /* display.repaint(); */
            }
        }
    }


    static void die_if(boolean test, String fail)
    {
        if(test) {
            System.err.println(fail);
            Thread.dumpStack();
            System.err.println("Failing.");
            System.exit(1);
        }
    }


    public static void main(String[] args)
    {
        /* init */
        /* make the sphere A-module */

        /* resolve */
        resolve(null); /* TMP */

        /* print */
        System.out.println("Conclusion:");
        for(int s = T_CAP - 1; s >= 0; s--) {
            for(int t = s; t < T_CAP; t++) {
                int n = ngens(s,t);
                if(n > 0)
                    System.out.printf("%2d ", ngens(s,t));
                else System.out.print("   ");
            }
            System.out.println("###");
        }

    }

}


class CellData
{
    DModSet[] gimg; /* images of generators as dot-sums in bidegree s-1,t*/
    DModSet[] kbasis; /* kernel basis dot-sums in bidegree s,t */

    CellData() { }
    CellData(DModSet[] g, DModSet[] k) {
        gimg = g;
        kbasis = k;
    }
}


class Dot
{
    /* kernel basis vector */
    Sq sq;
    int t;
    int idx;

    String id_cache = null;

    Dot(Sq _sq, int _t, int _idx) {
        sq = _sq; t = _t; idx = _idx;
    }

    public int hashCode()
    {
        return toString().hashCode();
    }
    public String toString()
    {
        if(id_cache == null)
            id_cache = sq.toString() + "(" + t + ";" + idx + ")";
        return id_cache;
    }
    public boolean equals(Object o)
    {
        return o.hashCode() == hashCode();
    }
}


class Sq
{
    int[] q; /* Indices of the power operations. -1 indicates Bockstein. */

    Sq(int[] qq) { q = qq; }

    Iterable<Sq> times(Sq o)
    {
        int[] ret = new int[q.length + o.q.length];
        for(int i = 0; i < q.length; i++)
            ret[i] = q[i];
        for(int i = 0; i < o.q.length; i++)
            ret[q.length + i] = o.q[i];

        return new Sq(ret).resolve();
    }

    static boolean binom(int a, int b)
    {
        return ((~a) & b) == 0;
    }

    Iterable<Sq> resolve()
    {
        ModSet<Sq> ret = new ModSet<Sq>();

        for(int i = q.length - 2; i >= 0; i--) {
            int a = q[i];
            int b = q[i+1];

            if(a >= 2*b)
                continue;

            /* apply Adem relation */
            for(int c = 0; c <= a/2; c++) {

                if(! binom(b - c - 1, a - 2*c))
                    continue;

                int[] t;
                if(c == 0) {
                    t = Arrays.copyOf(q, q.length - 1);
                    for(int k = i+2; k < q.length; k++)
                        t[k-1] = q[k];
                    t[i] = a+b-c;
                } else {
                    t = Arrays.copyOf(q, q.length);
                    t[i] = a+b-c;
                    t[i+1] = c;
                }

                /* recurse */
                for(Sq sub : new Sq(t).resolve())
                    ret.add(sub);
            }

            return ret;
        }

        /* all clear */
        ret.add(this);
        return ret;
    }

    public String toString()
    {
        if(q.length == 0) return "1";
        String s = "";
        for(int i : q) s += "Sq"+i;
        return s;
    }
}


class ModSet<T> extends HashSet<T>
{
    public boolean add(T d)
    {
        if(contains(d)) {
            remove(d);
            return true;
        } else return super.add(d);
    }
    public void union(Set<T> s)
    {
        for(T d : s)
            super.add(d);
    }
    public String toString()
    {
        if(isEmpty())
            return "0";
        String s = "";
        for(T t : this) {
            if(s.length() != 0)
                s += " + ";
            s += t.toString();
        }
        return s;
    }
}

class DModSet extends ModSet<Dot> { } /* to work around generic array restrictions */



class Matrices
{
    /* Static matrix operation methods */

    /* row-reduces a matrix (in place).
     * returns an array giving the column position of the leading 1 in each row */
    static int[] rref(boolean[][] mat, int preserve_right)
    {
        if(mat.length == 0)
            return new int[] {};

        int good_rows = 0;
        int[] leading_cols = new int[mat.length];
        for(int j = 0; j < mat[0].length - preserve_right; j++) {
            int i;
            for(i = good_rows; i < mat.length; i++) {
                if(mat[i][j]) break;
            }
            if(i == mat.length) continue;

            /* swap the rows */
            boolean[] row = mat[good_rows];
            mat[good_rows] = mat[i];
            mat[i] = row;
            i = good_rows++;
            leading_cols[i] = j;

            /* clear the rest of the column */
            for(int k = 0; k < mat.length; k++) {
                if(k == i) continue;
                if(!mat[k][j]) continue;
                for(int l = 0; l < mat[0].length; l++)
                    mat[k][l] ^= mat[i][l];
            }
        }

        return Arrays.copyOf(leading_cols, good_rows);
    }


    /*
    static boolean[][] mult(boolean[][] left, boolean[][] mat)
    {
        if(mat.length == 0)
            return new boolean[0][0];
        /* Strictly speaking this doesn't make sense.
         * We should be able to multiply m x 0 by 0 x n, but we don't have
         * enough information to do so.
         * But for our application, <left> is always square. *//*

        boolean[][] ret = new boolean[left.length][mat[0].length];

        for(int i = 0; i < left.length; i++)
            for(int j = 0; j < mat[0].length; j++)
                for(int k = 0; k < mat.length; k++)
                    ret[i][j] ^= (left[i][k] && mat[k][j]);

        return ret;
    }
    */


    static void printMatrix(String name, boolean[][] mat)
    {
        if(!ResMain.DEBUG) return;

        System.out.print(name + ":");
        if(mat.length == 0) {
            System.out.println(" <zero lines>");
            return;
        }

        for(int i = 0; i < mat.length; i++) {
            System.out.println();
            for(int j = 0; j < mat[0].length; j++)
                System.out.printf("%d ", mat[i][j] ? 1 : 0);
        }
        System.out.println();

    }
}


class DotMatrix extends HashMap<Dot,DModSet>
{

    DModSet[] ker()
    {
        /* choose an ordering on all keys and values */
        Dot[] keys = keySet().toArray(new Dot[] {});
        DModSet val_set = new DModSet();
        for(DModSet ms : values()) 
            val_set.union(ms);
        Dot[] values = val_set.toArray(new Dot[] {});

        if(ResMain.DEBUG) System.out.printf("ker(): %d x %d\n", values.length, keys.length);

        /* convert to a matrix of booleans */
        boolean[][] mat = new boolean[values.length][keys.length];
        for(int i = 0; i < values.length; i++)
            for(int j = 0; j < keys.length; j++)
                mat[i][j] = get(keys[j]).contains(values[i]);

        Matrices.printMatrix("mat", mat);

        /* convert to row-reduced echelon form */
        int[] leading_cols = Matrices.rref(mat, 0);

        /* read out the kernel */
        int idx = 0;
        List<DModSet> ker = new ArrayList<DModSet>();

        for(int j = 0; j < keys.length; j++) {

            /* keep an eye out for leading ones and skip them */
            if(idx < leading_cols.length && leading_cols[idx] == j) {
                idx++;
                continue;
            }

            /* not a leading column, so we obtain a kernel element */
            DModSet ms = new DModSet();
            ms.add(keys[j]);
            for(int i = 0; i < values.length; i++) {
                if(mat[i][j]) {
                    ResMain.die_if(i >= leading_cols.length, "bad rref: no leading one");
                    ms.add(keys[leading_cols[i]]);
                }
            }

            ker.add(ms);
        }

        return ker.toArray(new DModSet[]{});
    }

}



class AMod
{

}

