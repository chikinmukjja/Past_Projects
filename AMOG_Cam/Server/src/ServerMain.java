import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

import com.googlecode.javacv.FrameGrabber;
import com.googlecode.javacv.OpenCVFrameGrabber;

public class ServerMain {
	
	public static void main(String[] args) throws Exception{

		
		
		FrameGrabber grabber = new OpenCVFrameGrabber(5);
		grabber.setFrameRate(24);
		grabber.start();
		boolean check = false;
		Recording record =null;
		//record.start();
		/* 서버 준비 완료 */
		
		
		
		while(true){
			
			ServerSocket serverSocket = new ServerSocket(8111);
			System.out.println("Waiting...");
			/* 클라이언트 연결 완료 */
			Socket socket = serverSocket.accept();
			System.out.println("Connected...");		
			/* message receive Thread 실행 */
			boolean connect = true;
			
			
			SendImage send = new SendImage(socket,connect,grabber);
			send.start();
			
			boolean out = true;
							
			InputStream in = null;
			in = socket.getInputStream();
			DataInputStream din =null;
			
			while(out){

				din = new DataInputStream(in);
				
				String control = null;

				try {
					control = din.readUTF();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					control = new String("null"); // 종료 상태 값
					//e.printStackTrace();
				}
				if(check == false)
					 record = new Recording(grabber);
				// 연결 종료시 -> exception으로 이동후 서버가 다시 연결을 기다림
				if(control.equalsIgnoreCase("start")&& check == false){
					record.startRecording();
					record.start();
					check = true;
					
				}
				else if(control.equalsIgnoreCase("finish")){
					record.stopRecording();
					check = false;
				}
				else {
					System.out.println("Aknown msg : only accept start, finish : "+control);
					if(control.equalsIgnoreCase("null"))out =false;
				}
			

			}
			
			System.out.println("연결 종료");
			send.connectExit();
			send.join();
			
			socket.close();
			serverSocket.close();
			
		}
		
	}
}