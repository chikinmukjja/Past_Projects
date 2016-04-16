package com.example.junwoo.jerichotest;

import android.bluetooth.BluetoothAdapter;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;


import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.HTMLElementName;
import net.htmlparser.jericho.Source;

import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    String urlNotice = null;
    String urlMenu = null;
    TextView tv;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tv = (TextView)findViewById(R.id.textviewex);


        // main에서 네트워크데이터 처리시 error 발생을 막기위해 추가
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()

                .detectNetwork()
                .penaltyLog().build());



        List<Jnotice> notice = new ArrayList<Jnotice>();
        List<Jmenu> menu = new ArrayList<Jmenu>();


        urlMenu = new String("http://seoul.jbdream.or.kr/bbs/board.php?bo_table=sub02_01");
        urlNotice = new String("http://seoul.jbdream.or.kr/bbs/board.php?bo_table=sub05_01");

        try {
            URL URLNotice = new URL(urlNotice);
            URL URLMenu = new URL(urlMenu);

            //Source sourceNotice = new Source(URLNotice);
            Source sourceMenu = new Source(URLMenu);

            ParseMenu(sourceMenu, menu);
            //ParseNotice(sourceNotice, notice);

            showMenu(menu);
            //showNotice(notice);

        }catch (IOException e) {

        }

    }

    public void showNotice(List<Jnotice> notice)
    {
        tv.setText("공지사항\n");
        tv.append("번호  제목\n");
        tv.append("날짜 작성자 조회수\n");
        tv.append("link\n");

        for(int i =0; i<notice.size();i++)
        {
            if(notice.get(i).getBoardNum() == -1)
                tv.append("\n"+"공지 ");
            else tv.append("\n"+notice.get(i).getBoardNum()+" ");
            tv.append(notice.get(i).getTitle()+"\n ");
            tv.append(notice.get(i).getDate()+" ");
            tv.append(notice.get(i).getUser()+" ");
            tv.append(notice.get(i).getClicks()+"\n");
            tv.append(notice.get(i).getLink()+"\n");
            tv.append(notice.get(i).getLinkContent(notice.get(i).getLink())+"\n");


        }

    }
    public void showMenu(List<Jmenu> menu)
    {
        SimpleDateFormat format = new SimpleDateFormat("dd");
        Date d = new Date();
        int today = Integer.parseInt(format.format(d));

        tv.setText("식단\n");
        //System.out.println(menu.size());

        for(int i =0; i<menu.size();i++)
        {
            //System.out.println(menu.get(i).day);
            if(menu.get(i).getStatus() == 1)
            {
                if(today == menu.get(i).getDate())tv.append("\n"+menu.get(i).getDay()+" "+menu.get(i).getDate()+" <-- today !");
                else tv.append("\n"+menu.get(i).getDay()+" "+menu.get(i).getDate());
                tv.append("\n"+menu.get(i).getBreakfast());
                tv.append("\n"+menu.get(i).getLunch());
                tv.append("\n"+menu.get(i).getDinner()+"\n");
            }

        }
    }

    public void ParseNotice(Source _source,List<Jnotice> arrayList)
    {
        String commonAttr = new String("30");

        List<Element> TrTags = _source.getAllElements(HTMLElementName.TR);

        for(int i= 0;i<TrTags.size();i++)
        {
            Jnotice temp = new Jnotice();
            Element TrElement = TrTags.get(i);

            if(TrElement.getAttributes().toString().contains(commonAttr))
            {
                // tr안에 td( align = center )4개 a 한개

                List<Element> ATags;
                ATags = TrElement.getAllElements(HTMLElementName.A);
                if(ATags.size() == 1){
                    temp.setTitle(ATags.get(0).getTextExtractor().toString());
                    temp.setLink(ATags.get(0).getAttributeValue("href"));
                }

                List<Element> TdTags;
                TdTags = TrElement.getAllElements(HTMLElementName.TD);
                int order = 0;

                for(int j=0;j<TdTags.size();j++) {

                    Element TdElement = TdTags.get(j);
                    if(TdElement.getAttributes().toString().contains("center")) {
                        if(order == 0) {
                            temp.setBoardNum(TdElement.getTextExtractor().toString());
                            System.out.println(order + " " + TdElement.getTextExtractor().toString());
                            order++;
                        }
                        else if(order == 1){

                            temp.setUser(TdElement.getTextExtractor().toString());
                            System.out.println(order + " " + TdElement.getTextExtractor().toString());
                            order++;
                        }
                        else if(order == 2){
                            System.out.println(order +" "+TdElement.getTextExtractor().toString());
                            temp.setDate(TdElement.getTextExtractor().toString());
                            order++;
                        }
                        else if(order == 3){
                            System.out.println(order +" "+TdElement.getTextExtractor().toString());
                            temp.setClicks(TdElement.getTextExtractor().toString());
                            order++;
                        }
                        else if(order == 4)break;
                    }

                }
                arrayList.add(temp);
            }
        }
    }

    public boolean ParseMenu(Source _source,List<Jmenu> arrayList)
    {
        String commonAttr = new String("60");
        List<Element> trTags = _source.getAllElements(HTMLElementName.TR);

        for(int i= 0;i<trTags.size();i++)
        {
            Element trElement = trTags.get(i);

            if(trElement.getAttributes().toString().contains(commonAttr))
            {
               // System.out.println(i+", "+trTags.size());
               // System.out.println(trTags.get(i).getTextExtractor().toString());

                // TR 안의 TD tag parsing
                List<Element> tdTags = trTags.get(i).getAllElements(HTMLElementName.TD);
                Jmenu temp = new Jmenu();
                for(int j = 0;j<tdTags.size();j++)
                {

                    //System.out.println(i+" , "+j+" , " + tdTags.size());
                    Element tdElement = tdTags.get(j);
                    if(tdTags.size() == 5) {

                        if (tdElement.getAttributes().toString().contains("center")) {

                            if (j == 0) {
                                temp.InsertDay(tdElement.getTextExtractor().toString());
                            }
                            if (j == 1) {
                                temp.InsertDate(tdElement.getTextExtractor().toString());
                            }
                        }
                        if (tdElement.getAttributes().toString().contains("c_tc")) {
                            if (j == 2) {
                                temp.InsertBreakfast(tdElement.getTextExtractor().toString());
                            }
                            if (j == 3) {
                                temp.Insertlunch(tdElement.getTextExtractor().toString());
                            }
                            if (j == 4) {
                                temp.InsertDinner(tdElement.getTextExtractor().toString());
                                temp.InsertStatus(1); // vaild data
                                arrayList.add(temp);
                            }
                        }
                    }
                    else
                    {
                        //error -1, since web page tree is changed, renew parsing method
                        temp.InsertStatus(-1);
                    }

                }

            }
        }
        /* for debugging
        for(int i=0;i<7;i++)
        {
            System.out.println(arrayList.get(i).getDate());
        }
        */

        return true;
    }
}

