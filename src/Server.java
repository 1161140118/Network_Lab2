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
        // 服务器发送线程，发送到 20000 端口，通过10000端口接受ack
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
        
        // 服务器接收线程，接收于20001端口，返回ack给10001
        new Thread(new Runnable() {
            @Override
            public void run() {
                new Receiver(10001,20001, "ServerReceive.txt");
            }
        }).start();

    }

}
