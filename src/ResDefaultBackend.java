import java.util.*;


/* Computes Ext_A^{s,t} (M, Z/2) through a minimal resolution of M. */
/* This seems to work effectively through about t=75, and then becomes prohibitively slow. */

class ResDefaultBackend implements ResBackend
{
    PingListener listener = null;

    HashMap<String,CellData> output = new HashMap<String,CellData>();
    static String keystr(int s, int t) {
        return s+","+t;
    }

    /* convenience methods for cell data lookup */
    int ngens(int s, int t) {
        CellData dat = output.get(keystr(s,t));
        if(dat == null) return -1;
        return dat.gimg.length;
    }
    DModSet[] kbasis(int s, int t) {
        CellData dat = output.get(keystr(s,t));
        Main.die_if(dat == null, "Data null in ("+s+","+t+")");
        return dat.kbasis;
    }
    @Override public int[] novikov_grading(int s, int t) {
        CellData dat = output.get(keystr(s,t));
        if(dat == null) return null;
        return dat.novikov_grading;
    }
    @Override public DModSet[] gimg(int s, int t) {
        CellData dat = output.get(keystr(s,t));
        if(dat == null) return null;
        return dat.gimg;
    }
    @Override public boolean isComputed(int s, int t) {
        return output.containsKey(keystr(s,t));
    }



    /* Main minimal-resolution procedure. */

    @Override public void start()
    {
        long start;
        if(Config.TIMING) start = System.currentTimeMillis();

        for(int t = 0; t <= Config.T_CAP; t++) {

            /* first handle the s=0 case: kludge a start */
            CellData dat0 = new CellData();
            dat0.gimg = new DModSet[] {};
            dat0.novikov_grading = new int[] { 0 };
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
            if(Config.STDOUT) System.out.printf("(%2d,%2d): %2d gen, %2d ker\n", 0, t, 0, dat0.kbasis.length);
            output.put(keystr(0,t), dat0);


            /* now the typical s>0 case */

            for(int s = 1; s <= t; s++) {

                /* compute the basis for this resolution bidegree */
                ArrayList<Dot> basis_l = new ArrayList<Dot>();
                for(int gt = s; gt < t; gt++) {
                    for(int i = 0; i < ngens(s,gt); i++) {
                        for(Sq q : Sq.steenrod(t - gt)) {
                            Dot dot = new Dot(q,gt,i);
                            basis_l.add(dot);
                        }
                    }
                }
                DModSet[] okbasis = kbasis(s-1,t);

                /* compute what the map does in this basis. this takes maybe 10% of the running time */
                DotMatrix mat = new DotMatrix();
                if(Config.DEBUG) System.out.printf("(%d,%d) Map:\n",s,t);

                Collection<DModSet> easy_ker = new ArrayList<DModSet>();
                for(Dot dot : basis_l) {
                    /* compute the image of this basis vector */
                    DModSet image = new DModSet();
                    for(Map.Entry<Dot,Integer> d : gimg(s, dot.t)[dot.idx].entrySet()) {
                        ModSet<Sq> c = dot.sq.times(d.getKey().sq);
                        for(Map.Entry<Sq,Integer> q : c.entrySet())
                            image.add(new Dot(q.getKey(), d.getKey().t, d.getKey().idx), d.getValue() * q.getValue());
                    }

                    if(Config.DEBUG) System.out.println("Image of "+dot+" is "+image);

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
                if(dat.gimg.length > 0 && Config.STDOUT) {
                    System.out.println("Generators:");
                    for(DModSet g : dat.gimg) System.out.println(g);
                }

                /* compute the novikov filtration */
                if(Config.MICHAEL_MODE) {
                    dat.novikov_grading = new int[dat.gimg.length];
                    for(i = 0; i < dat.gimg.length; i++) {
                        DModSet g = dat.gimg[i];
                        int val = -1;
                        for(Dot d : g.keySet()) {
                            calc_nov(d, s-1);
                            if(val == -1 || val > d.nov)
                                val = d.nov;
                        }
                        if(Config.STDOUT) System.out.println("generator has extra grading "+val);
                        dat.novikov_grading[i] = val;
                    }
                }

                print_result(t);
                if(Config.STDOUT) System.out.printf("(%2d,%2d): %2d gen, %2d ker\n\n", s, t, dat.gimg.length, dat.kbasis.length);
                if(listener != null)
                    listener.ping();
            }

            if(Config.TIMING && t >= 1) {
                long elapsed = System.currentTimeMillis() - start;
                double log = Math.log(elapsed);
                double score = log / t; 
                //System.out.printf("t=%d elapsed=%dms log/t=%f\n", t, elapsed, score);
                System.out.printf("%d %d\n", t, elapsed);
            }
        }
    }
    
    
    /* Computes a basis complement to the image of mat inside the span of okbasis */

    private static CellData calc_gimg(DotMatrix mat, DModSet[] okbasis, int s)
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
        DModSet val_set = new DModSet();
        for(DModSet ms : okbasis)
            val_set.union(ms);
        if(Config.DEBUG) System.out.println("val_set: "+val_set);
        Dot[] values = val_set.toArray(); 
        if(Config.STDOUT) System.out.printf("o:%d k:%d v:%d\n", okbasis.length, keys.length, values.length);

        /* construct our huge augmented behemoth */
        int[][] aug = new int[values.length][okbasis.length + keys.length + values.length];
        for(int i = 0; i < values.length; i++) {
            for(int j = 0; j < okbasis.length; j++)
                aug[i][j] = ResMath.dmod(okbasis[j].getsafe(values[i]));
            for(int j = 0; j < keys.length; j++)
                aug[i][j + okbasis.length] = ResMath.dmod(mat.get(keys[j]).getsafe(values[i]));
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
        if(Config.DEBUG) System.out.printf("l1: %2d   l2: %2d\n", l1, l2);

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
                    Main.die_if(i >= bmatrr_leads.length, "bad rref: no leading one");
                    ms.add(keys[bmatrr_leads[i]], -bmatrr[i][j]);
                }
            }

            ker.add(ms);
        }
        ret.kbasis = ker.toArray(new DModSet[]{});

        if(Config.DEBUG && ker.size() != 0) {
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


    private void calc_nov(Dot dot, int s)
    {
        if(dot.nov != -1)
            return;

        int n = novikov_grading(s,dot.t)[dot.idx];
        boolean found_beta = false;
        for(int pow : dot.sq.q)
            if((pow % Config.P) != 0)
                found_beta = true;
        if(Config.DEBUG) System.out.printf("nov-grading %d, square %s, beta %s\n", n, dot.sq.toString(), found_beta ? "true" : "false");
        if(! found_beta)
            n++;

        dot.nov = n;
    }


    private void print_result(int s_max)
    {
        if(!Config.STDOUT) return;
        for(int s = s_max; s >= 0; s--) {
            for(int t = s; ; t++) {
                int n = ngens(s,t);
                if(n < 0) break;
                if(n > 0)
                    System.out.printf("%2d ", n);
                else
                    System.out.print("   ");
            }
            System.out.println("###");
        }
    }


    public void register_listener(PingListener p)
    {
        listener = p;
    }
}


class CellData
{
    DModSet[] gimg; /* images of generators as dot-sums in bidegree s-1,t*/
    DModSet[] kbasis; /* kernel basis dot-sums in bidegree s,t */
    int[] novikov_grading;

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
    int nov = -1;
    String id_cache = null;

    Dot(Sq _sq, int _t, int _idx) {
        sq = _sq; t = _t; idx = _idx;
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
            if(nov != -1)
                id_cache += "(n=" + nov + ")";
        }
        return id_cache;
    }
    public boolean equals(Object o)
    {
        return o.hashCode() == hashCode();
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


class DotMatrix extends HashMap<Dot,DModSet>
{
}

