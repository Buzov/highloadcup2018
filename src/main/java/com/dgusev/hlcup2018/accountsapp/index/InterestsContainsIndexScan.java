package com.dgusev.hlcup2018.accountsapp.index;

import java.util.ArrayList;
import java.util.List;

public class InterestsContainsIndexScan extends AbstractIndexScan {

    private int[][] indexList;
    private int[] indexes;
    private int[] state;
    private boolean alwaysFalse;

    public InterestsContainsIndexScan(IndexHolder indexHolder, String[] interests) {
        super(indexHolder);
        int count = 0;
        for (String interes: interests) {
            if (indexHolder.interestsIndex.containsKey(interes)) {
                count++;
            }
        }
        indexList = new int[count][];
        index = 0;
        for (String interes: interests) {
            if (indexHolder.interestsIndex.containsKey(interes)) {
                indexList[index++] = indexHolder.interestsIndex.get(interes);
            } else {
                alwaysFalse = true;
            }
        }
        indexes = new int[count];
        state = new int[count];
        for (int i = 0; i < count; i++) {
            if (indexList[i].length != 0) {
                state[i] = indexList[i][indexes[i]++];
            } else {
                state[i] = -1;
            }
        }
    }

    @Override
    public int getNext() {
        if (alwaysFalse) {
            return -1;
        }
        if (indexList != null && indexList.length != 0) {
            while (true) {
                boolean equals = true;
                int min = Integer.MAX_VALUE;
                int prev = -1;
                for (int i = 0; i< state.length; i++) {
                    if (state[i] < min) {
                        min = state[i];
                    }
                    if (prev != -1) {
                        if (state[i] != prev) {
                            equals = false;
                        }
                    }
                    prev = state[i];
                }

                if (equals) {
                    int result = state[0];
                    for (int i = 0; i < indexList.length; i++) {
                        if (indexes[i] < indexList[i].length) {
                            state[i] = indexList[i][indexes[i]++];
                        } else {
                            state[i] = -1;
                        }
                    }
                    return result;
                }

                for (int i = 0; i < indexList.length; i++) {
                    if (state[i] != min) {
                        if (indexes[i] < indexList[i].length) {
                            state[i] = indexList[i][indexes[i]++];
                        } else {
                            state[i] = -1;
                        }
                        if (state[i] == -1) {
                            return -1;
                        }
                    }
                }
            }
        } else {
            return -1;
        }
    }

}
