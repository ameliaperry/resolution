package res.algebratypes;

/* Slightly awkward type issue here. We need to give the right T2 action a
 * different name, in order not to clash with times() from GradedAlgebra
 * after type-parameter erasure.
 */
public interface GradedAlgebraWithAction<T1,T2> extends GradedAlgebra<T1> {
    ModSet<T1> times_r(T1 a, T2 b); 
}

