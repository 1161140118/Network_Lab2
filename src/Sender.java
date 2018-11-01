import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.sql.Date;
import java.sql.Time;
import java.util.ArrayList;
import java.util.List;
import javax.xml.crypto.Data;
import org.omg.CORBA.Current;

/**
 * @author chen
 *
 */
public class Sender {
    /** ��������С */
    public static final int bufferLength = 1026;
    /** ���ʹ��ڴ�С */
    public static final int sendWinSize = 10;
    /** ���кŸ��� 20: 0~19 */
    public static final int seqSize = 20;
    /** �յ�ACK��� */
    public final boolean[] ack = new boolean[seqSize];
    
    private DatagramSocket datagramSocket ;
    private final InetAddress inetAddress;
    private final int port;
    private final int localPort = 12340;

    private int nextSeq; // ��ǰ���ݰ�seq
    private int sendBase; // ��ǰ�ȴ�ȷ�ϵ�ack
    private int totalSeq; // �յ��İ�������
    private int totalPacker; // ��Ҫ���͵İ�����
    
    
    
    /**
     * @throws SocketException 
     * 
     */
    public Sender(InetAddress inetAddress,int port) throws SocketException {
        this.inetAddress = inetAddress;
        this.port = port;
        datagramSocket = new DatagramSocket(localPort);
    }
    
    /**
     * ��ǰ���к� curSeq �Ƿ����
     * 
     * @return true������
     */
    private boolean seqIsAvailable() {
        int step = nextSeq - sendBase;
        step = step >= 0 ? step : step + seqSize;
        if (step >= sendWinSize) {
            // ���к��ڷ��ʹ�����
            return false;
        }
        if (ack[nextSeq]) {
            // ���кŶ�Ӧ������ȷ��
            return true;
        }
        return false;
    }

    /**
     * ��ʱ�ش�����ʱʱ�����������ڵ�����֡��Ҫ�ش�
     */
    private void timeOutHandler() {
  
        
    }

    /**
     * �ۼ�ȷ�ϣ� �յ�ACK��ȡ����֡�ĵ�һ���ֽ�
     * 
     * @param c
     */
    private void ackHandler(char c) {
        char index = (char) ((int) c - 1);
        System.out.println("Receive a ack of " + index);

        if (sendBase <= index) {
            //�ӵ�ǰ��ȷ�ϵ���ack����ȷ��
            for (int i = sendBase; i <= index; i++) {
                ack[i] = true;
            }
            sendBase = (index + 1) % seqSize;
        } else {
            // ack �������ֵ���ص� curAck ���
            for (int i = sendBase; i < seqSize; i++) {
                ack[i] = true;
            }
            for (int i = 0; i <= index; i++) {
                ack[i] = true;
            }
            sendBase = index + 1;
        }
    }

    private void sendData(String data) {
         byte[] buffer = ( (char)nextSeq + data).getBytes();
         DatagramPacket datagramPacket = new DatagramPacket(buffer, buffer.length);
         
         
    }

    private int incNextSeq() {
        if (nextSeq+1 > seqSize-1) {
            nextSeq = 0;
            return nextSeq = 0;
        }
        return ++nextSeq;
    }
    
    /**
     * @param args
     */
    public static void main(String[] args) {
        // ��ʼ���׽���
        
        
        


    }

}
