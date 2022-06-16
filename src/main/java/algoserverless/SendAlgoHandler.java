package algoserverless;

import algoserverless.model.RequestModel;
import algoserverless.model.ResponseModel;
import algoserverless.service.AlgoService;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;

// 이번에는 RequestHandler<RequestModel, ResponseModel> 인터페이스를 구현
// RequestModel(요청) & ResponseModel(응답)
// 2가지는 심플한 POJO (Plain Old Java Class)
// *POJO = 오래된 방식의 간단한 자바 오브젝트 ⇒ 특정 기술에 종속되어 동작되는 것이 아닌 순수한 자바 객체

// Java 런타임은 이벤트를 RequestModel 개체로 자동 역직렬화하고
// 응답을 ResponseModel 개체로 직렬화합니다.

// 또한 계정 암호 구문 (ACC_PASSPRASE) 이라는 매개변수를 사용하여 다른 생성자를 호출
// 우리는 일고를 지갑에서 다른 주소로 보내고 싶고 거래소에 서명해야 하기 때문이다.

// SendAlgoHandler 클래스의 HandelRequest 메서드의 코드를 분석해보자.
public class SendAlgoHandler implements RequestHandler<RequestModel, ResponseModel> {

    // 알고랜드 로직은 AlgoService 뒤에 숨어 있기 때문에 간단하다.
    @Override
    public ResponseModel handleRequest(final RequestModel request, final Context context) {

        // json 형식으로 보낼 것.. RequestModel 보면 "주소, 보낼 양" 이다.
        AlgoService algoService = new AlgoService(
                "https://node.testnet.algoexplorerapi.io/",
                443,
                null,
                "https://algoindexer.testnet.algoexplorerapi.io/idx2",
                443,
                request.getSenderadd()
        );

        // 로거
//        LambdaLogger logger = context.getLogger();
        try {
            // sendAlgo() 메소드는 단순히 '수신 주소'와 보낼 'MicroAlgos의 양'을 전달 받음 + 커스텀으로 보내는 사람 주소 '송신 주소 니모닉'
            // 이 메서드는 우리의 전송이 포함된 알고랜드 트랜잭션의 id 를 반환합니다.
            String txId = algoService.sendAlgo(request.getAddress(), request.getAmount(), request.getSenderadd());

//            logger.log("Transaction executed");
            // 그런 다음 이 트랜잭션 ID와 메시지를 사용하여
            // ResponseModel 개체를 생성하고 이를 반환문에 사용할 수 있습다.
            return new ResponseModel(txId, "Transaction executed / 트랜잭션 실행");
        } catch (Exception e) {
            // 오류가 발생하면 TxId가 비어 있고
            // 오류 메시지가 있는 ResponseModel 개체가 반환됩니다.
//            logger.log("Transaction Failed");
            return new ResponseModel("", "Transaction Failed / 트랜잭션 실패 왜 ㅡㅡ");
        }
    }
}
