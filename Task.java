import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by sanchitmehta on 12/04/17.
 */
public class Task {

    int taskNo;
    Status currStatus;
    List<Activity> activities = new ArrayList<>();
    Map<Integer,Integer> allocatedResources = new HashMap<>();
    int runTime = 0;
    int waitTime = 0;
    int claim = 0;

    public enum Status {
        initiate, request, compute, release, terminate,none;
    }

    public Task(int i){
        this.taskNo = i;
        this.currStatus = Status.none;
    }

    public void addNewActivity(Status s,int resource_type,int resourceCount ){
        if(s==Status.initiate)
            this.claim = resourceCount;
        activities.add(new Activity(s,resource_type,resourceCount));
    }

    public void allocateResource(int resourceId,int resourceCount){
        if(allocatedResources.get(resourceId)==null){
            allocatedResources.put(resourceId,resourceCount);
        }else{
            allocatedResources.put(resourceId,allocatedResources.get(resourceId)+resourceCount);
        }
    }


}
