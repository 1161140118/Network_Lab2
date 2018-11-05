import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

/**
 * @author chen
 *
 */
public class Receiver extends Thread {
    public static final int localPort = 20000;

    public static final int bufferLength = 1026;
    public static final int seqSize = 20;
    public static final int rcvWinSize = 10;

    private double timeoutPro = 0.1;
    
    private InetAddress senderInetAddress;
    private int senderPort = 10000;

    private PrintWriter writer;
    private DatagramSocket datagramSocket;
    private DatagramPacket datagramPacket;

    private String[] rcvWindow = new String[rcvWinSize];
    private boolean[] rcvAck = new boolean[rcvWinSize];
    private int rcvBase = 0;
    private int baseSeq = 0; // 接收窗口内base对应的Seq

    /**
     * 
     * @param senderPort 发送方端口，返回ack端口
     * @param receiverPort 接收方端口，接受报文
     * @param filePath
     */
    public Receiver(int senderPort,int receiverPort, String filePath) {
        this.senderPort = senderPort;
        try {
            senderInetAddress = InetAddress.getByName("127.0.0.1");
            datagramSocket = new DatagramSocket(receiverPort);
            writer = new PrintWriter(new FileOutputStream(new File(filePath),false));
        } catch (SocketException e) {
            System.err.println("Failed to init receiver datagram socket.");
            e.printStackTrace();
            System.exit(1);
        } catch (UnknownHostException e) {
            System.err.println("Failed to set InetAddress.");
            e.printStackTrace();
            System.exit(1);
        } catch (FileNotFoundException e) {
            System.err.println("Failed to init file.");
            e.printStackTrace();
            System.exit(1);
        }
        
        start();
    }

    public Receiver(String filePath) {
        this(10000,localPort, filePath);
    }

    /*
     * (non-Javadoc)s
     * 
     * @see java.lang.Thread#run()
     */
    @Override
    public void run() {
        while (true) {
            String string = getAndAck();
            if (string==null) {
                continue;
            }
            if (string.contains("EOF")) {
                // 文件结束
                writer.write(string.substring(0, string.indexOf("EOF")));
                break;
            }
            writer.write(string);
            writer.flush();
        }
        System.out.println("已获得完整文件...");

    }

    private String getAndAck() {
        byte[] buffer = new byte[bufferLength];
        datagramPacket = new DatagramPacket(buffer, buffer.length);

        try {
            datagramSocket.receive(datagramPacket);
            
            if (Math.random()<timeoutPro) {
                // 随机数模拟超时
                System.err.println("模拟超时："+buffer[0]);
                return null;
            }
            
            System.out.println("收到数据报 "+buffer[0]+" : "+new String(buffer, 1, 10));
            datagramSocket.send(new DatagramPacket(((char) buffer[0] + "0").getBytes(), 2,
                    senderInetAddress, senderPort)); // 返回ACK
            String string = new String(buffer, 1, bufferLength - 2);
            return submit(buffer[0], string);
        } catch (IOException e) {
            System.err.println("Failed to receive datagram packet.");
        }
        return null;
    }

    /**
     * 接受窗口滑动，组装提交正确顺序报文
     * @param bseq
     * @param string
     * @return
     */
    private String submit(byte bseq, String string) {
        int seq = (int) bseq;
        int terSeq = (baseSeq + rcvWinSize -1)%seqSize; // 接收窗口末端对应序列号
        if (!(seq>=baseSeq || seq<=terSeq)) {
            //不在接收窗口内
            return null;
        }
        int winseq = getWinSeq(seq);
        if (rcvAck[winseq]==true) {
            //冗余
            return null;
        }
        
        rcvAck[winseq] = true;
        rcvWindow[winseq] = string;
        
        String part = "";

        // 接收窗口滑动
        while (rcvAck[rcvBase] == true) {
            part += rcvWindow[winseq];
            rcvAck[rcvBase] = false;
            rcvBase = (rcvBase + 1) % rcvWinSize;
            baseSeq = (baseSeq + 1) % seqSize;
        }
        return part;
    }

    /**
     * 将数据包序列号转化为发送窗口序列号
     * 
     * @param seq
     * @return
     */
    private int getWinSeq(int seq) {
        seq = seq < baseSeq ? seq + seqSize : seq;
        // 6 7 8 9 0 1 2
        // 2 3 0 1 2 3 0 : winsize = 4
        int winseq = (seq + rcvBase - baseSeq) % rcvWinSize;
        return winseq;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        new Receiver("receiverfile.txt");
    }

}
