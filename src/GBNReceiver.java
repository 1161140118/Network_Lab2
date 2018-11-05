
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

/**
 * @author chen
 *
 */
public class GBNReceiver {
    private int port = 8080;
    private DatagramSocket datagramSocket;
    private DatagramPacket datagramPacket;
    private int exceptedSeq = 1;

    public GBNReceiver(int port) throws IOException {
        this.port = port;
        try {
            datagramSocket = new DatagramSocket(port);
            while (true) {
                byte[] receivedData = new byte[1024];
                datagramPacket = new DatagramPacket(receivedData, receivedData.length);
                datagramSocket.receive(datagramPacket);
                // 收到的数据
                System.out.println(receivedData[0]+new String(receivedData, 1, receivedData.length-1));
                if ( receivedData[0] == exceptedSeq) {
                    // 发送ack
                    sendAck(exceptedSeq);
                    // 期待值加1
                    exceptedSeq++;
                    System.out.println("下一个序列号应该为>>" + exceptedSeq);
//                    System.out.println();
                } else {
                    System.err.println("丢包！下一个序列号应该为>>" + exceptedSeq);
                    // 重发ack
                    sendAck(exceptedSeq-1 );
                    System.out.println();
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }


    // 返回ack
    public void sendAck(int ack) throws IOException {
        String response = (char)ack+"<<<返回ack";
        byte[] responseData = response.getBytes();
        InetAddress responseAddress = datagramPacket.getAddress();
        int responsePort = datagramPacket.getPort();
        datagramPacket = new DatagramPacket(responseData, responseData.length, responseAddress,
                responsePort);
        datagramSocket.send(datagramPacket);
    }



    public static final void main(String[] args) throws IOException {
        new GBNReceiver(8080);
    }

}
