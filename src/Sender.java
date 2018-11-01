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
    /** 缓冲区大小 */
    public static final int bufferLength = 1026;
    /** 发送窗口大小 */
    public static final int sendWinSize = 10;
    /** 序列号个数 20: 0~19 */
    public static final int seqSize = 20;
    /** 收到ACK情况 */
    public final boolean[] ack = new boolean[seqSize];
    
    private DatagramSocket datagramSocket ;
    private final InetAddress inetAddress;
    private final int port;
    private final int localPort = 12340;

    private int nextSeq; // 当前数据包seq
    private int sendBase; // 当前等待确认的ack
    private int totalSeq; // 收到的包的总数
    private int totalPacker; // 需要发送的包总数
    
    
    
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
     * 当前序列号 curSeq 是否可用
     * 
     * @return true：可用
     */
    private boolean seqIsAvailable() {
        int step = nextSeq - sendBase;
        step = step >= 0 ? step : step + seqSize;
        if (step >= sendWinSize) {
            // 序列号在发送窗口内
            return false;
        }
        if (ack[nextSeq]) {
            // 序列号对应窗口已确认
            return true;
        }
        return false;
    }

    /**
     * 超时重传：超时时，滑动窗口内的数据帧都要重传
     */
    private void timeOutHandler() {
  
        
    }

    /**
     * 累计确认： 收到ACK，取数据帧的第一个字节
     * 
     * @param c
     */
    private void ackHandler(char c) {
        char index = (char) ((int) c - 1);
        System.out.println("Receive a ack of " + index);

        if (sendBase <= index) {
            //从当前已确认到新ack，均确认
            for (int i = sendBase; i <= index; i++) {
                ack[i] = true;
            }
            sendBase = (index + 1) % seqSize;
        } else {
            // ack 超过最大值，回到 curAck 左边
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
        // 初始化套接字
        
        
        


    }

}
