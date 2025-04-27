import java.io.Serializable;

public class Block implements Serializable {
    public static final int BLOCK_SIZE = 5;
    public static final int SIZE = 448;
    private int size = 0;


    Zap[] zap_block = new Zap[BLOCK_SIZE];
    private int nextb = -1;

    public boolean isFull() {
        return size >= BLOCK_SIZE;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public Zap getZap(int index) {
        return zap_block[index];
    }

    public int size() {
        return size;
    }


    public boolean addZap(Zap zap) {
        for (int i = 0; i < zap_block.length; i++) {
            if (zap_block[i] == null) {
                zap_block[i] = zap;
                size++;
                return true;
            }
        }
        return false;
    }


    public boolean updateOrRemoveZapById(int id_zachet, Zap newZap) {
        int index = findZapIndexById(id_zachet);
        if (index == -1) return false;

        if (newZap != null) {
            zap_block[index] = newZap;
        } else {
            if (size > 1) {
                zap_block[index] = zap_block[size - 1];
                zap_block[size - 1] = null;
                size--;
            } else {
                zap_block[index] = null;
                size--;
            }
        }
        return true;
    }


    public int findZapIndexById(int id_zachet) {
        for (int i = 0; i < zap_block.length; i++) {
            if (zap_block[i] != null && zap_block[i].getId_zachet() == id_zachet) {
                return i;
            }
        }
        return -1;
    }


    public int getNextb() {
        return nextb;
    }

    public void setNextb(int nextb) {
        this.nextb = nextb;
    }
}
