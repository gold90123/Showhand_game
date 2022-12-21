import java.io.*;
import java.net.*;
import java.util.*;

class Global_cards{
    static List<String> cards = new LinkedList<String>(); // 記住整副撲克牌
}

class Global_player{
    static List<String> player_name = new LinkedList<String>(); // 記住玩家的名字
}

class client1_cards{
    static List<String> cards = new LinkedList<String>(); // 記住玩家 1 的手牌
    static long score = 0; // 記住玩家 1 的分數
    static boolean ready = false; // 記住玩家 1 是否已回傳分數
    static boolean raise = false;
    static boolean pass = false;
    static boolean drop = false;
    static boolean showhand = false;

}

class client2_cards{
    static List<String> cards = new LinkedList<String>(); // 記住玩家 2 的手牌
    static long score = 0; // 記住玩家 2 的分數
    static boolean ready = false; // 記住玩家 2 是否已回傳分數
    static boolean raise = false;
    static boolean pass = false;
    static boolean drop = false;
    static boolean showhand = false;
}

public class showhand_server {
    // 定義最多遊玩人數
    static final int PLAY_NUM = 2;
    // 定義入場費
    static final int Entrance_fee = 200;
    // 定義撲克牌的花色
    static String[] suits = {"d", "c", "h", "s"}; // 方塊、梅花、紅心、黑桃
    // 定義撲克牌的點數
    // 11 = J, 12 = Q, 13 = K, 14 = A
    static String[] values = {"02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14"};
    // 遊戲初始金額 (初始化)
    static Integer[] Initial_amount = new Integer[PLAY_NUM];
    // 每次的下注值 (初始化)
    static Integer[] bet_amount = new Integer[PLAY_NUM];
    // 定義所有的玩家 (初始化)
    static String[] players = new String[PLAY_NUM];
    // 所有玩家手上的撲克牌
    static List<String>[] playersCards = new List[PLAY_NUM];
    // 開啟伺服器，使用 TCP 來連接
    private static ServerSocket SSocket;
    private static int port;
    private Hashtable ht = new Hashtable();
    Socket socket;

    public showhand_server() throws IOException
    {
        // 開啟伺服器前，先洗牌
        initCards();
        // 開啟 socket，接收 client
        try {
            SSocket = new ServerSocket(port);
            System.out.println("Server created.");
            System.out.println("waiting for client to connect...");
            int player = 0;
            while(true) {
                // 每進來一次代表就有一個 client 連線，計算 client 總數
                player++;

                // 獲取 client 資訊
                socket = SSocket.accept();
                System.out.println("connected from Client " + socket.getInetAddress().getHostAddress());
                DataOutputStream outstream = new DataOutputStream(socket.getOutputStream());

                // 直接分配每個 client 5 張牌
                switch(player){
                    case 1:
                        // 顯示玩家的牌
                        System.out.print("Player1: ");
                        for(int i = 0; i < 5; i++){
                            client1_cards.cards.add(Global_cards.cards.get(0));
                            Global_cards.cards.remove(0);
                            // 顯示玩家的牌
                            System.out.print(client1_cards.cards.get(i));
                        }
                        // 顯示玩家的牌
                        System.out.print("\n");
                        break;
                    case 2:
                        // 顯示玩家的牌
                        System.out.print("Player2: ");
                        for(int i = 0; i < 5; i++){
                            client2_cards.cards.add(Global_cards.cards.get(0));
                            Global_cards.cards.remove(0);
                            // 顯示玩家的牌
                            System.out.print(client2_cards.cards.get(i));
                        }
                        // 顯示玩家的牌
                        System.out.print("\n");
                        break;
                    default:
                        System.out.println("Players are too much!!"); // 目前只支持雙人對戰
                }

                // 把該 client 的資訊放進 HashTable
                ht.put(socket, outstream);
                // 為該 client 開啟一個 thread
                Thread thread = new Thread(new ServerThread(socket, ht, player));
                thread.start();
                System.out.println("player: " + player);
            }
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    // 初始化牌組 (發牌一次就是發一張，如果是第一輪，就發送兩次)
    public void initCards(){
        // 首先放入 52 張撲克牌
        for(int i = 0; i < suits.length; i++){
            for(int j = 0; j < values.length; j++){
                Global_cards.cards.add(suits[i] + values[j]);
            }
        }
        // 隨機排列
        Collections.shuffle(Global_cards.cards);
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

    public static String sort_card(String cards)
    {
        int i;
        String[] card = cards.split(",");
        String sorted = "";
        Arrays.sort(card);
        for(i = 0;i < 5;i++) {
            sorted += card[i] + ",";
            //System.out.println(card[i]);
        }
        return sorted;
    }
    public static char color(String card)
    {
        return card.charAt(0);
    }
    public static int point(String card)
    {
        return Integer.parseInt(card.substring(1, 3));
    }
    public static int color_point(String card)
    {
        switch (color(card)) {
            case 'S' : return 4;
            case 'H' : return 3;
            case 'D' : return 2;
            case 'C' : return 1;
            default : return 0;
        }
    }
    public static long score_counting(String cards)
    {
        //同色同花 兩對一樣的兔胚
        cards = sort_card(cards);
        String[] card = cards.split(",");
        long card_score = 0;
        int i;
        int card_type = 0;


        boolean straight_flush = false; //同花順
        boolean flush = true;
        boolean straight = true;
        int[] num_sort = new int[5];
        for(i=0;i<5;i++) {
            num_sort[i] = point(card[i]);
        }
        Arrays.sort(num_sort);
        /*System.out.println(num_sort);
        for(i=0;i<5;i++) {
            System.out.println(num_sort[i]);
        }*/
        for(i=0;i<4;i++) {
            if(num_sort[i] != num_sort[i+1] - 1) {
                straight = false;
            }
            if(color(card[i]) != color(card[i+1])) {
                flush = false;
            }
        }
        if(straight) {
            card_score += Math.pow(10,5)*num_sort[4];
            for(i = 0;i<5;i++) {
                if(point(card[i]) == num_sort[4]) {
                    card_score += color_point(card[i]);
                }
            }
        }
        if (flush) {
            card_score += Math.pow(10, 6)*num_sort[4];
            card_score+=color_point(card[0]);
        }
        if(straight && flush) {
            straight_flush = true;
            flush = false;
            card_score += Math.pow(10, 9)*point(card[4]);
            card_score += color_point(card[4]);
        }



        boolean four_of_a_kind = false;
        boolean full_house = false;
        boolean three_of_a_kind = false;
        int pair_count = 0;
        List count_repeat = new ArrayList();
        for(i=0;i<5;i++) {
            count_repeat.add(point(card[i]));
        }
        for(i = 2;i<=14;i++)
        {
            //System.out.println(i + " = " + Collections.frequency(count_repeat, i));
            switch (Collections.frequency(count_repeat, i))
            {
                case 4 :
                    four_of_a_kind = true;
                    card_score += Math.pow(10, 8) * point(card[i]);
                    break;
                case 3 :
                    three_of_a_kind = true;
                    card_score += Math.pow(10, 4) * i;
                    break;
                case 2 :
                    pair_count += 1;
                    card_score += 100 * i;
                    //System.out.println(i);
                    if(pair_count == 2) {
                        card_score -= i*100;
                        long tmp = card_score%1000/100;
                        card_score += Math.max(tmp, i)*100;
                        card_score -= tmp*100;
                        //card_score += 2000;
                    }
                    break;
            }
        }
        if(three_of_a_kind == true && pair_count == 1) {
            full_house = true;
            three_of_a_kind = false;
            pair_count -= 1;
            card_score += Math.pow(10, 7)*(card_score/10000);
        }

        if(pair_count >= 0) {
            for(i = 0;i < 5;i++) {
                //System.out.println(color_point(card[i]) + "" + point(card[i]));
                if(point(card[i]) == card_score/100 && color_point(card[i]) == 4) {
                    card_score += 4;
                }
            }
        }

        if(card_score == 0) {
            card_score += color_point(card[4]) + point(card[4]) * 5;
        }


        return card_score;

    }
}


class ServerThread extends Thread implements Runnable {
    private Socket socket;
    private Hashtable ht;
    private int player;

    public ServerThread(Socket socket, Hashtable ht, int player) {
        this.socket = socket;
        this.ht = ht;
        this.player = player;
    }

    public void run() {
        DataOutputStream outstream;
        DataInputStream instream;
        try {
            outstream = new DataOutputStream(socket.getOutputStream());
            instream = new DataInputStream(socket.getInputStream());
            while (true) {
                // 詢問 id
                String ask_id = "Hello, please input your id in below.";
                outstream.writeUTF(ask_id);
                System.out.println("已傳送問題(詢問 id)");
                // 回收他們回傳的 id
                String id = instream.readUTF();
                Global_player.player_name.add(id); // 把 id 加到全域的 player_name

                // 使用兩個 linkedList 存牌
                LinkedList my_cards = new LinkedList(); // 負責存自己的明牌
                LinkedList opponent_cards = new LinkedList(); // 負責存對手的明牌

                // 複製牌型
                switch (player) {
                    case 1:
                        my_cards = (LinkedList) ((LinkedList) client1_cards.cards).clone();
                        opponent_cards = (LinkedList) ((LinkedList) client2_cards.cards).clone();
                        break;
                    case 2:
                        my_cards = (LinkedList) ((LinkedList) client2_cards.cards).clone();
                        opponent_cards = (LinkedList) ((LinkedList) client1_cards.cards).clone();
                        break;
                    default:
                        System.out.println("Players too much!!"); // 目前只支持兩人對戰
                }

                // 發送給 client 第一輪的撲克牌
                outstream.writeUTF((String) my_cards.get(0));  // 發送第一張牌，也就是底牌
                System.out.println("send player" + player + "'s (hole-card): " + my_cards.get(0));
                outstream.writeUTF((String) my_cards.get(1)); // 發送第二張牌給自己，也就是第一張明牌
                System.out.println("send player" + player + "'s (first-card) " + my_cards.get(1));

                // 發送給 client，對手的明牌
                outstream.writeUTF((String) opponent_cards.get(1)); // 發送對手的第一張明牌
                System.out.println("send opponent's (first-card): " + opponent_cards.get(1));

                // 接收 client 算好的牌分，並且紀錄好
                long score = instream.readLong();
                switch (player) {
                    case 1:
                        client1_cards.score = score; // 儲存分數
                        client1_cards.ready = true; // 代表已經儲存好了
                        System.out.println("client1 is ready");
                        break;
                    case 2:
                        client2_cards.score = score; // 儲存分數
                        client2_cards.ready = true; // 代表已經儲存好了
                        System.out.println("client2 is ready");
                        break;
                    default:
                        System.out.println("Players too much"); // 目前只支持兩人對戰
                }
                System.out.println("Score: " + score);


                while (!client1_cards.ready || !client2_cards.ready) {
                    /*System.out.println("client1:" + client1_cards.ready);
                    System.out.println("client2:" + client2_cards.ready);*/
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                // 比較牌分大小，牌分比較大的可以講話，牌分小的則是要等到對面講完話，自己才可以講
                // 傳輸訊息給點數較大者，讓他選擇動作
                // 傳輸訊息給點數較小者，讓他等待
                // System.out.println("無窮迴圈中"); // 加了這行，牌分小的才會收到訊息??
                if (client1_cards.ready && client2_cards.ready) {
                    System.out.println("已進到判定式");
                    if (client1_cards.score > client2_cards.score) { // 代表 client1 先說話
                        switch (player) {
                            case 1:
                                outstream.writeUTF("Your card is bigger than your opponent, do you want to raise or pass or drop or even showhand.(Please enter your decision)");
                                System.out.println("已傳送問題(詢問 talker 下一步動作)");
                                // 等待點數較大的人的回答
                                String decision = instream.readUTF();
                                System.out.println("已接收到 talker 的回答");
                                if (decision.equalsIgnoreCase("raise")) { // 選擇了加注
                                    client1_cards.raise = true;
                                } else if (decision.equalsIgnoreCase("pass")) { // 選擇了過牌
                                    client1_cards.pass = true;
                                } else if (decision.equalsIgnoreCase("drop")) { // 選擇了棄牌
                                    client1_cards.drop = true;
                                } else if (decision.equalsIgnoreCase("showhand")) { // 選擇了梭哈
                                    client1_cards.showhand = true;
                                } else {
                                    System.out.println("沒有回答要做甚麼動作，或回答錯誤");
                                }
                                break;
                            case 2:
                                outstream.writeUTF("Your card is smaller than your opponent, please wait for his choice...");
                                System.out.println("已叫牌分較小的使用者等待 talker");
                                while (true) {
                                    if (client1_cards.raise) { // 表示對手選擇了加注
                                        outstream.writeUTF("Your opponent chose to raise!");
                                        client1_cards.raise = false;
                                        break;
                                    } else if (client1_cards.pass) {
                                        outstream.writeUTF("Your opponent chose to pass!");
                                        client1_cards.pass = false;
                                        break;
                                    } else if (client1_cards.drop) {
                                        outstream.writeUTF("Your opponent chose to drop!");
                                        client1_cards.drop = false;
                                        break;
                                    } else if (client1_cards.showhand) {
                                        outstream.writeUTF("Your opponent chose to showhand!!");
                                        client1_cards.showhand = false;
                                        break;
                                    }
                                }
                                break;
                        }
                    } else if (client2_cards.score > client1_cards.score) {
                        switch (player) {
                            case 1:
                                outstream.writeUTF("Your card is smaller than your opponent, please wait for his choice");
                                while (true) {
                                    if (client2_cards.raise) { // 表示對手選擇了加注
                                        outstream.writeUTF("Your opponent chose to raise!");
                                        client2_cards.raise = false;
                                        break;
                                    } else if (client2_cards.pass) {
                                        outstream.writeUTF("Your opponent chose to pass!");
                                        client2_cards.pass = false;
                                        break;
                                    } else if (client2_cards.drop) {
                                        outstream.writeUTF("Your opponent chose to drop!");
                                        client2_cards.drop = false;
                                        break;
                                    } else if (client2_cards.showhand) {
                                        outstream.writeUTF("Your opponent chose to showhand!!");
                                        client2_cards.showhand = false;
                                        break;
                                    }
                                }
                                break;
                            case 2:
                                outstream.writeUTF("Your card is bigger than your opponent, do you want to raise or pass or drop or even showhand.(Please enter your decision)");
                                // 等待點數較大的人的回答
                                String decision = instream.readUTF();
                                if (decision.equalsIgnoreCase("raise")) { // 選擇了加注
                                    client2_cards.raise = true;
                                } else if (decision.equalsIgnoreCase("pass")) { // 選擇了過牌
                                    client2_cards.pass = true;
                                } else if (decision.equalsIgnoreCase("drop")) { // 選擇了棄牌
                                    client2_cards.drop = true;
                                } else if (decision.equalsIgnoreCase("showhand")) { // 選擇了梭哈
                                    client2_cards.showhand = true;
                                } else {
                                    System.out.println("沒有回答要做甚麼動作，或回答錯誤");
                                }
                                break;
                        }
                    }
                    break; // 表示有收到兩邊 client 的確認，跳出永久 while 迴圈
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

