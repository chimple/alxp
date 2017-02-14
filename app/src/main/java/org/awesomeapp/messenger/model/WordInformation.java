package org.awesomeapp.messenger.model;

/**
 * Created by Shyamal.Upadhyaya on 09/02/17.
 */

public class WordInformation {

    private String name;
    private String meaning;
    private String imageUrl;
    private String spName;
    private String spMeaning;

    public WordInformation() {

    }

    public WordInformation(String name, String meaning, String imageUrl, String spName, String spMeaning) {
        this.name = name;
        this.meaning = meaning;
        this.imageUrl = imageUrl;
        this.spName = spName;
        this.spMeaning = spMeaning;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMeaning() {
        return meaning;
    }

    public void setMeaning(String meaning) {
        this.meaning = meaning;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getSpName() {
        return spName;
    }

    public void setSpName(String spName) {
        this.spName = spName;
    }

    public String getSpMeaning() {
        return spMeaning;
    }

    public void setSpMeaning(String spMeaning) {
        this.spMeaning = spMeaning;
    }
}
