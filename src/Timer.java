/**
 * @author chen
 *
 */
public class Timer extends Thread {
    private final int timeout;
    private final int seq;
    private final Sender sender;

    public Timer(Sender sender, int timeout, int seq) {
        super();
        this.sender = sender;
        this.timeout = timeout;
        this.seq = seq;

        start();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Thread#run()
     */
    @Override
    public void run() {
        while (true) {
            try {
                sleep(timeout);
            } catch (InterruptedException e) {
                break;
            }
            System.err.println("序列号  "+seq+"  确认超时！");
            sender.timeoutHandler(seq);
        }
    }

}
