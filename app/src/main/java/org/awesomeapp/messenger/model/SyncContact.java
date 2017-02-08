package org.awesomeapp.messenger.model;

import com.google.gson.annotations.SerializedName;

/**
 * Created by Shyamal.Upadhyaya on 28/01/17.
 */

public class SyncContact {
    @SerializedName("nickName")
    public String nickName;

    @SerializedName("address")
    public String address;

    @SerializedName("userName")
    public String userName;

    public SyncContact() {

    }

    public SyncContact(String nickName, String userName, String address) {
        this.nickName = nickName;
        this.userName = userName;
        this.address = address;
    }

}
