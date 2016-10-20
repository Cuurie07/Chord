package edu.buffalo.cse.cse486586.simpledht;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;
import java.util.LinkedList;

import android.app.Activity;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.database.sqlite.SQLiteDatabase;
import android.content.ContentUris;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.TextView;
import java.util.*;
import java.lang.Object;
import android.database.MatrixCursor;



public class SimpleDhtProvider extends ContentProvider {

    private DBHelper db;

    static String ports;


    String trial;

    private static final String TAG = SimpleDhtProvider.class.getName();

    private static final String PROVIDER_NAME = "edu.buffalo.cse.cse486586.simpledht.provider";
    private static final String BASE_PATH = "MyTable";
    static final String URL = "content://" + PROVIDER_NAME + "/" + BASE_PATH;
    static final Uri CONTENT_URI = Uri.parse(URL);


    private SQLiteDatabase database;
    private static final String DATABASE_NAME = "PA3";

    private static final String JOIN = "join";

    String nodeid;

    Cursor c = null;

    static final String REMOTE_PORT0 = "11108";
    static final String REMOTE_PORT1 = "11112";
    static final String REMOTE_PORT2 = "11116";
    static final String REMOTE_PORT3 = "11120";
    static final String REMOTE_PORT4 = "11124";
    static final int SERVER_PORT = 10000;

    private static final String KEY_FIELD = "key";
    private static final String VALUE_FIELD = "value";

    static LinkedList<ListNode> l = new LinkedList<ListNode>();

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // TODO Auto-generated method stub

        SQLiteDatabase sqlDB = db.getWritableDatabase();
        int rowsDeleted = 0;
        String star= "*";
        String at="@";
        if(selection.equalsIgnoreCase(star) || selection.equalsIgnoreCase(at)) {
            sqlDB.execSQL("DELETE FROM " + dbTable.TABLE_NAME);

        }
        else
        {
            sqlDB.execSQL("DELETE FROM " + dbTable.TABLE_NAME + " WHERE key=" + "'" + selection + "'");
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return rowsDeleted;
    }

    @Override
    public String getType(Uri uri) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        // TODO Auto-generated method stub
        String mykey;
        String hashport;
        String hashpred;
        SQLiteDatabase sqlDB = db.getWritableDatabase();
        Log.e(TAG, "insert come");
        Log.e(TAG,"insert key:"+values.getAsString("key"));
        String[] argu = {values.getAsString("key")};
        int back=0;

        if((l.size()==1)||(l.isEmpty())){
            Log.e(TAG,"entered insert for 1");
            Cursor c1 = sqlDB.query(dbTable.TABLE_NAME,
                    null,
                    "key=?",
                    argu,
                    null,
                    null,
                    null);
            if (c1.getCount() < 1) {
                sqlDB.insert(dbTable.TABLE_NAME, null, values);
            } else {
                sqlDB.update(dbTable.TABLE_NAME, values, "key=?", argu);
            }

            Uri _uri = ContentUris.withAppendedId(CONTENT_URI, c1.getCount());
            getContext().getContentResolver().notifyChange(_uri, null);
            return _uri;
        }

        for (int i = 0; i < l.size(); i++) {
            Log.e(TAG, "insert: for"+l.get(i).getVal());
            try {
                mykey = genHash(values.getAsString("key"));
                hashport = genHash(l.get(i).getVal());
                String p = l.get(i).getPred();
                int temp = Integer.parseInt(p) / 2;
                String pred = String.valueOf(temp);
                hashpred = genHash(pred);
                Log.e(TAG,"comp1:"+mykey.compareToIgnoreCase(hashpred));
                Log.e(TAG,"comp2"+mykey.compareToIgnoreCase(hashport));
                if ((mykey.compareToIgnoreCase(hashpred) > 0) && (mykey.compareToIgnoreCase(hashport) <= 0)) {
                    back = 1;
                    Log.e(TAG,"key"+values.getAsString("key")+ "belongs with me:" + l.get(i).getVal());
                    insertfunc(values.getAsString("key"), values.getAsString("value"), l.get(i).getVal());
                    break;
                }


            } catch (NoSuchAlgorithmException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }
           if (back == 0) {
                Log.e(TAG,"did not match anyone");
               insertfunc(values.getAsString("key"), values.getAsString("value"), l.get(0).getVal());}

        return null;
    }

    private void insertfunc(String key, String value,String port){

        int remotePort=Integer.parseInt(port)*2;
        Log.e(TAG,"came to insertfunc");
        Log.e(TAG, "port insert:" + port);

            try {

                Socket insertsocket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                        remotePort);

                StringBuffer msgbuff = new StringBuffer();
                msgbuff.append("just");
                msgbuff.append(",");
                msgbuff.append("fine");
                msgbuff.append(",");
                msgbuff.append("insert");
                msgbuff.append(",");
                msgbuff.append(key);
                msgbuff.append(",");
                msgbuff.append(value);

                String msgToSend = msgbuff.toString();

                PrintWriter pw = new PrintWriter(insertsocket.getOutputStream(), true);
                pw.print(msgToSend);
                pw.flush();
                pw.close();
                insertsocket.close();
            } catch (UnknownHostException e) {
                Log.e(TAG, "ClientTask UnknownHostException");
            } catch (IOException e) {
                Log.e(TAG, "ClientTask socket IOException");
            }



    }

    @Override
    public boolean onCreate() {
        // TODO Auto-generated method stub
        db = new DBHelper(getContext());
        TelephonyManager tel = (TelephonyManager) this.getContext().getSystemService(
                Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));
        ports=myPort;
        Log.e(TAG, "port:" + portStr);

        try {
            nodeid = genHash(portStr);
        } catch (NoSuchAlgorithmException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        try {
            /*
             * Create a server socket as well as a thread (AsyncTask) that listens on the server
             * port.
             *
             * AsyncTask is a simplified thread construct that Android provides. Please make sure
             * you know how it works by reading
             * http://developer.android.com/reference/android/os/AsyncTask.html */

            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            /*
             * Log is a good way to debug your code. LogCat prints out all the messages that
             * Log class writes.
             *
             * Please read http://developer.android.com/tools/debugging/debugging-projects.html
             * and http://developer.android.com/tools/debugging/debugging-log.html
             * for more information on debugging.
             */
            Log.e(TAG, "Can't create a ServerSocket");
        }

        new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, myPort, portStr, JOIN, nodeid);
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        // TODO Auto-generated method stub

        c=null;
        SQLiteDatabase data = db.getWritableDatabase();
        String[] args = {selection};
        String star = "*";
        String at = "@";
        Log.e(TAG, "query called");
        Log.e(TAG, "query for:" + selection);
        Log.e(TAG, "myports val:" + ports);

        if (selection.equalsIgnoreCase(at)) {
            Log.e(TAG, "selection at");
            c = data.query(dbTable.TABLE_NAME, // a. table
                    null, // b. column names to return
                    null, // c. selections "where clause"
                    null, // d. selections args "where values"
                    null, // e. group by
                    null, // f. having
                    null, // g. order by
                    null); // h. limit
            return c;
        } else if (selection.equalsIgnoreCase(star) && (l.isEmpty() || l.size() == 1)) {
            c = data.query(dbTable.TABLE_NAME, // a. table
                    null, // b. column names to return
                    null, // c. selections "where clause"
                    null, // d. selections args "where values"
                    null, // e. group by
                    null, // f. having
                    null, // g. order by
                    null); // h. limit
            return c;

        } else if (selection.equalsIgnoreCase(star) && l.size() > 1) {
            Log.e(TAG, "selection star for 5");
            MatrixCursor mc1 = new MatrixCursor(new String[] {"key", "value"});
            for(int i=0;i< l.size();i++) {
                try {
                    String remoteP=l.get(i).getVal();
                    int remotePort1=Integer.parseInt(remoteP)*2;
                    Socket starsocket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            remotePort1);

                    StringBuffer msgbuff1 = new StringBuffer();
                    msgbuff1.append("just");
                    msgbuff1.append(",");
                    msgbuff1.append("fine");
                    msgbuff1.append(",");
                    msgbuff1.append("squery");
                    msgbuff1.append(",");
                    msgbuff1.append(ports);


                    String msgforstar = msgbuff1.toString();

                    Log.e(TAG, "starquery msgtosend:" + msgforstar);


                    PrintWriter pw1 = new PrintWriter(starsocket.getOutputStream(), true);
                    pw1.println(msgforstar);

                    BufferedReader in1=new BufferedReader(new InputStreamReader(starsocket.getInputStream()));
                    String smsg=in1.readLine();
                    Log.e(TAG,"msg in starquery:"+smsg);
                    if(smsg.equalsIgnoreCase("novalue")){
                            continue;
                    }
                    else{
                        Log.e(TAG,"star cursor message:"+smsg);
                        String[] totarr=smsg.split("/");
                        String[] keyarr=totarr[0].split(",");
                        String[] valarr=totarr[1].split(",");
                        for(int j=0;j<valarr.length;j++){
                            mc1.addRow(new String[]{keyarr[j], valarr[j]});
                        }

                    }

                } catch (UnknownHostException e) {
                    Log.e(TAG, "ClientTask UnknownHostException");
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(TAG, "ClientTask socket IOException");
                }
            }
            return mc1;
        } else {
            Log.e(TAG, "entered else");
            c = data.query(dbTable.TABLE_NAME,
                    null,
                    "key=?",
                    args,
                    null,
                    null,
                    null);
            if (c.getCount() > 0) {
                return c;
            } else {

                Log.e(TAG, "came to queryfunc");

                for (int j=0;j < l.size();j++) {
                    try {

                        String remoteP=l.get(j).getVal();
                        int remotePort=Integer.parseInt(remoteP)*2;
                        Socket starsocket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                remotePort);

                        StringBuffer msgbuff = new StringBuffer();
                        msgbuff.append("just");
                        msgbuff.append(",");
                        msgbuff.append("fine");
                        msgbuff.append(",");
                        msgbuff.append("query");
                        msgbuff.append(",");
                        msgbuff.append(uri.toString());
                        msgbuff.append(",");
                        msgbuff.append(selection);
                        msgbuff.append(",");
                        msgbuff.append(ports);


                        String msgToSend = msgbuff.toString();

                        Log.e(TAG, "query msgtosend:" + msgToSend);


                        PrintWriter pw = new PrintWriter(starsocket.getOutputStream(), true);
                        pw.println(msgToSend);

                        BufferedReader in=new BufferedReader(new InputStreamReader(starsocket.getInputStream()));
                        String msg=in.readLine();
                        Log.e(TAG,"msg in query:"+msg);
                        if(msg.equalsIgnoreCase("novalue")){
                            continue;
                        }
                        else{
                        Log.e(TAG,"cursor message:"+msg);
                            String[] strarr=msg.split(",");
                            MatrixCursor mc = new MatrixCursor(new String[] {"key", "value"});
                            mc.addRow(new String[]{strarr[0], strarr[1]});
                            return mc;
                        }

                    } catch (UnknownHostException e) {
                        Log.e(TAG, "ClientTask UnknownHostException");
                    } catch (IOException e) {
                        Log.e(TAG, "ClientTask socket IOException");
                    }
                }

            }

        }
        return null;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // TODO Auto-generated method stub
        return 0;
    }

    private Cursor starqueryfunc(Uri uri,String port){

        int remotePort=11104;
        Log.e(TAG,"came to starfunc");
        Log.e(TAG,"uri:"+uri.toString());
        int i = 0;

        while(i<5) {
            try {
                remotePort=remotePort+4;
                Socket starsocket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                        remotePort);

                i++;

                StringBuffer msgbuff = new StringBuffer();
                msgbuff.append("just");
                msgbuff.append(",");
                msgbuff.append("fine");
                msgbuff.append(",");
                msgbuff.append("squery");
                msgbuff.append(",");
                msgbuff.append(uri.toString());
                msgbuff.append(",");
                msgbuff.append(port);

                String msgToSend = msgbuff.toString();

                Log.e(TAG,"msgtosend star:"+msgToSend);

                PrintWriter pw = new PrintWriter(starsocket.getOutputStream(), true);
                pw.print(msgToSend);
                pw.flush();
                pw.close();
                starsocket.close();
            } catch (UnknownHostException e) {
                Log.e(TAG, "ClientTask UnknownHostException");
            } catch (IOException e) {
                Log.e(TAG, "ClientTask socket IOException");
            }

        }
        return null;//how to return actual cursor val

    }

    private String genHash(String input) throws NoSuchAlgorithmException {
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        byte[] sha1Hash = sha1.digest(input.getBytes());
        Formatter formatter = new Formatter();
        for (byte b : sha1Hash) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }



    class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        @Override
        protected Void doInBackground(ServerSocket... sockets) {

            ServerSocket serverSocket = sockets[0];
            Log.e(TAG, "ServerTask created at" + sockets[0].toString());

            try {
                while (true) {
                    Socket socket = serverSocket.accept();
                    Log.e(TAG,"ACCEPTED");
                    try {
                        BufferedReader in = new BufferedReader(
                                new InputStreamReader(socket.getInputStream()));
                        //while (true) {
                            String input = in.readLine();

                        String strRecieved = input.trim();
                        String[] strsplit = strRecieved.split(",");
                        Log.e(TAG, "inside update:" + strRecieved);
                        if(strsplit[2].equalsIgnoreCase("cursor")){
                            trial=strsplit[1];
                            Log.e(TAG,"cursor trial::"+trial);
                        }
                        else if(strsplit[2].equalsIgnoreCase("query")){
                            Log.e(TAG,"query called from server for:"+strsplit[4]);
                            String key=strsplit[4];
                            SQLiteDatabase data = db.getWritableDatabase();

                            String selection = "key="+"'"+key+"'";
                             Cursor c1 = data.query(dbTable.TABLE_NAME,
                                     null,
                                     selection,
                                     null,
                                     null,
                                     null,
                                     null);
                            PrintWriter pw=new PrintWriter(socket.getOutputStream(),true);
                            if(c1!=null && c1.getCount()>0){
                                Log.e(TAG,"found the key");

                                c1.moveToFirst();
                                StringBuffer msgbuff=new StringBuffer();

                                while (!c1.isAfterLast()) {
                                    String k = c1.getString(0);
                                    msgbuff.append(k);
                                    Log.e(TAG, "keyin while: " + k);
                                    msgbuff.append(",");
                                    String value = c1.getString(1);
                                    msgbuff.append(value);
                                    Log.e(TAG, "valuein while: " +value);
                                    c1.moveToNext();
                                }

                                String msgTosend=msgbuff.toString();
                                Log.e(TAG,"msgtosend in query:"+msgTosend);
                                pw.println(msgTosend);

                            }
                            else{

                                String msgTosend="novalue";
                                pw.println(msgTosend);

                            }

                        }
                        else if (strsplit[2].equalsIgnoreCase("squery")){

                            Log.e(TAG,"squery called server");
                            SQLiteDatabase data = db.getWritableDatabase();
                            Cursor c2 = data.query(dbTable.TABLE_NAME, // a. table
                                    null, // b. column names to return
                                    null, // c. selections "where clause"
                                    null, // d. selections args "where values"
                                    null, // e. group by
                                    null, // f. having
                                    null, // g. order by
                                    null); // h. limit

                            PrintWriter pw=new PrintWriter(socket.getOutputStream(),true);
                            if(c2!=null && c2.getCount()>0){
                                Log.e(TAG,"found the key");

                                c2.moveToFirst();
                                StringBuffer keybuff=new StringBuffer();
                                StringBuffer valbuff=new StringBuffer();

                                while (!c2.isAfterLast()) {
                                    String k = c2.getString(0);
                                    keybuff.append(k);
                                    Log.e(TAG, "keyin while: " + k);
                                    keybuff.append(",");
                                    String value = c2.getString(1);
                                    valbuff.append(value);
                                    valbuff.append(",");
                                    Log.e(TAG, "valuein while: " +value);
                                    c2.moveToNext();
                                }

                                String keystr=keybuff.toString();
                                String valstr=valbuff.toString();

                                StringBuffer totbuff=new StringBuffer();
                                totbuff.append(keystr);
                                totbuff.append("/");
                                totbuff.append(valstr);

                                String msgTosend=totbuff.toString();
                                Log.e(TAG,"msgtosend in query:"+msgTosend);
                                pw.println(msgTosend);

                            }
                            else{

                                String msgTosend="novalue";
                                pw.println(msgTosend);

                            }


                        }
                        else if(strsplit[2].equalsIgnoreCase("insert")){
                            Log.e(TAG,"insert called server");
                            Log.e(TAG,"key:"+strsplit[3]);
                            Log.e(TAG,"value:"+strsplit[4]);
                            SQLiteDatabase sqlDB = db.getWritableDatabase();
                            String[] argu = {strsplit[3]};
                            ContentValues cv= new ContentValues();
                            cv.put(KEY_FIELD,strsplit[3]);
                            cv.put(VALUE_FIELD, strsplit[4]);
                            Cursor c1 = sqlDB.query(dbTable.TABLE_NAME,null,"key=?",argu,null, null, null);
                            if (c1.getCount() < 1) {sqlDB.insert(dbTable.TABLE_NAME, null, cv);}

                            else {sqlDB.update(dbTable.TABLE_NAME, cv, "key=?", argu);}


                        }
                        else if(strsplit[2].equalsIgnoreCase("list")){
                            Log.e(TAG,"we recieved list");
                            if(l.isEmpty()){
                                Log.e(TAG,"list empty here ");
                                Log.e(TAG,"now ill add");
                                String[] predsplit=strsplit[3].split("/");
                                String[] valsplit=strsplit[4].split("/");
                                String[] succsplit=strsplit[5].split("/");
                                for (int k=0;k<predsplit.length;k++){

                                    ListNode no= new ListNode(predsplit[k],succsplit[k],valsplit[k]);
                                    l.add(no);
                                }
                                Log.e(TAG,"After add");
                                for(int i=0;i<l.size();i++){
                                    Log.e (TAG,"list:"+l.get(i).getVal());
                                }


                            }
                            else{
                                l.clear();
                                String[] predsplit=strsplit[3].split("/");
                                String[] valsplit=strsplit[4].split("/");
                                String[] succsplit=strsplit[5].split("/");
                                for (int k=0;k<predsplit.length;k++){

                                    ListNode no= new ListNode(predsplit[k],succsplit[k],valsplit[k]);
                                    l.add(no);
                                }
                                Log.e(TAG,"After clear n add");
                                for(int i=0;i<l.size();i++){
                                    Log.e(TAG, "list:" + l.get(i).getVal());
                                }

                            }
                        }

                        else if (strsplit[2].equalsIgnoreCase("join")) {
                            if (l.isEmpty()) {

                                Log.e(TAG, "first join");
                                ListNode node = new ListNode(strsplit[0], strsplit[0], strsplit[1]);
                                l.add(node);
                            } else if (l.getLast().getVal() == l.getFirst().getVal()) {

                                Log.e(TAG, "one element join");

                                ListNode obj = l.getFirst();
                                String v = obj.getVal();
                                String id = null;
                                String myid = null;
                                try {
                                    myid = genHash(strsplit[1]);
                                    id = genHash(v);

                                } catch (NoSuchAlgorithmException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                }
                                if (myid.compareToIgnoreCase(id) < 0) {
                                    Log.e(TAG, "one element greater");
                                    int se = Integer.parseInt(obj.getVal()) * 2;
                                    obj.setPred(strsplit[0]);
                                    obj.setSucc(strsplit[0]);
                                    ListNode node = new ListNode(String.valueOf(se), String.valueOf(se), strsplit[1]);
                                    l.addFirst(node);
                                } else {
                                    Log.e(TAG, "one element lesser");
                                    int se = Integer.parseInt(obj.getVal()) * 2;
                                    obj.setSucc(strsplit[0]);
                                    obj.setPred(strsplit[0]);
                                    ListNode node = new ListNode(String.valueOf(se), String.valueOf(se), strsplit[1]);
                                    l.add(node);
                                }
                            } else {

                                Log.e(TAG, "more than two");

                                String id= null;
                                String myid1 = null;
                                int flag=0;

                                for (int i = 0; i < l.size(); i++) {
                                    Log.e(TAG, "entered inner for:" + i);
                                    ListNode obj = l.get(i);
                                    String v = obj.getVal();
                                    try {
                                        myid1 = genHash(strsplit[1]);
                                        id = genHash(v);

                                    } catch (NoSuchAlgorithmException e) {
                                        // TODO Auto-generated catch block
                                        e.printStackTrace();
                                    }
                                    if ((myid1.compareToIgnoreCase(id) < 0)){


                                        Log.e(TAG, "if element less than found");
                                        int p = Integer.parseInt(l.get(i).getPred());
                                        int s = Integer.parseInt(l.get(i).getVal()) * 2;
                                        if(i-1 >= 0 && i+1 < l.size()) {
                                            Log.e(TAG,"in betw less");
                                            l.get(i).setPred(strsplit[0]);
                                            l.get(i - 1).setSucc(strsplit[0]);
                                            ListNode no = new ListNode(String.valueOf(p), String.valueOf(s), strsplit[1]);
                                            l.add(i, no);
                                            flag=1;
                                            break;
                                        }
                                        else if(i-1<0){
                                            Log.e(TAG,"first less");
                                            int s1 = Integer.parseInt(l.get(i).getVal()) * 2;
                                            l.get(i).setPred(strsplit[0]);
                                            ListNode no1 = new ListNode(strsplit[0], String.valueOf(s1), strsplit[1]);
                                            l.addFirst(no1);
                                            flag=1;
                                            break;
                                        }
                                        else if(i+1>=l.size()){
                                            Log.e(TAG, "finally less");
                                            int suc = Integer.parseInt(l.get(i).getVal()) * 2;
                                            ListNode no2 = new ListNode(l.get(i).getPred(), String.valueOf(suc), strsplit[1]);
                                            l.get(i - 1).setSucc(strsplit[0]);
                                            l.get(i).setPred(strsplit[0]);
                                            l.add(i, no2);
                                            flag=1;
                                            break;

                                        }

                                    }
                                }
                                if(flag==0){
                                    Log.e(TAG,"final big add");
                                    int p2 = Integer.parseInt(l.get(l.size()-1).getVal()) * 2;
                                    ListNode no3= new ListNode(String.valueOf(p2),strsplit[0],strsplit[1]);
                                    l.getLast().setSucc(strsplit[0]);
                                    l.addLast(no3);

                                }

                                int fp=Integer.parseInt(l.getLast().getVal())*2;
                                l.get(0).setPred(String.valueOf(fp));
                                int sp=Integer.parseInt(l.get(0).getVal())*2;
                                l.getLast().setSucc(String.valueOf(sp));

                            }

                            for(int i = 0;i<l.size();i++)

                            {
                                Log.e(TAG, "PORT ORDER:" + l.get(i).getVal());
                                Log.e(TAG, "pred:" + l.get(i).getPred());
                                Log.e(TAG, "succ:" + l.get(i).getSucc());
                            }

                              Log.e(TAG,"finished");
                                sendlist(l);
                        }

                    } catch (IOException e) {
                        Log.e(TAG, "Code to receive msg failed");
                    } finally {
                        try {
                            socket.close();
                        } catch (IOException e) {
                            Log.e(TAG, "Couldn't close a socket, what's going on?");
                        }
                   }
                }
            } catch (IOException e) {
                Log.e(TAG, "Code to receive msg failed after publish");
            } //finally {
                //try {
                  //  serverSocket.close();
                //} catch (IOException e) {
                  //  Log.e(TAG, "Couldn't close a serversocket");
               //}
           //}
            /*
             * TODO: Fill in your server code that receives messages and passes them
             * to onProgressUpdate().
             */
            return null;
        }


        protected void onProgressUpdate(String... strings) {
            /*
             * The following code displays what is received in doInBackground().
             */


        }

        private Uri buildUri(String scheme, String authority) {
            Uri.Builder uriBuilder = new Uri.Builder();
            uriBuilder.authority(authority);
            uriBuilder.scheme(scheme);
            return uriBuilder.build();
        }
    }



    private void sendlist(LinkedList<ListNode> li){

        Log.e(TAG,"came to sendlist");

        for(int j=0;j<li.size();j++){

            String remoteP=li.get(j).getVal();
            Log.e(TAG, "sendlist: remoteP: "+remoteP);
            int remotePort=Integer.parseInt(remoteP)*2;
            try {
                if(remotePort!=11108) {
                    Socket sendsocket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            remotePort);

                /*
                 * TODO: Fill in your client code that sends out a message.
                 */
                    StringBuffer msgbuff = new StringBuffer();
                    msgbuff.append("just");
                    msgbuff.append(",");
                    msgbuff.append("fine");
                    msgbuff.append(",");
                    msgbuff.append("list");
                    msgbuff.append(",");

                    StringBuffer predbuff = new StringBuffer();

                    for (int k = 0; k < li.size(); k++) {
                        predbuff.append(li.get(k).getPred());
                        predbuff.append("/");
                    }

                    String pred = predbuff.toString();

                    msgbuff.append(pred);
                    msgbuff.append(",");

                    StringBuffer valbuff = new StringBuffer();

                    for (int k = 0; k < li.size(); k++) {
                        valbuff.append(li.get(k).getVal());
                        valbuff.append("/");
                    }

                    String val = valbuff.toString();

                    msgbuff.append(val);
                    msgbuff.append(",");

                    StringBuffer succbuff = new StringBuffer();

                    for (int k = 0; k < li.size(); k++) {
                        succbuff.append(li.get(k).getSucc());
                        succbuff.append("/");
                    }

                    String succ = succbuff.toString();

                    msgbuff.append(succ);
                    msgbuff.append(",");

                    String msgToSend = msgbuff.toString();

                    PrintWriter pw = new PrintWriter(sendsocket.getOutputStream(), true);
                    pw.println(msgToSend);
                    pw.flush();
                    pw.close();
                    sendsocket.close();

                }
            } catch (UnknownHostException e) {
                Log.e(TAG, "ClientTask UnknownHostException");
            } catch (IOException e) {
                Log.e(TAG, "ClientTask socket IOException");
            }
        }
    }

    /***
     * ClientTask is an AsyncTask that should send a string over the network.
     * It is created by ClientTask.executeOnExecutor() call whenever OnKeyListener.onKey() detects
     * an enter key press event.
     */
    class ClientTask extends AsyncTask<String, Void, Void> {


        @Override
        protected Void doInBackground(String... msgs) {

            Log.e(TAG, "client task created for" + msgs[0]);

            int remotePort = 11108;

            try {
                Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                        remotePort);
                /*
                 * TODO: Fill in your client code that sends out a message.*/

                StringBuffer msgbuff = new StringBuffer();
                msgbuff.append(msgs[0]);
                msgbuff.append(",");
                msgbuff.append(msgs[1]);
                msgbuff.append(",");
                msgbuff.append(msgs[2]);
                msgbuff.append(",");
                msgbuff.append(msgs[3]);
                msgbuff.append(",");
                String msgToSend = msgbuff.toString();
                Log.e(TAG,"msg in client:"+msgToSend);

                PrintWriter pw = new PrintWriter(socket.getOutputStream(), true);
                pw.print(msgToSend);
                pw.flush();

                socket.close();
            } catch (UnknownHostException e) {
                Log.e(TAG, "ClientTask UnknownHostException");
            } catch (IOException e) {
                Log.e(TAG, "ClientTask socket IOException");
            }

            return null;
        }
    }

}


class ListNode {
    public String pred ;
    public String succ;
    public String node;


    public ListNode(String p, String s,String n) {
        this.pred =p;
        this.succ =s;
        this.node=n;
    }

    public String getPred() {
        return pred;
    }

    public String getSucc(){
        return succ;
    }

    public String getVal(){
        return node;
    }

    public void setPred(String p){
        pred=p;
    }

    public void setSucc(String s){
        succ=s;
    }
}




