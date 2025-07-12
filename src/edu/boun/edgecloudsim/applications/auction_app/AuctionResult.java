package edu.boun.edgecloudsim.applications.auction_app;

public class AuctionResult {
    private final int winnerId;
    private final double payment;

    public AuctionResult(int winnerId, double payment) {
    	this.winnerId = winnerId;
    	this.payment = payment;
    }
    public int getWinnerId() { return winnerId; }
    public double getPayment() { return payment; }
}

