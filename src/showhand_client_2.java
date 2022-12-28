import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.*;
import java.util.List;

class Client2_OwnCard{
    static List<String> owncardlist = new LinkedList<String>();
    static long my_bet = 20000; // 初始金額
}
class Client2_EnemyCard{
    static List<String> owncardlist = new LinkedList<String>();

    static long enemy_bet = 0;
}

public class showhand_client_2 extends Frame implements Runnable {
    Socket socket;
    static String servername;
    static int port;
    DataOutputStream  outstream;
    DataInputStream  instream;

    public static void main(String args[]) {
        //if (args.length < 2){
        //   System.out.println("USAGE: java work25_User1 [servername] [port]");
        //   System.exit(1);
        //}

        //servername= args[0];

        servername= "localhost";
        //port=Integer.parseInt(args[1]);
        port = 1235;
        showhand_client_2 ClientStart = new showhand_client_2();
    }

    public showhand_client_2()
    {
        super("showhand_client_2");
        try{
            socket = new Socket(InetAddress.getByName(servername),port);
            outstream = new DataOutputStream(socket.getOutputStream());
            instream = new DataInputStream(socket.getInputStream());

            this.outstream = new DataOutputStream(outstream);
            this.instream = new DataInputStream(instream);

            new Thread(this).start();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void run()
    {
        while(true){
            try{
                // 把自己跟敵人的牌庫都先清空
                Client2_OwnCard.owncardlist.clear();
                Client2_EnemyCard.owncardlist.clear();

                String card = ""; // 所有收到的牌都會先是這個
                String User_input = ""; // 使用者輸入操作
                String answer = "";

                outstream.writeLong(Client2_OwnCard.my_bet); //回合一開始就傳自己的本金(server 199行)

                // 接收問題，並回傳 id
                Scanner inputReader = new Scanner(System.in); // 創建 scanner
                String question = instream.readUTF(); // 接收伺服器的問題
                System.out.println(question); // 印出問題
                User_input = inputReader.next(); // 讀取使用者的回答
                outstream.writeUTF(User_input); // 把使用者的回答回傳給伺服器

                // 接收第一張(底牌 hole-card)
                card = instream.readUTF();
                Client2_OwnCard.owncardlist.add(card); // 加進專門存放自己的牌組的 list
                // 接收第二張(第一張明牌)
                card = instream.readUTF();
                Client2_OwnCard.owncardlist.add(card); //加進專門存放自己的牌組的 list

                // 接收對手的第一張明牌
                card = instream.readUTF();
                Client2_EnemyCard.owncardlist.add(card);

                // 顯示自己以及對手的牌
                System.out.println("Your hole-card is: " + Client2_OwnCard.owncardlist.get(0));
                System.out.println("Your card 1 is: " + Client2_OwnCard.owncardlist.get(1));
                System.out.println("Your opponent's card 1 is: " + card);

                // 算牌分，回傳給 server
                // 底牌分數要最後算
                String card_string = "000,000,000,000,000";
                card_string = card_string.replaceFirst("000", Client2_OwnCard.owncardlist.get(1));
                System.out.println("Your cards: " + card_string);
                long score = score_counting(card_string);
                System.out.println("score is: " + score);
                outstream.writeLong(score);

                Client2_EnemyCard.enemy_bet = instream.readLong(); //收對手的原始賭金(server 261行)

                for(int card_count = 2; card_count < 6; card_count++)
                {
                    // 顯示伺服器端的評估結果，看自己是牌分較大的人(先講話)，還是牌分較小的人(等待對手講完才可以講)
                    int smaller_or_bigger = instream.read();

                    long opponent_bets = 0; // 對手的賭金( follow 用 )
                    switch (smaller_or_bigger) {
                        case 0:
                            // 如果伺服器回傳 0，代表我的牌分比較小，需要等待對面做完動作，才換我動作
                            System.out.println("Your card is smaller than your opponent, please wait for his choice...");
                            // 伺服器會回傳對面的動作
                            answer = instream.readUTF();
                            System.out.println(answer); // 印出對面做的動作
                            String[] find_bet = answer.split(" ");

                            if(find_bet[4].equalsIgnoreCase("raise")) {//如果對手raise
                                opponent_bets = Integer.parseInt(find_bet[5]); // 收集對手的賭金
                                Client2_EnemyCard.enemy_bet -= opponent_bets;
                            }
                            // 讀取 client 的動作
                            if(find_bet[4].equalsIgnoreCase("drop!")) {//如果對手drop
                                answer = "drop";//自己也要跟著drop出去結算(分數不歸零)
                                break;
                            }
                            if(find_bet[4].equalsIgnoreCase("showhand!!")) {
                                opponent_bets = Math.min(Client2_OwnCard.my_bet, Client2_EnemyCard.enemy_bet);
                            }

                            User_input = inputReader.next();

                            outstream.writeUTF(User_input);

                            break;
                        case 1:
                            // 如果伺服器回傳 1，代表我的牌分比較大，可以先做動作
                            System.out.println("Your card is bigger than your opponent, do you want to raise or pass or drop or even showhand.(Please enter your decision)");
                            // 讀取 client 的動作
                            User_input = inputReader.next();
                            outstream.writeUTF(User_input);
                            break;
                        default:
                            System.out.println("伺服器傳送了非 0 或非 1 的值，是伺服器的錯");
                    }

                    if(answer.equalsIgnoreCase("drop")) {
                        break;
                    }

                    // 就 client 做的動作做出相應的處置
                    long bet = 0;
                    if (User_input.equalsIgnoreCase("raise")) { // 選擇了加注
                        // 詢問使用者要下注多少錢
                        System.out.println("How much would you like to raise? (Please enter your bet)");
                        bet = inputReader.nextLong(); // 讀取下注金額
                        Client2_OwnCard.my_bet -= bet; // 扣掉自己的錢
                        outstream.writeLong(bet); // 傳送下注金額給伺服器
                        // 傳送訊息叫他等待
                        System.out.println("Please wait for your opponent's choice...");
                        // 讀取伺服器回傳對手的動作
                        String follow_message = instream.readUTF();
                        System.out.println(follow_message);
                        if(follow_message.equalsIgnoreCase("Your opponent chose to follow you!")) Client2_EnemyCard.enemy_bet-=bet;
                        if(follow_message.equalsIgnoreCase("Your opponent chose to drop!")) break;
                    }
                    else if(User_input.equalsIgnoreCase("follow")) {
                        Client2_OwnCard.my_bet -= opponent_bets; // 扣掉自己的錢
                        outstream.writeLong(opponent_bets);
                    }
                    else if (User_input.equalsIgnoreCase("drop")) {
                        break;
                    }
                    else if (User_input.equalsIgnoreCase("showhand")) {
                        long available_bets = Math.min(Client2_OwnCard.my_bet, Client2_EnemyCard.enemy_bet);
                        outstream.writeLong(available_bets);
                        Client2_OwnCard.my_bet -= available_bets;

                        System.out.println("Please wait for your opponent's choice...");
                        // 讀取伺服器回傳對手的動作
                        String follow_message = instream.readUTF();
                        System.out.println(follow_message);
                        if(follow_message.equalsIgnoreCase("Your opponent chose to drop!")) break;
                    }
                    // 最後一圈迴圈只是用來下注，不需要收牌
                    if(card_count < 5){
                        card = instream.readUTF();
                        Client2_OwnCard.owncardlist.add(card); // 加進專門存放自己的牌組的 list
                        card_string = card_string.replaceFirst("000", Client2_OwnCard.owncardlist.get(card_count)); // 把拿到的卡加進 card_string，這樣才可以算分數
                        System.out.println("Your cards: " + card_string); // 顯示自己的牌

                        card = instream.readUTF();
                        Client2_EnemyCard.owncardlist.add(card); // 收對手的牌加進 list
                        System.out.println("get opponent's " + card_count + "th card " + card);

                        // 算牌分，回傳給 server
                        score = score_counting(card_string);
                        System.out.println("score is: " + score);
                        outstream.writeLong(score);
                    }
                }

                // 回傳自己整副牌的分數
                card_string = card_string.replaceFirst("000", Client2_OwnCard.owncardlist.get(0)); // 加入底牌
                System.out.println("Your cards: " + card_string);
                score = score_counting(card_string);
                System.out.println("Final score is: " + score);
                outstream.writeLong(score);

                // 等待伺服器回傳勝負結果
                int win_or_lose = instream.read();
                switch (win_or_lose){
                    case 0:
                        // 如果伺服器回傳 0，代表我輸了
                        System.out.println("You lose...");
                        System.out.println("You only left: " + Client2_OwnCard.my_bet + " dollar.");
                        break;
                    case 1:
                        // 如果伺服器回傳 1，代表我贏了
                        System.out.println("You are WINNER!!");
                        // 接收伺服器回傳的檯面上的金額
                        long money = instream.readLong();
                        System.out.println("原本剩餘金額: " + Client2_OwnCard.my_bet);
                        System.out.println("收到金額: " + money);
                        Client2_OwnCard.my_bet += money;
                        System.out.println("Now you have " + Client2_OwnCard.my_bet + " dollar.");
                        break;
                    default:
                        System.out.println("伺服器傳送了非 0 或非 1 的值，是伺服器的錯");
                }
                Client2_EnemyCard.enemy_bet = 0;
            }
            catch(IOException ex){
                ex.printStackTrace();
            }
        }
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
    public static long score_counting(String cards)  // 判定各個牌型並計算牌分，輸入字串(ex."C03,C12,C07,000,000")傳回一個long型態的牌分
    {
        cards = sort_card(cards); // 排序手牌，傳回如輸入格式的字串
        String[] card = cards.split(",");
        long card_score = 0;
        int i;
        int card_type = 0;


        boolean straight_flush = false; // 同花順
        boolean flush = true; // 同花
        boolean straight = true; // 順子
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
        if(straight) {  // 確定為順子時
            card_score += Math.pow(10,5)*num_sort[4];
            for(i = 0;i<5;i++) {
                if(point(card[i]) == num_sort[4]) {
                    card_score += color_point(card[i]);
                }
            }
        }
        if (flush) { // 確定為同花時
            card_score += Math.pow(10, 6)*num_sort[4];
            card_score+=color_point(card[0]);
        }
        if(straight && flush) {  // 如果為同花順
            straight_flush = true;
            flush = false;
            card_score += Math.pow(10, 9)*point(card[4]);
            card_score += color_point(card[4]);
        }



        boolean four_of_a_kind = false; // 鐵支
        boolean full_house = false;// 葫蘆
        boolean three_of_a_kind = false;// 三條
        int pair_count = 0; // 計算坯的數量，1為胚，2為兔胚
        List count_repeat = new ArrayList();
        for(i=0;i<5;i++) {
            count_repeat.add(point(card[i]));
        }
        for(i = 2;i<=14;i++) // 將數字頻率記錄成list
        {
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
                    // System.out.println(i);
                    if(pair_count == 2) {
                        card_score -= i*100;
                        long tmp = card_score%1000/100;
                        card_score += Math.max(tmp, i)*100;
                        card_score -= tmp*100;
                        // card_score += 2000;
                    }
                    break;
            }
        }
        if(three_of_a_kind && pair_count == 1) {
            full_house = true;
            three_of_a_kind = false;
            pair_count -= 1;
            card_score += Math.pow(10, 7)*(card_score/10000);
        }

        if(pair_count >= 0) {
            for(i = 0;i < 5;i++) {
                // System.out.println(color_point(card[i]) + "" + point(card[i]));
                if(point(card[i]) == card_score/100 && color_point(card[i]) == 4) {
                    card_score += 4;
                }
            }
        }

        if(card_score == 0) { // 如果不符合上述牌型，即為散牌
            int tmp_max = 0,tmp_max_pos = 0;
            for(i = 0;i < 5;i++) {
                if(point(card[i]) > tmp_max) {
                    tmp_max = point(card[i]);
                    tmp_max_pos = i;
                }
            }
            card_score += tmp_max * 5 + color_point(card[tmp_max_pos]);
        }


        return card_score;

    }
}


