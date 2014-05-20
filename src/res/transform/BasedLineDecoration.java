package res.transform;

import java.awt.Color;

public class BasedLineDecoration<T> {
    public T src;
    public T dest;
    public Color color;

    public BasedLineDecoration(T src, T dest, Color color) {
        this.src = src;
        this.dest = dest;
        this.color = color;
    }
}

