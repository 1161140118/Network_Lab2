import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * @author chen
 *
 */
public class GBNSender {
    private int port = 8080;
    private DatagramSocket datagramSocket = new DatagramSocket();
    private DatagramPacket datagramPacket;
    private InetAddress inetAddress;
    private Clock timer;
    private int nextSeq = 1;
    private int base = 1;
    private int N = 5;


    public GBNSender(int port) throws Exception {
        this.port = port;
        inetAddress = InetAddress.getLocalHost();
        timer = new Clock(this, 3000);
        timer.start();

        while (true) {

            sendData();
            // 从接收方接受ACK
            byte[] bytes = new byte[1024];
            datagramPacket = new DatagramPacket(bytes, bytes.length);
            datagramSocket.receive(datagramPacket);
            String fromServer = new String(bytes, 1, bytes.length - 1);
            System.out.println(bytes[0]);
            int ack = (int) bytes[0];
            // 窗口滑动
            base = ack + 1;
            if (base == nextSeq) {
                // 停止计时器
                // model.setTime(0);
                timer.setTimeout(0);
            } else {
                // 开始计时器
                // model.setTime(3);
                timer.interrupt();
            }
            System.out.println("来自接收方 >> " + bytes[0] + fromServer);
        }

    }

    /**
     * 向接收方发送数据
     *
     * @throws Exception
     */
    private void sendData() throws Exception {
        while (nextSeq < base + N && nextSeq <= 10) {
            // 随机数模拟丢失
            if (Math.random()<0.1) {
                nextSeq++;
                continue;
            }

            String clientData = (char) nextSeq + " << 得到ACK";
            System.out.println(nextSeq + " << 已发送数据报");

            byte[] data = clientData.getBytes();
            DatagramPacket datagramPacket =
                    new DatagramPacket(data, data.length, inetAddress, port);
            datagramSocket.send(datagramPacket);

            if (nextSeq == base) {
                // 开始计时
                // model.setTime(3);
                timer.setTimeout(3000);
            }
            nextSeq++;
        }
    }

    /**
     * 超时数据重传
     */
    public void timeOut() {
        System.err.println("\n超时！");
        for (int i = base; i < nextSeq; i++) {
            String clientData = (char) i + " << 重发数据报";
            System.out.println("已重发数据报:" + i);
            byte[] data = clientData.getBytes();
            DatagramPacket datagramPacket =
                    new DatagramPacket(data, data.length, inetAddress, port);
            try {
                datagramSocket.send(datagramPacket);
            } catch (IOException e) {
                i--;
                continue;
            }
        }
    }

    public static void main(String[] args) throws Exception {
        new GBNSender(8080);

    }
}


class Clock extends Thread {
    private int timeout;
    private GBNSender gbnSender;

    /**
     * 
     */
    public Clock(GBNSender gbnSender, int timeout) {
        this.gbnSender = gbnSender;
        this.timeout = timeout;
    }

    /**
     * @param timeout the timeout to set
     */
    public synchronized void setTimeout(int timeout) {
        this.timeout = timeout;
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
                if (timeout != 0) {
                    gbnSender.timeOut();
                }
            } catch (InterruptedException e) {
                continue;
            }
        }

    }
}
