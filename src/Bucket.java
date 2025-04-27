import java.io.Serializable;

public class Bucket implements Serializable {
    private int nf = -1;
    private int nl = -1;

    public int getNf() {
        return nf;
    }

    public void setNf(int nf) {
        this.nf = nf;
    }

    public int getNl() {
        return nl;
    }

    public void setNl(int nl) {
        this.nl = nl;
    }
}
