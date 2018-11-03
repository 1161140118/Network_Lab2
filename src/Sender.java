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
    /** 发送方默认端口号 */
    public static final int localPort = 10000;
    /** 缓冲区大小 */
    public static final int bufferLength = 1026;
    /** 发送窗口大小 */
    public static final int sendWinSize = 10;
    /** 序列号个数 20: 0~19 */
    public static final int seqSize = 20;
    /** 收到ACK情况 */
    public final boolean[] ack = new boolean[sendWinSize];

    private DatagramSocket datagramSocket;
    private final InetAddress receiverInetAddress;
    private final int receiverPort;
    private final int timeout = 2000; // 设置ack超时时间
    private int senderPort;

    private List<String> fileFrag;

    private int nextSeq = 0; // 下一个可用seq
    private int sendBase = 0; // 当前最前等待确认的ack，即滑动窗口端点序号
    private int baseSeq = 0; // sendBase对应Seq
    private int totalsend = 0; // 已经进入发送窗口
    private int totalAck = 0; // 已确认成功发送的总数
    private int totalPacker; // 需要发送的包总数

    private String[] sendWindow = new String[sendWinSize];
    private Timer[] timers = new Timer[sendWinSize];

    /**
     * 
     * @param receiverInetAddress 接收方端口
     * @param receiverPort
     * @param localPort 接受ack
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
            receiveAck(); // 接收处理ack，窗口滑动
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
            // 循环直到均已确认
            // System.out
            // .println("已确认/已发送/总任务 ： " + totalAck + " / " + totalsend + " / " + totalPacker);
            // System.out.println(Arrays.toString(ack));
            while (sendDatagram()); // 窗口有空闲，即发送
            // receiveAck(); // 接收处理ack，窗口滑动
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
        System.out.println("文件发送完成...");
        System.exit(0);
    }

    /**
     * 发送报文，更新nextseq，启动计时器
     * 
     * @return true：发送报文
     */
    private synchronized boolean sendDatagram() {
        if (totalsend == totalPacker) {
            // 数据报已全部发送
            return false;
        }
//        System.out.println("已确认/已发送/总任务 ： " + totalAck + " / " + totalsend + " / " + totalPacker);
        // System.err.println(nextSeq + " " + sendBase + " " + baseSeq);
        if (nextSeq < baseSeq + sendWinSize) {
            System.out
                    .println("已确认/已发送/总任务 ： " + totalAck + " / " + totalsend + " / " + totalPacker);

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
            System.out.println("已发送数据报 " + nextSeq + " ，并启动定时器 " + winseq);
            // 更新nextseq
            nextSeq = (nextSeq + 1) % seqSize;
            totalsend++;
            return true;
        }
        return false;
    }

    /**
     * 将数据包序列号转化为发送窗口序列号
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
     * 超时重传指定数据帧
     * 
     * @param seq 序列号seq数据帧超时
     */
    public void timeoutHandler(int seq) {
        try {
            sendData(sendWindow[seq], (char) seq);
            System.err.println("已重传数据报 ：" + seq);
        } catch (IOException e) {
            System.err.println("Failed to resend datagram :" + seq);
            e.printStackTrace();
        }
    }


    /**
     * 发送一个数据报
     * 
     * @param string 待发送数据
     * @param seq 序列号
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
     * 收到ack，确认，并停止计时，窗口滑动
     * 
     * @param c ack
     */
    private synchronized void ackHandler(char c) {
        int index = Integer.valueOf(c);
        int terSeq = (baseSeq + sendWinSize - 1) % seqSize; // 接收窗口末端对应序列号
        if (!(index >= baseSeq || index <= terSeq)) {
            // 不在接收窗口内
            return;
        }
        System.out.println("Receive a ack of : " + index);
        int winseq = getWinSeq(index);
        System.out.println(winseq);

        ack[winseq] = true; // 确认该报文
        timers[winseq].interrupt();// 停止计时
        totalAck++;

        // 窗口滑动
        while (ack[sendBase] == true) {
            ack[sendBase] = false;
            sendBase = (sendBase + 1) % sendWinSize;
            baseSeq = (baseSeq + 1) % seqSize;
        }

    }


    /**
     * 从文件中获得字节流
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
        list.add("EOF"); // 标识结束
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
