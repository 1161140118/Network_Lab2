
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
                // �յ�������
                System.out.println(receivedData[0]+new String(receivedData, 1, receivedData.length-1));
                if ( receivedData[0] == exceptedSeq) {
                    // ����ack
                    sendAck(exceptedSeq);
                    // �ڴ�ֵ��1
                    exceptedSeq++;
                    System.out.println("��һ�����к�Ӧ��Ϊ>>" + exceptedSeq);
//                    System.out.println();
                } else {
                    System.err.println("��������һ�����к�Ӧ��Ϊ>>" + exceptedSeq);
                    // �ط�ack
                    sendAck(exceptedSeq-1 );
                    System.out.println();
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }


    // ����ack
    public void sendAck(int ack) throws IOException {
        String response = (char)ack+"<<<����ack";
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
