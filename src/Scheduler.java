import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

/**
 * Created by anish on 3/2/17.
 */
public class Scheduler {

    static List<Integer> randomNos = new ArrayList<>();
    int filePos = 0;
    int cycle = 0;

    int cpuUtilization = 0;
    int ioUtilization = 0;

    enum State {
        unstarted, ready, running, blocked, terminated
    }

    enum Algo {
        fcfs, uni, sjf, rr
    }

    class Details {
        int finishing;
        int turnaround;
        int io;
        int waiting;
    }

    class Process {
        final int quantum = 2;
        int A;
        int B;
        int C;
        int M;
        int cpuRemaining;
        int cpuBurst;
        int cpuBurstRemaining;
        int ioBurstRemaining;
        int rrQuantumRemaining = quantum;
        int readyCycle;
        State state;
        Details details;

        public Process(int A, int B, int C, int M) {
            this.A = A;
            this.B = B;
            this.C = C;
            this.M = M;
            this.cpuRemaining = C;
            details = new Details();
            state = State.unstarted;
        }

        public void resetQuantum() {
            rrQuantumRemaining = quantum;
        }

        public void resetFields() {
            cpuRemaining = C;
            cpuBurst = 0;
            cpuBurstRemaining = 0;
            ioBurstRemaining = 0;
            resetQuantum();
            readyCycle = 0;
            details = new Details();
            state = State.unstarted;
        }
    }

    public void reset(List<Process> processList) {
        filePos = 0;
        cycle = 0;
        cpuUtilization = 0;
        ioUtilization = 0;
        for (Process p : processList)
            p.resetFields();
    }

    public void scheduleJobs(final List<Process> processList, boolean verbose, Algo algo) {
        Queue<Process> processQueue = new LinkedList<>();
        processQueue.addAll(processList);
        PriorityQueue<Process> readyQueue = new PriorityQueue<>();

        if (algo.equals(Algo.sjf)) readyQueue = getReadyQueueForSjf(processList);
        else if (algo.equals(Algo.fcfs)) readyQueue = getReadyQueueForFcfs(processList);
        else if (algo.equals(Algo.uni)) readyQueue = getReadyQueueForUni(processList);
        else if (algo.equals(Algo.rr)) readyQueue = getReadyQueueForFcfs(processList);

        PriorityQueue<Process> blockedQueue = new PriorityQueue<>(processList.size(), new Comparator<Process>() {
            @Override
            public int compare(Process o1, Process o2) {
                return o1.ioBurstRemaining - o2.ioBurstRemaining;
            }
        });
        Process runningProcess = null;
        if (verbose) {
            System.out.println("This detailed printout gives the state and remaining burst for each process");
            System.out.println();
        }
        do {

            if (verbose) printIteration(processList, cycle);

            for (Process p : readyQueue) {
                p.details.waiting++;
            }

            while (!processQueue.isEmpty() && processQueue.peek().A == cycle) {
                Process p = processQueue.poll();
                p.state = State.ready;
                p.readyCycle = cycle;
                readyQueue.offer(p);
            }

            if (!blockedQueue.isEmpty())
                ioUtilization++;

            for (Process p : blockedQueue) {
                p.ioBurstRemaining--;
                p.details.io++;
            }

            while (!blockedQueue.isEmpty() && blockedQueue.peek().ioBurstRemaining == 0) {
                Process p = blockedQueue.poll();
                p.state = State.ready;
                p.readyCycle = cycle;
                readyQueue.offer(p);
            }

            if (runningProcess != null && runningProcess.cpuBurstRemaining != 0) {
                cpuUtilization++;
                runningProcess.cpuBurstRemaining--;
                runningProcess.cpuRemaining--;
                runningProcess.rrQuantumRemaining--;
                if (runningProcess.cpuBurstRemaining == 0) {
                    if (runningProcess.cpuRemaining == 0) {
                        runningProcess.state = State.terminated;
                        runningProcess.details.finishing = cycle;
                        runningProcess.details.turnaround = cycle - runningProcess.A;
                    }
                    else {
                        runningProcess.ioBurstRemaining = runningProcess.cpuBurst * runningProcess.M;
                        runningProcess.state = State.blocked;
                        blockedQueue.offer(runningProcess);
                    }
                }
                else if (algo.equals(Algo.rr) && runningProcess.rrQuantumRemaining == 0) {
                    runningProcess.state = State.ready;
                    runningProcess.readyCycle = cycle;
                    readyQueue.offer(runningProcess);
                }
            }

            if ((runningProcess == null|| runningProcess.cpuBurstRemaining == 0 ||
                    (algo.equals(Algo.rr) && runningProcess.rrQuantumRemaining == 0))
                    && !readyQueue.isEmpty()) {
                if (algo.equals(Algo.uni) && runningProcess != null && !runningProcess.state.equals(State.terminated)
                        && !readyQueue.peek().equals(runningProcess)) {
                    cycle++;
                    continue;
                }
                runningProcess = readyQueue.poll();
                runningProcess.state = State.running;
                runningProcess.resetQuantum();
                if (runningProcess.cpuBurstRemaining == 0) {
                    int rand = randomOS(runningProcess.B);
                    runningProcess.cpuBurst = rand > runningProcess.cpuRemaining ? runningProcess.cpuRemaining : rand;
                    runningProcess.cpuBurstRemaining = runningProcess.cpuBurst;
                }
            }

            cycle++;
        } while (!processQueue.isEmpty() || !readyQueue.isEmpty() || !blockedQueue.isEmpty()
                || !runningProcess.state.equals(State.terminated));
    }

    private PriorityQueue<Process> getReadyQueueForUni(final List<Process> processList) {
        return new PriorityQueue<>(processList.size(), new Comparator<Process>() {
            @Override
            public int compare(Process o1, Process o2) {
                if (o1.A < o2. A) return -1;
                else if (o1.A > o2.A) return 1;
                else return processList.indexOf(o1) < processList.indexOf(o2) ? -1 : 1;
            }
        });
    }

    private PriorityQueue<Process> getReadyQueueForFcfs(final List<Process> processList) {
        return new PriorityQueue<>(processList.size(), new Comparator<Process>() {
            @Override
            public int compare(Process o1, Process o2) {
                if (o1.readyCycle < o2.readyCycle) return -1;
                else if (o1.readyCycle > o2.readyCycle) return 1;
                else {
                    if (o1.A < o2. A) return -1;
                    else if (o1.A > o2.A) return 1;
                    else return processList.indexOf(o1) < processList.indexOf(o2) ? -1 : 1;
                }
            }
        });
    }

    private PriorityQueue<Process> getReadyQueueForSjf(final List<Process> processList) {
        return new PriorityQueue<>(processList.size(), new Comparator<Process>() {
            @Override
            public int compare(Process o1, Process o2) {
                if (o1.cpuRemaining < o2.cpuRemaining) return -1;
                else if (o1.cpuRemaining > o2.cpuRemaining) return 1;
                else {
                    if (o1.A < o2. A) return -1;
                    else if (o1.A > o2.A) return 1;
                    else return processList.indexOf(o1) < processList.indexOf(o2) ? -1 : 1;
                }
            }
        });
    }

    private void printIteration(List<Process> processList, int cycle) {
        System.out.print("Before cycle" + '\t' + cycle + ":");
        for (Process p : processList) {
            int burstRemaining = 0;
            if (p.state.equals(State.running)) burstRemaining = p.cpuBurstRemaining;
            else if (p.state.equals(State.blocked)) burstRemaining = p.ioBurstRemaining;

            System.out.print("\t" + p.state + " " + burstRemaining);
        }
        System.out.println();
    }

    public int randomOS(int range) {
        int num = randomNos.get(filePos++);
        return 1 + num%range;
    }

    public static void main(String[] args) throws FileNotFoundException {
        Scanner sc = new Scanner(new File("/Users/anish/NYU Coursework/OS/Labs/Lab2/random-numbers"));
        while (sc.hasNext()) randomNos.add(sc.nextInt());
        Scheduler s = new Scheduler();
        String input = args[args.length-1];
        boolean verbose = false;
        if (args.length > 1 && args[0].equals("--verbose")) verbose = true;
        s.beginScheduling(input, verbose);
    }

    public void beginScheduling(String input, boolean verbose) throws FileNotFoundException {
        List<Process> processList = new ArrayList<>();
        Scanner sc = new Scanner(new File(input));
        int num = sc.nextInt();
        String origin = "The original input was: " + num;
        for (int i = 0; i < num; i++) {
            int A = Integer.parseInt(sc.next().substring(1));
            int B = sc.nextInt();
            int C = sc.nextInt();
            String m = sc.next();
            int M = Integer.parseInt(m.substring(0, m.length()-1));

            origin += " (" + A + " " + B + " " + C + " " + M + ")";
            processList.add(new Process(A, B, C, M));
        }

        Collections.sort(processList, new Comparator<Process>() {
            @Override
            public int compare(Process o1, Process o2) {
                return o1.A - o2.A;
            }
        });

        String sorted = "The (sorted) input is: " + num;
        for (Process p : processList) {
            sorted += " (" + p.A + " " + p.B + " " + p.C + " " + p.M + ")";
        }

        scheduleForAlgo(processList, Algo.fcfs, origin, sorted, verbose);

        reset(processList);
        System.out.println();

        scheduleForAlgo(processList, Algo.sjf, origin, sorted, verbose);

        reset(processList);
        System.out.println();

        scheduleForAlgo(processList, Algo.uni, origin, sorted, verbose);

        reset(processList);
        System.out.println();

        scheduleForAlgo(processList, Algo.rr, origin, sorted, verbose);
    }

    private void scheduleForAlgo(List<Process> processList, Algo algo, String origin, String sorted, boolean verbose) {
        String algoStr = "";
        switch (algo) {
            case fcfs: algoStr = "First Come First Served";
                break;
            case sjf: algoStr = "Shortest Job First";
                break;
            case uni: algoStr = "Uniprocessing";
                break;
            case rr: algoStr = "Round Robin";
                break;
        }

        System.out.println(origin);
        System.out.println(sorted);
        System.out.println();

        scheduleJobs(processList, verbose, algo);
        System.out.println("The scheduling algorithm used was " + algoStr);
        System.out.println();
        printDetails(processList);
        printSummary(processList);
    }

    private void printDetails(List<Process> processList) {
        int i = 0;
        for (Process p : processList) {
            System.out.println("Process " + i++ + ":");
            System.out.println("(A,B,C,M) = (" + p.A + "," + p.B + "," + p.C + "," + p.M + ")");
            System.out.println("Finishing time: " + p.details.finishing);
            System.out.println("Turnaround time: " + p.details.turnaround);
            System.out.println("I/O time: " + p.details.io);
            System.out.println("Waiting time: " + p.details.waiting);
            System.out.println();
        }
    }

    private void printSummary(List<Process> processList) {
        float avgtt = 0, avgwt = 0;
        for (Process p : processList) {
            avgtt += p.details.turnaround;
            avgwt += p.details.waiting;
        }
        avgtt /= processList.size();
        avgwt /= processList.size();
        cycle--;
        System.out.println("Summary data:");
        System.out.println("Finishing time: " + cycle);
        System.out.println("CPU Utilization: " + (float) cpuUtilization/cycle);
        System.out.println("I/O Utilization: " + (float) ioUtilization/cycle);
        System.out.println("Throughput: " + processList.size()*100.0/cycle + " processes per hundred cycles");
        System.out.println("Average turnaround time: " + avgtt);
        System.out.println("Average waiting time: " + avgwt);
    }
}
