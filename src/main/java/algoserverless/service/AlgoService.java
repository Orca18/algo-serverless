package algoserverless.service;

import com.algorand.algosdk.account.Account;
import com.algorand.algosdk.crypto.Address;
import com.algorand.algosdk.transaction.SignedTransaction;
import com.algorand.algosdk.util.Encoder;
import com.algorand.algosdk.v2.client.common.AlgodClient;
import com.algorand.algosdk.v2.client.common.IndexerClient;
import com.algorand.algosdk.v2.client.common.Response;
import com.algorand.algosdk.v2.client.model.NodeStatusResponse;
import com.algorand.algosdk.v2.client.model.PendingTransactionResponse;
import com.algorand.algosdk.v2.client.model.PostTransactionsResponse;
import com.algorand.algosdk.v2.client.model.TransactionParametersResponse;
import com.amazonaws.services.lambda.runtime.LambdaLogger;

import java.math.BigDecimal;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

// AlgoService 클래스를 사용하면 알고랜드 블록체인과 상호 작용할 수 있습니다.

// 우선, 2개의 다른 생성자를 가지고 있다.
// 두번째 암호에는 추가 매개변수인 계정 암호 구문이 있으며,
// 트랜잭션에 서명하는 경우에 사용해야합니다.
// 클래스는 3개의 메서드를 가집니다
// 1. getAccountAmount
// 2. sendAlgo
// 3. waitForConfirmation
public class AlgoService {

    private AlgodClient algodClient;
    private IndexerClient indexerClient;

    private Account algoAccount;
    private Address algoAddress;
    private Account sender_algoAccount;
    private Address sender_algoAddress;

    public AlgoService(String algodApiAddr, Integer algodPort, String algodApiToken,
                       String indexerApiAddr, Integer indexerApiPort) {
        algodClient = new AlgodClient(algodApiAddr, algodPort, algodApiToken);
        indexerClient = new IndexerClient(indexerApiAddr, indexerApiPort);
    }

    public AlgoService(String algodApiAddr, Integer algodPort, String algodApiToken, String indexerApiAddr,
                       Integer indexerApiPort, String accPassphrase) {
        algodClient = new AlgodClient(algodApiAddr, algodPort, algodApiToken);
        indexerClient = new IndexerClient(indexerApiAddr, indexerApiPort);

        try {
            // 니모닉 25단어로 계정 알아냄
            algoAccount = new Account(accPassphrase);
            algoAddress = algoAccount.getAddress();
        }
        catch (GeneralSecurityException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * 1. getAccountAmount :
     * get the Algos owned by address
     * @param address
     * @return
     */

    public Optional<Long> getAccountAmount(String address) {

        Response<com.algorand.algosdk.v2.client.model.Account> accountResponse;
        try {
            /** 이 메서드는 주소 문자열을 입력으로 수신하고,
             * Algorand Java SDK 함수 AccountInformation을 호출하여 지갑에 저장된 Algo의 총량을 검색합니다. */
            Address destAddress = new Address(address);
            accountResponse = algodClient.AccountInformation(destAddress).execute();
        }
        catch (Exception e) {
            return Optional.empty();
        }

        if (accountResponse.isSuccessful()) {
            return Optional.of(accountResponse.body().amount);
        }
        else {
            return Optional.empty();
        }
    }


    /**
     * 2. sendAlgo : 이 함수는 전송 대상 주소와 microAlgo 양을 수신합니다.
     * Send amount (il Algo) to receiverAddress.
     * @param receiverAddress
     * @param amount
     * @return
     * @throws Exception
     */
    public String sendAlgo(String receiverAddress, Long amount, String senderAddress) throws Exception {
        sender_algoAccount = new Account(senderAddress);
        sender_algoAddress = sender_algoAccount.getAddress();
        // 수신 주소, 전송 수량
        /** 거래를 준비한 다음 개인키로 서명하고 (with SignTrasaction call) */
        // 주소 준비
        Address address = new Address(receiverAddress);
        // note : 서버리스 테스트
        String note = "AlgoServerless Test";
        // 트랜잭션 파라미터 응답 = algodApiAddr, algodPort, algodApiToken
        TransactionParametersResponse params = algodClient.TransactionParams().execute().body();
        com.algorand.algosdk.transaction.Transaction tx =
                com.algorand.algosdk.transaction.Transaction.PaymentTransactionBuilder()
                        .sender(sender_algoAddress)// 보내는 사람의 주소 (니모닉 25자로 구한거)
                        .note(note.getBytes())// AlgoServerless Test 바이트화
                        .amount(amount)// 보내는 수량
                        .receiver(address)// 받는 사람 주소
                        .suggestedParams(params)//algodClient(algodApiAddr, algodPort, algodApiToken) 몰루?
                        .build();

        // 트랜잭션 서명 → 인코딩
        SignedTransaction signedTx = algoAccount.signTransaction(tx);
        byte[] encodedSignedTx = Encoder.encodeToMsgPack(signedTx);

        /** 마지막으로 블록체인으로 전송합니다. (RawTransaction 파트) */
        Response<PostTransactionsResponse> txResponse = algodClient.RawTransaction().rawtxn(encodedSignedTx).execute();
        
        // 성공 시, 대기
        if (txResponse.isSuccessful()) {
            String txId = txResponse.body().txId;
            // 대기
            waitForConfirmation(txId, 6);
            return txId;
        } else {
            throw new Exception("Transaction Error");
        }
    }

    /** 3. waitForConfirmation
     * 거래 제출 후, 블록체인에 등록될 때까지 기다린다. */
    public void waitForConfirmation(String txId, int timeout) throws Exception {

        Long txConfirmedRound = -1L;
        Response<NodeStatusResponse> statusResponse = algodClient.GetStatus().execute();

        long lastRound;
        if (statusResponse.isSuccessful()) {
            lastRound = statusResponse.body().lastRound + 1L;
        }
        else {
            throw new IllegalStateException("Cannot get node status");
        }

        long maxRound = lastRound + timeout;

        for (long currentRound = lastRound; currentRound < maxRound; currentRound++) {
            Response<PendingTransactionResponse> response = algodClient.PendingTransactionInformation(txId).execute();

            if (response.isSuccessful()) {
                txConfirmedRound = response.body().confirmedRound;
                if (txConfirmedRound == null) {
                    if (!algodClient.WaitForBlock(currentRound).execute().isSuccessful()) {
                        throw new Exception();
                    }
                }
                else {
                    return;
                }
            } else {
                throw new IllegalStateException("The transaction has been rejected");
            }
        }

        throw new IllegalStateException("Transaction not confirmed after %1" + timeout + " rounds!");
    }


}
