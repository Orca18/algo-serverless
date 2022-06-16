package algoserverless.model;

public class RequestModel {

    String address;
    Long amount;
    String senderadd;

    public RequestModel() {
    }

    public RequestModel(String address, Long amount, String senderadd) {
        this.address = address;
        this.amount = amount;
        this.senderadd = senderadd;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public String getSenderadd() {
        return senderadd;
    }

    public void setSenderadd(String senderadd) {
        this.senderadd = senderadd;
    }
}
