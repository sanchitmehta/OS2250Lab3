/**
 * Created by sanchitmehta on 12/04/17.
 */
public class Activity {
    Task.Status status;
    int resourceType;
    int resourceCount;

    public Activity(Task.Status s,int resourceType,int resourceCount){
        //System.out.println("Creating Activity with "+resourceType+" & "+resourceCount);
        this.status=s;
        this.resourceType = resourceType;
        this.resourceCount = resourceCount;
    }
}
