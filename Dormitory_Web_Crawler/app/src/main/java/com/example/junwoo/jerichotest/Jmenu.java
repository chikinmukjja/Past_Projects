package com.example.junwoo.jerichotest;

/**
 * Created by junwoo on 2015-12-27.
 */
public class Jmenu {

    private String day = null;
    private int date = 0;
    private String breakfast = null;
    private String lunch = null;
    private String dinner = null;
    private int Status = 0;

    public void InsertStatus(int _code)
    {
        // 1 vaild data
        // 0 inital
        // -1 adnormal

        Status = _code;
    }

    public int getStatus()
    {
        return this.Status;
    }
    public String getDay()
    {
        return this.day;
    }
    public int getDate()
    {
        return this.date;
    }
    public String getBreakfast()
    {
        return this.breakfast;
    }
    public String getLunch()
    {
        return this.lunch;
    }
    public String getDinner()
    {
        return this.dinner;
    }

    public boolean InsertDay(String _day)
    {
        if(_day != null) {
            this.day = _day;
            return true;
        }
        else return false;
    }
    public boolean InsertDate(String _date)
    {
        if(_date != null) {
            this.date = Integer.parseInt(_date);
            return true;
        }
        else return false;

    }

    public boolean InsertBreakfast(String _breakfast)
    {
        if(_breakfast != null) {
            this.breakfast = _breakfast;
            return true;
        }
        else return false;

    }

    public boolean Insertlunch(String _lunch)
    {
        if(_lunch != null) {
            this.lunch = _lunch;
            return true;
        }
        else return false;

    }

    public boolean InsertDinner(String _dinner)
    {
        if(_dinner != null) {
            this.dinner = _dinner;
            return true;
        }
        else return false;

    }
}
