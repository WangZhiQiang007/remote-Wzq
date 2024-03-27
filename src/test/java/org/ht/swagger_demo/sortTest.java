package org.ht.swagger_demo;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

public class sortTest {
    @Test
    void test(){
        List<Object> objects = new ArrayList<>();
        objects.add("scs");
        objects.add(234);
        objects.add(false);
        objects.add(3.01);
        for (Object o :objects){
            System.out.println(o);
        }
    }

}
