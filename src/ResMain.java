import java.util.*;


/* Computes Ext_A^{s,t} (M, Z/2) through a minimal resolution of M. */
/* This seems to work effectively through about t=75, and then becomes prohibitively slow. */

public class ResMain
{

    /* upper bound on total degree to compute */
    static final int T_CAP = 100;
    static final boolean DEBUG = false;
    static final boolean MATRIX_DEBUG = false;
    static final boolean MICHAEL_MODE = true;

    static final Sq P1 = new Sq(new int[] {2});
    static final Sq P1B = new Sq(new int[] {2,1});
    static final Sq P2 = new Sq(new int[] {4});
    static final Sq P3 = new Sq(new int[] {6});

    static HashMap<String,CellData> output = new HashMap<String,CellData>();
    static String keystr(int s, int t) {
        return s+","+t;
    }

    /* convenience methods for cell data lookup */
    static int ngens(int s, int t) {
        CellData dat = output.get(keystr(s,t));
        if(dat == null) return -1;
        return dat.gimg.length;
    }
    static int nhidden(int s, int t) {
        CellData dat = output.get(keystr(s,t));
        if(dat == null) return -1;
        return dat.hidden;
    }
    static int[] extra_grading(int s, int t) {
        CellData dat = output.get(keystr(s,t));
        if(dat == null) return null;
        return dat.extra_grading;
    }
    static boolean[] hiddens(int s, int t) {
        CellData dat = output.get(keystr(s,t));
        die_if(dat == null, "Data null in ("+s+","+t+")");
        return dat.hiddens;
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





    /* Main minimal-resolution procedure. */

    static void resolve(AMod a)
    {

        for(int t = 0; t <= T_CAP; t++) {

            /* first handle the s=0 case: process the input */
            /* XXX TMP just using the sphere as input */
            CellData dat0 = new CellData();
            dat0.gimg = new DModSet[] {};
            dat0.hiddens = new boolean[] { false };
            dat0.extra_grading = new int[] { 0 };
            if(t == 0) {
                dat0.kbasis = new DModSet[] {};
            } else {
                List<DModSet> kbasis0 = new ArrayList<DModSet>();
                for(Sq q : Sq.steenrod(t)) {
                    DModSet ms = new DModSet();
                    ms.add(new Dot(q,0,0), 1);
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
                        for(Sq q : Sq.steenrod(t - gt)) {
                            Dot dot = new Dot(q,gt,i,s);
                            basis_l.add(dot);
                        }
                    }
                }
                DModSet[] okbasis = kbasis(s-1,t);

                /* compute what the map does in this basis. this takes maybe 10% of the running time */
                DotMatrix mat = new DotMatrix();
                if(DEBUG) System.out.printf("(%d,%d) Map:\n",s,t);

                Collection<DModSet> easy_ker = new ArrayList<DModSet>();
                for(Dot dot : basis_l) {
                    /* compute the image of this basis vector */
                    DModSet image = new DModSet();
                    for(Map.Entry<Dot,Integer> d : gimg(s, dot.t)[dot.idx].entrySet()) {
                        ModSet<Sq> c = dot.sq.times(d.getKey().sq);
                        for(Map.Entry<Sq,Integer> q : c.entrySet())
                            image.add(new Dot(q.getKey(), d.getKey().t, d.getKey().idx, s-1), d.getValue() * q.getValue());
                    }

                    if(DEBUG) System.out.println("Image of "+dot+" is "+image);

                    if(image.isEmpty()) {
                        easy_ker.add(new DModSet(dot));
                    } else { /* have to do actual linear algebra ... */
                        mat.put(dot, image);
                    }
                }

                /* OK, do all the heavy lifting = linear algebra. timesuck */
                CellData dat = calc_gimg(mat, okbasis, s);
                output.put(keystr(s,t), dat);
                
                /* add in the "easy" elements */
                DModSet[] newkbasis = Arrays.copyOf(dat.kbasis, dat.kbasis.length + easy_ker.size());
                int i = 0; 
                for(DModSet k : easy_ker)
                    newkbasis[dat.kbasis.length + (i++)] = k;
                dat.kbasis = newkbasis;
                
                /* list generators */
                if(dat.gimg.length > 0) {
                    System.out.println("Generators:");
                    for(DModSet g : dat.gimg) System.out.println(g);
                }

                /* compute the novikov filtration */
                if(MICHAEL_MODE) {
                    dat.extra_grading = new int[dat.gimg.length];
                    for(i = 0; i < dat.gimg.length; i++) {
                        DModSet g = dat.gimg[i];
                        int val = -1;
                        for(Dot d : g.keySet()) {
                            int oval = d.get_n(s-1);
                            if(val == -1 || val > oval)
                                val = oval;
                        }
                        if(STDOUT) System.out.println("generator has extra grading "+val);
                        dat.extra_grading[i] = val;
                    }
                }

                /* figure out how many gimg elements are to be "hidden" */
                /* XXX TMP */
                dat.hiddens = new boolean[dat.gimg.length];
                if(MICHAEL_MODE) {
                    for(int l = 0; l < dat.gimg.length; l++) {
                        DModSet g = dat.gimg[l];
                        boolean nonfunky_occured = false;
                        for(Dot d : g.keySet()) {
                            System.out.println(d.sq);
                            if(d.sq.equals(P1) || hiddens(s-1,d.t)[d.idx]) {
                            } else {
                                System.out.println("nonfunky occured: "+d.sq);
                                nonfunky_occured = true;
                                break;
                            }
                        }
                        if(! nonfunky_occured) {
                            dat.hiddens[l] = true;
                            dat.hidden++;
                            System.out.println("funky element");
                        }
                    }
                }

                print_result(t);
                System.out.printf("(%2d,%2d): %2d gen, %2d ker\n", s, t, dat.gimg.length, dat.kbasis.length);
                System.out.println();
            }
        }
    }
    
    
    static Comparator<Dot> buildNovikovComparator(final int s)
    {
        return new Comparator<Dot>() {
            @Override public int compare(Dot a, Dot b) {
                return a.get_n(s) - b.get_n(s);
            }
        };
    }
    
    /* Computes a basis complement to the image of mat inside the span of okbasis */

    static CellData calc_gimg(DotMatrix mat, DModSet[] okbasis, int s)
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
         *
         * The code for computing the kernel of mat was merged into this function (since
         * it was basically doing all the same computations), so this function now does
         * all of the interesting linear algebra.
         *
         * Probably the last step of inverting a matrix via RREF could be sped up by keeping better
         * track of row operations used in earlier steps...
         */
        CellData ret = new CellData();

        /* choose an ordering on all keys and values */
        Dot[] keys = mat.keySet().toArray(new Dot[] {});
        if(s >= 1) Arrays.sort(keys, buildNovikovComparator(s)); 
        DModSet val_set = new DModSet();
        for(DModSet ms : okbasis)
            val_set.union(ms);
        if(DEBUG) System.out.println("val_set: "+val_set);
        Dot[] values = val_set.toArray(); 
        if(s >= 2) Arrays.sort(values, buildNovikovComparator(s-1));
        System.out.printf("o:%d k:%d v:%d\n", okbasis.length, keys.length, values.length);

        /* construct our huge augmented behemoth */
        int[][] aug = new int[values.length][okbasis.length + keys.length + values.length];
        for(int i = 0; i < values.length; i++) {
            for(int j = 0; j < okbasis.length; j++)
                aug[i][j] = Math.dmod(okbasis[j].getsafe(values[i]));
            for(int j = 0; j < keys.length; j++)
                aug[i][j + okbasis.length] = Math.dmod(mat.get(keys[j]).getsafe(values[i]));
            for(int j = 0; j < values.length; j++)
                aug[i][j + okbasis.length + keys.length] = (i == j ? 1 : 0);
        }
        Matrices.printMatrix("aug", aug);

        /* rref it */
        int l1 = Matrices.rref(aug, keys.length + values.length).length;
        Matrices.printMatrix("rref(aug)", aug);

        /* extract mat | id */
        int[][] bmatrr = new int[values.length][keys.length + values.length];
        for(int i = 0; i < values.length; i++)
            for(int j = 0; j < keys.length + values.length; j++)
                bmatrr[i][j] = aug[i][j + okbasis.length];
        Matrices.printMatrix("bmatrr", bmatrr);

        /* rref it some more */
        int[] bmatrr_leads = Matrices.rref(bmatrr, values.length);
        int l2 = bmatrr_leads.length;
        Matrices.printMatrix("rref(bmatrr)", bmatrr);
        if(DEBUG) System.out.printf("l1: %2d   l2: %2d\n", l1, l2);

        /* read out the kernel */
        List<DModSet> ker = new ArrayList<DModSet>();

        int idx = 0;
        for(int j = 0; j < keys.length; j++) {

            /* keep an eye out for leading ones and skip them */
            if(idx < bmatrr_leads.length && bmatrr_leads[idx] == j) {
                idx++;
                continue;
            }

            /* not a leading column, so we obtain a kernel element */
            DModSet ms = new DModSet();
            ms.add(keys[j], 1);
            for(int i = 0; i < values.length; i++) {
                if(bmatrr[i][j] != 0) {
                    ResMain.die_if(i >= bmatrr_leads.length, "bad rref: no leading one");
                    ms.add(keys[bmatrr_leads[i]], -bmatrr[i][j]);
                }
            }

            ker.add(ms);
        }
        ret.kbasis = ker.toArray(new DModSet[]{});

        if(ResMain.DEBUG && ker.size() != 0) {
            System.out.println("Kernel:");
            for(DModSet dm : ret.kbasis)
                System.out.println(dm);
        }


        /* extract and invert (via rref, tracking only the relevant part) the row transform matrix */
        int[][] transf = new int[values.length][2 * values.length];
        for(int i = 0; i < values.length; i++) {
            for(int j = 0; j < values.length; j++)
                transf[i][j] = bmatrr[i][j + keys.length];
            for(int j = 0; j < l1 - l2; j++)
                transf[i][j + values.length] = (i == j + l2 ? 1 : 0);
        }
        Matrices.printMatrix("transf", transf);
        Matrices.rref(transf, values.length);
        Matrices.printMatrix("rref(transf)", transf); 

        /* read off the output */
        ret.gimg = new DModSet[l1 - l2];
        for(int j = 0; j < l1 - l2; j++) {
            DModSet out = new DModSet();
            for(int i = 0; i < values.length; i++)
                out.add(values[i], transf[i][j + values.length]);
            ret.gimg[j] = out;
        }

        return ret;
    }



    static void print_result(int s_max)
    {
        for(int s = s_max; s >= 0; s--) {
            for(int t = s; ; t++) {
                int ng = ngens(s,t);
                if(ng < 0) break;
                int n = ng - nhidden(s,t); /* TMP XXX */
                if(n > 0)
                    System.out.printf("%2d ", n);
                else if(ng > 0)
                    System.out.print(" ` ");
                else
                    System.out.print("   ");
            }
            System.out.println("###");
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
        /* tests */

        /* init */

        /* make the sphere A-module */

        /* resolve */
        resolve(null); /* TMP */

        /* print */
        System.out.println("Conclusion:");
        print_result(T_CAP);
    }

}


class CellData
{
    DModSet[] gimg; /* images of generators as dot-sums in bidegree s-1,t*/
    DModSet[] kbasis; /* kernel basis dot-sums in bidegree s,t */
    boolean[] hiddens;
    int hidden = 0;
    int[] extra_grading;

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
    int s_hint = -1;

    String id_cache = null;
    int n_cache = -1;

    Dot(Sq _sq, int _t, int _idx) {
        sq = _sq; t = _t; idx = _idx;
    }
    Dot(Sq _sq, int _t, int _idx, int _s) {
        this(_sq,_t,_idx);
        s_hint = _s;
    }

    public int get_n(int s)
    {
        if(n_cache != -1)
            return n_cache;

        int n = ResMain.extra_grading(s,t)[idx];
        boolean found_beta = false;
        for(int pow : sq.q)
            if((pow % ResMath.P) != 0)
                found_beta = true;
        if(ResMain.DEBUG) System.out.printf("nov-grading %d, square %s, beta %s\n", n, sq.toString(), found_beta ? "true" : "false");
        if(! found_beta)
            n++;

        n_cache = n;
        return n;
    }

    public int hashCode()
    {
        int hash = sq.hashCode();
        hash *= 27863521;
        hash ^= t;
        hash *= 27863521;
        hash ^= idx;
        return hash;
    }
    public String toString()
    {
        if(id_cache == null) {
            id_cache = sq.toString() + "(" + t + ";" + idx + ")";
            if(s_hint != -1)
                id_cache += "(n=" + get_n(s_hint) + ")";
        }
        return id_cache;
    }
    public boolean equals(Object o)
    {
        return o.hashCode() == hashCode();
    }
}


class Math
{
//    static final int P = 3;
    static final int P = 2;
//    static final int[] inverse = { 0, 1, 3, 2, 4 }; /* multiplicative inverses mod P */
    static final int[] inverse = { 0, 1, 2 }; /* multiplicative inverses mod P */

    static boolean binom_2(int a, int b)
    {
        return ((~a) & b) == 0;
    }

    static Map<String,Integer> binom_cache = new HashMap<String,Integer>();
    static String binom_cache_str(int a, int b) { return a+"/"+b; }
    static int binom_p(int a, int b)
    {
        String s = binom_cache_str(a,b);
        Integer i = binom_cache.get(s);
        if(i != null) return i;

        int ret;
        if(a < 0 || b < 0 || b > a)
            ret = 0;
        else if(a == 0)
            ret = 1;
        else ret = dmod(binom_p(a-1,b) + binom_p(a-1,b-1));

        binom_cache.put(s,ret);
        return ret;
    }

    static int dmod(int n)
    {
        return (n + (P << 8)) % P;
    }
}


class Sq
{
    int[] q; /* Indices of the power operations.
                Mod 2, i indicates Sq^i.
                Mod p>2, 2k(p-1) indicates P^i, 2k(p-1)+1 indicates B P^i. */


    public Sq(int[] qq) { q = qq; }

    public ModSet<Sq> times(Sq o)
    {
        int[] ret = new int[q.length + o.q.length];
        for(int i = 0; i < q.length; i++)
            ret[i] = q[i];
        for(int i = 0; i < o.q.length; i++)
            ret[q.length + i] = o.q[i];

        if(Math.P == 2 && !ResMain.MICHAEL_MODE)
            return new Sq(ret).resolve_2();
        else
            return new Sq(ret).resolve_p();
    }

//    private static Map<String,ModSet<Sq>> resolve_cache = new HashMap<String,ModSet<Sq>>();
    private ModSet<Sq> resolve_2()
    {
        ModSet<Sq> ret;
//        String key = toString();
//        ret = resolve_cache.get(key);
//        if(ret != null)
//            return ret;

        ret = new ModSet<Sq>();

        for(int i = q.length - 2; i >= 0; i--) {
            int a = q[i];
            int b = q[i+1];

            if(a >= 2 * b)
                continue;

            /* apply Adem relation */
            for(int c = 0; c <= a/2; c++) {

                if(! Math.binom_2(b - c - 1, a - 2*c))
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
                for(Map.Entry<Sq,Integer> sub : new Sq(t).resolve_2().entrySet())
                    ret.add(sub.getKey(), sub.getValue());
            }

//            resolve_cache.put(key, ret);
            return ret;
        }

        /* all clear */
        ret.add(this, 1);
//        resolve_cache.put(key, ret);
        return ret;
    }

    private ModSet<Sq> resolve_p()
    {
        ModSet<Sq> ret;

//        String key = toString();
//        ModSet<Sq> ret = resolve_cache.get(key);
//        if(ret != null)
//            return ret;

        ret = new ModSet<Sq>();
        
        int Q = 2 * (Math.P - 1); /* convenience */

        for(int i = q.length - 2; i >= 0; i--) {
            int x = q[i];
            int y = q[i+1];

            if(x >= Math.P * y)
                continue;

            /* apply Adem relation */
            int a = x / Q;
            int b = y / Q;
            int rx = x % Q;
            int ry = y % Q;

            for(int c = 0; c <= a/Math.P; c++) {

                int sign = 1 - 2 * ((a+c) % 2);

//                System.out.printf("adem: x=%d y=%d a=%d b=%d sign=%d\n", x, y, a, b, sign);

                if(rx == 0 && ry == 0)
                    resolve_p_add_term(sign*Math.binom_p((Math.P-1)*(b-c)-1,a-c*Math.P), (a+b-c)*Q, c*Q, i, ret);
                else if(rx == 1 && ry == 0)
                    resolve_p_add_term(sign*Math.binom_p((Math.P-1)*(b-c)-1,a-c*Math.P), (a+b-c)*Q+1, c*Q, i, ret);
                else if(rx == 0 && ry == 1) {
                    resolve_p_add_term(sign*Math.binom_p((Math.P-1)*(b-c),a-c*Math.P), (a+b-c)*Q+1, c*Q, i, ret);
                    resolve_p_add_term(-sign*Math.binom_p((Math.P-1)*(b-c)-1,a-c*Math.P-1), (a+b-c)*Q, c*Q+1, i, ret);
                }
                else if(rx == 1 && ry == 1)
                    resolve_p_add_term(-sign*Math.binom_p((Math.P-1)*(b-c)-1,a-c*Math.P-1), (a+b-c)*Q+1, c*Q+1, i, ret);
                else ResMain.die_if(true, "Bad Adem case.");
                       
            }

//            resolve_cache.put(key, ret);
            return ret;
        }

        /* all clear */
        ret.add(this, 1);
//        resolve_cache.put(key, ret);
        return ret;
    }

    private void resolve_p_add_term(int coeff, int a, int b, int i, ModSet<Sq> ret)
    {
//        System.out.printf("adem_term: coeff=%d a=%d b=%d\n", coeff, a, b);

        coeff = Math.dmod(coeff);
        if(coeff == 0) return; /* save some work... */

        int[] t;
        if(b == 0) {
            t = Arrays.copyOf(q, q.length - 1);
            for(int k = i+2; k < q.length; k++)
                t[k-1] = q[k];
            t[i] = a;
        } else {
            t = Arrays.copyOf(q, q.length);
            t[i] = a;
            t[i+1] = b;
        }

        /* recurse */
        for(Map.Entry<Sq,Integer> sub : new Sq(t).resolve_p().entrySet())
            ret.add(sub.getKey(), sub.getValue() * coeff);
    }

    public String toString()
    {
        if(q.length == 0) return "1";
        String s = "";
        for(int i : q) s += "Sq"+i;
        return s;
    }

    public int hashCode()
    {
        int hash = 0;
        for(int i : q)
            hash = hash * 27863521 + i;
        return hash;
    }

    public boolean equals(Object o)
    {
        return toString().equals(o.toString());
    }


    /* The Steenrod algebra. */
    public static Iterable<Sq> steenrod(int n)
    {
        Iterable<int[]> p;
        if(Math.P == 2) p = part_2(n,n);
        else            p = part_p(n,n);
        Collection<Sq> ret = new ArrayList<Sq>();

        for(int[] q : p)
            ret.add(new Sq(q));

        return ret;
    }

    private static Map<String,Iterable<int[]>> part_cache = new HashMap<String,Iterable<int[]>>();
    private static String part_cache_keystr(int n, int max) {
        return "("+n+"/"+max+"/"+Math.P+")";
    }

    private static Iterable<int[]> part_2(int n, int max)
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
            for(int[] q0 : part_2(n-i, i/2)) {
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

    private static Iterable<int[]> part_p(int n, int max)
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

        for(int i = 0; i <= max; i += 2 * (Math.P - 1)) { /* XXX i could start higher? */
            /* try P^i */
            for(int[] q0 : part_p(n-i, i/Math.P)) {
                int[] q1 = new int[q0.length + 1];
                q1[0] = i;
                for(int j = 0; j < q0.length; j++)
                    q1[j+1] = q0[j];
                ret.add(q1);
            }
            /* try BP^i */
            if(i+1 > max) break;
            for(int[] q0 : part_p(n-(i+1), (i+1)/Math.P)) {
                int[] q1 = new int[q0.length + 1];
                q1[0] = i+1;
                for(int j = 0; j < q0.length; j++)
                    q1[j+1] = q0[j];
                ret.add(q1);
            }
        }

        part_cache.put(part_cache_keystr(n,max), ret);

        return ret;
    }
}


/* A formal F_p-linear combination of things of type T. */
class ModSet<T> extends HashMap<T,Integer>
{
    public void add(T d, int mult)
    {
        int c;
        if(containsKey(d)) c = get(d);
        else c = 0;

        c = Math.dmod(c + mult);

        if(c == 0) 
            remove(d);
        else
            put(d, c);
    } 

    public int getsafe(T d)
    {
        Integer i = get(d);
        if(i == null)
            return 0;
        return i;
    }

    public boolean contains(T d)
    {
        return (getsafe(d) % Math.P != 0);
    }

    public void union(ModSet<T> s)
    {
        for(T d : s.keySet()) {
            if(!containsKey(d))
                put(d,1);
        }
    }

    public String toString()
    {
        return toStringDelim(" + ");
    }

    public String toStringDelim(String delim)
    {
        if(isEmpty())
            return "0";
        String s = "";
        for(Map.Entry<T,Integer> e : entrySet()) {
            if(s.length() != 0)
                s += delim;
            if(e.getValue() != 1)
                s += e.getValue();
            s += e.getKey().toString();
        }
        return s;
    }
}

class DModSet extends ModSet<Dot> { /* to work around generic array restrictions */
    DModSet() {}
    DModSet(Dot d) {
        add(d,1);
    }
    public Dot[] toArray() {
        return keySet().toArray(new Dot[] {});
    }
}



class Matrices
{
    /* row-reduces a matrix (in place).
     * Returns an array giving the column position of the leading 1 in each row. 
     * It should be noted that matrices are assumed to be reduced to lowest
     * non-negative residues (mod p), and this operation respects that. */
    static int[] rref(int[][] mat, int preserve_right)
    {
        if(mat.length == 0)
            return new int[] {};

        int h = mat.length;
        int w = mat[0].length;

        int good_rows = 0;
        int[] leading_cols = new int[h];
        for(int j = 0; j < w - preserve_right; j++) {
            /* find the first nonzero entry in this column */
            int i;
            for(i = good_rows; i < h && mat[i][j] == 0; i++);
            if(i == h) continue;

            /* swap the rows */
            int[] row = mat[good_rows];
            mat[good_rows] = mat[i];
            mat[i] = row;
            i = good_rows++;
            leading_cols[i] = j;

            /* normalize the row */
            int inv = Math.inverse[mat[i][j]];
            for(int k = h; k < w; k++)
                mat[i][k] = (mat[i][k] * inv) % Math.P;

            /* clear the rest of the column. this part is cubic-time so we optimize P=2 */
            if(Math.P == 2) {
                for(int k = 0; k < h; k++) {
                    if(mat[k][j] == 0) continue;
                    if(k == i) continue;
                    for(int l = 0; l < w; l++)
                        mat[k][l] ^= mat[i][l];
                }
            } else {
                for(int k = 0; k < h; k++) {
                    if(mat[k][j] == 0) continue;
                    if(k == i) continue;
                    int mul = Math.P - mat[k][j];
                    for(int l = 0; l < w; l++)
                        mat[k][l] = (mat[k][l] + mat[i][l] * mul) % Math.P;
                }
            }
        }

        return Arrays.copyOf(leading_cols, good_rows);
    }


    static void printMatrix(String name, int[][] mat)
    {
        if(!ResMain.MATRIX_DEBUG) return;

        System.out.print(name + ":");
        if(mat.length == 0) {
            System.out.println(" <zero lines>");
            return;
        }

        for(int i = 0; i < mat.length; i++) {
            System.out.println();
            for(int j = 0; j < mat[0].length; j++)
                System.out.printf("%2d ", mat[i][j]);
        }
        System.out.println();
    }
}


class DotMatrix extends HashMap<Dot,DModSet>
{
}


class AMod
{
    /* TODO encode a general A-module and be able to resolve it */
}

