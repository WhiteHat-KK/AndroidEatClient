package com.techbytecare.kk.androideatclient.Model;

/**
 * Created by kundan on 12/17/2017.
 */

public class User {

    private String name;
    private String Password;
    private String phone;
    private String IsStaff;
    private String secureCode;
    private String homeAddress;
    private Object balance;

    public User() {
    }

    public User(String name, String password, String secureCode) {
        this.name = name;
        Password = password;
        IsStaff = "false";
        this.secureCode = secureCode;
    }

    public Object getBalance() {
        return balance;
    }

    public void setBalance(Object balance) {
        this.balance = balance;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return Password;
    }

    public void setPassword(String password) {
        Password = password;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getIsStaff() {
        return IsStaff;
    }

    public void setIsStaff(String isStaff) {
        IsStaff = isStaff;
    }

    public String getSecureCode() {
        return secureCode;
    }

    public void setSecureCode(String secureCode) {
        this.secureCode = secureCode;
    }

    public String getHomeAddress() {
        return homeAddress;
    }

    public void setHomeAddress(String homeAddress) {
        this.homeAddress = homeAddress;
    }
}
