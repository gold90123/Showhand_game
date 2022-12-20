import java.io.*;
import java.net.*;
import java.util.*;
public class showhand_server {
    // 初始化玩家，為每個玩家取名
    int player_numbers = 0;
    public void initPlayer(String names)
    {
        // 檢查玩家數量
        player_numbers++;
        if(player_numbers >= 5){
            //
        }
    }

    // 開啟伺服器，使用 TCP 來連接
    private static ServerSocket SSocket;
    private static int port;
    private Hashtable ht = new Hashtable();
    Socket socket;

    public showhand_server() throws IOException
    {
        try {
            SSocket = new ServerSocket(port);
            System.out.println("Server created.");
            System.out.println("waiting for client to connect...");

            while(true) {
                socket = SSocket.accept();
                System.out.println("connected from Client " +
                        socket.getInetAddress().getHostAddress());

                DataOutputStream outstream = new DataOutputStream(socket.getOutputStream());
                ht.put(socket, outstream);
                Thread thread = new Thread(new ServerThread(socket, ht));
                thread.start();
            }
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        //if (args.length < 1) {
        //   System.out.println("Usage: java Server25 [port]");
        //   System.exit(1);
        //}

        //port=Integer.parseInt(args[0]) ;
        port = 1235;
        showhand_server ServerStart = new showhand_server();
    }
}


class ServerThread extends Thread implements Runnable {
    private Socket socket;
    private Hashtable ht;

    public ServerThread(Socket socket, Hashtable ht) {
        this.socket = socket;
        this.ht = ht;
    }

    public void run() {
        DataInputStream instream;

        try {
            instream = new DataInputStream(socket.getInputStream());

            while (true) {
                // 定義最多遊玩人數
                final int PLAY_NUM = 5;
                // 定義入場費
                final int Entrance_fee = 200;
                // 定義撲克牌的花色
                String[] suits = {"d", "c", "h", "s"}; // 方塊、梅花、紅心、黑桃
                // 定義撲克牌的點數
                // 11 = J, 12 = Q, 13 = K, 14 = A
                String[] values = {"2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14"};
                // 遊戲初始金額 (初始化)
                Integer[] Initial_amount = new Integer[PLAY_NUM];
                // 每次的下注值 (初始化)
                Integer[] bet_amount = new Integer[PLAY_NUM];
                // cards 是一局遊戲中剩下的撲克牌 (初始化)
                List<String> cards = new LinkedList<String>();
                // 定義所有的玩家 (初始化)
                String[] players = new String[PLAY_NUM];
                // 所有玩家手上的撲克牌
                List<String>[] playersCards = new List[PLAY_NUM];
                // 初始化牌組，發牌一次就是發一張，如果是第一輪，就發送兩次
                // 首先放入 52 張撲克牌
                for(int i = 0; i < suits.length; i++){
                    for(int j = 0; j < values.length; j++){
                        cards.add(suits[i] + values[j]);
                    }
                }
                // 隨機排列
                Collections.shuffle(cards);
                synchronized (ht) {
                    for (Enumeration e = ht.elements(); e.hasMoreElements(); ) {
                        // 傳輸問題訊問 id
                        String ask_id = "Hello, please input your id in below.";
                        DataOutputStream outstream = (DataOutputStream)e.nextElement();
                        outstream.writeUTF(ask_id);
                        System.out.println("已傳送問題(詢問 id)");
                        // 回收他們回傳的 id
                        String id = instream.readUTF();
                        System.out.println("id: " + id);
                        // 發送給他們撲克牌
                        String send_poker = cards.get(0);
                        // 發送第一張牌，也就是底牌
                        outstream.writeUTF(send_poker);
                        System.out.println("send_poker: " + send_poker);
                        send_poker = cards.get(1);
                        // 發送第二章牌，也就是第一張明牌
                        outstream.writeUTF(send_poker);
                        System.out.println("send_poker: " + send_poker);
                    }
                }
            }
        }
        catch (IOException ex) {
        }
        finally {
            synchronized (ht) {
                System.out.println("Remove connection: " + socket);

                ht.remove(socket);

                try {
                    socket.close();
                } catch (IOException ex) {
                }
            }
        }
    }
}

