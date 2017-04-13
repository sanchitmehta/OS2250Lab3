/*
 *
 * @Course : Operating Systems , Lab 3
 * @Author : Sanchit Mehta<sanchit.mehta@cs.nyu.edu>
 * @Desc: Fifo & Bankers Algorithm Implementation
 *
 */

import java.io.File;
import java.util.*;

public class Lab3 {

    private final String inputFile;
    public static int numTasks;
    public static int numResources;
    int terminatedTasksCount = 0, time = 0, abortedTasksCount = 0;
    List<Task> taskPool;
    List<Resource> resources;
    Map<Integer, Integer> releasedResourcesBuffer = new HashMap<>();
    List<Task> blockedBuffer = new ArrayList<>();
    List<Task> terminatedTasks = new ArrayList<>();
    List<Task> blockedTasks;
    List<Task> abortedTasks;
    int[][] max;
    int[][] avail;


    public Lab3(String filename) throws Exception {
        this.inputFile = filename;
        for (int i = 1; i < 3; i++) {
            readInput(filename);
            runTasks(i % 2 == 0);
            System.out.println("\n");
            printOutput(i%2==0?"BANKER'S":"FIFO");
            clearBuffer();
            System.out.println("\n\n\n");
        }
    }

    /*
        Clears all the common Buffers and DataStructures when
        FIFO Completes and Banker's algo is being run
     */
    public void clearBuffer() {
        blockedBuffer.clear();
        blockedTasks.clear();
        abortedTasks.clear();
        terminatedTasks.clear();
        releasedResourcesBuffer.clear();
        resources.clear();
        taskPool.clear();
        terminatedTasksCount = 0;
        time = 0;
        abortedTasksCount = 0;
    }

    /*
      Reads Input and Creates MaximumDemand and Total Availaible arrays
      for the Banker's Algorithm
     */
    public void readInput(String fileName) throws Exception {
        File f = new File(fileName);
        Scanner sc = new Scanner(f);
        this.numTasks = sc.nextInt();
        this.numResources = sc.nextInt();
        taskPool = new ArrayList<>(this.numTasks);
        this.resources = new ArrayList<>();
        this.blockedTasks = new ArrayList<>();
        this.abortedTasks = new ArrayList<>();

        max = new int[this.numResources][this.numTasks];
        avail = new int[this.numTasks][this.numResources];

        for (int i = 0; i < this.numTasks; i++)
            taskPool.add(new Task(i));
        for (int i = 0; i < this.numResources; i++) {
            int rCount = sc.nextInt();
            resources.add(new Resource(rCount));
            avail[0][i] = rCount;
        }

        while (sc.hasNext()) {
            String activityType = sc.next();
            int task_no = sc.nextInt();
            int resource_type = sc.nextInt();
            int resourceCount = sc.nextInt();
            Task.Status s = Task.Status.none;
            switch (activityType.toLowerCase()) {
                case "initiate":
                    s = Task.Status.initiate;
                    max[resource_type - 1][task_no - 1] = resourceCount;
                    break;
                case "request":
                    s = Task.Status.request;
                    break;
                case "compute":
                    s = Task.Status.compute;
                    break;
                case "release":
                    s = Task.Status.release;
                    break;
                case "terminate":
                    s = Task.Status.terminate;
                    break;
            }
            taskPool.get(task_no - 1).addNewActivity(s, resource_type, resourceCount);
        }
    }

    /*
        Executes a cycle for all the tasks, gives preference to Blocked
        tasks first.
    */
    public void runTasks(boolean isBankers) {
        //initiate all the tasks
        while (this.abortedTasksCount + this.terminatedTasksCount != this.taskPool.size()) { //all the tasks must either be aborted or completed.
            // kill a task in the beginning if there is a deadlock
            if (!isBankers) {
                if (blockedTasks.size() + abortedTasks.size() + terminatedTasks.size() == this.taskPool.size()) {
                    killTasks();
                }
            }

            if (!this.blockedTasks.isEmpty()) {
                executeCycle(this.blockedTasks, true, isBankers);
            }
            executeCycle(taskPool, false, isBankers);
            for (Task t : blockedBuffer)
                blockedTasks.remove(t);
            blockedBuffer.clear();
        }
    }

    public void executeCycle(List<Task> Tasks, boolean blockedRun, boolean isBankers) {

        if (blockedRun)
            for (Task t : blockedTasks)
                t.waitTime++;

        for (Integer k : releasedResourcesBuffer.keySet()) {
            resources.get(k).available += releasedResourcesBuffer.get(k);
            releasedResourcesBuffer.put(k, 0);
        }
        releasedResourcesBuffer.clear();
        for (Task t : Tasks) {
            if (!(blockedRun ^ blockedTasks.contains(t)) && !abortedTasks.contains(t)
                    && !terminatedTasks.contains(t)) {
                Activity a = t.activities.remove(0);
                switch (a.status) {
                    case initiate:
                        t.currStatus = Task.Status.initiate;
                        t.runTime++;
                        break;
                    case request:
                        t.currStatus = Task.Status.request;
                        t.runTime++;
                        break;
                    case release:
                        t.currStatus = Task.Status.release;
                        t.runTime++;
                        break;
                    case terminate:
                        t.currStatus = Task.Status.terminate;
                        break;
                    case compute:
                        t.runTime++;
                        t.currStatus = Task.Status.compute;
                }
                executeActivity(t, t.currStatus, a, isBankers);
            }
        }
        time = time + 1; //increment the time
    }

    /*
        Executes an Activity based on it's status.
        Ensures there is no deadlock when Banker's Algo is Run
     */
    public void executeActivity(Task T, Task.Status requestType, Activity a, boolean isBankers) {
        if (requestType == Task.Status.initiate) {
            //Some Logic Here
            if (isBankers && max[a.resourceType - 1][T.taskNo] > resources.get(a.resourceType - 1).claim) {
                //abort sequence
                abortTask(T);
            }
        } else if (requestType == Task.Status.terminate) {
            terminatedTasks.add(T);
            this.terminatedTasksCount++;
        } else if (requestType == Task.Status.request) {
            if (resources.get(a.resourceType - 1).available >= a.resourceCount) {
                if (isBankers) {
                    //Ensuring A Task does not demands more than it claims
                    int allocRec = T.allocatedResources.get(a.resourceType - 1) == null ? 0 : T.allocatedResources.get(a.resourceType - 1);
                    if (a.resourceCount + allocRec > T.claim) {
                        //abort sequence
                        abortTask(T);
                        return;
                    }
                    int[] avail = new int[numResources];
                    for (int i = 0; i < this.numResources; i++) {
                        avail[i] = resources.get(i).available;
                    }
                    int[][] alloc = createAllocationMatrix();
                    //Checking if grating this resource would not result
                    // in an unsafe state
                    if (!isSafe(createNeedMatrix(), avail, T.taskNo)) {
                        T.activities.add(0, a);
                        if (!this.blockedTasks.contains(T)) {
                            this.blockedTasks.add(T); //add in the blocked Queue
                        }
                        return;
                    }
                }
                //Grant the request to that task
                if (blockedTasks.contains(T))
                    blockedBuffer.add(T);
                resources.get(a.resourceType - 1).available -= a.resourceCount;
                T.allocateResource(a.resourceType - 1, a.resourceCount);
            } else {
                //The resources requested by this task are currently no availaible,
                //this task is added to blocked tasks pool
                T.activities.add(0, a);
                if (!this.blockedTasks.contains(T)) {
                    this.blockedTasks.add(T); //add in the blocked Queue
                }
            }
        } else if (requestType == Task.Status.compute) {
            a.resourceType--;
            if (a.resourceType != 0)
                T.activities.add(0, a);
        } else if (requestType == Task.Status.release) {
            for (Integer k : T.allocatedResources.keySet()) {
                if (k == a.resourceType - 1) {
                    if (releasedResourcesBuffer.get(k) != null)
                        releasedResourcesBuffer.put(k, releasedResourcesBuffer.get(k) + T.allocatedResources.get(k));
                    else
                        releasedResourcesBuffer.put(k, T.allocatedResources.get(k));
                    T.allocatedResources.put(k, 0);
                }
            }
        }
    }

    /*
        Prints the formatted output
     */
    public void printOutput(String runType) {
        System.out.print("\t\t" + runType + "\t\t");
        System.out.print("\n");
        int totalRunTime = 0;
        int totalWaitTime = 0;
        for (Task t : this.taskPool) {
            if (abortedTasks.contains(t)) {
                System.out.println("Task " + (t.taskNo + 1) + "\taborted");
                continue;
            }
            totalRunTime += t.runTime;
            totalWaitTime += t.waitTime;
            System.out.print("Task " + (t.taskNo + 1) + "\t" + t.runTime + "\t" +
                    t.waitTime + "\t" +
                    (t.waitTime / (double) t.runTime) * 100 + "%"
                    + "\n");
        }

        System.out.print("Total \t" + totalRunTime + "\t" + totalWaitTime + "\t" + (totalWaitTime / (double) totalRunTime) * 100 + "%");

    }

    /*
        Kills a task in case of a dadlock . Releases it's resources
     */
    public void killTasks() {
        Task t = null;
        for (int i = 0; i < taskPool.size(); i++) {
            t = taskPool.get(i);
            if (!isAbortedOrTerminated(t))
                break;
        }
        abortTask(t);
        if (canAClaimBeSatisfied())
            return;
        killTasks();
    }

    /*
        Helper method for kill task. Also used when
        to abort a task having invalid claims
     */
    public void abortTask(Task t) {
        abortedTasksCount++;
        abortedTasks.add(t); //add to the list of aborted
        if (blockedTasks.contains(t))
            blockedTasks.remove(t); //Remove from the list of blocked tasks

        //add that resource
        for (Integer resource : t.allocatedResources.keySet()) {
            resources.get(resource).available += t.allocatedResources.get(resource);
            t.allocatedResources.put(resource, 0);
        }
    }

    public boolean canAClaimBeSatisfied() {
        for (Task t : taskPool) {
            if (!isAbortedOrTerminated(t)) {
                Activity a = t.activities.get(0);
                boolean isClaimSatisfied = a.resourceCount <= resources.get(a.resourceType - 1).available;
                if (isClaimSatisfied)
                    return true;
            }
        }
        return false;
    }

    public boolean isAbortedOrTerminated(Task t) {
        return abortedTasks.contains(t) || terminatedTasks.contains(t);
    }

    /*
        Ensure that granting a resource to a task does not result
        in Deadlock in future by Banker's Algo
     */
    public boolean isSafe(int[][] need, int[] avail, int processId) {
        for (int i = 0; i < numResources; i++)
            if (need[i][processId] > avail[i])
                return false;
        return true;
    }

    private int[][] createAllocationMatrix() {
        int[][] ret = new int[this.numResources][this.numTasks];
        for (int i = 0; i < this.numResources; i++) {
            for (int j = 0; j < this.numTasks; j++) {
                Task t = taskPool.get(j);
                if (t.allocatedResources.get(i) != null)
                    ret[i][j] = t.allocatedResources.get(i);
                else
                    ret[i][j] = 0;
            }

        }
        return ret;
    }

    public int[][] createNeedMatrix() {
        int need[][] = new int[this.numResources][this.numTasks];
        int alloc[][] = createAllocationMatrix();

        for (int i = 0; i < this.numTasks; i++)  //need loop
        {
            for (int j = 0; j < this.numResources; j++) {
                need[j][i] = max[j][i] - alloc[j][i];
            }
        }
        return need;
    }

    public static void main(String args[]) {
        if (args.length != 1)
            throw new IllegalArgumentException("Exactly 1 argument required : <Input File Name>");
        try {
            Lab3 l = new Lab3(args[0]);
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}
