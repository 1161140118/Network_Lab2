import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * 
 */

/**
 * @author chen
 *
 */
public class Client {

    /**
     * @param args
     */
    public static void main(String[] args) {
        // �ͻ��˽����̣߳�����20000�˿ڣ�����ack��10000
        // new Thread(new Runnable() {
        // @Override
        // public void run() {
        // new Receiver(10000, 20000, "ClientReceive.txt");
        // }
        // }).start();
        //
        // // �ͻ��˷����̣߳����͵�20001�˿ڣ�ͨ��10001�˿ڽ���ack
        // new Thread(new Runnable() {
        // @Override
        // public void run() {
        // try {
        // new Sender(InetAddress.getByName("127.0.0.1"), 20001, 10001,"senderfile.txt");
        // } catch (UnknownHostException e) {
        // e.printStackTrace();
        // }
        // }
        // }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    new GBNReceiver(20000);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    new GBNSender(10000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();

    }

}
