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

}
