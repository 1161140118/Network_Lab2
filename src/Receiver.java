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
    private int baseSeq = 0; // ���մ�����base��Ӧ��Seq

    /**
     * 
     * @param senderPort ���ͷ��˿ڣ�����ack�˿�
     * @param receiverPort ���շ��˿ڣ����ܱ���
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
                // �ļ�����
                writer.write(string.substring(0, string.indexOf("EOF")));
                break;
            }
            writer.write(string);
            writer.flush();
        }
        System.out.println("�ѻ�������ļ�...");

    }

    private String getAndAck() {
        byte[] buffer = new byte[bufferLength];
        datagramPacket = new DatagramPacket(buffer, buffer.length);

        try {
            datagramSocket.receive(datagramPacket);
            
            if (Math.random()<timeoutPro) {
                // �����ģ�ⳬʱ
                System.err.println("ģ�ⳬʱ��"+buffer[0]);
                return null;
            }
            
            System.out.println("�յ����ݱ� "+buffer[0]+" : "+new String(buffer, 1, 10));
            datagramSocket.send(new DatagramPacket(((char) buffer[0] + "0").getBytes(), 2,
                    senderInetAddress, senderPort)); // ����ACK
            String string = new String(buffer, 1, bufferLength - 2);
            return submit(buffer[0], string);
        } catch (IOException e) {
            System.err.println("Failed to receive datagram packet.");
        }
        return null;
    }

    /**
     * ���ܴ��ڻ�������װ�ύ��ȷ˳����
     * @param bseq
     * @param string
     * @return
     */
    private String submit(byte bseq, String string) {
        int seq = (int) bseq;
        int terSeq = (baseSeq + rcvWinSize -1)%seqSize; // ���մ���ĩ�˶�Ӧ���к�
        if (!(seq>=baseSeq || seq<=terSeq)) {
            //���ڽ��մ�����
            return null;
        }
        int winseq = getWinSeq(seq);
        if (rcvAck[winseq]==true) {
            //����
            return null;
        }
        
        rcvAck[winseq] = true;
        rcvWindow[winseq] = string;
        
        String part = "";

        // ���մ��ڻ���
        while (rcvAck[rcvBase] == true) {
            part += rcvWindow[winseq];
            rcvAck[rcvBase] = false;
            rcvBase = (rcvBase + 1) % rcvWinSize;
            baseSeq = (baseSeq + 1) % seqSize;
        }
        return part;
    }

    /**
     * �����ݰ����к�ת��Ϊ���ʹ������к�
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
