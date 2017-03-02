package org.chimple.messenger.model;

import com.google.gson.annotations.SerializedName;

/**
 * Created by Shyamal.Upadhyaya on 28/01/17.
 */

public class SyncContact {
    @SerializedName("nickName")
    private String nickName;

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @SerializedName("address")
    private String address;

    @SerializedName("userName")
    private String userName;

    private String version;

    public SyncContact() {

    }

    public SyncContact(String nickName, String userName, String address, String version) {
        this.nickName = nickName;
        this.userName = userName;
        this.address = address;
        this.version = version;
    }
}
