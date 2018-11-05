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
            // �ӽ��շ�����ACK
            byte[] bytes = new byte[1024];
            datagramPacket = new DatagramPacket(bytes, bytes.length);
            datagramSocket.receive(datagramPacket);
            String fromServer = new String(bytes, 1, bytes.length - 1);
            System.out.println(bytes[0]);
            int ack = (int) bytes[0];
            // ���ڻ���
            base = ack + 1;
            if (base == nextSeq) {
                // ֹͣ��ʱ��
                // model.setTime(0);
                timer.setTimeout(0);
            } else {
                // ��ʼ��ʱ��
                // model.setTime(3);
                timer.interrupt();
            }
            System.out.println("���Խ��շ� >> " + bytes[0] + fromServer);
        }

    }

    /**
     * ����շ���������
     *
     * @throws Exception
     */
    private void sendData() throws Exception {
        while (nextSeq < base + N && nextSeq <= 10) {
            // �����ģ�ⶪʧ
            if (Math.random()<0.1) {
                nextSeq++;
                continue;
            }

            String clientData = (char) nextSeq + " << �õ�ACK";
            System.out.println(nextSeq + " << �ѷ������ݱ�");

            byte[] data = clientData.getBytes();
            DatagramPacket datagramPacket =
                    new DatagramPacket(data, data.length, inetAddress, port);
            datagramSocket.send(datagramPacket);

            if (nextSeq == base) {
                // ��ʼ��ʱ
                // model.setTime(3);
                timer.setTimeout(3000);
            }
            nextSeq++;
        }
    }

    /**
     * ��ʱ�����ش�
     */
    public void timeOut() {
        System.err.println("\n��ʱ��");
        for (int i = base; i < nextSeq; i++) {
            String clientData = (char) i + " << �ط����ݱ�";
            System.out.println("���ط����ݱ�:" + i);
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