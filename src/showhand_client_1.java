import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.*;
import java.util.List;

class Client1_OwnCard{
    static List<String> owncardlist = new LinkedList<String>();
}

public class showhand_client_1 extends Frame implements Runnable {
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
        showhand_client_1 ClientStart = new showhand_client_1();
    }

    public showhand_client_1()
    {
        super("showhand_client_1");
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
                Client1_OwnCard.owncardlist.add(card); // 加進專門存放自己的牌組的 list
                // 接收第二張(第一張明牌)
                card = instream.readUTF();
                Client1_OwnCard.owncardlist.add(card); //加進專門存放自己的牌組的 list

                // 接收對手的第一張明牌
                card = instream.readUTF();

                // 顯示自己以及對手的牌
                System.out.println("Your hole-card is: " + Client1_OwnCard.owncardlist.get(0));
                System.out.println("Your card 1 is: " + Client1_OwnCard.owncardlist.get(1));
                System.out.println("Your opponent's card 1 is: " + card);

                // 算牌分，回傳給 server
                long score = 9876;
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
}
