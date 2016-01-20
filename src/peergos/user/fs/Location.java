package peergos.user.fs;

import peergos.crypto.symmetric.SymmetricKey;
import peergos.crypto.UserPublicKey;
import peergos.util.ByteArrayWrapper;
import peergos.util.Serialize;

import java.io.*;

public class Location
{
    public static final int MAP_KEY_SIZE = 32;
    public final UserPublicKey owner;
    public final UserPublicKey writerKey;
    public final ByteArrayWrapper mapKey;

    public Location(UserPublicKey owner, UserPublicKey subKey, ByteArrayWrapper mapKey) {
        this.owner = owner;
        this.writerKey = subKey;
        this.mapKey = mapKey;
    }

    public void serialise(DataOutput dout) throws IOException {
        Serialize.serialize(owner.serialize(), dout);
        Serialize.serialize(writerKey.serialize(), dout);
        Serialize.serialize(mapKey.data, dout);
    }

    public byte[] encrypt(SymmetricKey key, byte[] iv) {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        try {
            DataOutputStream dout = new DataOutputStream(bout);
            serialise(dout);
        } catch (IOException e) {e.printStackTrace();}
        return key.encrypt(bout.toByteArray(), iv);
    }

    public static Location deserialise(DataInputStream din) throws IOException {
        UserPublicKey owner = UserPublicKey.deserialize(din);
        UserPublicKey pub = UserPublicKey.deserialize(din);
        ByteArrayWrapper mapKey = new ByteArrayWrapper(Serialize.deserializeByteArray(din, MAP_KEY_SIZE));
        return new Location(owner, pub, mapKey);
    }

    public static Location decrypt(SymmetricKey key, byte[] iv, byte[] data) throws IOException {
        byte[] raw = key.decrypt(data, iv);
        ByteArrayInputStream bin = new ByteArrayInputStream(raw);
        DataInputStream din = new DataInputStream(bin);
        return deserialise(din);
    }
}