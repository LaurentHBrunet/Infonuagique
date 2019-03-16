package ca.polymtl.inf8480.tp1.shared;

import java.io.Serializable;

public class Tuple<X, Y> implements Serializable {
    public final X x;
    public final Y y;
    public Tuple(X x, Y y) {
        this.x = x;
        this.y = y;
    }
    public X getKey(){
        return x;
    }
    public Y getValue(){
        return y;
    }
}