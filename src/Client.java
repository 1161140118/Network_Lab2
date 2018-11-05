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
        // 客户端接收线程，接收20000端口，返回ack给10000
        // new Thread(new Runnable() {
        // @Override
        // public void run() {
        // new Receiver(10000, 20000, "ClientReceive.txt");
        // }
        // }).start();
        //
        // // 客户端发送线程，发送到20001端口，通过10001端口接受ack
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
