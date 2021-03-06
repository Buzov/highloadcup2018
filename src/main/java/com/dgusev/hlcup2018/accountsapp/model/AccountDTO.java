package com.dgusev.hlcup2018.accountsapp.model;

public class AccountDTO {

    public int id = -1;
    public byte[] email;
    public String fname;
    public String sname;
    public byte[] phone;
    public String sex;
    public int birth = Integer.MIN_VALUE;
    public String country;
    public String city;
    public int joined = Integer.MIN_VALUE;
    public String status;
    public String[] interests;
    public int premiumStart;
    public int premiumFinish;
    public long[] likes;

}
