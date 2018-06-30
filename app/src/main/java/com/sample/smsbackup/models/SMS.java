package com.sample.smsbackup.models;

public class SMS {

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    private String address;
    private String body;
    private String date;
    private String type;

    @Override
    public boolean equals(Object obj) {
        return obj instanceof SMS && Long.valueOf(address) + Long.valueOf(date) ==
                Long.valueOf(((SMS)obj).address) + Long.valueOf(((SMS)obj).date);
    }
}
