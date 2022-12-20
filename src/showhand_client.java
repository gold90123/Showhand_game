import java.util.*;
import java.util.Arrays;
import java.util.List;
import java.util.Collections;


public class showhand_client {
    public static void main(String[] args)
    {

        //score_counting("S14,S13,S12,S11,S10");
        score_counting("S04,H04,D04,C06,H06");
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


        boolean straight_flush = true; //同花順
        for(i=0;i<4;i++) {
            if(color(card[i]) != color(card[i+1]) || point(card[i]) != point(card[i+1])-1) {
                straight_flush = false;
            }
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
                break;
            }
            if(Collections.frequency(count_repeat, i) == 3) {
                three_of_a_kind = true;
            }
            if(Collections.frequency(count_repeat, i) == 2) {
                pair_count += 1;
            }
        }
        if(three_of_a_kind && pair_count == 1) {
            full_house = true;
            three_of_a_kind = false;
            pair_count -= 1;
        }







        System.out.println("鐵支 = " + four_of_a_kind);
        System.out.println("葫蘆 = " + full_house);
        System.out.println("三條 = " + three_of_a_kind);
        System.out.println("胚數 = " + pair_count);
    }

}
