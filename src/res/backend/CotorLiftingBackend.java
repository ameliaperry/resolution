package res.backend;

import res.*;
import res.algebratypes.*;
import res.algebras.*;
import res.transform.*;
import res.utils.*;

import java.awt.Color;
import java.util.*;
import java.util.concurrent.*;
import java.util.Map.Entry;
import javax.swing.JOptionPane;


/* Computes Cotor^{s,w,t}(F_2, Q(0)), by separately computing minimal resolutions for H*(K(Z/2, w)) and doubling degree. */

public class CotorLiftingBackend
    extends MultigradedComputation<Generator<Sq>>
    implements Backend<Generator<Sq>, MultigradedComputation<Generator<Sq>>>
{
    final boolean COMPUTE_ALL_QTRANS = false;

    private final GradedAlgebra<Sq> alg = new SteenrodAlgebra();

    private Map<int[],CotorLiftingCellData> output3 = new TreeMap<int[],CotorLiftingCellData>(Multidegrees.multidegComparator);
    private Map<int[],Collection<Generator<Sq>>> output2 = new TreeMap<int[],Collection<Generator<Sq>>>(Multidegrees.multidegComparator);


    int posts = -1;
    int postt = -1; 
    int postw = -1;

    Generator[] wgens = new Generator[10000];

    public CotorLiftingBackend() {
        Main.die_if(Config.P != 2, "Cartan-Eilenberg SS only implemented for p=2");
        for(int i = 0; i < 10000; i++)
            wgens[i] = new Generator(new int[] {-1,0,i},0);
    }


    /* methods implementing MultigradedAlgebra */

    @Override public int num_gradings() {
        return 3;
    }

    @Override public Iterable<Generator<Sq>> gens(int[] i)
    {
        if(i.length == 2) {
            if(i[0] < 0 || i[1] < 0)
                return Collections.emptyList();
            synchronized(output2) {
                return output2.get(i);
            }
        }

        if(i.length == 3) {
            if(i[0] < 0 || i[1] < 0 || i[2] < 0)
                return Collections.emptyList();
            CotorLiftingCellData dat = dat(i[0],i[1],i[2]);
            if(dat != null) 
                return dat.gens;
        }

        return null;
    }


    @Override public int getState(int[] i)
    {
        if(i.length == 2) {
            if(i[0] < 0 || i[1] < i[0])
               return STATE_FORMALLY_VANISHES; 
            /* XXX currently no way to tell if a cell is partial or done. */
            if(output2.containsKey(i))
                return STATE_DONE;
            return STATE_NOT_COMPUTED;
        }

        if(i.length == 3) {
            if(i[0] < 0 || i[1] < i[0] || i[2] < 0)
                return STATE_FORMALLY_VANISHES;
            /* XXX can do *much* better than this for vanishing */
            if(output3.containsKey(i))
                return STATE_DONE;
            return STATE_NOT_COMPUTED;
        }

        return STATE_FORMALLY_VANISHES;
    }


    /* internal lookup / put */

    private CotorLiftingCellData dat(int s, int t, int w)
    {
        synchronized(output3) {
            return output3.get(new int[] {s,t,w});
        }
    }
    private Collection<Generator<Sq>> gens(int s, int t, int w)
    {
        CotorLiftingCellData dat = dat(s,t,w);
        if(dat == null) return null;
        return dat.gens;
    }
    private boolean isComputed(int s, int t, int w)
    {
        return getState(new int[] {s,t,w}) == STATE_DONE;
    }
    private Map<Dot<Sq>,ModSet<Dot<Sq>>> resmap(int s, int t, int w)
    {
        return dat(s,t,w).resmap;
    }
    private void putOutput(int s, int t, int w, CotorLiftingCellData dat)
    {
        synchronized(output3) {
            output3.put(new int[] {s,t,w}, dat);
        }
        if(dat.gens != null) {
            int[] key = new int[] {s+w, 2*t+w};
            synchronized(output2) {
                Collection<Generator<Sq>> c = output2.get(key);
                if(c == null) {
                    c = new ArrayList<Generator<Sq>>();
                    output2.put(key, c);
                }

                synchronized(c) {
                    c.addAll(dat.gens);
                }
            }
        }
    }


    /* task management */
    BlockingQueue<CotorLiftingResTask> tasks;
    TreeSet<int[]> claims;

    @Override public void start()
    {
        //runTests();

        if(! COMPUTE_ALL_QTRANS) {
            while(true) {
                String in = JOptionPane.showInputDialog("Input a tridegree s,t,u (e.g. 2,1,10):");
                String[] tok = in.split(",");
                if(tok.length == 0) break;
                if(tok.length != 3) continue;

                try { 
                    int is = Integer.parseInt(tok[0]);
                    int it = Integer.parseInt(tok[1]);
                    int iu = Integer.parseInt(tok[2]);

                    if(iu % 2 != 0) continue;

                    posts = is;
                    postt = iu / 2;
                    postw = it;
                } catch(NumberFormatException e) { continue; }
                break;
            }
        }

        tasks = new PriorityBlockingQueue<CotorLiftingResTask>();
        claims = new TreeSet<int[]>(Multidegrees.multidegComparator);
        putTask(new CotorLiftingResTask(CotorLiftingResTask.COMPUTE, 0, 0, 0));

        for(int i = 0; i < Config.THREADS; i++)
            new CotorLiftingResTaskThread(this).start();
    }

    private boolean atomic_claim_grid(int s, int t, int w)
    {
        int[] key = new int[] {s,t,w};
        synchronized(claims) {
            if(claims.contains(key))
                return false;
            claims.add(key);
        }
        return true;
    }
    
    private void putTask(CotorLiftingResTask t)
    {
        while(true) {
            try {
                tasks.put(t);
                return;
            } catch(InterruptedException e) {
                continue;
            }
        }
    }

    
    /* math */

    Map<Integer,GradedModule<Sq,Sq>> weightModules = new TreeMap<Integer,GradedModule<Sq,Sq>>();
    private GradedModule<Sq,Sq> getWeightModule(int w)
    {
        GradedModule<Sq,Sq> ret = weightModules.get(w);
        if(ret != null) return ret;

        ret = new ExcessModule(w, alg);

        weightModules.put(w, ret);
        return ret;
    }


    /* enumerate all pairs 1 tensor Sq^J, of degree d and with excess(J) at most w */
    private Iterable<Dot<Sq>> enumerateOtherRes(int d, final int w)
    {
        Iterable<Sq> raw = getWeightModule(w).gens(d);
        return new MapIterable<Sq,Dot<Sq>>(raw, new Func<Sq,Dot<Sq>>() {
            @Override public Dot<Sq> run(Sq in) {
                return new Dot<Sq>(wgens[w],in);
            }
        });
    }
    /* enumerate all pairs 1 tensor Sq^J, of degree d and with excess(J) at most w */
    private Iterable<ModSet<Dot<Sq>>> enumerateOtherRes_wrap(int d, final int w)
    {
        Iterable<Sq> raw = getWeightModule(w).gens(d);
        return new MapIterable<Sq,ModSet<Dot<Sq>>>(raw, new Func<Sq,ModSet<Dot<Sq>>>() {
            @Override public ModSet<Dot<Sq>> run(Sq in) {
                return new ModSet<Dot<Sq>>(new Dot<Sq>(wgens[w],in));
            }
        });
    }

    private ModSet<Pair> mapOtherRes(Dot<Sq> b, int w)
    {
        GradedModule<Sq,Sq> mod = getWeightModule(w);

        ModSet<Pair> ret = new ModSet<Pair>();

        /* first term: a Sq^1 tens b */
        ret.add(new Pair(Sq.HOPF[0], b), 1);

        /* second term: a tens Sq^1 b */
        ModSet<Dot<Sq>> secondmult = mult_dots(new ModSet<Dot<Sq>>(b), Sq.HOPF[0], mod);
        for(Entry<Dot<Sq>,Integer> ent : secondmult.entrySet())
            ret.add(new Pair(Sq.UNIT, ent.getKey()), ent.getValue());

        return ret;
    }

    private ModSet<Dot<Sq>> mult_dots(ModSet<Dot<Sq>> a, ModSet<Dot<Sq>> b, GradedModule<Sq,Sq> mod) {
        ModSet<Dot<Sq>> ret = new ModSet<Dot<Sq>>();
        for(Entry<Dot<Sq>,Integer> e1 : a.entrySet()) {
            for(Entry<Dot<Sq>,Integer> e2 : b.entrySet()) {
                ModSet<Sq> pre = mod.times(e1.getKey().sq, e2.getKey().sq);
                for(Entry<Sq,Integer> e3 : pre.entrySet())
                    ret.add(new Dot(e1.getKey().base, e3.getKey()), e1.getValue() * e2.getValue() * e3.getValue());
            }
        }
        return ret;
    }
    private ModSet<Dot<Sq>> mult_dots(ModSet<Dot<Sq>> a, Sq b, GradedModule<Sq,Sq> mod) {
        ModSet<Dot<Sq>> ret = new ModSet<Dot<Sq>>();
        for(Entry<Dot<Sq>,Integer> e1 : a.entrySet()) {
            ModSet<Sq> pre = mod.times(e1.getKey().sq, b);
            for(Entry<Sq,Integer> e3 : pre.entrySet())
                ret.add(new Dot(e1.getKey().base, e3.getKey()), e1.getValue() * e3.getValue());
        }
        return ret;
    }


    void compute(int s, int t, int w)
    {
        if(Config.DEBUG) System.out.printf("(%d,%d,%d)\n", s,t,w);

        GradedModule<Sq,Sq> module = getWeightModule(w);

        /* get the old kernel basis */
        CotorLiftingCellData olddat = dat(s-1, t, w);
        Iterable<ModSet<Dot<Sq>>> okbasis;
        if(s == 0) okbasis = enumerateOtherRes_wrap(t,w);
        else       okbasis = olddat.kbasis;

        Map<Dot<Sq>,ModSet<Dot<Sq>>> list_x = new TreeMap<Dot<Sq>,ModSet<Dot<Sq>>>();
        Map<Dot<Sq>,ModSet<Dot<Sq>>> list_dx = new TreeMap<Dot<Sq>,ModSet<Dot<Sq>>>();
        ArrayList<ModSet<Dot<Sq>>> ker = new ArrayList<ModSet<Dot<Sq>>>();
        /* loop over existing dots in this bidegree */
        for(int gt = s; gt < t; gt++) {
            if(Config.DEBUG && gens(s,gt,w) == null)
                System.out.printf("null gens at (%d,%d)\n", s, gt);
            if(Config.DEBUG) System.out.printf("%d gens at (%d,%d,%d)\n", gens(s,gt,w).size(), s, gt, w);

            for(Generator<Sq> g : gens(s,gt,w)) {
                for(Sq q : alg.gens(t-gt)) {
                    ModSet<Dot<Sq>> x = new ModSet<Dot<Sq>>(new Dot<Sq>(g,q));
                    /* compute the image */
                    ModSet<Dot<Sq>> dx;
                    if(s > 0) dx = mult_dots((ModSet<Dot<Sq>>) g.img, q, alg);
                    else dx = mult_dots((ModSet<Dot<Sq>>) g.img, q, module);
                    if(Config.DEBUG) System.out.printf("1: %s --> %s", x, dx);

                    /* reduce against the existing image */
                    while(! dx.isEmpty()) {
                        Map.Entry<Dot<Sq>,Integer> high = dx.lastEntry();
                        Dot<Sq> d = high.getKey();
                        Integer coeff = high.getValue();

                        ModSet<Dot<Sq>> modx = list_x.get(d);
                        if(modx == null)
                            break;

                        x.add(modx, -coeff);
                        dx.add(list_dx.get(d), -coeff);
                        if(Config.DEBUG) System.out.printf(" reduces to %s --> %s", x, dx);
                    }
                    if(Config.DEBUG) System.out.println();

                    if(dx.isEmpty()) { /* dx = 0, add to kernel */
                        if(Config.DEBUG) System.out.printf("Adding %s to kernel\n", x);
                        ker.add(x);
                    } else { /* register this as the entry with highest dot <highest> */
                        Dot<Sq> highest = dx.lastKey();
                        if(Config.DEBUG) System.out.println("highest term "+highest);
                        if(Config.DEBUG) Main.die_if(list_x.containsKey(highest), "key clash on "+highest);
                        list_x.put(highest, x);
                        list_dx.put(highest, dx);
                    }
                }
            }
        }

        if(Config.DEBUG) {
            System.out.println("Dump of image:");
            for(Dot<Sq> d : list_x.keySet())
                System.out.printf("%s : %s --> %s\n", d, list_x.get(d), list_dx.get(d));
        }

        

        /* now see how we're doing with respect to the old kernel. modifies okbasis elements */
        ArrayList<Generator<Sq>> gens = new ArrayList<Generator<Sq>>();
        for(ModSet<Dot<Sq>> k : okbasis) {
            if(Config.DEBUG) System.out.printf("kernel element %s ", k);
            /* reduce against the image */
            while(! k.isEmpty()) {
                Map.Entry<Dot<Sq>,Integer> ent = k.lastEntry();
                Dot<Sq> d = ent.getKey();
                Integer coeff = ent.getValue();

                ModSet<Dot<Sq>> moddx = list_dx.get(d);
                if(moddx == null)
                    break;
                k.add(moddx, -coeff);
            }
            if(Config.DEBUG) System.out.printf("reduces to %s\n", k);

            if(k.isEmpty()) { /* successfully killed this kernel class */
                if(Config.DEBUG) System.out.println("has been already killed");
                continue;
            }


            if(Config.DEBUG) System.out.printf("adding a generator to kill %s\n", k);
            
            /* haven't yet killed this kernel class -- add a generator */
            
            int[] deg = new int[] {s,t,w};
            Generator<Sq> gen = new Generator<Sq>(deg, gens.size());
            gen.extraInfo += String.format("Tridegree (s,t,u)=(%d,%d,%d)\n", s,w,2*t);
            gen.img = k;

            /* add this into the existing image */
            list_x.put(k.lastKey(), new ModSet<Dot<Sq>>(new Dot<Sq>(gen, alg.unit())));
            list_dx.put(k.lastKey(), k);

            gens.add(gen);
        }

        /* okbasis is done (and modified), free it */
        if(olddat != null) 
            olddat.kbasis = null;
        

        /* compute the resolution maps */
        Map<Dot<Sq>,ModSet<Dot<Sq>>> resmap = new TreeMap<Dot<Sq>,ModSet<Dot<Sq>>>();
        Map<Dot<Sq>,ModSet<Dot<Sq>>> lastresmap = null;
        if(s != 0) lastresmap = olddat.resmap;

        for(Dot<Sq> pairb : enumerateOtherRes(t-s,w)) {

            ModSet<Dot<Sq>> target;
            if(s == 0)
                target = new ModSet<Dot<Sq>>(pairb);
            else {

                /* map down in the other resolution */
                ModSet<Pair> img = mapOtherRes(pairb,w);

                /* map across using the previous map */
                target = new ModSet<Dot<Sq>>();
                for(Entry<Pair,Integer> ent : img.entrySet()) {
                    Pair p = ent.getKey();
                    ModSet<Dot<Sq>> lift = resmap(s-1, t-p.a.deg(), w).get(p.b);
//                    if(s == 1)
//                        lift = lift.times(p.a, module);
//                    else
                        lift = mult_dots(lift, p.a, alg);

                    for(Entry<Dot<Sq>,Integer> ent2 : lift.entrySet())
                        target.add(ent2.getKey(), ent2.getValue() * ent.getValue());
                }

                if(Config.DEBUG)
                    System.out.printf("chain 1 \u2297 %s --> %s --> %s\n", pairb, img, target);
            }

            /* lift up the minimal resolution */
            ModSet<Dot<Sq>> lift = new ModSet<Dot<Sq>>();
            while(! target.isEmpty()) {
                /* reduce against */
                Map.Entry<Dot<Sq>,Integer> ent = target.lastEntry();
                Dot<Sq> d = ent.getKey();
                Integer coeff = ent.getValue();

                ModSet<Dot<Sq>> modx = list_x.get(d);
                ModSet<Dot<Sq>> moddx = list_dx.get(d);

                if(modx == null || moddx == null) {
                    System.err.printf("(%d,%d,%d) Error lifting: %s, highest term %s\n", s,t,w, target, d);
                    System.err.printf("chain 1 \u2297 %s --> %s\n", pairb, target);

                    return;
                }

                lift.add(modx, coeff);
                target.add(moddx, -coeff);
            }

            resmap.put(pairb, lift);
            if(Config.DEBUG)
                System.out.printf("(%2d,%2d,%2d): 1 \u2297 %s maps to %s\n", s,t,w, pairb, lift); 
        }

        
        /* save the result -- at this point the computation is considered finished */
        CotorLiftingCellData dat = new CotorLiftingCellData(gens, ker, resmap);
        putOutput(s, t, w, dat);
        ping(new int[] {s,t,w});

        if(Config.STDOUT) System.out.printf("(%2d,%2d,%2d): %2d gen, %2d ker\n\n", s, t, w, dat.gens.size(), dat.kbasis.size());

        /* kick off the child tasks */
        putTask(new CotorLiftingResTask(CotorLiftingResTask.POSTPROCESS, s,t,w));

        if(s < t && (t == s+1 || isComputed(s+1, t-1, w))) 
            if(atomic_claim_grid(s+1,t,w))
                putTask(new CotorLiftingResTask(CotorLiftingResTask.COMPUTE, s+1, t, w)); /* move up-left */
        if(isComputed(s-1, t+1, w))
            if(atomic_claim_grid(s,t+1,w))
                putTask(new CotorLiftingResTask(CotorLiftingResTask.COMPUTE, s, t+1, w)); /* move right */
        if(s == 0 && t == 0)
            if(atomic_claim_grid(s,t,w+1))
                putTask(new CotorLiftingResTask(CotorLiftingResTask.COMPUTE, s, t, w+1)); /* move in */
    }


    void postprocess(int s, int t, int w)
    {
        if(w == 0) return;
        if(gens(s,t,w).isEmpty()) return;
        boolean doOutput = (s == posts && t == postt && w == postw);

        /* compute the dual mapping on generators */
        Map<Generator<Sq>, ModSet<Dot<Sq>>> invmap = new TreeMap<Generator<Sq>, ModSet<Dot<Sq>>>();
        for(Generator<Sq> g : gens(s,t,w))
            invmap.put(g, new ModSet<Dot<Sq>>());

        for(Entry<Dot<Sq>, ModSet<Dot<Sq>>> ent : resmap(s,t,w).entrySet()) {
            for(Entry<Dot<Sq>, Integer> ent2 : ent.getValue().entrySet()) {
                if(! ent2.getKey().sq.equals(Sq.UNIT)) continue;
                /* otherwise this dot is a generator */
                ModSet<Dot<Sq>> image = invmap.get(ent2.getKey().base);
                image.add(ent.getKey(), ent2.getValue());
            }
        }

        /* compute the action of each q_I monomial on the other resolution */
        Map<QMonom,ModSet<Dot<Sq>>> monom_action = new TreeMap<QMonom,ModSet<Dot<Sq>>>();
        for(QMonom q : QMonom.gens(s,t,w)) 
            monom_action.put(q, new ModSet<Dot<Sq>>());
        for(Dot<Sq> o : enumerateOtherRes(t-s,w)) {
            /* TODO we can probably do this in a much more efficient way than computing iterated diagonals */
            ModSet<Sq[]> itd = iterated_diagonal(o.sq, w-1);
            if(Config.DEBUG) System.out.printf("%d-iterated diagonal of %s is %s\n", w, o.sq, itd.toString(sqArrayStringifier));

            /* for each term Sq^2^n1 Sq^2^n1-1 ... Sq^1 tens ... tens Sq^2^nr Sq^2^r-1 ... Sq^1, add o to [q_n1, ... q_nr] */
            for(Entry<QMonom,ModSet<Dot<Sq>>> ent : monom_action.entrySet()) {
                QMonom q = ent.getKey();
                Sq[] key = new Sq[w];
                for(int i = 0; i < w; i++) {
                    int[] sq = new int[q.q.length-1];
                    for(int j = 0; j < sq.length; j++)
                        sq[j] = 1<<(sq.length-j-1);
                    key[w-i-1] = new Sq(sq);
                    q = q.reduce();
                }

                if(Config.DEBUG) System.out.printf("testing %s with %s\n", ent.getKey(), sqArrayStringifier.toString(key));

                Integer coeff = itd.get(key);
                if(coeff != null)
                    ent.getValue().put(o, coeff);
            }
        }

        /* dump the q_I */
        if(Config.DEBUG) {
            System.out.println("dumping the q_I:");
            for(Entry<QMonom,ModSet<Dot<Sq>>> ent : monom_action.entrySet())
                System.out.printf("%s detects %s\n", ent.getKey(), ent.getValue());
        }

        /* register highest terms from the above */
        Map<Dot<Sq>,ModSet<QMonom>> list_x = new TreeMap<Dot<Sq>,ModSet<QMonom>>();
        Map<Dot<Sq>,ModSet<Dot<Sq>>> list_dx = new TreeMap<Dot<Sq>,ModSet<Dot<Sq>>>();
        for(QMonom q : monom_action.keySet()) {
            ModSet<QMonom> x = new ModSet<QMonom>(q);
            ModSet<Dot<Sq>> dx = monom_action.get(q);
            while(! dx.isEmpty()) {
                Dot<Sq> highest = dx.lastKey();
                ModSet<QMonom> hx = list_x.get(highest);
                ModSet<Dot<Sq>> hdx = list_dx.get(highest);
                
                if(hx == null) {
                    list_x.put(highest, x);
                    list_dx.put(highest, dx);
                    break;
                } else {
                    x.add(hx,-1);
                    dx.add(hdx,-1);
                }
            }
        }

        /* convert each generator to q_I form */
        String output = "";
        for(Entry<Generator<Sq>, ModSet<Dot<Sq>>> ent : invmap.entrySet()) {
            Generator<Sq> g = ent.getKey();
            ModSet<Dot<Sq>> dx = ent.getValue();

            if(doOutput)
                output += String.format("%s --> ", ent.getKey(), ent.getValue());

            ModSet<QMonom> x = new ModSet<QMonom>();

            while(! dx.isEmpty()) {
                Dot<Sq> highest = dx.lastKey();
                ModSet<QMonom> hx = list_x.get(highest);
                ModSet<Dot<Sq>> hdx = list_dx.get(highest);

                if(hx == null) {
                    JOptionPane.showMessageDialog(null, "error converting to q_I form! the following results are probably incorrect");
                    break;
                } else {
                    x.add(hx, 1);
                    dx.add(hdx, -1);
                }
            }

            g.extraInfo += "Q(0) result: "+x+"\n";
            if(doOutput)
                output += x + "\n";
        }

        if(doOutput)
            JOptionPane.showMessageDialog(null, output);
    }


    private static Comparator<Sq[]> sqArrayComparator = new Comparator<Sq[]>() {
        @Override public int compare(Sq[] a, Sq[] b) {
            int c = a.length - b.length;
            if(c != 0) return c;
            for(int i = 0; i < a.length; i++) {
                c = a[i].compareTo(b[i]);
                if(c != 0) return c;
            }
            return 0;
        }
    };

    private static ModSet<Sq[]> iterated_diagonal(Sq q, int w)
    {
        ModSet<Sq[]> ret = new ModSet<Sq[]>(sqArrayComparator);

        if(w == 0) {
            ret.add(new Sq[] {q}, 1);
            return ret;
        }

        ModSet<Sq[]> last = iterated_diagonal(q, w-1);
        for(Entry<Sq[],Integer> ent : last.entrySet()) {
            Sq[] s = ent.getKey();

            int i = s.length - 1;
            ModSet<Sq[]> diag = filter_diagonal(s[i]);
            for(Entry<Sq[],Integer> ent2 : diag.entrySet()) {
                Sq[] newEntry = Arrays.copyOf(s, s.length+1);
                newEntry[i] = ent2.getKey()[0];
                newEntry[i+1] = ent2.getKey()[1];

                if(newEntry[i+1].deg() < newEntry[i].deg())
                    continue;
                if(i != 0 && newEntry[i].deg() < newEntry[i-1].deg())
                    continue;

                ret.add(newEntry, ent2.getValue() * ent.getValue());
            }
        }

        System.out.printf("%d-itd diag of %s is %s\n", w, q, ret.toString(sqArrayStringifier));

        return ret;
    }

    /* here we throw out all diagonal terms whose LHS has excess greater than 1 */
    private static Map<Sq,ModSet<Sq[]>> filter_diagonal_cache = new TreeMap<Sq,ModSet<Sq[]>>();
    private static ModSet<Sq[]> filter_diagonal(Sq q)
    {
        ModSet<Sq[]> ret = filter_diagonal_cache.get(q);
        if(ret != null) return ret;

        ret = new ModSet<Sq[]>(sqArrayComparator);

        ModSet<Sq[]> orig = diagonal(q);
        for(Entry<Sq[],Integer> ent : orig.entrySet())
            if(ent.getKey()[0].excess() <= 1)
                ret.put(ent.getKey(), ent.getValue());
        
        filter_diagonal_cache.put(q,ret);
        return ret;
    }

    private static Map<Sq,ModSet<Sq[]>> diagonal_cache = new TreeMap<Sq,ModSet<Sq[]>>();
    private static ModSet<Sq[]> diagonal(Sq q)
    {
        ModSet<Sq[]> ret = diagonal_cache.get(q);
        if(ret != null) return ret;

        ret = new ModSet<Sq[]>(sqArrayComparator);

        if(q.q.length == 0) {
            ret.add(new Sq[] { Sq.UNIT, Sq.UNIT }, 1);
            diagonal_cache.put(q,ret);
            return ret;
        }

        if(q.q.length == 1) { /* cartan rule */
            ret.add(new Sq[] { Sq.UNIT, q }, 1);
            for(int i = 1; i < q.q[0]; i++)
                ret.add(new Sq[] { new Sq(i), new Sq(q.q[0]-i) }, 1);
            ret.add(new Sq[] { q, Sq.UNIT }, 1);
            diagonal_cache.put(q,ret);
            return ret;
        }

        /* general case: recurse by multiplication */
        Sq a = new Sq(Arrays.copyOf(q.q, q.q.length-1));
        Sq b = new Sq(q.q[q.q.length-1]);
        ModSet<Sq[]> da = diagonal(a);
        ModSet<Sq[]> db = diagonal(b);

        for(Entry<Sq[],Integer> ea : da.entrySet()) {
            Sq[] sqa = ea.getKey();
            for(Entry<Sq[],Integer> eb : db.entrySet()) {
                Sq[] sqb = eb.getKey();
                for(Entry<Sq,Integer> e0 : sqa[0].times(sqb[0]).entrySet()) {
                    for(Entry<Sq,Integer> e1 : sqa[1].times(sqb[1]).entrySet()) {
                        ret.add(new Sq[] { e0.getKey(), e1.getKey() }, ea.getValue() * eb.getValue() * e0.getValue() * e1.getValue());
                    }
                }
            }
        }

        diagonal_cache.put(q,ret);
        return ret;
    }

    private static Stringifier<Sq[]> sqArrayStringifier = new Stringifier<Sq[]>() {
        @Override public String toString(Sq[] sql) {
            if(sql.length == 0) return "1";

            String ret = "";
            boolean first = true;
            for(Sq sq : sql) {
                if(first) first = false;
                else ret += " \u2297 ";
                ret += sq.toString();
            }

            return ret;
        }
    };


    /* admin */
    public Decorated<Generator<Sq>, MultigradedComputation<Generator<Sq>>> getDecorated()
    {
        CompoundDecorated<Generator<Sq>,MultigradedComputation<Generator<Sq>>> dec = new CompoundDecorated<Generator<Sq>,MultigradedComputation<Generator<Sq>>>(this);

        Collection<DifferentialRule> diffrules = new ArrayList<DifferentialRule>();
        diffrules.add(new DifferentialRule(new int[] {1,0,1}, new int[] {0,0,1}, Color.green));
        diffrules.add(new DifferentialRule(new int[] {3,1,-2}, new int[] {2,1,-2}, Color.red));
        dec.add(new DifferentialDecorated<Generator<Sq>,MultigradedComputation<Generator<Sq>>>(this, diffrules));

        /* TODO add product decorated */

        return dec;
    }

    private void runTests()
    {
        System.out.println("TESTS:");
        
        iterated_diagonal(new Sq(new int[] {6,1}), 1).toString(sqArrayStringifier);

        System.out.println();

        int i = 8;
//        for(int i = 0; i < 8; i++)
            for(Sq q : alg.gens(i)) {
                iterated_diagonal(q,10);
                System.out.println();
            }


/*        int s = 3;
        int t = 9;
        int w = 2;
        System.out.printf("Q basis in (%d,%d,%d):\n", s,t,w);
        for(QMonom q : QMonom.gens(s,t,w))
            System.out.println(q);*/

        System.exit(0);
    }

}

class CotorLiftingResTaskThread extends Thread
{
    CotorLiftingBackend back;
    int id;
    volatile static int ids;
    CotorLiftingResTaskThread(CotorLiftingBackend back) {
        setPriority(Thread.MIN_PRIORITY);
        this.back = back;
        id = ids++;
    }

    @Override public void run()
    {
        while(true) {
            if(Config.DEBUG_THREADS) System.out.println(id + ": Waiting for task...");
            CotorLiftingResTask t;
            try {
                t = back.tasks.take();
            } catch(InterruptedException e) {
                continue;
            }
            if(Config.DEBUG_THREADS) System.out.println(id + ": got task ...");

            if(t.t + t.w > Config.T_CAP)
                continue;

            switch(t.type) {
                case CotorLiftingResTask.COMPUTE:
                    back.compute(t.s, t.t, t.w);
                    break;
                case CotorLiftingResTask.POSTPROCESS:
                   if(back.COMPUTE_ALL_QTRANS || (t.s == back.posts && t.t == back.postt && t.w == back.postw))
                        back.postprocess(t.s, t.t, t.w);
                    break;
                default:
                    Main.die_if(true, "Bad task type.");
            }
        }
    }
}

class CotorLiftingResTask implements Comparable<CotorLiftingResTask>
{
    /* task types */
    final static int COMPUTE = 0;
    final static int POSTPROCESS = 1;

    int type;
    int s, t, w;

    CotorLiftingResTask(int type, int s, int t, int w)
    {
        this.type = type;
        this.s = s;
        this.t = t;
        this.w = w;
    }

    @Override public int compareTo(CotorLiftingResTask o)
    {
        int sum = t + w;
        int osum = o.t + o.w;
        if(sum != osum)
            return sum - osum;
        if(t != o.t) return t - o.t;
        return s - o.s;
    }
}

class CotorLiftingCellData
{
    Collection<Generator<Sq>> gens;
    Collection<ModSet<Dot<Sq>>> kbasis; /* kernel basis dot-sums in bidegree s,t */
    Map<Dot<Sq>,ModSet<Dot<Sq>>> resmap;

    CotorLiftingCellData() { }
    CotorLiftingCellData(Collection<Generator<Sq>> g, Collection<ModSet<Dot<Sq>>> k, Map<Dot<Sq>,ModSet<Dot<Sq>>> m) {
        gens = g;
        kbasis = k;
        resmap = m;
    }
}

class Pair implements Comparable<Pair>
{
    Sq a;
    Dot<Sq> b;
    
    Pair(Sq a, Dot<Sq> b) {
        this.a = a;
        this.b = b;
    }

    @Override public int compareTo(Pair o)
    {
        int c = a.compareTo(o.a);
        if(c != 0) return c;
        return b.compareTo(o.b);
    }

    @Override public String toString()
    {
        return a.toString() + " \u2297 " + b.toString();
    }
}


class QMonom implements Comparable<QMonom>
{
    int[] q;

    QMonom(int[] q) {
        this.q = q;
    }

    /* monomials q_I, of total degree t-s, and with w terms */
    static Iterable<QMonom> gens(int s, int t, int w)
    {
        return basis_aux(t-s, w, new int[] {});
    }

    static Collection<QMonom> basis_aux(int d, int w, int[] base)
    {
/*        System.out.printf("basis_aux %d %d / ", d, w);
        for(int i : base) System.out.print(i + " ");
        System.out.println(); */

        if(w == 0) {
            if(d == 0) return Collections.singletonList(new QMonom(base));
            else return Collections.emptyList();
        }

        Collection<QMonom> ret = new ArrayList<QMonom>();
        int start = (base.length == 0) ? 0 : base.length - 1;
        for(int op = start;; op++) {
            int opdeg = (1<<op) - 1;
            if(opdeg > d) break;

            int[] next = Arrays.copyOf(base, op+1);
            next[op]++;
            ret.addAll(basis_aux(d-opdeg, w-1, next));
        }

        return ret;
    }

    QMonom reduce()
    {
        if(q.length == 0)
            return null;

        if(q[q.length-1] == 1) {
            /* find the next nonzero */
            int i;
            for(i = q.length-2; i >= 0 && q[i] == 0; i--);
            if(i == -1) return new QMonom(new int[] {});
            else return new QMonom(Arrays.copyOf(q, i+1));
        } else {
            int[] qnew = Arrays.copyOf(q, q.length);
            qnew[q.length-1]--;
            return new QMonom(qnew);
        }
    }

    @Override public int compareTo(QMonom o) {
        return Multidegrees.multidegComparator.compare(q, o.q);
    }

    @Override public String toString() {
        if(q.length == 0) return "1";
        
        String ret = "";
        boolean first = true;
        for(int i = 0; i < q.length; i++) {
            if(q[i] == 0) continue;
            if(first) first = false;
            else ret += " ";
            if(q[i] == 1) ret += "q"+i;
            else ret += "q"+i+"^"+q[i];
        }
        return ret;
    }
}
