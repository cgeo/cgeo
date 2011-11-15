package cgeo.geocaching.gcvote;

public final class GCVoteRating {
    private final float rating;
    private final int votes;
    private final Float myVote;

    public GCVoteRating(float rating, int votes, Float myVote) {
        this.rating = rating;
        this.votes = votes;
        this.myVote = myVote;
    }

    public float getRating() {
        return rating;
    }

    public int getVotes() {
        return votes;
    }

    public Float getMyVote() {
        return myVote;
    }
}
