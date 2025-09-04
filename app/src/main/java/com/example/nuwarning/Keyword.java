package com.example.nuwarning;

import java.io.Serializable;
import java.util.UUID;

public class Keyword implements Serializable {
    private String name;
    private boolean isChecked;
    private String id;

    public Keyword(String name) {
        this.name = name;
        this.isChecked = true;
        this.id = UUID.randomUUID().toString();  // 一意のIDを生成
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isChecked() {
        return isChecked;
    }

    public void setChecked(boolean checked) {
        isChecked = checked;
    }

    public String getId() {
        return id;
    }
}
