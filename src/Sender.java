import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author chen
 *
 */
public class Sender extends Thread {
    /** ���ͷ�Ĭ�϶˿ں� */
    public static final int localPort = 10000;
    /** ��������С */
    public static final int bufferLength = 1026;
    /** ���ʹ��ڴ�С */
    public static final int sendWinSize = 10;
    /** ���кŸ��� 20: 0~19 */
    public static final int seqSize = 20;
    /** �յ�ACK��� */
    public final boolean[] ack = new boolean[sendWinSize];

    private DatagramSocket datagramSocket;
    private final InetAddress receiverInetAddress;
    private final int receiverPort;
    private final int timeout = 2000; // ����ack��ʱʱ��
    private int senderPort;

    private List<String> fileFrag;

    private int nextSeq = 0; // ��һ������seq
    private int sendBase = 0; // ��ǰ��ǰ�ȴ�ȷ�ϵ�ack�����������ڶ˵����
    private int baseSeq = 0; // sendBase��ӦSeq
    private int totalsend = 0; // �Ѿ����뷢�ʹ���
    private int totalAck = 0; // ��ȷ�ϳɹ����͵�����
    private int totalPacker; // ��Ҫ���͵İ�����

    private String[] sendWindow = new String[sendWinSize];
    private Timer[] timers = new Timer[sendWinSize];

    /**
     * 
     * @param receiverInetAddress ���շ��˿�
     * @param receiverPort
     * @param localPort ����ack
     * @param filePath
     */
    public Sender(InetAddress receiverInetAddress, int receiverPort, int senderPort,
            String filePath) {
        super();
        this.receiverInetAddress = receiverInetAddress;
        this.receiverPort = receiverPort;
        this.senderPort = senderPort;
        try {
            fileFrag = getFile(filePath);
            totalPacker = fileFrag.size();
        } catch (IOException e) {
            System.err.println("Failed to read file.");
            System.exit(1);
        }
        try {
            datagramSocket = new DatagramSocket(senderPort);
        } catch (SocketException e) {
            System.err.println("Failed to init datagram socket.");
            System.exit(1);
        }

        start();
        while (true) {
            receiveAck(); // ���մ���ack�����ڻ���
        }
    }

    /**
     * @throws URISyntaxException
     * @throws IOException
     * 
     */
    public Sender(InetAddress receiverInetAddress, int receiverPort, String filePath) {
        this(receiverInetAddress, receiverPort, localPort, filePath);
    }


    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Thread#run()
     */
    @Override
    public void run() {
        while (totalAck < totalPacker) {
            // ѭ��ֱ������ȷ��
            // System.out
            // .println("��ȷ��/�ѷ���/������ �� " + totalAck + " / " + totalsend + " / " + totalPacker);
            // System.out.println(Arrays.toString(ack));
            while (sendDatagram()); // �����п��У�������
            // receiveAck(); // ���մ���ack�����ڻ���
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
        System.out.println("�ļ��������...");
        System.exit(0);
    }

    /**
     * ���ͱ��ģ�����nextseq��������ʱ��
     * 
     * @return true�����ͱ���
     */
    private synchronized boolean sendDatagram() {
        if (totalsend == totalPacker) {
            // ���ݱ���ȫ������
            return false;
        }
//        System.out.println("��ȷ��/�ѷ���/������ �� " + totalAck + " / " + totalsend + " / " + totalPacker);
        // System.err.println(nextSeq + " " + sendBase + " " + baseSeq);
        if (nextSeq < baseSeq + sendWinSize) {
            System.out
                    .println("��ȷ��/�ѷ���/������ �� " + totalAck + " / " + totalsend + " / " + totalPacker);

            int winseq = getWinSeq(nextSeq);
            try {
                sendWindow[winseq] = fileFrag.get(totalsend);
                sendData(fileFrag.get(totalsend), (char) nextSeq);
            } catch (IOException e) {
                System.err.println("Failed to send datagram.");
                e.printStackTrace();
            }
            ack[winseq] = false;
            timers[winseq] = new Timer(this, timeout, winseq);
            System.out.println("�ѷ������ݱ� " + nextSeq + " ����������ʱ�� " + winseq);
            // ����nextseq
            nextSeq = (nextSeq + 1) % seqSize;
            totalsend++;
            return true;
        }
        return false;
    }

    /**
     * �����ݰ����к�ת��Ϊ���ʹ������к�
     * 
     * @param seq
     * @return
     */
    private synchronized int getWinSeq(int seq) {
        seq = seq < baseSeq ? seq + seqSize : seq;
        // 6 7 8 9 0 1 2
        // 2 3 0 1 2 3 0 : winsize = 4
        int winseq = (seq + sendBase - baseSeq) % sendWinSize;
        return winseq;
    }

    /**
     * ��ʱ�ش�ָ������֡
     * 
     * @param seq ���к�seq����֡��ʱ
     */
    public void timeoutHandler(int seq) {
        try {
            sendData(sendWindow[seq], (char) seq);
            System.err.println("���ش����ݱ� ��" + seq);
        } catch (IOException e) {
            System.err.println("Failed to resend datagram :" + seq);
            e.printStackTrace();
        }
    }


    /**
     * ����һ�����ݱ�
     * 
     * @param string ����������
     * @param seq ���к�
     * @throws IOException
     */
    private void sendData(String string, char seq) throws IOException {
        byte[] buffer = (seq + string + "0").getBytes();
        DatagramPacket datagramPacket =
                new DatagramPacket(buffer, buffer.length, receiverInetAddress, receiverPort);
        datagramSocket.send(datagramPacket);
    }

    private void receiveAck() {
        byte[] bytes = new byte[bufferLength];
        DatagramPacket datagramPacket = new DatagramPacket(bytes, bytes.length);
        try {
            datagramSocket.receive(datagramPacket);
        } catch (IOException e) {
            System.err.println("Failed to receive ack!");
            e.printStackTrace();
        }
        ackHandler((char) bytes[0]);
    }

    /**
     * �յ�ack��ȷ�ϣ���ֹͣ��ʱ�����ڻ���
     * 
     * @param c ack
     */
    private synchronized void ackHandler(char c) {
        int index = Integer.valueOf(c);
        int terSeq = (baseSeq + sendWinSize - 1) % seqSize; // ���մ���ĩ�˶�Ӧ���к�
        if (!(index >= baseSeq || index <= terSeq)) {
            // ���ڽ��մ�����
            return;
        }
        System.out.println("Receive a ack of : " + index);
        int winseq = getWinSeq(index);
        System.out.println(winseq);

        ack[winseq] = true; // ȷ�ϸñ���
        timers[winseq].interrupt();// ֹͣ��ʱ
        totalAck++;

        // ���ڻ���
        while (ack[sendBase] == true) {
            ack[sendBase] = false;
            sendBase = (sendBase + 1) % sendWinSize;
            baseSeq = (baseSeq + 1) % seqSize;
        }

    }


    /**
     * ���ļ��л���ֽ���
     * 
     * @param filePath
     * @return
     * @throws IOException
     */
    private List<String> getFile(String filePath) throws IOException {
        List<String> list = new ArrayList<>();
        InputStream inputStream = new FileInputStream(new File(filePath));
        byte[] bytes = new byte[bufferLength - 2];
        int length;
        while ((length = inputStream.read(bytes)) != -1) {
            list.add(new String(bytes));
            bytes = new byte[bufferLength - 2];
        }
        list.add("EOF"); // ��ʶ����
        inputStream.close();
        return list;
    }

    /**
     * @param args
     * @throws URISyntaxException
     * @throws IOException
     * @throws UnknownHostException
     */
    public static void main(String[] args)
            throws UnknownHostException, IOException, URISyntaxException {
        new Sender(InetAddress.getByName("127.0.0.1"), 20000, "senderfile.txt");

    }

}
