
package peergos.tests;

import static org.junit.Assert.*;

import peergos.crypto.*;
import peergos.corenode.*;
import peergos.user.UserContext;
import peergos.util.ArrayOps;
import peergos.util.ByteArrayWrapper;

import org.junit.Test;

import java.util.*;
import java.net.*;

public class CoreNode
{
    private static Random random = new Random(666);

    public void coreNodeTests(AbstractCoreNode coreNode) throws Exception {
        UserContext context = null;
        try {
            User user = User.random();
            String username = "USER";
            context = new UserContext(username, user, null, coreNode);
            assertTrue("Checking for username", !context.isRegistered());
            assertTrue("added user", context.register());

            //
            //add sharing key to this user
            //
            User follower = User.random();
            assertTrue("follow user", context.addSharingKey(follower.getKey()));

            //
            //retrieve sharing key
            //
            boolean retrievedSharingKey = false;
            Iterator<UserPublicKey> sharingKeys = coreNode.getSharingKeys(username);
            while (sharingKeys.hasNext()) {
                UserPublicKey next = sharingKeys.next();
                if (Arrays.equals(next.getKey().getEncoded(), follower.getKey().getEncoded()))
                    retrievedSharingKey = true;
            }
            assertTrue("retrieved sharing key ", retrievedSharingKey);

            //generate some test data
            //
            byte[] fragmentData = new byte[500];
            random.nextBytes(fragmentData);
            byte[] cipherText = follower.encryptMessageFor(fragmentData);

            //
            //add fragment
            //
            byte[] mapKey = new byte[10];
            boolean addedFragment = coreNode.addMetadataBlob(username, follower.getPublicKey(), mapKey, cipherText, follower.hashAndSignMessage(cipherText));
            assertTrue("added fragment", !addedFragment);

            // add storage allowance

            int frags = 10;
            for (int i = 0; i < frags; i++) {
                byte[] signature = follower.hashAndSignMessage(ArrayOps.concat(follower.getPublicKey(), new byte[10 + i]));
                coreNode.registerFragmentStorage(username, new InetSocketAddress("localhost", 666), username, follower.getPublicKey(), new byte[10 + i], signature);
            }
            long quota = coreNode.getQuota(username);
            assertTrue("quota after registering fragment", quota == coreNode.fragmentLength() * frags);

            // try again adding fragment
            addedFragment = coreNode.addMetadataBlob(username, follower.getPublicKey(), mapKey, cipherText, follower.hashAndSignMessage(cipherText));
            assertTrue("added fragment", addedFragment);



            //
            //add fragment hashes
            //

            byte[] generatedHashes = new byte[UserPublicKey.HASH_BYTES *10];
            random.nextBytes(generatedHashes);
            byte[] signedHash = follower.hashAndSignMessage(ArrayOps.concat(mapKey, cipherText, generatedHashes)); 
            boolean addedHashes = coreNode.addFragmentHashes(username, follower.getPublicKey(), mapKey, cipherText, ArrayOps.split(generatedHashes, UserPublicKey.HASH_BYTES), signedHash);
            assertTrue("added hashes to metadatablob", addedHashes);


            //
            // is fragment allowed?
            //
            byte[] queryHash = Arrays.copyOfRange(generatedHashes,0, UserPublicKey.HASH_BYTES);
            boolean isFragmentAllowed = coreNode.isFragmentAllowed(username, follower.getPublicKey(), mapKey, queryHash);
            assertTrue("fragment is allowed ", isFragmentAllowed);

            // non valid hash
            queryHash = Arrays.copyOfRange(generatedHashes,3, UserPublicKey.HASH_BYTES +3);
            isFragmentAllowed = coreNode.isFragmentAllowed(username, follower.getPublicKey(), mapKey, queryHash);

            assertFalse("fragment is not allowed ", isFragmentAllowed);

            //
            // retrieve metadata blob with hashes
            //
            AbstractCoreNode.MetadataBlob blob = coreNode.getMetadataBlob(username, follower.getPublicKey(), mapKey);
            assertTrue("retrieved blob equality", new ByteArrayWrapper(cipherText).equals(blob.metadata()));
            assertTrue("retrieved blob hashes equality", Arrays.equals(generatedHashes, blob.fragmentHashes()));

            //
            //create a friend
            //
            User friend = User.random();
            String friendname = "FRIEND";
            UserContext friendContext = new UserContext(friendname, friend, null, coreNode);

            //
            //add to coreNode
            //
            assertTrue("added friend", friendContext.register());
        } finally {
            context.shutdown();
        }
    }

    /*
       @Test public void abstractTest() throws Throwable 
       {
       try
       {
       AbstractCoreNode coreNode = AbstractCoreNode.getDefault()();
       coreNodeTests(coreNode);
       } catch (Throwable t) { 
       t.printStackTrace();
       throw t;
       }
       }
       */

    @Test public void httpTest() throws Exception
    {
        try
        {
            HTTPCoreNodeServer server = null;
            try
            {
                AbstractCoreNode mockCoreNode = AbstractCoreNode.getDefault();
                InetAddress address = InetAddress.getByName("localhost");

                server = new HTTPCoreNodeServer(mockCoreNode,address, AbstractCoreNode.PORT);
                server.start();

                URL url = new URL("http://localhost:"+AbstractCoreNode.PORT+"/");
                HTTPCoreNode clientCoreNode = new HTTPCoreNode(url);

                coreNodeTests(clientCoreNode);
            } finally {
                if (server != null)
                    server.close();    
            }
        } catch (Throwable t) {
            t.printStackTrace();
            throw t;
        }
    }

    /*
       @Test public void sqlTest()
       {
       Random random = new Random(666);

       SQLiteCoreNode coreNode = null;

       String dbPath = "corenode_test.db";
       File f = new File(dbPath);
       f.delete();
       try
       {
       coreNode = new SQLiteCoreNode(dbPath);
       coreNodeTests(coreNode);
       } catch (Throwable t){
       t.printStackTrace();
       fail();
       } finally {
       coreNode.close();
       f.delete();
       }

       }
       */



}