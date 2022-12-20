import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.*;
import java.util.List;

public class showhand_client_1 extends Frame implements Runnable {
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
        showhand_client_1 ClientStart = new showhand_client_1();
    }

    public showhand_client_1()
    {
        super("showhand_client");
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
        try{
            // 接收問題，並回傳 id
            String question = instream.readUTF();
            System.out.println(question);
            Scanner inputReader = new Scanner(System.in);
            String User_input = inputReader.next();
            outstream.writeUTF(User_input);
            // 接收牌組
            List<String> cards = new LinkedList<String>();
            // 接收第一張(底牌 hole-card)
            String card = instream.readUTF();
            cards.add(card); // 加進 list
            // 接收第二張(第一張明牌)
            card = instream.readUTF();
            cards.add(card); // 加進 list
            System.out.println("Your hole-card is: " + cards.get(0));
            System.out.println("Your card 1 is: " + cards.get(1));
        }
        catch(IOException ex){
            ex.printStackTrace();
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
    public static void score_counting(String cards)
    {
        cards = sort_card(cards);
        String[] card = cards.split(",");
        int card_score = 0;
        int i;
        int card_type = 0;


        boolean straight_flush = false; //同花順
        boolean flush = true;
        boolean straight = true;
        for(i=0;i<4;i++) {
            if(color(card[i]) != color(card[i+1])) {
                flush = false;
            }
            if(point(card[i]) != point(card[i+1])-1) {
                straight = false;
            }
        }
        if(straight)card_type = 4;
        if (flush)card_type = 5;
        if(straight && flush) {
            straight_flush = true;
            flush = false;
            straight = false;
            card_type = 8;
        }




        boolean four_of_a_kind = false;
        boolean full_house = false;
        boolean three_of_a_kind = false;
        int pair_count = 0;
        List count_repeat = new ArrayList();
        for(i=0;i<5;i++) {
            count_repeat.add(point(card[i]));
        }
        for(i = 2;i<=14;i++) {
            if(Collections.frequency(count_repeat, i) == 4) {
                four_of_a_kind = true;
                card_type = 7;
                break;
            }
            if(Collections.frequency(count_repeat, i) == 3) {
                three_of_a_kind = true;
                card_type = 3;
            }
            if(Collections.frequency(count_repeat, i) == 2) {
                pair_count += 1;
                if(pair_count == 2) {
                    card_type = 2;
                }
                else card_type = 1;
            }
        }
        if(three_of_a_kind == true && pair_count == 1) {
            full_house = true;
            three_of_a_kind = false;
            pair_count -= 1;
            card_type = 6;
        }



    }

}
