package util;

import com.rits.cloning.Cloner;

public final class CloneUtil {
    private static Cloner cloner = null;
    private CloneUtil(){}

    public static Cloner getCloner() {
        if (cloner == null) {
            synchronized(CloneUtil.class) {
                Cloner cln = cloner;
                if (cln == null) {
                    synchronized(CloneUtil.class) {
                        cloner = new Cloner();
                    }
                }
            }
        }
        return cloner;
    }
}
