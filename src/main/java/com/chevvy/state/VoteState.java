package com.chevvy.state;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class VoteState {

    public enum VoteType { DAY, NIGHT, CLEAR, RAIN }
    public record ActiveVote(VoteType type, UUID starter, Set<UUID> votes, long startTime, String descriptionJp, String descriptionEn) {}

    private static ActiveVote currentVote = null;

    public static boolean isVoteActive() {
        return currentVote != null;
    }

    public static ActiveVote getCurrentVote() {
        return currentVote;
    }

    public static void startVote(VoteType type, UUID starter, String descriptionJp, String descriptionEn) {
        if (!isVoteActive()) {
            currentVote = new ActiveVote(type, starter, new HashSet<>(), System.currentTimeMillis(), descriptionJp, descriptionEn);
            // The person who starts the vote automatically votes yes.
            addVote(starter);
        }
    }

    public static void addVote(UUID playerUuid) {
        if (isVoteActive()) {
            currentVote.votes.add(playerUuid);
        }
    }

    public static void endVote() {
        currentVote = null;
    }
}