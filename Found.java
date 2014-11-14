
import java.util.ArrayList;
import java.util.List;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author Joshua
 */
public class Found {

    private String name = "";
    private List<String> list = new ArrayList<String>();

    public String getName() {
        return name;
    }

    public void setName(String s) {
        name = s;
    }
    
    public boolean isEmpty()
    {
        return list.isEmpty();
    }

    public void printIndex() {
        if (!list.isEmpty()) {
            for (String element : list) {
                System.out.println(element);
            }
        }
        else
        {
            System.out.println("THIS INDEX IS EMPTY!!");
        }
    }

    public void addToIndex(String s, int p) {
        list.add("Line: " + Integer.toString(p) + " " + s);
    }

}
