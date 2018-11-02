import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
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
public class Sender extends Thread {
	/** ��������С */
	public static final int bufferLength = 1026;
	/** ���ʹ��ڴ�С */
	public static final int sendWinSize = 10;
	/** ���кŸ��� 20: 0~19 */
	public static final int seqSize = 20;
	/** �յ�ACK��� */
	public final boolean[] ack = new boolean[seqSize];

	private DatagramSocket datagramSocket;
	private final InetAddress inetAddress;
	private final int port;
	private final int localPort = 12340;

	private final String testFile; // �����ļ�·��
	private final List<String> fileFrag;

	private int nextSeq; // ��ǰ���ݰ�seq
	private int sendBase; // ��ǰ�ȴ�ȷ�ϵ�ack
	private int totalSeq = 0; // �յ��İ�������
	private int totalPacker; // ��Ҫ���͵İ�����

	/**
	 * @throws URISyntaxException
	 * @throws IOException
	 * 
	 */
	public Sender(InetAddress inetAddress, int port, String testfile) throws IOException, URISyntaxException {
		this.inetAddress = inetAddress;
		this.port = port;
		testFile = testfile;
		fileFrag = getFile(testfile);
		totalPacker = fileFrag.size();
		
		System.out.println(fileFrag.size());
		
		for (String string : fileFrag) {
			System.out.println("--------------------");
			System.out.println(string);
		}

		datagramSocket = new DatagramSocket(localPort);
	}

	/**
	 * ���ļ��л���ֽ���
	 * 
	 * @param filePath
	 * @return
	 * @throws IOException
	 */
	private List<String> getFile(String filePath) throws IOException {
		List<String> list = new ArrayList<>();
		InputStream inputStream = new FileInputStream(new File(filePath));
		byte[] bytes = new byte[bufferLength - 2];
		int length;
		while ((length = inputStream.read(bytes)) != -1) {
			list.add(new String(bytes));
		}
		return list;
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
	 * 
	 * @param i ���к�i����֡��ʱ
	 */
	private void timeOutHandler(int i) {

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
			// �ӵ�ǰ��ȷ�ϵ���ack����ȷ��
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

	private void sendData(String data) throws IOException {
		byte[] buffer = ((char) nextSeq + data).getBytes();
		DatagramPacket datagramPacket = new DatagramPacket(buffer, buffer.length, inetAddress, port);
		datagramSocket.send(datagramPacket);

	}

	private void receiveAck() throws IOException {
		byte[] bytes = new byte[4096];
		DatagramPacket datagramPacket = new DatagramPacket(bytes, bytes.length);
		datagramSocket.receive(datagramPacket);
		ackHandler((char) bytes[0]);
	}

	private int incNextSeq() {
		if (nextSeq + 1 > seqSize - 1) {
			nextSeq = 0;
			return nextSeq = 0;
		}
		return ++nextSeq;
	}

	/**
	 * @param args
	 * @throws URISyntaxException 
	 * @throws IOException 
	 * @throws UnknownHostException 
	 */
	public static void main(String[] args) throws UnknownHostException, IOException, URISyntaxException {
		new Sender(InetAddress.getByName("127.0.0.1"), 12340, "senderfile.txt");
		
		
		
	}

}
