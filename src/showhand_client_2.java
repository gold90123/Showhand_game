import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.*;
import java.util.List;

class Client2_OwnCard{
    static List<String> owncardlist = new LinkedList<String>();
}

public class showhand_client_2 extends Frame implements Runnable {
    Socket socket;
    static String servername;
    static int port;
    DataOutputStream  outstream;
    DataInputStream  instream;
    static int PLAY_NUM = 2;
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
                // 接收問題，並回傳 id
                String question = instream.readUTF();
                System.out.println(question);
                Scanner inputReader = new Scanner(System.in);
                String User_input = inputReader.next();
                outstream.writeUTF(User_input);

                // 接收第一張(底牌 hole-card)
                String card = instream.readUTF();
                Client2_OwnCard.owncardlist.add(card); // 加進專門存放自己的牌組的 list
                // 接收第二張(第一張明牌)
                card = instream.readUTF();
                Client2_OwnCard.owncardlist.add(card); //加進專門存放自己的牌組的 list

                // 接收對手的第一張明牌
                card = instream.readUTF();

                // 顯示自己以及對手的牌
                System.out.println("Your hole-card is: " + Client2_OwnCard.owncardlist.get(0));
                System.out.println("Your card 1 is: " + Client2_OwnCard.owncardlist.get(1));
                System.out.println("Your opponent's card 1 is: " + card);

                // 算牌分，回傳給 server
                card = Client2_OwnCard.owncardlist.get(0) + "," + Client2_OwnCard.owncardlist.get(1) + ",000,000,000";
                System.out.println(card);
                long score = score_counting(card);
                System.out.println("real score is: " + score);
                outstream.writeLong(score);

                // 顯示伺服器的評估結果
                String answer = instream.readUTF();
                System.out.println(answer);
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
        if(three_of_a_kind && pair_count == 1) {
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


