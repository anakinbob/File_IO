/**
 * Created by ankitbahl on 5/5/17.
 */
public class ReferenceMap {
    int refIndex; //index in the large byte array that the reference occurs
    int ref;
    public ReferenceMap(int refIndex, int ref) {
        this.refIndex = refIndex;
        this.ref = ref;
    }
}
