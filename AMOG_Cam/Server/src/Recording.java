
import static com.googlecode.javacv.cpp.opencv_highgui.CV_FOURCC;


import com.googlecode.javacv.FrameGrabber;
import com.googlecode.javacv.FrameRecorder;
import com.googlecode.javacv.OpenCVFrameRecorder;
import com.googlecode.javacv.FrameGrabber.Exception;
import com.googlecode.javacv.cpp.opencv_core.IplImage;


public class Recording extends Thread {

   private boolean recorded;
   FrameGrabber grabber;
   private FrameRecorder recorder1;
   
   public Recording(FrameGrabber _grabber)
   {
      grabber = _grabber;
   }
   
   public void setupRecording() {
      

	    String inTime   = new java.text.SimpleDateFormat("HHmmss").format(new java.util.Date());
        inTime += ".avi";
	    recorder1 = new OpenCVFrameRecorder(inTime,320,240);
        recorder1.setCodecID(CV_FOURCC('x','v','i','d'));
        recorder1.setPixelFormat(1);
        
      
   }
   
   public void startRecording() throws com.googlecode.javacv.FrameRecorder.Exception{
      
      if(recorded){
         System.out.println("이미 녹화중 ");
      }
      else {
         this.setupRecording();
         recorded = true;
      }
      
   }
   public void stopRecording(){
      if(recorded){
         recorded = false;
      }
      else System.out.println("녹화 중 아님 ");
      
   }
   
   public void run(){
      //CanvasFrame canvasFrame = new CanvasFrame("녹화중");
      IplImage image =null;
      try {
           
            while(recorded){                    

            	recorder1.start();

            	for(;;){

            		image = grabber.grab();
            		if(image == null) break;

            		recorder1.record(image);

            		//char c = (char) cvWaitKey(15);
            		//if(c == 'q') break;

            		if(recorded == false) break;
            	}

            	recorder1.stop();
            	recorder1.release();

            }
            
         } catch (Exception | com.googlecode.javacv.FrameRecorder.Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
         }
         
   }
}
   
   
   
