import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * 
 */

/**
 * @author chen
 *
 */
public class Server {

    /**
     * @param args
     */
    public static void main(String[] args) {
        // �����������̣߳����͵� 20000 �˿ڣ�ͨ��10000�˿ڽ���ack
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    new Sender(InetAddress.getByName("127.0.0.1"), 20000,10000, "senderfile.txt");
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        
        // �����������̣߳�������20001�˿ڣ�����ack��10001
        new Thread(new Runnable() {
            @Override
            public void run() {
                new Receiver(10001,20001, "ServerReceive.txt");
            }
        }).start();

    }

}
