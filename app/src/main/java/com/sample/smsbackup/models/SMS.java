package com.sample.smsbackup.models;

public class SMS {

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

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

    private long id;
    private String address;
    private String body;
    private String date;
    private String type;

    @Override
    public boolean equals(Object obj) {
        return obj instanceof SMS && id == ((SMS) obj).id;
    }

}
