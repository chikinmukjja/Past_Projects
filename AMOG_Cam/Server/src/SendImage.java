import static com.googlecode.javacv.cpp.opencv_core.IPL_DEPTH_8U;
import static com.googlecode.javacv.cpp.opencv_core.cvAbsDiff;
import static com.googlecode.javacv.cpp.opencv_core.cvClearMemStorage;
import static com.googlecode.javacv.cpp.opencv_imgproc.CV_GAUSSIAN;
import static com.googlecode.javacv.cpp.opencv_imgproc.CV_RGB2GRAY;
import static com.googlecode.javacv.cpp.opencv_imgproc.CV_THRESH_BINARY;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvCvtColor;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvSmooth;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvThreshold;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

import javax.imageio.ImageIO;

import com.googlecode.javacv.FrameGrabber;
import com.googlecode.javacv.FrameGrabber.Exception;
import com.googlecode.javacv.cpp.opencv_core.CvMemStorage;
import com.googlecode.javacv.cpp.opencv_core.IplImage;

public class SendImage extends Thread{
   private Socket socket               = null;
   private OutputStream      out       = null;
   FrameGrabber grabber        = null;
   private boolean motionDetect 	   = false;
   private boolean connect;
      
   /* sendMsg 생성자, 변수들을 초기화 시켜 준다. */
   public SendImage(Socket _Socket,boolean _connect,FrameGrabber _grabber) throws IOException
   {
      socket        = _Socket;
      out           = socket.getOutputStream();
      connect = _connect;
      grabber = _grabber;
   }
   
   public void connectExit(){
   	connect = false;
   }

 
  
   public void run()
   {
	   /* Set up webcam */
	  // grabber = new OpenCVFrameGrabber(5); 
	   
	 //  grabber.setFrameRate(30);
	 //  grabber.setImageWidth(640);
	  // grabber.setImageHeight(480);


	   System.out.print("Initializing camera");



	 /*  try {
		   grabber.start();
	   } catch (Exception e2) {
		   // TODO Auto-generated catch block
		   e2.printStackTrace();
	   }
	  */
	   
	   System.out.println("done!");

	   IplImage frame;
	   IplImage tmpimage = null;
	   IplImage prevImage = null;
	   IplImage diff = null;
	   int count = 0;                 // motion 인식알람 시작후 주기가  적어도 변화가 없는 횟수가 10이상이여야한다
	   int maxdiff = 0;               // motion 가장 빠른 변화
	   int mediumDelay = 0;           // motion 인식 시작 후 적어도 10 frame은 지나야 알람을 띄울것이다. 

	   while(connect)
	   {

		   try {

			   IplImage image = null;
			try {
				image = grabber.grab();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			   //System.out.println(image.width() +" "+image.height());
			   
			   if(image != null && image.width() > 0 ){
				   /* get Buffered Image */
				   BufferedImage bufferImage = image.getBufferedImage();

				   /* change BufferedImage to Byte Array */
				   ByteArrayOutputStream bScrn = new ByteArrayOutputStream();
				   ImageIO.write(bufferImage, "jpg", bScrn);
				   byte[] imgByte = bScrn.toByteArray();
				   bScrn.flush();
				   bScrn.close();
				   /* get file size */
				   
				   String Header;

				   String fileSize = String.valueOf(imgByte.length);

				   if(motionDetect == false){
					   Header   = "0000000000".substring(0,10-fileSize.length()) + fileSize;
				   }
				   else {
					   Header = "100000000".substring(0,10-fileSize.length()) + fileSize;
					   motionDetect = false;
				   }

				   /* Write File Size */
				   out.write(Header.getBytes());
				   /* Write Image buffer Array */
				   out.write(imgByte, 0, Integer.parseInt(fileSize));

				   frame = image;

				   CvMemStorage storage = CvMemStorage.create();

				   cvClearMemStorage(storage);
				   cvSmooth(frame, frame, CV_GAUSSIAN, 9, 9, 2, 2);

				   if (tmpimage == null) {

					   tmpimage = IplImage.create(frame.width(), frame.height(), IPL_DEPTH_8U, 1);

					   cvCvtColor(frame, tmpimage, CV_RGB2GRAY);

				   } else {

					   prevImage = IplImage.create(frame.width(), frame.height(), IPL_DEPTH_8U, 1);

					   prevImage = tmpimage;

					   tmpimage = IplImage.create(frame.width(), frame.height(), IPL_DEPTH_8U, 1);

					   cvCvtColor(frame, tmpimage, CV_RGB2GRAY);

				   }

				   if (diff == null) {

					   diff = IplImage.create(frame.width(), frame.height(), IPL_DEPTH_8U, 1);

				   }

				   if (prevImage != null) {

					   // perform ABS difference

					   cvAbsDiff(tmpimage, prevImage, diff);

					   // do some threshold for wipe away useless details

					   cvThreshold(diff, diff, 64, 255, CV_THRESH_BINARY);

					   BufferedImage bufferImage1 = diff.getBufferedImage();

					   ByteArrayOutputStream baos = new ByteArrayOutputStream();

					   ImageIO.write(bufferImage1,"jpg",baos);
					   baos.flush();
					   byte[] imageInByte = baos.toByteArray();
					   baos.close();

					   System.out.println(imageInByte.length);

					   if(imageInByte.length > 6000 &count > 10){

						   if(maxdiff < imageInByte.length)maxdiff = imageInByte.length;
						   else if(mediumDelay > 10){
							   //System.out.println("alarm !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! ");
							   motionDetect = true;
							   maxdiff = 0;
							   count = 0;
							   mediumDelay = 0;
						   }
						   mediumDelay += 1;
					   }
					   else if(imageInByte.length < 6000)count +=1;

				   }


			   }

			   /* IMAGE가 NULL이 아닌 경우 client에게 image전달 */
		   } catch ( IOException e1) {}
	   }         

	
	   System.out.println("thread 종료 id "+Thread.currentThread().getId());

   }
   
   /* 객체 소멸자 */
   public void finalize() throws IOException, Exception {
	   if(out != null)
		   out.close();
   }
}