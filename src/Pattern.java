import java.util.List;

/**
 * Created by ankitbahl on 4/29/17.
 */
public class Pattern {
    List<Byte> pattern;
    int ref;
    int arrayIndex;
    Pattern(List<Byte> pattern, int ref, int arrayIndex) {
        this.pattern = pattern;
        this.ref = ref;
        this.arrayIndex = arrayIndex;
    }
}
