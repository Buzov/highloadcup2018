package com.dgusev.hlcup2018.accountsapp.predicate;

import com.dgusev.hlcup2018.accountsapp.model.AccountDTO;

import java.util.List;
import java.util.function.Predicate;

public class InterestsAnyPredicate implements Predicate<AccountDTO> {

    private List<String> interests;

    public InterestsAnyPredicate(List<String> interests) {
        this.interests = interests;
    }

    @Override
    public boolean test(AccountDTO accountDTO) {
        if (accountDTO.interests != null && !accountDTO.interests.isEmpty()) {
            for (String interes: interests) {
                if (accountDTO.interests.contains(interes)) {
                    return true;
                }
            }
            return false;
        } else {
            return false;
        }
    }
}