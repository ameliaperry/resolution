# resolution

Variations on a theme: the cohomology of the Steenrod algebra, providing the E_2 page of the Adams spectral sequence.

This program computes Ext_A(M,F_p), where

* *A* is the Steenrod algebra or its subalgebra A(n),
* *M* is an A-module, either one of a few standard examples, or else specified in Bruner's MDF format,
* *p* is any prime.

A couple of other miscellaneous computations are included, such as the E_2 page of the Cartan-Eilenberg spectral sequence.

Output takes the form of a scrollable chart, with products by the first three Hopf elements drawn. For a laugh, there's also a 3D viewer for the trigraded computations.



# How to use

Running the program requires only the file `resolution.jar`, available at 

http://willperry.me/downloads/resolution-latest.jar

For trigraded computations in the 2D viewer, there are controls to limit the visible range for the third grading; these are also jointly controlled by the PgUp and PgDn keys on your keyboard, in case you want to quickly step through cross-sections.

For trigraded computations in the 3D viewer, you can drag the mouse to rotate, Shift+drag to pan the view, and either scroll or Ctrl+drag to zoom.

If there are features you'd like to see in this program, please get in touch!



# Module format

The program can load modules specified in the `module definition format' of Bob Bruner:
http://www.math.wayne.edu/~rrb/cohom/modfmt.html

The format changes slightly for odd primes, since we need to include coefficients in the action. The format for the action lines is now:

    g r k c_1 g_1 c_2 g_2 ... c_k g_k

to indicate that Sq^r of generator g equals c_1 g_1 + c_2 g_2 + ... + c_k g_k. Choice of integer representative (mod p) of the coefficients doesn't matter.

So e.g. S/p as a module over the mod-p Steenrod algebra is given as:

    2
    0 1
    0 1 1 1 1

for any odd p.
(Usually, of course, the same module file won't work for all odd primes.)



# Compiling

This repository includes shell scripts `make` for compiling, `run` for running, and `prof` for profiling. Currently they're a bit specialized to my machine, in particular referring to a Java 6 runtime `rt.jar` in a subfolder, but it shouldn't be too hard to get it built if you're into Java. Feel free to contact me.


# Acknowledgements

Most of the features in this program were developed during a period of collaboration with Michael Andrews, and so he has guided its direction to a large extent. Thanks also to Mark Behrens for feature suggestions.

