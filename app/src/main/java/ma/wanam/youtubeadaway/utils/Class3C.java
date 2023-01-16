package ma.wanam.youtubeadaway.utils;

import java.util.Iterator;

public class Class3C implements Iterator<String> {
    private char[] data = "abcdefghijklmnopqrstuvwxyz".toCharArray();
    private int[] counter = {0, 0, 0};

    @Override
    public boolean hasNext() {
        return counter[0] < data.length && counter[1] < data.length && counter[2] < data.length;
    }

    @Override
    public String next() {
        StringBuilder sb = new StringBuilder();
        sb.append(data[counter[0]]);
        sb.append(data[counter[1]]);
        sb.append(data[counter[2]]);
        counter[2]++;
        for (int i = 2; i >= 0; i--) {
            if (i > 0 && counter[i] == data.length) {
                counter[i] = 0;
                counter[i - 1]++;
            } else {
                break;
            }
        }
        return sb.toString();
    }

}
