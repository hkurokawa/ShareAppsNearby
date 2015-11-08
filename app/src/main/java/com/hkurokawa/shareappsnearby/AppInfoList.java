package com.hkurokawa.shareappsnearby;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;

class AppInfoList extends ArrayList<AppInfo> implements Serializable {
    public byte[] toBytes() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            try {
                oos.writeObject(this);
            } finally {
                oos.close();
            }
        } finally {
            baos.close();
        }
        return baos.toByteArray();
    }

    public static AppInfoList fromBytes(byte[] content) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bais = new ByteArrayInputStream(content);
        try {
            ObjectInputStream ois = new ObjectInputStream(bais);
            try {
                return (AppInfoList) ois.readObject();
            } finally {
                ois.close();
            }
        } finally {
            bais.close();
        }
    }
}
