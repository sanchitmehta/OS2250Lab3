/**
 * Created by sanchitmehta on 12/04/17.
 */
public class Resource {
    int available = 0;
    int claim =0;
    int resourceType = -1;

    public Resource(int totalCount){
        this.available = totalCount;
        this.claim = totalCount;
    }
}
