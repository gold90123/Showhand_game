import java.io.*;
import java.net.*;
import java.util.*;

class Global_cards{
    static List<String> cards = new LinkedList<String>(); // 記住整副撲克牌
    static long bet_sum = 0; // 記住檯面上的總額
    static final Object lock = new Object(); // 一個可以讓 thread 持有的鎖，讓 thread 可以互相等待

    static boolean quick_exit_drop = false; //快速跳到迴圈外結算遊戲
}

class Global_player{
    static List<String> player_name = new LinkedList<String>(); // 記住玩家的名字
}

class client1_cards{
    static List<String> cards = new LinkedList<String>(); // 記住玩家 1 的手牌
    static long score = 0; // 記住玩家 1 的分數
    static int ready = 0; // 記住玩家 1 是否已回傳分數
    static boolean raise = false; // 加注
    static boolean drop = false; // 棄牌
    static boolean showhand = false; // 梭哈
    static long bet = 0; // 玩家 1 下注的金額
    static boolean shuffle_yet_or_not_yet = false; // 洗牌以及發牌了沒有，false = 還沒、true = 洗了

    static long showhand_bet = 0; //在最後一輪時傳送玩家自己的金額

}

class client2_cards{
    static List<String> cards = new LinkedList<String>(); // 記住玩家 2 的手牌
    static long score = 0; // 記住玩家 2 的分數
    static int ready = 0; // 記住玩家 2 是否已回傳分數
    static boolean raise = false; // 加注
    static boolean drop = false; // 棄牌
    static boolean showhand = false; // 梭哈
    static long bet = 0; // 玩家 2 下注的金額
    static boolean shuffle_yet_or_not_yet = false; // 洗牌以及發牌了沒有，false = 還沒、true = 洗了

    static long showhand_bet = 0; //在最後一輪時傳送玩家自己的金額
}

public class showhand_server {
    // 定義撲克牌的花色
    static String[] suits = {"D", "C", "H", "S"}; // 方塊、梅花、紅心、黑桃
    // 定義撲克牌的點數
    // 11 = J, 12 = Q, 13 = K, 14 = A
    static String[] values = {"02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14"};

    // 開啟伺服器，使用 TCP 來連接
    private static ServerSocket SSocket;
    private static int port;
    Socket socket;


    public showhand_server() throws IOException
    {
        // 開啟伺服器前，先洗牌
        // initCards();
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


                // 為該 client 開啟一個 thread
                Thread thread = new Thread(new ServerThread(socket, player));
                thread.start();
            }
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    // 初始化牌組
    public static void initCards(){
        // 先清空整個 linkedList
        Global_cards.cards.clear();
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
}


class ServerThread extends Thread implements Runnable {
    private Socket socket;
    private int player;
    static Object lock = new Object();

    public ServerThread(Socket socket, int player) {
        this.socket = socket;
        this.player = player; // 儲存現在進來的是 player 1 還是 player 2
    }

    public void run() {
        DataOutputStream outstream;
        DataInputStream instream;

        try {
            // 建置輸出串流以及輸入串流
            outstream = new DataOutputStream(socket.getOutputStream());
            instream = new DataInputStream(socket.getInputStream());

            // 進入遊戲環節
            while (true) {
                Global_cards.quick_exit_drop = false;
                client1_cards.drop = false;
                client2_cards.drop = false;
                client1_cards.showhand = false;
                client2_cards.showhand = false;

                // 洗牌，只需要洗一次
                switch (player){
                    case 1:
                        // 要及早把 client2_cards.shuffle_yet_or_not_yet 設成 false
                        client2_cards.shuffle_yet_or_not_yet = false;
                        // 先把 client1_cards.cards 的牌清空
                        client1_cards.cards.clear();
                        // 統一由 client 1 來洗牌，因為每一輪遊戲，只需要洗一次牌
                        showhand_server.initCards();
                        // 從 Global_cards 將牌分配給 client1_cards
                        System.out.print("Player1: ");
                        for(int i = 0; i < 5; i++){
                            client1_cards.cards.add(Global_cards.cards.get(0));
                            Global_cards.cards.remove(0); // 發出去了，就把該牌移除
                            // 顯示玩家的牌
                            System.out.print(client1_cards.cards.get(i) + " ");
                        }
                        client1_cards.shuffle_yet_or_not_yet = true; // 標示已經洗好牌了，放在那麼後面是因為避免跟 client 2 發同一張牌
                        System.out.print("\n");
                        break;
                    case 2:
                        // 先把 client2_cards.cards 的牌清空
                        client2_cards.cards.clear();
                        // 需要等待 client 1 洗完牌才可以發牌給 client 2
                        while(!client1_cards.shuffle_yet_or_not_yet){
                            // 最好加一個 sleep，讓他不要跑太快
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        }
                        // 跳脫 while loop，表示 client1 洗完牌了也發完牌了
                        // 從 Global_cards 將牌分配給 client2_cards
                        System.out.print("Player2: ");
                        for(int i = 0; i < 5; i++){
                            client2_cards.cards.add(Global_cards.cards.get(0));
                            Global_cards.cards.remove(0);
                            // 顯示玩家的牌
                            System.out.print(client2_cards.cards.get(i) + " ");
                        }
                        // 把 client1_cards.shuffle_yet_or_not_yet 歸零
                        client1_cards.shuffle_yet_or_not_yet = false;
                        // 把自己的 shuffle_yet_or_not_yet 設為 true
                        client2_cards.shuffle_yet_or_not_yet = true;
                        // 顯示玩家的牌
                        System.out.print("\n");
                        break;
                    default:
                        System.out.println("Players are too much!!"); // 目前只支持雙人對戰
                }


                //在這邊接收兩個client的資本
                switch (player) {
                    case 1 :
                        client1_cards.showhand_bet = instream.readLong();
                        System.out.println(client1_cards.showhand_bet + " plz " + player);//測試用輸出
                        break;
                    case 2 :
                        client2_cards.showhand_bet = instream.readLong();
                        System.out.println(client2_cards.showhand_bet + " plz" + player);//測試用輸出
                        break;
                    default:System.out.println("FUCK");
                }

                // 詢問 id
                String ask_id = "Hello, please input your id in below.";
                outstream.writeUTF(ask_id);
                System.out.println("已傳送問題(詢問 id)" + player);
                // 回收他們回傳的 id
                String id = instream.readUTF();
                Global_player.player_name.add(id); // 把 id 加到全域的 player_name

                // 使用兩個 linkedList 存牌
                LinkedList my_cards = new LinkedList(); // 負責存自己的明牌
                LinkedList opponent_cards = new LinkedList(); // 負責存對手的明牌

                // 如果 client 2 還沒有發好牌，就停在這裡等他
                while(!client2_cards.shuffle_yet_or_not_yet){
                    // 最好加一個 sleep，讓他不要跑太快
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }

                // client 2 的牌也發好了之後，才可以下來，防止 client 1 衝太快，拿到空的 opponent_cards
                switch (player) {
                    case 1:
                        // 如果是 player 1，自己的牌就拿 client 1 那副牌
                        my_cards = (LinkedList) ((LinkedList) client1_cards.cards).clone();
                        // 代表敵人的牌就拿 client 2 那副牌
                        opponent_cards = (LinkedList) ((LinkedList) client2_cards.cards).clone();
                        break;
                    case 2:
                        // 如果是 player 2，就拿 client 2 那副牌
                        my_cards = (LinkedList) ((LinkedList) client2_cards.cards).clone();
                        // 代表敵人的牌就拿 client 1 那副牌
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


                //把兩家的資本傳送給另一家client(計算showhand最大值用)
                switch (player){
                    case 1 :
                        outstream.writeLong(client2_cards.showhand_bet);
                        break;
                    case 2 :
                        outstream.writeLong(client1_cards.showhand_bet);
                        break;
                    default:
                        System.out.println("???" + player);
                        break;
                }


                // 總共可以下注次數為 4 次
                for (int i = 2; i < 6; i++) {
                    // 接收 client 算好的牌分，並且紀錄好
                    if (Global_cards.quick_exit_drop) break; //後家drop時在這邊抓出去
                    long score = instream.readLong();
                    System.out.println("接收到分數:" + score);
                    switch (player) {
                        case 1:
                            // 第一個進來的 player，就存在 client1_cards 裡
                            client1_cards.score = score; // 儲存分數
                            client1_cards.ready = i; // 代表已經儲存好了
                            System.out.println("client1 is ready");
                            break;
                        case 2:
                            // 第二個進來的 player，就存在 client2_cards 裡
                            client2_cards.score = score; // 儲存分數
                            client2_cards.ready = i; // 代表已經儲存好了
                            System.out.println("client2 is ready");
                            break;
                        default:
                            System.out.println("Players too much"); // 目前只支持兩人對戰
                    }
                    System.out.println("Score: " + score);

                    // 等待另一個線程也跑好
                    synchronized (Global_cards.lock) {
                        if (!(client1_cards.ready == i && client2_cards.ready == i)) {
                            // 如果有其中一個 client 沒有跑好，就進來等待
                            try {
                                Global_cards.lock.wait();
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        } else {
                            // 如果進到這裡，代表兩個 client 都已經 ready
                            Global_cards.lock.notifyAll(); // 使用 notifyAll 喚醒正在 wait 的執行緒
                        }
                    }

                    // 有進到下面來，表示一定是兩個 client 都 ready 了
                    // 比較牌分大小，牌分比較大的可以講話，牌分小的則是要等到對面講完話，自己才可以講
                    // 傳輸訊息給點數較大者，讓他做動作
                    // 傳輸訊息給點數較小者，讓他等待牌分較大者做完動作之後，才可以動作
                    System.out.println("已進到判定式");
                    if (client1_cards.score > client2_cards.score) { // 代表 client1 先說話
                        switch (player) {
                            case 1:
                                outstream.write(1); // Your card is bigger than your opponent, do you want to raise or drop or even showhand.(Please enter your decision)
                                // 等待點數較大的人的回答
                                String decision = instream.readUTF();
                                System.out.println("已接收到 talker 的回答");
                                if (decision.equalsIgnoreCase("raise")) { // 選擇了加注
                                    // 接收他下注的金額
                                    client1_cards.bet = instream.readLong();
                                    Global_cards.bet_sum += client1_cards.bet;
                                    System.out.println("賭金加了 " + client1_cards.bet + ". 現在有: " + Global_cards.bet_sum);
                                    // 確認下注完成
                                    client1_cards.raise = true;
                                } else if (decision.equalsIgnoreCase("drop")) { // 選擇了棄牌
                                    Global_cards.quick_exit_drop = true;//開一個快速break出去的路
                                    client1_cards.drop = true;
                                } else if (decision.equalsIgnoreCase("showhand")) { // 選擇了梭哈
                                    client1_cards.bet = instream.readLong();
                                    Global_cards.bet_sum += client1_cards.bet;
                                    System.out.println("賭金加了 " + client1_cards.bet + ". 現在有: " + Global_cards.bet_sum);
                                    client1_cards.showhand = true;
                                } else {
                                    System.out.println("沒有回答要做甚麼動作，或回答錯誤");
                                }

                                if (Global_cards.quick_exit_drop) break; //如果drop 開一個快速break出去的路(在等回復之前)

                                // 使用 while loop，等待另一位使用者作出決定
                                while (true) {
                                    // 最好加一個 sleep，讓他不要跑太快
                                    try {
                                        Thread.sleep(100);
                                    } catch (InterruptedException e) {
                                        throw new RuntimeException(e);
                                    }
                                    // 判別對手做的動作
                                    if (client2_cards.raise) { // 表示對手選擇了加注
                                        outstream.writeUTF("Your opponent chose to follow you!");
                                        client2_cards.raise = false; // 幫助歸零
                                        break;
                                    } else if (client2_cards.drop) {
                                        outstream.writeUTF("Your opponent chose to drop!");
                                        //client2_cards.drop = false; // 幫助歸零
                                        Global_cards.quick_exit_drop = true;
                                        break;
                                    } else if (client2_cards.showhand) {
                                        outstream.writeUTF("Your opponent chose to showhand!!");
                                        client2_cards.showhand = false; // 幫助歸零
                                        break;
                                    }
                                }
                                break;
                            case 2:
                                outstream.write(0); // "Your card is smaller than your opponent, please wait for his choice..."
                                System.out.println("已叫牌分較小的使用者等待 talker");
                                // 使用 while loop，等待另一位使用者作出決定
                                while (true) {
                                    // 最好加一個 sleep，讓他不要跑太快
                                    try {
                                        Thread.sleep(100);
                                    } catch (InterruptedException e) {
                                        throw new RuntimeException(e);
                                    }
                                    // 判別對手做的動作
                                    if (client1_cards.raise) { // 表示對手選擇了加注
                                        outstream.writeUTF("Your opponent chose to raise " + client1_cards.bet + " dollar! Do you want to follow or drop.(Please enter your decision)");
                                        client1_cards.raise = false; // 幫助歸零
                                        break;
                                    } else if (client1_cards.drop) {
                                        outstream.writeUTF("Your opponent chose to drop!");
                                        //client1_cards.drop = false; // 幫助歸零
                                        Global_cards.quick_exit_drop = true; //開一個快速break出去的路
                                        break;
                                    } else if (client1_cards.showhand) {
                                        outstream.writeUTF("Your opponent chose to showhand!! do you want to follow or drop.(Please enter your decision)");
                                        client1_cards.showhand = false; // 幫助歸零
                                        break;
                                    }
                                }

                                if (Global_cards.quick_exit_drop) break; //開一個快速break出去的路(在等回復之前)

                                // 讀取這個 client 的回答
                                decision = instream.readUTF();
                                if (decision.equalsIgnoreCase("follow")) { // 選擇了加注
                                    // 接收他下注的金額
                                    client2_cards.bet = instream.readLong();
                                    Global_cards.bet_sum += client2_cards.bet;
                                    System.out.println("賭金加了 " + client2_cards.bet + ". 現在有: " + Global_cards.bet_sum);
                                    // 確認下注完成
                                    client2_cards.raise = true;
                                } else if (decision.equalsIgnoreCase("drop")) { // 選擇了棄牌
                                    client2_cards.drop = true;
                                    Global_cards.quick_exit_drop = true;
                                } else if (decision.equalsIgnoreCase("showhand")) { // 選擇了梭哈
                                    client1_cards.bet = instream.readLong();
                                    Global_cards.bet_sum += client1_cards.bet;
                                    System.out.println("賭金加了 " + client1_cards.bet + ". 現在有: " + Global_cards.bet_sum);
                                    client2_cards.showhand = true;
                                } else {
                                    System.out.println("沒有回答要做甚麼動作，或回答錯誤");
                                }
                                break;
                        }
                    } else if (client1_cards.score < client2_cards.score) {
                        switch (player) {
                            case 1:
                                outstream.write(0); // "Your card is smaller than your opponent, please wait for his choice..."
                                // 使用 while loop，等待另一位使用者作出決定
                                while (true) {
                                    // 最好加一個 sleep，讓他不要跑太快
                                    try {
                                        Thread.sleep(100);
                                    } catch (InterruptedException e) {
                                        throw new RuntimeException(e);
                                    }
                                    // 判別對手做的動作
                                    if (client2_cards.raise) { // 表示對手選擇了加注
                                        outstream.writeUTF("Your opponent chose to raise " + client2_cards.bet + " dollar! Do you want to follow or drop.(Please enter your decision)");
                                        client2_cards.raise = false; // 幫助歸零
                                        break;
                                    } else if (client2_cards.drop) {
                                        outstream.writeUTF("Your opponent chose to drop!");
                                        //client2_cards.drop = false; // 幫助歸零
                                        Global_cards.quick_exit_drop = true;
                                        break;
                                    } else if (client2_cards.showhand) {
                                        outstream.writeUTF("Your opponent chose to showhand!! do you want to follow or drop.(Please enter your decision)");
                                        client2_cards.showhand = false; // 幫助歸零
                                        break;
                                    }
                                }
                                if (Global_cards.quick_exit_drop) break;
                                // 讀取他的回答
                                String decision = instream.readUTF();
                                if (decision.equalsIgnoreCase("follow")) { // 選擇了加注
                                    // 接收他下注的金額
                                    client1_cards.bet = instream.readLong();
                                    Global_cards.bet_sum += client1_cards.bet;
                                    System.out.println("賭金加了 " + client1_cards.bet + ". 現在有: " + Global_cards.bet_sum);
                                    // 確認下注完成
                                    client1_cards.raise = true;
                                } else if (decision.equalsIgnoreCase("drop")) { // 選擇了棄牌
                                    Global_cards.quick_exit_drop = true;//開一個快速break出去的路
                                    client1_cards.drop = true;
                                } else if (decision.equalsIgnoreCase("showhand")) { // 選擇了梭哈
                                    client1_cards.showhand = true;
                                } else {
                                    System.out.println("沒有回答要做甚麼動作，或回答錯誤");
                                }
                                break;
                            case 2:
                                outstream.write(1); // Your card is bigger than your opponent, do you want to raise or drop or even showhand.(Please enter your decision)
                                // 等待點數較大的人的回答
                                decision = instream.readUTF();
                                if (decision.equalsIgnoreCase("raise")) { // 選擇了加注
                                    // 接收他下注的金額
                                    client2_cards.bet = instream.readLong();
                                    Global_cards.bet_sum += client2_cards.bet;
                                    System.out.println("賭金加了 " + client2_cards.bet + ". 現在有: " + Global_cards.bet_sum);
                                    // 確認下注完成
                                    client2_cards.raise = true;
                                } else if (decision.equalsIgnoreCase("drop")) { // 選擇了棄牌
                                    Global_cards.quick_exit_drop = true;//開一個快速break出去的路
                                    client2_cards.drop = true;
                                } else if (decision.equalsIgnoreCase("showhand")) { // 選擇了梭哈
                                    client2_cards.bet = instream.readLong();
                                    Global_cards.bet_sum += client2_cards.bet;
                                    System.out.println("賭金加了 " + client2_cards.bet + ". 現在有: " + Global_cards.bet_sum);
                                    client2_cards.showhand = true;
                                } else {
                                    System.out.println("沒有回答要做甚麼動作，或回答錯誤");
                                }

                                if (Global_cards.quick_exit_drop) break; //開一個快速break出去的路(在等回復之前)

                                // 使用 while loop，等待另一位使用者作出決定
                                while (true) {
                                    // 最好加一個 sleep，讓他不要跑太快
                                    try {
                                        Thread.sleep(100);
                                    } catch (InterruptedException e) {
                                        throw new RuntimeException(e);
                                    }
                                    // 判別對手做的動作
                                    if (client1_cards.raise) { // 表示對手選擇了加注
                                        outstream.writeUTF("Your opponent chose to follow you!");
                                        client1_cards.raise = false; // 幫助歸零
                                        break;
                                    } else if (client1_cards.drop) {
                                        outstream.writeUTF("Your opponent chose to drop!");
                                        client1_cards.drop = false; // 幫助歸零
                                        Global_cards.quick_exit_drop = true;
                                        break;
                                    } else if (client1_cards.showhand) {
                                        outstream.writeUTF("Your opponent chose to showhand!!");
                                        client1_cards.showhand = false; // 幫助歸零
                                        break;
                                    }
                                }
                                break;
                        }
                    }

                    if(Global_cards.quick_exit_drop) break;//快速道路(以防萬一)

                    // 最後一圈迴圈不需要發牌，只需要收分數，然後讓 client 下注
                    if (i < 5) {
                        // 直接發牌
                        outstream.writeUTF((String) my_cards.get(i)); // 發送第 i 張牌給自己，也就是第 i + 1 張明牌
                        System.out.println("send player" + player + "'s (" + i + " -card) " + my_cards.get(i));

                        // 發送給 client，對手的明牌
                        outstream.writeUTF((String) opponent_cards.get(i)); // 發送對手的第 i 張牌
                        System.out.println("send opponent's (" + i + " -card): " + opponent_cards.get(i));
                    }
                }//迴圈

                // 接收 client 回傳的總分 (加入底牌之後的分數)
                long score = instream.readLong();


                // 判別現在正在執行的是 client1 還是 client2
                switch (player) {
                    case 1:
                        client1_cards.score = score; // 儲存分數
                        if(client1_cards.drop) {
                            client1_cards.score = 0;
                        }
                        client1_cards.ready = 10; // 代表已經儲存好了
                        System.out.println("client1 is ready(計算最後總分)");
                        break;
                    case 2:
                        client2_cards.score = score; // 儲存分數
                        if(client2_cards.drop) {
                            client2_cards.score = 0;
                        }
                        client2_cards.ready = 10; // 代表已經儲存好了
                        System.out.println("client2 is ready(計算最後總分)");
                        break;
                    default:
                        System.out.println("Players too much"); // 目前只支持兩人對戰
                }
                System.out.println("Score: " + score);

                // 等待另一個線程也跑好
                synchronized (Global_cards.lock){
                    if (!(client1_cards.ready == 10 && client2_cards.ready == 10)) {
                        // 如果有其中一個 client 沒有跑好，就進來等待
                        System.out.println(client1_cards.ready +" "+ client2_cards.ready);
                        try {
                            Global_cards.lock.wait();
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }else{
                        // 如果進到這裡，代表兩個 client 都已經 ready
                        Global_cards.lock.notifyAll(); // 使用 notifyAll 喚醒正在 wait 的執行緒
                    }
                }

                // 比較分數
                if(client1_cards.score > client2_cards.score){
                    switch (player){
                        case 1:
                            outstream.write(1); // You are WINNER!!
                            outstream.writeLong(Global_cards.bet_sum); // 回傳檯面上的所有金額給贏家
                            System.out.println("返還 " + Global_cards.bet_sum + " 金額給贏家");
                            // 賭金已經發還給玩家，將檯面上的賭金清空
                            Global_cards.bet_sum = 0;
                            client1_cards.ready = 11;
                            break;
                        case 2:
                            outstream.write(0); // You lose...
                            client2_cards.ready = 11;
                            break;
                        default:
                            System.out.println("Players too much"); // 目前只支持兩人對戰
                    }
                }
                else if(client1_cards.score < client2_cards.score){
                    switch (player){
                        case 1:
                            outstream.write(0); // You lose...
                            client1_cards.ready = 11;
                            break;
                        case 2:
                            outstream.write(1); // You are WINNER!!
                            outstream.writeLong(Global_cards.bet_sum); // 回傳檯面上的所有金額給贏家
                            System.out.println("返還" + Global_cards.bet_sum + " 金額給贏家");
                            // 賭金已經發還給玩家，將檯面上的賭金清空
                            Global_cards.bet_sum = 0;
                            client2_cards.ready = 11;
                            break;
                        default:
                            System.out.println("Players too much"); // 目前只支持兩人對戰
                    }
                }

                synchronized (Global_cards.lock){
                    if (!(client1_cards.ready == 11 && client2_cards.ready == 11)) {
                        // 如果有其中一個 client 沒有跑好，就進來等待
                        System.out.println(client1_cards.ready +" "+ client2_cards.ready);
                        try {
                            Global_cards.lock.wait();
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }else{
                        // 如果進到這裡，代表兩個 client 都已經 ready
                        Global_cards.lock.notifyAll(); // 使用 notifyAll 喚醒正在 wait 的執行緒
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

